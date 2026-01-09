package com.example.russianroulette.game;

import com.example.russianroulette.RussianRoulettePlugin;
import com.example.russianroulette.config.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a single Russian Roulette game session.
 */
public class Game {

    private final RussianRoulettePlugin plugin;
    private final ConfigManager config;
    private final UUID gameId;

    private GameState state;
    private GameMode mode;
    private Revolver revolver;

    private final Map<UUID, PlayerData> players;
    private final List<UUID> turnOrder;
    private int currentTurnIndex;
    private UUID currentTurnPlayer;

    private BukkitTask countdownTask;
    private BukkitTask turnTimerTask;
    private int turnTimeRemaining;

    private final boolean reshuffleAfterShot;

    // Chair entities for seating players
    private final Map<UUID, ArmorStand> seatEntities;

    // Cinematic intro task
    private BukkitTask cinematicTask;

    public Game(RussianRoulettePlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        this.gameId = UUID.randomUUID();
        this.state = GameState.WAITING;
        this.mode = config.getGameMode();
        this.players = new ConcurrentHashMap<>();
        this.turnOrder = new ArrayList<>();
        this.currentTurnIndex = 0;
        this.reshuffleAfterShot = config.isReshuffleAfterShot();
        this.seatEntities = new HashMap<>();
    }

    /**
     * Add a player to the game.
     * 
     * @param player Player to add
     * @return true if player was added successfully
     */
    public boolean addPlayer(Player player) {
        if (state != GameState.WAITING) {
            return false;
        }

        if (players.containsKey(player.getUniqueId())) {
            return false;
        }

        if (players.size() >= config.getMaxPlayers()) {
            return false;
        }

        // Take bet before adding player
        if (!plugin.getRewardManager().takeBet(player, this)) {
            return false; // Bet failed (insufficient funds/items)
        }

        PlayerData data = new PlayerData(player);
        data.setTurnPosition(players.size());
        players.put(player.getUniqueId(), data);
        turnOrder.add(player.getUniqueId());

        // Broadcast join message
        broadcastMessage(config.getMessage("playerJoined")
                .replace("%player%", player.getName())
                .replace("%players%", String.valueOf(players.size()))
                .replace("%max%", String.valueOf(config.getMaxPlayers())));

        return true;
    }

    /**
     * Remove a player from the game.
     * 
     * @param player Player to remove
     * @param forced Whether removal is forced (disconnect, etc.)
     * @return true if player was removed
     */
    public boolean removePlayer(Player player, boolean forced) {
        UUID playerId = player.getUniqueId();

        if (!players.containsKey(playerId)) {
            return false;
        }

        // Check if it's their turn and not forced
        if (state == GameState.IN_PROGRESS &&
                currentTurnPlayer != null &&
                currentTurnPlayer.equals(playerId) &&
                !forced) {
            player.sendMessage(config.getMessage("cannotLeaveDuringTurn"));
            return false;
        }

        PlayerData data = players.remove(playerId);
        turnOrder.remove(playerId);

        // Return player's inventory to normal
        player.getInventory().remove(getRevolverItem());

        // Teleport back if needed
        if (config.isTeleportToArena() && data != null) {
            player.teleport(data.getOriginalLocation());
        }

        // Remove scoreboard
        plugin.getScoreboardManager().removeScoreboard(player);

        if (forced) {
            broadcastMessage(config.getMessage("disconnectedDeath")
                    .replace("%player%", player.getName()));
        } else {
            broadcastMessage(config.getMessage("playerLeft")
                    .replace("%player%", player.getName())
                    .replace("%players%", String.valueOf(players.size()))
                    .replace("%max%", String.valueOf(config.getMaxPlayers())));
        }

        // Check if game should continue
        if (state == GameState.IN_PROGRESS) {
            checkForWinner();

            // If current turn player left, move to next
            if (currentTurnPlayer != null && currentTurnPlayer.equals(playerId)) {
                nextTurn();
            }
        }

        return true;
    }

    /**
     * Start the game countdown.
     * 
     * @return true if game started successfully
     */
    public boolean start() {
        if (state != GameState.WAITING) {
            return false;
        }

        if (players.size() < config.getMinPlayers()) {
            return false;
        }

        state = GameState.STARTING;

        // Initialize revolver
        revolver = new Revolver(mode.getBulletCount());

        // Teleport players if configured
        if (config.isTeleportToArena()) {
            teleportPlayersToArena();
        }

        // Give revolver items to players
        giveRevolverItems();

        // Start countdown
        final int countdown = config.getStartCountdown();
        countdownTask = new BukkitRunnable() {
            int timeLeft = countdown;

            @Override
            public void run() {
                if (timeLeft <= 0) {
                    beginGame();
                    cancel();
                    return;
                }

                broadcastMessage(config.getMessage("gameStarting")
                        .replace("%time%", String.valueOf(timeLeft)));

                // Play countdown sound
                playSound("gameStart");

                timeLeft--;
            }
        }.runTaskTimer(plugin, 0L, 20L);

        return true;
    }

    /**
     * Begin the actual game after countdown.
     */
    private void beginGame() {
        state = GameState.IN_PROGRESS;

        broadcastMessage(config.getMessage("gameStarted"));
        playSound("gameStart");

        // Shuffle turn order
        Collections.shuffle(turnOrder);
        currentTurnIndex = 0;

        // Setup scoreboards
        updateScoreboards();

        // Play cinematic intro, then start first turn
        playCinematicIntro();
    }

    /**
     * Play cinematic camera intro showing all players around the table.
     */
    private void playCinematicIntro() {
        if (!config.isTeleportToArena() || players.size() < 2) {
            // Skip cinematic if not in arena or too few players
            startTurn();
            return;
        }

        World world = Bukkit.getWorld(config.getArenaWorld());
        if (world == null) {
            startTurn();
            return;
        }

        double centerX = config.getArenaCenterX();
        double centerY = config.getArenaCenterY();
        double centerZ = config.getArenaCenterZ();
        double radius = config.getCircleRadius() + 2; // Camera slightly further out

        // Store original game modes and set to spectator for cinematic
        Map<UUID, org.bukkit.GameMode> originalModes = new HashMap<>();
        List<UUID> playerList = new ArrayList<>(players.keySet());

        for (UUID playerId : playerList) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                originalModes.put(playerId, player.getGameMode());
                player.setGameMode(org.bukkit.GameMode.SPECTATOR);
            }
        }

        // Cinematic camera rotation
        final int[] step = { 0 };
        final int totalSteps = playerList.size() * 20 + 40; // 1 second per player + 2 sec intro/outro
        final int stepsPerPlayer = 20;

        cinematicTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (step[0] >= totalSteps || state != GameState.IN_PROGRESS) {
                    // End cinematic
                    cancel();
                    endCinematic(originalModes);
                    return;
                }

                // Calculate camera position orbiting around center
                double angle = (2 * Math.PI * step[0]) / totalSteps;
                double camX = centerX + radius * Math.cos(angle);
                double camZ = centerZ + radius * Math.sin(angle);
                double camY = centerY + 2; // Slightly above table level

                Location camLoc = new Location(world, camX, camY, camZ);
                // Face the center of the table
                camLoc.setYaw((float) Math.toDegrees(Math.atan2(centerZ - camZ, centerX - camX)) - 90);
                camLoc.setPitch(15); // Look slightly down at table

                // Move all players' cameras
                for (UUID playerId : playerList) {
                    Player player = Bukkit.getPlayer(playerId);
                    if (player != null && player.isOnline()) {
                        player.teleport(camLoc);
                    }
                }

                // Show player introductions
                int playerIndex = (step[0] - 20) / stepsPerPlayer;
                int stepInPlayer = (step[0] - 20) % stepsPerPlayer;

                if (playerIndex >= 0 && playerIndex < playerList.size() && stepInPlayer == 0) {
                    Player featured = Bukkit.getPlayer(playerList.get(playerIndex));
                    if (featured != null) {
                        broadcastMessage(config.getMessage("playerIntro")
                                .replace("%player%", featured.getName())
                                .replace("%number%", String.valueOf(playerIndex + 1)));
                    }
                }

                step[0]++;
            }
        }.runTaskTimer(plugin, 20L, 1L); // Start after 1 second, run every tick
    }

    /**
     * End cinematic and restore players to their seats.
     */
    private void endCinematic(Map<UUID, org.bukkit.GameMode> originalModes) {
        // Restore game modes and teleport back to seats
        for (UUID playerId : players.keySet()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                // Restore game mode
                org.bukkit.GameMode original = originalModes.getOrDefault(playerId, org.bukkit.GameMode.SURVIVAL);
                player.setGameMode(original);
            }
        }

        // Re-teleport and re-seat players
        teleportPlayersToArena();

        // Now start the actual game
        broadcastMessage(config.getMessage("cinematicEnd"));
        startTurn();
    }

    /**
     * Start the current player's turn.
     */
    private void startTurn() {
        if (turnOrder.isEmpty()) {
            endGame();
            return;
        }

        currentTurnPlayer = turnOrder.get(currentTurnIndex);
        Player player = Bukkit.getPlayer(currentTurnPlayer);

        if (player == null || !player.isOnline()) {
            // Player disconnected, eliminate and move on
            eliminatePlayer(currentTurnPlayer, true);
            return;
        }

        // Notify current player
        player.sendMessage(config.getMessage("yourTurn"));

        // Show title
        player.sendTitle(
                config.getRawMessage("yourTurnTitle"),
                config.getRawMessage("yourTurnTitleSubtitle"),
                10, 40, 10);

        // Play turn start sound
        playSound("turnStart", player);

        // Tell player to right-click the gun (no auto GUI)
        player.sendMessage(config.getMessage("rightClickToShoot"));

        // Broadcast to others
        for (UUID otherId : players.keySet()) {
            if (!otherId.equals(currentTurnPlayer)) {
                Player other = Bukkit.getPlayer(otherId);
                if (other != null) {
                    other.sendMessage(config.getMessage("playerTurn")
                            .replace("%turn%", player.getName()));
                }
            }
        }

        // Update scoreboards
        updateScoreboards();

        // Start turn timer
        turnTimeRemaining = config.getTurnTime();
        startTurnTimer();
    }

    /**
     * Start the turn timer task.
     */
    private void startTurnTimer() {
        if (turnTimerTask != null) {
            turnTimerTask.cancel();
        }

        turnTimerTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (state != GameState.IN_PROGRESS || currentTurnPlayer == null) {
                    cancel();
                    return;
                }

                turnTimeRemaining--;

                // Update scoreboards with time
                updateScoreboards();

                // Play tick sound in last 5 seconds
                if (turnTimeRemaining <= 5 && turnTimeRemaining > 0) {
                    Player player = Bukkit.getPlayer(currentTurnPlayer);
                    if (player != null) {
                        playSound("timerTick", player);
                        player.sendMessage(config.getMessage("turnTimer")
                                .replace("%time%", String.valueOf(turnTimeRemaining)));
                    }
                }

                // Time's up - auto pull trigger
                if (turnTimeRemaining <= 0) {
                    cancel();
                    Player player = Bukkit.getPlayer(currentTurnPlayer);
                    if (player != null) {
                        broadcastMessage(config.getMessage("autoTriggerPull")
                                .replace("%player%", player.getName()));
                        pullTrigger(player);
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    /**
     * Handle trigger pull by a player.
     * 
     * @param player The player pulling the trigger
     */
    public void pullTrigger(Player player) {
        if (state != GameState.IN_PROGRESS) {
            return;
        }

        if (!player.getUniqueId().equals(currentTurnPlayer)) {
            player.sendMessage(config.getMessage("notYourTurn"));
            return;
        }

        // Cancel turn timer
        if (turnTimerTask != null) {
            turnTimerTask.cancel();
            turnTimerTask = null;
        }

        // Close GUI
        player.closeInventory();

        // Pull trigger on revolver
        boolean hit = revolver.pullTrigger();

        if (hit) {
            // Player dies
            handleDeath(player);
        } else {
            // Player survives
            handleSurvival(player);
        }
    }

    /**
     * Handle a player's death.
     * 
     * @param player The player who died
     */
    private void handleDeath(Player player) {
        // Play effects
        plugin.getEffectsManager().playDeathEffects(player);

        // Broadcast death
        broadcastMessage(config.getMessage("playerDied")
                .replace("%player%", player.getName()));

        // Eliminate player
        eliminatePlayer(player.getUniqueId(), false);

        // Reshuffle if configured
        if (reshuffleAfterShot && revolver.hasBullets()) {
            revolver.shuffle();
        }

        // Check for winner
        if (!checkForWinner()) {
            // Continue to next turn
            nextTurn();
        }
    }

    /**
     * Handle a player's survival.
     * 
     * @param player The player who survived
     */
    private void handleSurvival(Player player) {
        // Play effects
        plugin.getEffectsManager().playSurvivalEffects(player);

        // Broadcast survival
        broadcastMessage(config.getMessage("playerSurvived")
                .replace("%player%", player.getName()));

        // Show title
        player.sendTitle(
                config.getRawMessage("survivedTitle"),
                config.getRawMessage("survivedSubtitle"),
                5, 30, 10);

        // Continue to next turn
        nextTurn();
    }

    /**
     * Eliminate a player from the game.
     * 
     * @param playerId     UUID of player to eliminate
     * @param disconnected Whether player disconnected
     */
    private void eliminatePlayer(UUID playerId, boolean disconnected) {
        PlayerData data = players.get(playerId);
        if (data != null) {
            data.setAlive(false);
        }

        turnOrder.remove(playerId);

        Player player = Bukkit.getPlayer(playerId);
        if (player != null && player.isOnline()) {
            // Remove revolver
            player.getInventory().remove(getRevolverItem().getType());

            // Teleport back if needed
            if (config.isTeleportToArena() && data != null) {
                player.teleport(data.getOriginalLocation());
            }

            // Remove scoreboard
            plugin.getScoreboardManager().removeScoreboard(player);
        }

        // Adjust turn index if needed
        if (currentTurnIndex >= turnOrder.size() && !turnOrder.isEmpty()) {
            currentTurnIndex = 0;
        }
    }

    /**
     * Move to the next player's turn.
     */
    private void nextTurn() {
        if (turnOrder.isEmpty()) {
            endGame();
            return;
        }

        currentTurnIndex = (currentTurnIndex + 1) % turnOrder.size();

        // Small delay before next turn
        new BukkitRunnable() {
            @Override
            public void run() {
                if (state == GameState.IN_PROGRESS) {
                    startTurn();
                }
            }
        }.runTaskLater(plugin, 40L); // 2 second delay
    }

    /**
     * Check if there's a winner.
     * 
     * @return true if game ended with a winner
     */
    private boolean checkForWinner() {
        List<UUID> alivePlayers = getAlivePlayers();

        if (alivePlayers.size() <= 1) {
            if (alivePlayers.size() == 1) {
                // We have a winner!
                UUID winnerId = alivePlayers.get(0);
                Player winner = Bukkit.getPlayer(winnerId);
                declareWinner(winner);
            }
            endGame();
            return true;
        }

        return false;
    }

    /**
     * Declare the winner and give rewards.
     * 
     * @param winner The winning player
     */
    private void declareWinner(Player winner) {
        if (winner == null)
            return;

        // Show winner title
        winner.sendTitle(
                config.getRawMessage("winTitle"),
                config.getRawMessage("winSubtitle"),
                10, 70, 20);

        winner.sendMessage(config.getMessage("youWin"));

        // Broadcast winner
        broadcastMessage(config.getMessage("winnerAnnouncement")
                .replace("%winner%", winner.getName()));

        // Give winnings from pot
        plugin.getRewardManager().giveWinnings(winner, this);
    }

    /**
     * End the game.
     */
    public void endGame() {
        state = GameState.ENDED;

        // Cancel all tasks
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
        if (turnTimerTask != null) {
            turnTimerTask.cancel();
            turnTimerTask = null;
        }

        // Save player IDs before clearing (for GameManager cleanup)
        Set<UUID> playerIds = new HashSet<>(players.keySet());

        // Clean up all players
        for (UUID playerId : playerIds) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                // Remove revolver
                player.getInventory().remove(getRevolverItem().getType());

                // Close any open GUIs
                player.closeInventory();

                // Teleport back
                PlayerData data = players.get(playerId);
                if (config.isTeleportToArena() && data != null) {
                    // Unseat player first
                    unseatPlayer(player);
                    player.teleport(data.getOriginalLocation());
                }

                // Remove scoreboard
                plugin.getScoreboardManager().removeScoreboard(player);
            }
        }

        // Clean up all seat entities
        cleanupSeats();

        players.clear();
        turnOrder.clear();

        broadcastMessage(config.getMessage("gameEnded"));

        // Remove from game manager (pass player IDs for cleanup)
        plugin.getGameManager().removeGame(this, playerIds);
    }

    /**
     * Force end the game (admin command).
     */
    public void forceEnd() {
        // Refund all bets
        plugin.getRewardManager().refundBets(this);
        broadcastMessage(config.getMessage("gameForceEnded"));
        endGame();
    }

    /**
     * Teleport all players to the arena in a circle.
     */
    private void teleportPlayersToArena() {
        World world = Bukkit.getWorld(config.getArenaWorld());
        if (world == null)
            return;

        double centerX = config.getArenaCenterX();
        double centerY = config.getArenaCenterY();
        double centerZ = config.getArenaCenterZ();
        double radius = config.getCircleRadius();

        int playerCount = players.size();
        double angleStep = (2 * Math.PI) / playerCount;

        int index = 0;
        for (UUID playerId : players.keySet()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                double angle = angleStep * index;
                double x = centerX + radius * Math.cos(angle);
                double z = centerZ + radius * Math.sin(angle);

                Location loc = new Location(world, x, centerY, z);
                // Make player face center
                loc.setYaw((float) Math.toDegrees(Math.atan2(centerZ - z, centerX - x)) - 90);

                player.teleport(loc);

                // Seat the player in an invisible chair
                seatPlayer(player, loc);

                index++;
            }
        }
    }

    /**
     * Seat a player in an invisible armor stand (chair).
     */
    private void seatPlayer(Player player, Location location) {
        // Create invisible armor stand as seat
        ArmorStand seat = location.getWorld().spawn(
                location.clone().subtract(0, 0.7, 0), // Lower so player appears seated
                ArmorStand.class,
                armorStand -> {
                    armorStand.setVisible(false);
                    armorStand.setGravity(false);
                    armorStand.setInvulnerable(true);
                    armorStand.setSmall(true);
                    armorStand.setMarker(true);
                    armorStand.setCustomName("RR_Seat_" + player.getUniqueId());
                    armorStand.setCustomNameVisible(false);
                });

        // Make player sit on it
        seat.addPassenger(player);
        seatEntities.put(player.getUniqueId(), seat);
    }

    /**
     * Unseat a player and remove their chair entity.
     */
    private void unseatPlayer(Player player) {
        ArmorStand seat = seatEntities.remove(player.getUniqueId());
        if (seat != null && !seat.isDead()) {
            seat.eject();
            seat.remove();
        }
    }

    /**
     * Clean up all seat entities.
     */
    private void cleanupSeats() {
        for (ArmorStand seat : seatEntities.values()) {
            if (seat != null && !seat.isDead()) {
                seat.eject();
                seat.remove();
            }
        }
        seatEntities.clear();
    }

    /**
     * Give revolver items to all players.
     */
    private void giveRevolverItems() {
        ItemStack revolver = getRevolverItem();

        for (UUID playerId : players.keySet()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                player.getInventory().addItem(revolver.clone());
            }
        }
    }

    /**
     * Get the revolver item.
     * 
     * @return ItemStack representing the revolver
     */
    public ItemStack getRevolverItem() {
        ItemStack item = new ItemStack(config.getRevolverMaterial());
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(config.getRevolverName());
            meta.setLore(config.getRevolverLore());
            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Update scoreboards for all players.
     */
    private void updateScoreboards() {
        if (!config.isScoreboardEnabled())
            return;

        String turnPlayerName = "N/A";
        if (currentTurnPlayer != null) {
            Player turnPlayer = Bukkit.getPlayer(currentTurnPlayer);
            if (turnPlayer != null) {
                turnPlayerName = turnPlayer.getName();
            }
        }

        for (UUID playerId : players.keySet()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                plugin.getScoreboardManager().updateScoreboard(
                        player,
                        getAlivePlayers().size(),
                        revolver != null ? revolver.getBulletsRemaining() : mode.getBulletCount(),
                        turnPlayerName,
                        state.name(),
                        turnTimeRemaining);
            }
        }
    }

    /**
     * Broadcast a message to all players in the game.
     * 
     * @param message Message to broadcast
     */
    private void broadcastMessage(String message) {
        for (UUID playerId : players.keySet()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                player.sendMessage(message);
            }
        }
    }

    /**
     * Play a sound to all players.
     * 
     * @param soundKey Config key for the sound
     */
    private void playSound(String soundKey) {
        for (UUID playerId : players.keySet()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                playSound(soundKey, player);
            }
        }
    }

    /**
     * Play a sound to a specific player.
     * 
     * @param soundKey Config key for the sound
     * @param player   Player to play sound to
     */
    private void playSound(String soundKey, Player player) {
        player.playSound(
                player.getLocation(),
                config.getSound(soundKey),
                config.getSoundVolume(soundKey),
                config.getSoundPitch(soundKey));
    }

    /**
     * Get list of alive player UUIDs.
     * 
     * @return List of alive player UUIDs
     */
    public List<UUID> getAlivePlayers() {
        return new ArrayList<>(turnOrder);
    }

    // Getters
    public UUID getGameId() {
        return gameId;
    }

    public GameState getState() {
        return state;
    }

    public GameMode getMode() {
        return mode;
    }

    public Map<UUID, PlayerData> getPlayers() {
        return players;
    }

    public int getPlayerCount() {
        return players.size();
    }

    public UUID getCurrentTurnPlayer() {
        return currentTurnPlayer;
    }

    public Revolver getRevolver() {
        return revolver;
    }

    public boolean hasPlayer(UUID playerId) {
        return players.containsKey(playerId);
    }

    public boolean isPlayerTurn(UUID playerId) {
        return currentTurnPlayer != null && currentTurnPlayer.equals(playerId);
    }
}

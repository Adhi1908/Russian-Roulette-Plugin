package com.example.russianroulette.game;

import com.example.russianroulette.RussianRoulettePlugin;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages all active Russian Roulette games.
 */
public class GameManager {

    private final RussianRoulettePlugin plugin;
    private final Map<UUID, Game> activeGames;
    private final Map<UUID, Game> playerGameMap;

    public GameManager(RussianRoulettePlugin plugin) {
        this.plugin = plugin;
        this.activeGames = new ConcurrentHashMap<>();
        this.playerGameMap = new ConcurrentHashMap<>();
    }

    /**
     * Create a new game.
     * 
     * @return The created game, or null if creation failed
     */
    public Game createGame() {
        if (!plugin.getConfigManager().allowMultipleGames() && !activeGames.isEmpty()) {
            return null;
        }

        Game game = new Game(plugin);
        activeGames.put(game.getGameId(), game);
        return game;
    }

    /**
     * Get an existing game that's waiting for players.
     * 
     * @return A waiting game, or null if none exists
     */
    public Game getWaitingGame() {
        for (Game game : activeGames.values()) {
            if (game.getState() == GameState.WAITING) {
                return game;
            }
        }
        return null;
    }

    /**
     * Get all active games.
     * 
     * @return Collection of active games
     */
    public Collection<Game> getActiveGames() {
        return activeGames.values();
    }

    /**
     * Remove a game from active games.
     * 
     * @param game Game to remove
     */
    public void removeGame(Game game) {
        activeGames.remove(game.getGameId());

        // Clean up player mappings
        for (UUID playerId : game.getPlayers().keySet()) {
            playerGameMap.remove(playerId);
        }
    }

    /**
     * Remove a game from active games with explicit player list.
     * Used when game's player list has already been cleared.
     * 
     * @param game      Game to remove
     * @param playerIds Set of player UUIDs to clean up
     */
    public void removeGame(Game game, java.util.Set<UUID> playerIds) {
        activeGames.remove(game.getGameId());

        // Clean up player mappings using provided player IDs
        for (UUID playerId : playerIds) {
            playerGameMap.remove(playerId);
        }
    }

    /**
     * Add a player to a game.
     * 
     * @param player Player to add
     * @param game   Game to add player to
     * @return true if successful
     */
    public boolean addPlayerToGame(Player player, Game game) {
        if (playerGameMap.containsKey(player.getUniqueId())) {
            return false;
        }

        if (game.addPlayer(player)) {
            playerGameMap.put(player.getUniqueId(), game);
            return true;
        }

        return false;
    }

    /**
     * Remove a player from their current game.
     * 
     * @param player Player to remove
     * @param forced Whether removal is forced
     * @return true if successful
     */
    public boolean removePlayerFromGame(Player player, boolean forced) {
        Game game = playerGameMap.get(player.getUniqueId());
        if (game == null) {
            return false;
        }

        if (game.removePlayer(player, forced)) {
            playerGameMap.remove(player.getUniqueId());
            return true;
        }

        return false;
    }

    /**
     * Get the game a player is in.
     * 
     * @param player Player to check
     * @return The game, or null if not in any game
     */
    public Game getPlayerGame(Player player) {
        return playerGameMap.get(player.getUniqueId());
    }

    /**
     * Get the game a player is in by UUID.
     * 
     * @param playerId Player UUID to check
     * @return The game, or null if not in any game
     */
    public Game getPlayerGame(UUID playerId) {
        return playerGameMap.get(playerId);
    }

    /**
     * Check if a player is in any game.
     * 
     * @param player Player to check
     * @return true if in a game
     */
    public boolean isPlayerInGame(Player player) {
        return playerGameMap.containsKey(player.getUniqueId());
    }

    /**
     * Check if a player is in any game by UUID.
     * 
     * @param playerId Player UUID to check
     * @return true if in a game
     */
    public boolean isPlayerInGame(UUID playerId) {
        return playerGameMap.containsKey(playerId);
    }

    /**
     * End all active games gracefully.
     */
    public void endAllGames() {
        for (Game game : activeGames.values()) {
            game.forceEnd();
        }
        activeGames.clear();
        playerGameMap.clear();
    }

    /**
     * Check if there are any active games.
     * 
     * @return true if games exist
     */
    public boolean hasActiveGames() {
        return !activeGames.isEmpty();
    }

    /**
     * Get the number of active games.
     * 
     * @return Number of active games
     */
    public int getActiveGameCount() {
        return activeGames.size();
    }
}

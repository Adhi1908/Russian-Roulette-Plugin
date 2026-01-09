package com.example.russianroulette.rewards;

import com.example.russianroulette.RussianRoulettePlugin;
import com.example.russianroulette.config.ConfigManager;
import com.example.russianroulette.game.Game;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Manages the betting system and pot distribution for game winners.
 */
public class RewardManager {

    private final RussianRoulettePlugin plugin;
    private final ConfigManager config;

    // Track money pot per game
    private final Map<UUID, Double> gamePots;
    // Track item bets per game (player UUID -> items they bet)
    private final Map<UUID, Map<UUID, List<ItemStack>>> gameItemBets;

    public RewardManager(RussianRoulettePlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        this.gamePots = new HashMap<>();
        this.gameItemBets = new HashMap<>();
    }

    /**
     * Take bet from a player when they join a game.
     * 
     * @param player Player placing bet
     * @param game   Game they're joining
     * @return true if bet was taken successfully
     */
    public boolean takeBet(Player player, Game game) {
        if (!config.isBettingEnabled()) {
            return true; // No betting required
        }

        UUID gameId = game.getGameId();

        if (config.isMoneyBetting()) {
            return takeMoneyBet(player, gameId);
        } else {
            return takeItemBet(player, gameId);
        }
    }

    /**
     * Take money bet from player.
     */
    private boolean takeMoneyBet(Player player, UUID gameId) {
        double amount = config.getBetAmount();

        if (!plugin.hasEconomy()) {
            plugin.getLogger().warning("Vault economy not available for betting!");
            return true; // Allow joining without bet
        }

        Economy economy = plugin.getEconomy();

        // Check if player has enough money
        if (economy.getBalance(player) < amount) {
            player.sendMessage(config.getMessage("insufficientFunds")
                    .replace("%amount%", String.format("%.2f", amount)));
            return false;
        }

        // Take the money
        economy.withdrawPlayer(player, amount);

        // Add to pot
        gamePots.merge(gameId, amount, Double::sum);

        player.sendMessage(config.getMessage("betTaken")
                .replace("%amount%", String.format("%.2f", amount)));

        return true;
    }

    /**
     * Take item bet from player using configurable allowed items.
     * Player must have items from allowed list with total value >= minItemValue.
     */
    private boolean takeItemBet(Player player, UUID gameId) {
        Map<Material, Integer> allowedItems = config.getAllowedBetItems();
        int minValue = config.getMinItemValue();

        // Collect player's allowed items and their values
        List<ItemStack> itemsToTake = new ArrayList<>();
        int totalValue = 0;

        // Scan player inventory for allowed items
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType() == Material.AIR)
                continue;

            Integer valuePerItem = allowedItems.get(item.getType());
            if (valuePerItem != null) {
                // Only take what we need to meet minimum
                if (totalValue < minValue) {
                    int needed = (int) Math.ceil((double) (minValue - totalValue) / valuePerItem);
                    int take = Math.min(needed, item.getAmount());

                    ItemStack toTake = item.clone();
                    toTake.setAmount(take);
                    itemsToTake.add(toTake);
                    totalValue += valuePerItem * take;
                }
            }
        }

        // Check if player has enough value
        if (totalValue < minValue) {
            player.sendMessage(config.getMessage("insufficientItems")
                    .replace("%value%", String.valueOf(totalValue))
                    .replace("%required%", String.valueOf(minValue)));
            return false;
        }

        // Take the items from player
        for (ItemStack itemToTake : itemsToTake) {
            removeItems(player, itemToTake.getType(), itemToTake.getAmount());
        }

        // Store items for pot
        gameItemBets.computeIfAbsent(gameId, k -> new HashMap<>())
                .put(player.getUniqueId(), itemsToTake);

        player.sendMessage(config.getMessage("betItemsTaken")
                .replace("%value%", String.valueOf(totalValue)));

        return true;
    }

    /**
     * Give all winnings to the winner.
     * 
     * @param winner The winning player
     * @param game   The game that was won
     */
    public void giveWinnings(Player winner, Game game) {
        if (!config.isBettingEnabled()) {
            return;
        }

        UUID gameId = game.getGameId();

        if (config.isMoneyBetting()) {
            giveMoneyWinnings(winner, gameId);
        } else {
            giveItemWinnings(winner, gameId);
        }

        // Play winner effects
        plugin.getEffectsManager().playWinnerEffects(winner);

        // Clean up game data
        cleanupGame(gameId);
    }

    /**
     * Give money pot to winner.
     */
    private void giveMoneyWinnings(Player winner, UUID gameId) {
        Double pot = gamePots.get(gameId);
        if (pot == null || pot <= 0) {
            return;
        }

        if (!plugin.hasEconomy()) {
            return;
        }

        // Apply house cut
        double houseCut = config.getHouseCut();
        double winnings = pot * (1 - houseCut);

        Economy economy = plugin.getEconomy();
        economy.depositPlayer(winner, winnings);

        winner.sendMessage(config.getMessage("winningsReceived")
                .replace("%amount%", String.format("%.2f", winnings)));
    }

    /**
     * Give item pot to winner.
     */
    private void giveItemWinnings(Player winner, UUID gameId) {
        Map<UUID, List<ItemStack>> playerBets = gameItemBets.get(gameId);
        if (playerBets == null || playerBets.isEmpty()) {
            return;
        }

        // Give all bet items to winner
        for (List<ItemStack> items : playerBets.values()) {
            for (ItemStack item : items) {
                if (winner.getInventory().firstEmpty() != -1) {
                    winner.getInventory().addItem(item.clone());
                } else {
                    winner.getWorld().dropItemNaturally(winner.getLocation(), item.clone());
                }
            }
        }

        winner.sendMessage(config.getMessage("itemsWon"));
    }

    /**
     * Refund all bets when a game is cancelled.
     * 
     * @param game The game that was cancelled
     */
    public void refundBets(Game game) {
        if (!config.isBettingEnabled() || !config.isRefundOnCancel()) {
            cleanupGame(game.getGameId());
            return;
        }

        UUID gameId = game.getGameId();
        int playerCount = game.getPlayerCount();

        if (config.isMoneyBetting()) {
            refundMoneyBets(game, gameId, playerCount);
        } else {
            refundItemBets(gameId);
        }

        cleanupGame(gameId);
    }

    /**
     * Refund money bets.
     */
    private void refundMoneyBets(Game game, UUID gameId, int playerCount) {
        Double pot = gamePots.get(gameId);
        if (pot == null || pot <= 0 || !plugin.hasEconomy()) {
            return;
        }

        Economy economy = plugin.getEconomy();
        double perPlayer = pot / playerCount;

        for (UUID playerId : game.getPlayers().keySet()) {
            Player player = plugin.getServer().getPlayer(playerId);
            if (player != null && player.isOnline()) {
                economy.depositPlayer(player, perPlayer);
                player.sendMessage(config.getMessage("betsRefunded")
                        .replace("%amount%", String.format("%.2f", perPlayer)));
            }
        }
    }

    /**
     * Refund item bets.
     */
    private void refundItemBets(UUID gameId) {
        Map<UUID, List<ItemStack>> playerBets = gameItemBets.get(gameId);
        if (playerBets == null) {
            return;
        }

        for (Map.Entry<UUID, List<ItemStack>> entry : playerBets.entrySet()) {
            Player player = plugin.getServer().getPlayer(entry.getKey());
            if (player != null && player.isOnline()) {
                for (ItemStack item : entry.getValue()) {
                    if (player.getInventory().firstEmpty() != -1) {
                        player.getInventory().addItem(item.clone());
                    } else {
                        player.getWorld().dropItemNaturally(player.getLocation(), item.clone());
                    }
                }
                player.sendMessage(config.getMessage("itemsRefunded"));
            }
        }
    }

    /**
     * Clean up game betting data.
     */
    private void cleanupGame(UUID gameId) {
        gamePots.remove(gameId);
        gameItemBets.remove(gameId);
    }

    /**
     * Remove items from player inventory.
     */
    private void removeItems(Player player, Material material, int amount) {
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();

        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack item = contents[i];
            if (item != null && item.getType() == material) {
                int take = Math.min(item.getAmount(), remaining);
                item.setAmount(item.getAmount() - take);
                remaining -= take;
                if (item.getAmount() <= 0) {
                    player.getInventory().setItem(i, null);
                }
            }
        }
    }

    /**
     * Get current pot amount for a game.
     */
    public double getPotAmount(UUID gameId) {
        return gamePots.getOrDefault(gameId, 0.0);
    }
}

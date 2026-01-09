package com.example.russianroulette.listeners;

import com.example.russianroulette.RussianRoulettePlugin;
import com.example.russianroulette.config.ConfigManager;
import com.example.russianroulette.game.Game;
import com.example.russianroulette.game.GameManager;
import com.example.russianroulette.game.GameState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Listener for player events related to the game.
 * Handles anti-abuse measures and revolver item interactions.
 */
public class PlayerListener implements Listener {

    private final RussianRoulettePlugin plugin;
    private final ConfigManager config;
    private final GameManager gameManager;

    public PlayerListener(RussianRoulettePlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        this.gameManager = plugin.getGameManager();
    }

    /**
     * Handle player disconnect - instant death if in game.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        if (gameManager.isPlayerInGame(player)) {
            // Force remove player (will trigger death if in active game)
            gameManager.removePlayerFromGame(player, true);
        }
    }

    /**
     * Handle revolver right-click to pull trigger.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR &&
                event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null) {
            return;
        }

        // Check if it's the revolver item
        if (!isRevolverItem(item)) {
            return;
        }

        event.setCancelled(true);

        // Get player's game
        Game game = gameManager.getPlayerGame(player);
        if (game == null) {
            return;
        }

        // Check if game is in progress
        if (game.getState() != GameState.IN_PROGRESS) {
            return;
        }

        // Check if it's player's turn
        if (!game.isPlayerTurn(player.getUniqueId())) {
            player.sendMessage(config.getMessage("notYourTurn"));
            return;
        }

        // Pull the trigger!
        game.pullTrigger(player);
    }

    /**
     * Prevent players from moving/teleporting during game.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        Game game = gameManager.getPlayerGame(player);

        if (game == null) {
            return;
        }

        // Only block during active game
        if (game.getState() != GameState.IN_PROGRESS) {
            return;
        }

        // Allow teleports caused by the plugin itself
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.PLUGIN) {
            return;
        }

        // Block other teleports
        event.setCancelled(true);
    }

    /**
     * Prevent external damage to players during game.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();
        Game game = gameManager.getPlayerGame(player);

        if (game == null) {
            return;
        }

        // Block all damage during game
        if (game.getState() == GameState.IN_PROGRESS || game.getState() == GameState.STARTING) {
            event.setCancelled(true);
        }
    }

    /**
     * Prevent PvP damage in game.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();

        if (gameManager.isPlayerInGame(player)) {
            event.setCancelled(true);
        }

        // Also prevent damage from players in game
        if (event.getDamager() instanceof Player) {
            Player damager = (Player) event.getDamager();
            if (gameManager.isPlayerInGame(damager)) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * Prevent dropping the revolver item.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onItemDrop(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();

        if (isRevolverItem(item)) {
            event.setCancelled(true);
        }
    }

    /**
     * Handle player respawn (shouldn't happen during game, but just in case).
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Game game = gameManager.getPlayerGame(player);

        if (game != null) {
            // Remove from game on respawn
            gameManager.removePlayerFromGame(player, true);
        }
    }

    /**
     * Check if an item is the revolver item.
     * 
     * @param item Item to check
     * @return true if it's the revolver
     */
    private boolean isRevolverItem(ItemStack item) {
        if (item == null || item.getType() != config.getRevolverMaterial()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return false;
        }

        return meta.getDisplayName().equals(config.getRevolverName());
    }
}

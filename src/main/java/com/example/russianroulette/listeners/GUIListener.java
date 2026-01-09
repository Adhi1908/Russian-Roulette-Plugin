package com.example.russianroulette.listeners;

import com.example.russianroulette.RussianRoulettePlugin;
import com.example.russianroulette.game.Game;
import com.example.russianroulette.game.GameManager;
import com.example.russianroulette.game.GameState;
import com.example.russianroulette.gui.RevolverGUI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

/**
 * Listener for GUI interactions.
 */
public class GUIListener implements Listener {

    private final RussianRoulettePlugin plugin;
    private final GameManager gameManager;

    public GUIListener(RussianRoulettePlugin plugin) {
        this.plugin = plugin;
        this.gameManager = plugin.getGameManager();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        // Check if it's the revolver GUI
        if (!RevolverGUI.isRevolverGUI(title)) {
            return;
        }

        // Cancel all clicks in the GUI
        event.setCancelled(true);

        // Check if clicked the trigger slot
        if (event.getRawSlot() != RevolverGUI.getTriggerSlot()) {
            return;
        }

        // Get player's game
        Game game = gameManager.getPlayerGame(player);
        if (game == null) {
            player.closeInventory();
            return;
        }

        // Check if game is in progress
        if (game.getState() != GameState.IN_PROGRESS) {
            player.closeInventory();
            return;
        }

        // Check if it's player's turn
        if (!game.isPlayerTurn(player.getUniqueId())) {
            player.sendMessage(plugin.getConfigManager().getMessage("notYourTurn"));
            return;
        }

        // Pull the trigger!
        game.pullTrigger(player);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        String title = event.getView().getTitle();

        // Prevent dragging in revolver GUI
        if (RevolverGUI.isRevolverGUI(title)) {
            event.setCancelled(true);
        }
    }
}

package com.example.russianroulette.gui;

import com.example.russianroulette.RussianRoulettePlugin;
import com.example.russianroulette.config.ConfigManager;
import com.example.russianroulette.game.Game;
import com.example.russianroulette.game.Revolver;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI for the revolver trigger interface.
 */
public class RevolverGUI {

    public static final String GUI_TITLE_ID = "§c§l☠ Pull The Trigger ☠";
    private static final int TRIGGER_SLOT = 4; // Center slot

    /**
     * Open the revolver GUI for a player.
     * 
     * @param player Player to open GUI for
     * @param game   Current game
     */
    public static void openGUI(Player player, Game game) {
        RussianRoulettePlugin plugin = RussianRoulettePlugin.getInstance();
        ConfigManager config = plugin.getConfigManager();

        Inventory gui = Bukkit.createInventory(null, 9, config.getRawMessage("guiTitle"));

        // Fill with glass panes
        ItemStack glass = createItem(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 9; i++) {
            gui.setItem(i, glass);
        }

        // Create trigger button
        Revolver revolver = game.getRevolver();
        int bulletsRemaining = revolver != null ? revolver.getBulletsRemaining() : 0;
        int chambersRemaining = 6;

        List<String> lore = new ArrayList<>();
        for (String line : config.getMessageList("pullTriggerLore")) {
            lore.add(line
                    .replace("%chambers%", String.valueOf(chambersRemaining))
                    .replace("%bullets%", String.valueOf(bulletsRemaining)));
        }

        ItemStack trigger = createItem(
                Material.LEVER,
                config.getRawMessage("pullTriggerButton"),
                lore);

        gui.setItem(TRIGGER_SLOT, trigger);

        // Add decorative items
        gui.setItem(0, createItem(Material.IRON_INGOT, "§7Chamber 1", null));
        gui.setItem(1, createItem(Material.IRON_INGOT, "§7Chamber 2", null));
        gui.setItem(2, createItem(Material.IRON_INGOT, "§7Chamber 3", null));
        gui.setItem(6, createItem(Material.IRON_INGOT, "§7Chamber 4", null));
        gui.setItem(7, createItem(Material.IRON_INGOT, "§7Chamber 5", null));
        gui.setItem(8, createItem(Material.IRON_INGOT, "§7Chamber 6", null));

        player.openInventory(gui);
    }

    /**
     * Create an item stack with custom name and lore.
     */
    private static ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            if (name != null) {
                meta.setDisplayName(name);
            }
            if (lore != null && !lore.isEmpty()) {
                meta.setLore(lore);
            }
            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Get the trigger slot index.
     * 
     * @return Trigger slot index
     */
    public static int getTriggerSlot() {
        return TRIGGER_SLOT;
    }

    /**
     * Check if an inventory is the revolver GUI.
     * 
     * @param title Inventory title
     * @return true if it's the revolver GUI
     */
    public static boolean isRevolverGUI(String title) {
        return title != null && title.contains("Pull The Trigger");
    }
}

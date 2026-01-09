package com.example.russianroulette.config;

import com.example.russianroulette.RussianRoulettePlugin;
import com.example.russianroulette.game.GameMode;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages all plugin configuration files.
 */
public class ConfigManager {

    private final RussianRoulettePlugin plugin;
    private FileConfiguration config;
    private FileConfiguration messages;
    private File messagesFile;

    public ConfigManager(RussianRoulettePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Load or reload all configuration files.
     */
    public void loadConfigs() {
        // Save default configs if they don't exist
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();

        // Load messages.yml
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messages = YamlConfiguration.loadConfiguration(messagesFile);
    }

    /**
     * Translate color codes in a string.
     */
    public String colorize(String text) {
        if (text == null)
            return "";
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    /**
     * Get a message from messages.yml with prefix.
     */
    public String getMessage(String key) {
        String prefix = colorize(messages.getString("prefix", "&8[&c&lRR&8] &r"));
        String message = messages.getString(key, "&cMissing message: " + key);
        return prefix + colorize(message);
    }

    /**
     * Get a raw message without prefix.
     */
    public String getRawMessage(String key) {
        return colorize(messages.getString(key, "&cMissing message: " + key));
    }

    /**
     * Get a list of strings from messages.yml.
     */
    public List<String> getMessageList(String key) {
        List<String> list = messages.getStringList(key);
        List<String> colorized = new ArrayList<>();
        for (String s : list) {
            colorized.add(colorize(s));
        }
        return colorized;
    }

    // ==================== GAME SETTINGS ====================

    public int getMinPlayers() {
        return config.getInt("game.minPlayers", 2);
    }

    public int getMaxPlayers() {
        int max = config.getInt("game.maxPlayers", 10);
        return max <= 0 ? Integer.MAX_VALUE : max;
    }

    public int getTurnTime() {
        return config.getInt("game.turnTime", 10);
    }

    public GameMode getGameMode() {
        String mode = config.getString("game.gameMode", "CLASSIC");
        try {
            return GameMode.valueOf(mode.toUpperCase());
        } catch (IllegalArgumentException e) {
            return GameMode.CLASSIC;
        }
    }

    public boolean isReshuffleAfterShot() {
        return config.getBoolean("game.reshuffleAfterShot", false);
    }

    public int getStartCountdown() {
        return config.getInt("game.startCountdown", 5);
    }

    public boolean allowMultipleGames() {
        return config.getBoolean("game.allowMultipleGames", false);
    }

    // ==================== ARENA SETTINGS ====================

    public String getArenaWorld() {
        return config.getString("arena.world", "world");
    }

    public boolean isTeleportToArena() {
        return config.getBoolean("arena.teleportToArena", false);
    }

    public double getArenaCenterX() {
        return config.getDouble("arena.center.x", 0);
    }

    public double getArenaCenterY() {
        return config.getDouble("arena.center.y", 64);
    }

    public double getArenaCenterZ() {
        return config.getDouble("arena.center.z", 0);
    }

    /**
     * Get list of seat locations from config.
     * Returns locations for seat1 through seat6.
     */
    public java.util.List<double[]> getSeatLocations() {
        java.util.List<double[]> seats = new java.util.ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            String path = "arena.seats.seat" + i;
            if (config.contains(path + ".x")) {
                double x = config.getDouble(path + ".x", 0);
                double y = config.getDouble(path + ".y", 64);
                double z = config.getDouble(path + ".z", 0);
                float yaw = (float) config.getDouble(path + ".yaw", 0);
                seats.add(new double[] { x, y, z, yaw });
            }
        }
        return seats;
    }

    /**
     * Set a seat location and save to config.
     */
    public void setSeatLocation(int seatNumber, org.bukkit.Location location) {
        String path = "arena.seats.seat" + seatNumber;
        config.set(path + ".x", location.getX());
        config.set(path + ".y", location.getY());
        config.set(path + ".z", location.getZ());
        config.set(path + ".yaw", location.getYaw());
        plugin.saveConfig();
    }

    /**
     * Set the table center location and save to config.
     */
    public void setCenterLocation(org.bukkit.Location location) {
        config.set("arena.center.x", location.getX());
        config.set("arena.center.y", location.getY());
        config.set("arena.center.z", location.getZ());
        config.set("arena.world", location.getWorld().getName());
        plugin.saveConfig();
    }

    // ==================== BETTING SETTINGS ====================

    public boolean isBettingEnabled() {
        return config.getBoolean("betting.enabled", true);
    }

    public String getBetType() {
        return config.getString("betting.type", "MONEY").toUpperCase();
    }

    public boolean isMoneyBetting() {
        return getBetType().equals("MONEY");
    }

    public double getBetAmount() {
        return config.getDouble("betting.moneyAmount", 1000);
    }

    public int getMinItemValue() {
        return config.getInt("betting.minItemValue", 100);
    }

    public java.util.Map<Material, Integer> getAllowedBetItems() {
        java.util.Map<Material, Integer> items = new java.util.HashMap<>();
        List<?> itemList = config.getList("betting.allowedItems");
        if (itemList != null) {
            for (Object obj : itemList) {
                if (obj instanceof java.util.Map) {
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, Object> map = (java.util.Map<String, Object>) obj;
                    String matName = (String) map.getOrDefault("material", "DIAMOND");
                    Material material = Material.matchMaterial(matName);
                    int value = 50; // default value
                    Object valueObj = map.get("valuePerItem");
                    if (valueObj instanceof Integer) {
                        value = (Integer) valueObj;
                    } else if (valueObj instanceof Double) {
                        value = ((Double) valueObj).intValue();
                    }
                    if (material != null) {
                        items.put(material, value);
                    }
                }
            }
        }
        // Add defaults if empty
        if (items.isEmpty()) {
            items.put(Material.DIAMOND, 50);
            items.put(Material.GOLD_INGOT, 10);
            items.put(Material.NETHERITE_INGOT, 500);
        }
        return items;
    }

    public double getHouseCut() {
        return config.getDouble("betting.houseCut", 0) / 100.0; // Convert percentage to decimal
    }

    public boolean isRefundOnCancel() {
        return config.getBoolean("betting.refundOnCancel", true);
    }

    // ==================== SOUND SETTINGS ====================

    public Sound getSound(String path) {
        String soundName = config.getString("sounds." + path + ".sound", "BLOCK_LEVER_CLICK");
        try {
            return Sound.valueOf(soundName.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Sound.BLOCK_LEVER_CLICK;
        }
    }

    public float getSoundVolume(String path) {
        return (float) config.getDouble("sounds." + path + ".volume", 1.0);
    }

    public float getSoundPitch(String path) {
        return (float) config.getDouble("sounds." + path + ".pitch", 1.0);
    }

    // ==================== EFFECT SETTINGS ====================

    public boolean isBloodParticlesEnabled() {
        return config.getBoolean("effects.bloodParticles", true);
    }

    public int getBloodParticleCount() {
        return config.getInt("effects.bloodParticleCount", 50);
    }

    public boolean isSmokeParticlesEnabled() {
        return config.getBoolean("effects.smokeParticles", true);
    }

    public boolean isSlowMotionDeathEnabled() {
        return config.getBoolean("effects.slowMotionDeath", true);
    }

    public int getSlowMotionDuration() {
        return config.getInt("effects.slowMotionDuration", 40);
    }

    public boolean isShowBangTitle() {
        return config.getBoolean("effects.showBangTitle", true);
    }

    // ==================== REVOLVER ITEM SETTINGS ====================

    public Material getRevolverMaterial() {
        String matName = config.getString("revolverItem.material", "IRON_HORSE_ARMOR");
        Material material = Material.matchMaterial(matName);
        return material != null ? material : Material.IRON_HORSE_ARMOR;
    }

    public String getRevolverName() {
        return colorize(config.getString("revolverItem.name", "&c&l⚡ REVOLVER ⚡"));
    }

    public List<String> getRevolverLore() {
        List<String> lore = config.getStringList("revolverItem.lore");
        List<String> colorized = new ArrayList<>();
        for (String s : lore) {
            colorized.add(colorize(s));
        }
        return colorized;
    }

    // ==================== SCOREBOARD SETTINGS ====================

    public boolean isScoreboardEnabled() {
        return config.getBoolean("scoreboard.enabled", true);
    }

    public String getScoreboardTitle() {
        return colorize(config.getString("scoreboard.title", "&c&l☠ RUSSIAN ROULETTE ☠"));
    }
}

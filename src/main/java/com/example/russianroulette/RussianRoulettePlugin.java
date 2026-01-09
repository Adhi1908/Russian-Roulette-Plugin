package com.example.russianroulette;

import com.example.russianroulette.commands.RRCommand;
import com.example.russianroulette.config.ConfigManager;
import com.example.russianroulette.effects.EffectsManager;
import com.example.russianroulette.game.GameManager;
import com.example.russianroulette.listeners.GUIListener;
import com.example.russianroulette.listeners.PlayerListener;
import com.example.russianroulette.rewards.RewardManager;
import com.example.russianroulette.scoreboard.ScoreboardManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main plugin class for Russian Roulette minigame.
 * Manages all plugin components and lifecycle.
 */
public class RussianRoulettePlugin extends JavaPlugin {

    private static RussianRoulettePlugin instance;
    
    private ConfigManager configManager;
    private GameManager gameManager;
    private EffectsManager effectsManager;
    private RewardManager rewardManager;
    private ScoreboardManager scoreboardManager;
    private Economy economy;

    @Override
    public void onEnable() {
        instance = this;
        
        // Initialize configuration
        configManager = new ConfigManager(this);
        configManager.loadConfigs();
        
        // Setup Vault economy
        setupEconomy();
        
        // Initialize managers
        gameManager = new GameManager(this);
        effectsManager = new EffectsManager(this);
        rewardManager = new RewardManager(this);
        scoreboardManager = new ScoreboardManager(this);
        
        // Register commands
        RRCommand rrCommand = new RRCommand(this);
        getCommand("rr").setExecutor(rrCommand);
        getCommand("rr").setTabCompleter(rrCommand);
        
        // Register listeners
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);
        
        getLogger().info("Russian Roulette has been enabled!");
        getLogger().info("Version: " + getDescription().getVersion());
        
        if (economy != null) {
            getLogger().info("Vault economy hooked successfully!");
        } else {
            getLogger().warning("Vault economy not found! Money rewards will be disabled.");
        }
    }

    @Override
    public void onDisable() {
        // End all active games gracefully
        if (gameManager != null) {
            gameManager.endAllGames();
        }
        
        // Clean up scoreboards
        if (scoreboardManager != null) {
            scoreboardManager.removeAllScoreboards();
        }
        
        getLogger().info("Russian Roulette has been disabled!");
        instance = null;
    }

    /**
     * Setup Vault economy integration.
     */
    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    /**
     * Reload all plugin configurations.
     */
    public void reload() {
        configManager.loadConfigs();
        getLogger().info("Configuration reloaded!");
    }

    // Getters for all managers
    public static RussianRoulettePlugin getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public GameManager getGameManager() {
        return gameManager;
    }

    public EffectsManager getEffectsManager() {
        return effectsManager;
    }

    public RewardManager getRewardManager() {
        return rewardManager;
    }

    public ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }

    public Economy getEconomy() {
        return economy;
    }

    public boolean hasEconomy() {
        return economy != null;
    }
}

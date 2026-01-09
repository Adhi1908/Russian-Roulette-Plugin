package com.example.russianroulette.scoreboard;

import com.example.russianroulette.RussianRoulettePlugin;
import com.example.russianroulette.config.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages game scoreboards for players.
 */
public class ScoreboardManager {

    private final RussianRoulettePlugin plugin;
    private final ConfigManager config;
    private final Map<UUID, Scoreboard> playerScoreboards;

    private static final String OBJECTIVE_NAME = "rr_game";

    public ScoreboardManager(RussianRoulettePlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        this.playerScoreboards = new HashMap<>();
    }

    /**
     * Update or create a scoreboard for a player.
     * 
     * @param player           Player to update scoreboard for
     * @param playersAlive     Number of players alive
     * @param bulletsRemaining Bullets remaining in revolver
     * @param currentTurn      Current turn player name
     * @param phase            Game phase/state
     * @param timeRemaining    Turn time remaining
     */
    public void updateScoreboard(Player player, int playersAlive, int bulletsRemaining,
            String currentTurn, String phase, int timeRemaining) {
        if (!config.isScoreboardEnabled()) {
            return;
        }

        Scoreboard scoreboard = playerScoreboards.get(player.getUniqueId());

        if (scoreboard == null) {
            scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
            playerScoreboards.put(player.getUniqueId(), scoreboard);
        }

        // Get or create objective
        Objective objective = scoreboard.getObjective(OBJECTIVE_NAME);
        if (objective != null) {
            objective.unregister();
        }

        objective = scoreboard.registerNewObjective(OBJECTIVE_NAME, Criteria.DUMMY, config.getScoreboardTitle());
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        // Clear old entries
        for (String entry : scoreboard.getEntries()) {
            scoreboard.resetScores(entry);
        }

        // Add scoreboard lines (scores go from high to low for order)
        int score = 10;

        // Empty line for spacing
        objective.getScore("§8§m----------").setScore(score--);

        // Current Turn
        String turnLine = config.getRawMessage("scoreboardTurn")
                .replace("%turn%", currentTurn);
        objective.getScore(turnLine).setScore(score--);

        // Empty line
        objective.getScore("§r").setScore(score--);

        // Players Alive
        String aliveLine = config.getRawMessage("scoreboardPlayers")
                .replace("%alive%", String.valueOf(playersAlive));
        objective.getScore(aliveLine).setScore(score--);

        // Bullets Remaining
        String bulletsLine = config.getRawMessage("scoreboardBullets")
                .replace("%bullets%", String.valueOf(bulletsRemaining));
        objective.getScore(bulletsLine).setScore(score--);

        // Empty line
        objective.getScore("§r§r").setScore(score--);

        // Phase
        String phaseLine = config.getRawMessage("scoreboardPhase")
                .replace("%phase%", formatPhase(phase));
        objective.getScore(phaseLine).setScore(score--);

        // Timer (only show if time remaining)
        if (timeRemaining > 0) {
            String timerLine = config.getRawMessage("scoreboardTimer")
                    .replace("%time%", String.valueOf(timeRemaining));
            objective.getScore(timerLine).setScore(score--);
        }

        // Bottom line
        objective.getScore("§8§m-----------").setScore(score);

        // Set scoreboard to player
        player.setScoreboard(scoreboard);
    }

    /**
     * Remove a player's scoreboard.
     * 
     * @param player Player to remove scoreboard from
     */
    public void removeScoreboard(Player player) {
        playerScoreboards.remove(player.getUniqueId());

        // Reset to main scoreboard
        if (Bukkit.getScoreboardManager() != null) {
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
    }

    /**
     * Remove all scoreboards (used on plugin disable).
     */
    public void removeAllScoreboards() {
        for (UUID playerId : playerScoreboards.keySet()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                if (Bukkit.getScoreboardManager() != null) {
                    player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
                }
            }
        }
        playerScoreboards.clear();
    }

    /**
     * Format the game phase for display.
     * 
     * @param phase Raw phase name
     * @return Formatted phase name
     */
    private String formatPhase(String phase) {
        switch (phase) {
            case "WAITING":
                return "§eWaiting";
            case "STARTING":
                return "§6Starting";
            case "IN_PROGRESS":
                return "§cIn Progress";
            case "ENDED":
                return "§7Ended";
            default:
                return "§7" + phase;
        }
    }
}

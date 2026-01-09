package com.example.russianroulette.commands;

import com.example.russianroulette.RussianRoulettePlugin;
import com.example.russianroulette.config.ConfigManager;
import com.example.russianroulette.game.Game;
import com.example.russianroulette.game.GameManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Main command handler for Russian Roulette.
 * Handles all /rr subcommands.
 */
public class RRCommand implements CommandExecutor, TabCompleter {

    private final RussianRoulettePlugin plugin;
    private final ConfigManager config;
    private final GameManager gameManager;

    private static final List<String> SUBCOMMANDS = Arrays.asList(
            "start", "join", "leave", "forceend", "reload", "setseat", "setcenter");

    private static final List<String> ADMIN_SUBCOMMANDS = Arrays.asList(
            "forceend", "reload", "setseat", "setcenter");

    public RRCommand(RussianRoulettePlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        this.gameManager = plugin.getGameManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "start":
                handleStart(sender);
                break;
            case "join":
                handleJoin(sender);
                break;
            case "leave":
                handleLeave(sender);
                break;
            case "forceend":
                handleForceEnd(sender);
                break;
            case "reload":
                handleReload(sender);
                break;
            case "setseat":
                handleSetSeat(sender, args);
                break;
            case "setcenter":
                handleSetCenter(sender);
                break;
            default:
                sender.sendMessage(config.getMessage("unknownCommand"));
                break;
        }

        return true;
    }

    /**
     * Handle /rr start command.
     */
    private void handleStart(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(config.getMessage("playerOnly"));
            return;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("russianroulette.start")) {
            player.sendMessage(config.getMessage("noPermission"));
            return;
        }

        // Check if player is already in a game
        if (gameManager.isPlayerInGame(player)) {
            player.sendMessage(config.getMessage("alreadyInGame"));
            return;
        }

        // Check if a game already exists and multiple games aren't allowed
        if (!config.allowMultipleGames() && gameManager.hasActiveGames()) {
            // Try to join existing game instead
            Game existingGame = gameManager.getWaitingGame();
            if (existingGame != null) {
                player.sendMessage(config.getMessage("gameAlreadyExists"));
                return;
            }
        }

        // Create new game
        Game game = gameManager.createGame();
        if (game == null) {
            player.sendMessage(config.getMessage("gameAlreadyExists"));
            return;
        }

        // Add the creator to the game
        if (gameManager.addPlayerToGame(player, game)) {
            player.sendMessage(config.getMessage("gameCreated"));
        }
    }

    /**
     * Handle /rr join command.
     */
    private void handleJoin(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(config.getMessage("playerOnly"));
            return;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("russianroulette.play")) {
            player.sendMessage(config.getMessage("noPermission"));
            return;
        }

        // Check if player is already in a game
        if (gameManager.isPlayerInGame(player)) {
            player.sendMessage(config.getMessage("alreadyInGame"));
            return;
        }

        // Find a waiting game
        Game game = gameManager.getWaitingGame();
        if (game == null) {
            player.sendMessage(config.getMessage("noActiveGame"));
            return;
        }

        // Check if game is full
        if (game.getPlayerCount() >= config.getMaxPlayers()) {
            player.sendMessage(config.getMessage("maxPlayersReached")
                    .replace("%max%", String.valueOf(config.getMaxPlayers())));
            return;
        }

        // Add player to game
        if (gameManager.addPlayerToGame(player, game)) {
            // Check if we have enough players to start
            if (game.getPlayerCount() >= config.getMinPlayers()) {
                // Auto-start the game
                if (!game.start()) {
                    player.sendMessage(config.getMessage("notEnoughPlayers")
                            .replace("%min%", String.valueOf(config.getMinPlayers()))
                            .replace("%players%", String.valueOf(game.getPlayerCount())));
                }
            }
        }
    }

    /**
     * Handle /rr leave command.
     */
    private void handleLeave(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(config.getMessage("playerOnly"));
            return;
        }

        Player player = (Player) sender;

        // Check if player is in a game
        if (!gameManager.isPlayerInGame(player)) {
            player.sendMessage(config.getMessage("notInGame"));
            return;
        }

        // Try to remove player (will fail if it's their turn)
        if (!gameManager.removePlayerFromGame(player, false)) {
            // Message already sent in removePlayer
        }
    }

    /**
     * Handle /rr forceend command.
     */
    private void handleForceEnd(CommandSender sender) {
        if (!sender.hasPermission("russianroulette.admin")) {
            sender.sendMessage(config.getMessage("noPermission"));
            return;
        }

        if (!gameManager.hasActiveGames()) {
            sender.sendMessage(config.getMessage("noActiveGame"));
            return;
        }

        // End all games
        gameManager.endAllGames();
        sender.sendMessage(config.getMessage("gameForceEnded"));
    }

    /**
     * Handle /rr reload command.
     */
    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("russianroulette.admin")) {
            sender.sendMessage(config.getMessage("noPermission"));
            return;
        }

        plugin.reload();
        sender.sendMessage(config.getMessage("reloadSuccess"));
    }

    /**
     * Send help message to sender.
     */
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(config.getMessage("helpHeader"));
        sender.sendMessage(config.getMessage("helpBranding"));
        sender.sendMessage("");
        sender.sendMessage(config.colorize("&e/rr start &7- Start a new game"));
        sender.sendMessage(config.colorize("&e/rr join &7- Join a waiting game"));
        sender.sendMessage(config.colorize("&e/rr leave &7- Leave current game"));

        if (sender.hasPermission("russianroulette.admin")) {
            sender.sendMessage(config.colorize("&e/rr forceend &7- Force end all games"));
            sender.sendMessage(config.colorize("&e/rr reload &7- Reload configuration"));
            sender.sendMessage(config.colorize("&e/rr setseat <1-6> &7- Set seat position"));
            sender.sendMessage(config.colorize("&e/rr setcenter &7- Set table center"));
        }
    }

    /**
     * Handle /rr setseat <number> command.
     */
    private void handleSetSeat(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(config.getMessage("playerOnly"));
            return;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("russianroulette.admin")) {
            player.sendMessage(config.getMessage("noPermission"));
            return;
        }

        if (args.length < 2) {
            player.sendMessage(config.colorize("&cUsage: /rr setseat <1-6>"));
            return;
        }

        int seatNumber;
        try {
            seatNumber = Integer.parseInt(args[1]);
            if (seatNumber < 1 || seatNumber > 6) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            player.sendMessage(config.colorize("&cInvalid seat number! Use 1-6."));
            return;
        }

        // Save the seat location
        config.setSeatLocation(seatNumber, player.getLocation());
        player.sendMessage(config.colorize("&aSeat " + seatNumber + " set to your current location!"));
        player.sendMessage(config.colorize("&7X: " + String.format("%.1f", player.getLocation().getX()) +
                ", Y: " + String.format("%.1f", player.getLocation().getY()) +
                ", Z: " + String.format("%.1f", player.getLocation().getZ()) +
                ", Yaw: " + String.format("%.0f", player.getLocation().getYaw())));
    }

    /**
     * Handle /rr setcenter command.
     */
    private void handleSetCenter(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(config.getMessage("playerOnly"));
            return;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("russianroulette.admin")) {
            player.sendMessage(config.getMessage("noPermission"));
            return;
        }

        // Save the center location
        config.setCenterLocation(player.getLocation());
        player.sendMessage(config.colorize("&aTable center set to your current location!"));
        player.sendMessage(config.colorize("&7X: " + String.format("%.1f", player.getLocation().getX()) +
                ", Y: " + String.format("%.1f", player.getLocation().getY()) +
                ", Z: " + String.format("%.1f", player.getLocation().getZ())));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String input = args[0].toLowerCase();

            List<String> completions = new ArrayList<>();
            for (String sub : SUBCOMMANDS) {
                if (sub.startsWith(input)) {
                    // Filter admin commands for non-admins
                    if (ADMIN_SUBCOMMANDS.contains(sub) && !sender.hasPermission("russianroulette.admin")) {
                        continue;
                    }
                    completions.add(sub);
                }
            }
            return completions;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("setseat")) {
            List<String> seatNumbers = Arrays.asList("1", "2", "3", "4", "5", "6");
            String input = args[1];
            List<String> completions = new ArrayList<>();
            for (String num : seatNumbers) {
                if (num.startsWith(input)) {
                    completions.add(num);
                }
            }
            return completions;
        }

        return new ArrayList<>();
    }
}

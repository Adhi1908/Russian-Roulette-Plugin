package com.example.russianroulette.game;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Stores data for a player in a game.
 */
public class PlayerData {

    private final UUID playerId;
    private final String playerName;
    private final Location originalLocation;
    private boolean alive;
    private int turnPosition;

    public PlayerData(Player player) {
        this.playerId = player.getUniqueId();
        this.playerName = player.getName();
        this.originalLocation = player.getLocation().clone();
        this.alive = true;
        this.turnPosition = 0;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public Location getOriginalLocation() {
        return originalLocation;
    }

    public boolean isAlive() {
        return alive;
    }

    public void setAlive(boolean alive) {
        this.alive = alive;
    }

    public int getTurnPosition() {
        return turnPosition;
    }

    public void setTurnPosition(int turnPosition) {
        this.turnPosition = turnPosition;
    }
}

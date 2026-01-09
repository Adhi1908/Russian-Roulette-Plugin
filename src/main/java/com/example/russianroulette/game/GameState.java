package com.example.russianroulette.game;

/**
 * Represents the current state of a game.
 */
public enum GameState {
    /**
     * Game is waiting for players to join.
     */
    WAITING,

    /**
     * Game is in countdown phase before starting.
     */
    STARTING,

    /**
     * Game is actively in progress.
     */
    IN_PROGRESS,

    /**
     * Game has ended.
     */
    ENDED
}

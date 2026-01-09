package com.example.russianroulette.game;

/**
 * Game modes with different bullet counts.
 */
public enum GameMode {
    /**
     * Classic mode - 1 bullet in the chamber.
     */
    CLASSIC(1),

    /**
     * Hardcore mode - 2 bullets in the chamber.
     */
    HARDCORE(2),

    /**
     * Insane mode - 3 bullets in the chamber.
     */
    INSANE(3);

    private final int bulletCount;

    GameMode(int bulletCount) {
        this.bulletCount = bulletCount;
    }

    /**
     * Get the number of bullets for this game mode.
     * 
     * @return Number of bullets
     */
    public int getBulletCount() {
        return bulletCount;
    }
}

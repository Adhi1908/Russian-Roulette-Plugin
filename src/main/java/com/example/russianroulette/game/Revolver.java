package com.example.russianroulette.game;

import java.util.Random;

/**
 * Represents a revolver with 6 chambers.
 * Handles bullet placement and trigger pulling logic.
 */
public class Revolver {

    private static final int CHAMBER_COUNT = 6;
    private final Random random = new Random();

    private boolean[] chambers;
    private int currentChamber;
    private int bulletsLoaded;
    private int bulletsRemaining;

    /**
     * Create a new revolver with the specified number of bullets.
     * 
     * @param bulletCount Number of bullets to load
     */
    public Revolver(int bulletCount) {
        this.bulletsLoaded = Math.min(bulletCount, CHAMBER_COUNT - 1);
        this.chambers = new boolean[CHAMBER_COUNT];
        this.currentChamber = 0;
        shuffle();
    }

    /**
     * Shuffle the cylinder - randomly place bullets in chambers.
     */
    public void shuffle() {
        // Reset all chambers
        for (int i = 0; i < CHAMBER_COUNT; i++) {
            chambers[i] = false;
        }

        // Randomly place bullets
        int bulletsPlaced = 0;
        while (bulletsPlaced < bulletsLoaded) {
            int position = random.nextInt(CHAMBER_COUNT);
            if (!chambers[position]) {
                chambers[position] = true;
                bulletsPlaced++;
            }
        }

        // Randomize starting position
        currentChamber = random.nextInt(CHAMBER_COUNT);
        bulletsRemaining = bulletsLoaded;
    }

    /**
     * Pull the trigger - advance to next chamber and check for bullet.
     * 
     * @return true if bullet fires (player dies), false if empty
     */
    public boolean pullTrigger() {
        boolean hit = chambers[currentChamber];

        if (hit) {
            chambers[currentChamber] = false;
            bulletsRemaining--;
        }

        // Advance to next chamber
        currentChamber = (currentChamber + 1) % CHAMBER_COUNT;

        return hit;
    }

    /**
     * Get the number of bullets remaining in the revolver.
     * 
     * @return Bullets remaining
     */
    public int getBulletsRemaining() {
        return bulletsRemaining;
    }

    /**
     * Get the total number of chambers.
     * 
     * @return Chamber count (always 6)
     */
    public int getChamberCount() {
        return CHAMBER_COUNT;
    }

    /**
     * Get the number of chambers that haven't been fired yet.
     * 
     * @return Remaining chamber count
     */
    public int getRemainingChambers() {
        return CHAMBER_COUNT;
    }

    /**
     * Check if the revolver has any bullets left.
     * 
     * @return true if bullets remain
     */
    public boolean hasBullets() {
        return bulletsRemaining > 0;
    }

    /**
     * Get the current chamber index (for display purposes).
     * 
     * @return Current chamber index (0-5)
     */
    public int getCurrentChamberIndex() {
        return currentChamber;
    }
}

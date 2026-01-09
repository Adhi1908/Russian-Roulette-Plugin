package com.example.russianroulette.effects;

import com.example.russianroulette.RussianRoulettePlugin;
import com.example.russianroulette.config.ConfigManager;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Manages visual and audio effects for the game.
 */
public class EffectsManager {

    private final RussianRoulettePlugin plugin;
    private final ConfigManager config;

    public EffectsManager(RussianRoulettePlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
    }

    /**
     * Play death effects for a player.
     * 
     * @param player The player who died
     */
    public void playDeathEffects(Player player) {
        Location loc = player.getLocation();

        // Play gunshot sound
        player.getWorld().playSound(
                loc,
                config.getSound("triggerFire"),
                config.getSoundVolume("triggerFire"),
                config.getSoundPitch("triggerFire"));

        // Blood particles
        if (config.isBloodParticlesEnabled()) {
            playBloodParticles(player);
        }

        // Smoke particles
        if (config.isSmokeParticlesEnabled()) {
            playSmokeParticles(player);
        }

        // Show BANG! title
        if (config.isShowBangTitle()) {
            player.sendTitle(
                    config.getRawMessage("bangTitle"),
                    config.getRawMessage("bangSubtitle"),
                    5, 40, 20);
        }

        // Slow-motion effect
        if (config.isSlowMotionDeathEnabled()) {
            playSlowMotionEffect(player);
        }
    }

    /**
     * Play survival effects for a player.
     * 
     * @param player The player who survived
     */
    public void playSurvivalEffects(Player player) {
        Location loc = player.getLocation();

        // Play empty click sound
        player.getWorld().playSound(
                loc,
                config.getSound("triggerEmpty"),
                config.getSoundVolume("triggerEmpty"),
                config.getSoundPitch("triggerEmpty"));

        // Small smoke puff
        if (config.isSmokeParticlesEnabled()) {
            player.getWorld().spawnParticle(
                    Particle.SMOKE_NORMAL,
                    loc.add(0, 1.5, 0),
                    10,
                    0.1, 0.1, 0.1,
                    0.02);
        }
    }

    /**
     * Play blood particle effects.
     * 
     * @param player Player location
     */
    private void playBloodParticles(Player player) {
        Location loc = player.getLocation().add(0, 1, 0);
        int count = config.getBloodParticleCount();

        // Use REDSTONE dust particles with red color
        Particle.DustOptions dustOptions = new Particle.DustOptions(Color.RED, 1.5f);

        // Burst of blood particles
        player.getWorld().spawnParticle(
                Particle.REDSTONE,
                loc,
                count,
                0.5, 0.5, 0.5,
                0.1,
                dustOptions);

        // Additional splatter effect
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= 5) {
                    cancel();
                    return;
                }

                Location splatterLoc = player.getLocation().add(0, 0.5, 0);
                player.getWorld().spawnParticle(
                        Particle.REDSTONE,
                        splatterLoc,
                        15,
                        0.8, 0.2, 0.8,
                        0.05,
                        dustOptions);

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    /**
     * Play smoke particle effects.
     * 
     * @param player Player location
     */
    private void playSmokeParticles(Player player) {
        Location loc = player.getLocation().add(0, 1.5, 0);

        // Large smoke explosion
        player.getWorld().spawnParticle(
                Particle.EXPLOSION_LARGE,
                loc,
                1,
                0, 0, 0,
                0);

        // Trailing smoke
        player.getWorld().spawnParticle(
                Particle.SMOKE_LARGE,
                loc,
                30,
                0.3, 0.3, 0.3,
                0.05);
    }

    /**
     * Apply slow-motion effect on death.
     * 
     * @param player Player to apply effect to
     */
    private void playSlowMotionEffect(Player player) {
        int duration = config.getSlowMotionDuration();

        // Apply slowness
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.SLOW,
                duration,
                3, // Slowness IV
                false,
                false,
                false));

        // Brief blindness for dramatic effect
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.BLINDNESS,
                duration / 2,
                0,
                false,
                false,
                false));

        // Darkness overlay effect (1.19+)
        try {
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.DARKNESS,
                    duration / 2,
                    0,
                    false,
                    false,
                    false));
        } catch (NoSuchFieldError | IllegalArgumentException e) {
            // DARKNESS doesn't exist in older versions, ignore
        }
    }

    /**
     * Play celebration effects for winner.
     * 
     * @param player The winning player
     */
    public void playWinnerEffects(Player player) {
        Location loc = player.getLocation();

        // Firework-like particles
        player.getWorld().spawnParticle(
                Particle.TOTEM,
                loc.add(0, 2, 0),
                100,
                1, 1, 1,
                0.5);

        // Play victory sound
        player.playSound(
                loc,
                org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE,
                1.0f,
                1.0f);

        // Continuous sparkle effect
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= 40 || !player.isOnline()) {
                    cancel();
                    return;
                }

                Location particleLoc = player.getLocation().add(0, 2, 0);
                player.getWorld().spawnParticle(
                        Particle.FIREWORKS_SPARK,
                        particleLoc,
                        10,
                        0.5, 0.5, 0.5,
                        0.1);

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }
}

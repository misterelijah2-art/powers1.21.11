package powerful.powers.ability.abilities;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import powerful.powers.ability.AbilityData;

import java.util.List;

/**
 * GLACIAL PULSE
 * Tap: freeze all entities in 6-block radius (slowness II, 3s).
 * Full charge: 12-block radius + slowness IV, 5s.
 */
public class GlacialPulseAbility {

    public static void execute(Player player, AbilityData data, ServerLevel level) {
        boolean fullCharge = data.getChargeRatio() >= 1.0f;
        double radius = fullCharge ? 12.0 : 6.0;
        int amplifier = fullCharge ? 3 : 1;   // slowness IV vs II
        int duration  = fullCharge ? 100 : 60; // 5s vs 3s

        // Pulse shockwave particles
        for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 24) {
            for (double r = 1; r <= radius; r += 1.5) {
                double px = player.getX() + Math.cos(angle) * r;
                double pz = player.getZ() + Math.sin(angle) * r;
                level.sendParticles(ParticleTypes.SNOWFLAKE,
                        px, player.getY() + 0.1, pz, 1, 0.0, 0.1, 0.0, 0.0);
                level.sendParticles(ParticleTypes.ITEM_SNOWBALL,
                        px, player.getY() + 0.5, pz, 1, 0.0, 0.05, 0.0, 0.0);
            }
        }

        level.playSound(null, player.blockPosition(),
                SoundEvents.POWDER_SNOW_PLACE,
                SoundSource.PLAYERS, 1.5f, 0.5f);
        level.playSound(null, player.blockPosition(),
                SoundEvents.GLASS_BREAK,
                SoundSource.PLAYERS, 0.8f, 0.4f);

        List<LivingEntity> targets = level.getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(radius),
                e -> e != player && e.isAlive()
        );
        for (LivingEntity target : targets) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, amplifier));
            // Freeze visual: apply freeze ticks
            target.setTicksFrozen(Math.max(target.getTicksFrozen(), 140));
            // Ice crystal burst on each target
            level.sendParticles(ParticleTypes.SNOWFLAKE,
                    target.getX(), target.getY() + 1.0, target.getZ(),
                    20, 0.3, 0.5, 0.3, 0.05);
        }
    }

    /** Ambient passive: ice crystal particles orbit feet. */
    public static void doPassive(Player player, ServerLevel level, long tick) {
        if (tick % 4 != 0) return;
        double angle = (tick * 7.0) * Math.PI / 180.0;
        double r = 0.6;
        double px = player.getX() + Math.cos(angle) * r;
        double pz = player.getZ() + Math.sin(angle) * r;
        level.sendParticles(ParticleTypes.SNOWFLAKE,
                px, player.getY() + 0.1, pz, 1, 0.0, 0.02, 0.0, 0.0);
        level.sendParticles(ParticleTypes.ITEM_SNOWBALL,
                px, player.getY() + 0.3, pz, 1, 0.0, 0.01, 0.0, 0.0);
        if (tick % 50 == 0) {
            level.playSound(null, player.blockPosition(),
                    SoundEvents.POWDER_SNOW_STEP,
                    SoundSource.PLAYERS, 0.15f, 1.8f);
        }
    }
}

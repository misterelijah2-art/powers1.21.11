package powerful.powers.ability.abilities;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Fireball;
import net.minecraft.world.phys.Vec3;
import powerful.powers.ability.AbilityData;

import java.util.Comparator;
import java.util.List;

/**
 * SOULFLARE
 * Tap: launch 1 homing fireball toward nearest player.
 * Full charge: launch 3 fireballs in a spread.
 */
public class SoulflareAbility {

    private static final double FIREBALL_SPEED = 0.4;
    private static final double TRACK_RANGE    = 48.0;

    public static void execute(Player player, AbilityData data, ServerLevel level) {
        boolean fullCharge = data.getChargeRatio() >= 1.0f;
        int count = fullCharge ? 3 : 1;

        // Find nearest target
        LivingEntity target = findNearestTarget(player, level);

        level.playSound(null, player.blockPosition(),
                SoundEvents.BLAZE_SHOOT,
                SoundSource.PLAYERS, 1.0f, 0.5f);

        for (int i = 0; i < count; i++) {
            Vec3 dir = computeDirection(player, target, count, i);
            spawnFireball(player, dir, level);
        }

        // Muzzle flash
        Vec3 pos = player.getEyePosition();
        level.sendParticles(ParticleTypes.FLAME,
                pos.x, pos.y, pos.z, 12, 0.2, 0.2, 0.2, 0.05);
        level.sendParticles(ParticleTypes.LAVA,
                pos.x, pos.y, pos.z, 6, 0.1, 0.1, 0.1, 0.0);
    }

    private static Vec3 computeDirection(Player player, LivingEntity target, int total, int index) {
        Vec3 base;
        if (target != null) {
            base = target.getEyePosition().subtract(player.getEyePosition()).normalize();
        } else {
            base = player.getLookAngle().normalize();
        }
        if (total == 1) return base;
        // spread: -15, 0, +15 degrees around Y axis
        double spreadDeg = (index - 1) * 15.0;
        double rad = Math.toRadians(spreadDeg);
        double cos = Math.cos(rad), sin = Math.sin(rad);
        return new Vec3(
                base.x * cos - base.z * sin,
                base.y,
                base.x * sin + base.z * cos
        ).normalize();
    }

    private static void spawnFireball(Player player, Vec3 dir, ServerLevel level) {
        Vec3 eye = player.getEyePosition().add(dir.scale(1.5));
        Fireball fb = new Fireball(level, player,
                dir.x * FIREBALL_SPEED,
                dir.y * FIREBALL_SPEED,
                dir.z * FIREBALL_SPEED);
        fb.setPos(eye.x, eye.y, eye.z);
        fb.setOwner(player);
        level.addFreshEntity(fb);
    }

    private static LivingEntity findNearestTarget(Player shooter, ServerLevel level) {
        List<LivingEntity> nearby = level.getEntitiesOfClass(
                LivingEntity.class,
                shooter.getBoundingBox().inflate(TRACK_RANGE),
                e -> e != shooter && e.isAlive()
        );
        return nearby.stream()
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(shooter)))
                .orElse(null);
    }

    /** Ambient passive: orange embers float up from body. */
    public static void doPassive(Player player, ServerLevel level, long tick) {
        if (tick % 5 != 0) return;
        double x = player.getX() + (level.random.nextDouble() - 0.5) * 0.5;
        double y = player.getY() + level.random.nextDouble() * player.getBbHeight();
        double z = player.getZ() + (level.random.nextDouble() - 0.5) * 0.5;
        level.sendParticles(ParticleTypes.FLAME, x, y, z, 1, 0.0, 0.04, 0.0, 0.0);
        level.sendParticles(ParticleTypes.LAVA,  x, y + 0.5, z, 1, 0.1, 0.05, 0.1, 0.0);
        if (tick % 60 == 0) {
            level.playSound(null, player.blockPosition(),
                    SoundEvents.BLAZE_AMBIENT,
                    SoundSource.PLAYERS, 0.1f, 1.5f);
        }
    }
}

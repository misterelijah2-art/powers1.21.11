package powerful.powers.ability.abilities;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import powerful.powers.ability.AbilityData;

/**
 * VOIDSTEP
 * Tap: teleport 15 blocks in look direction, leave void-smoke trail.
 * Full charge: teleport 30 blocks + blindness on arrival.
 */
public class VoidstepAbility {

    public static void execute(Player player, AbilityData data, ServerLevel level) {
        boolean fullCharge = data.getChargeRatio() >= 1.0f;
        double distance = fullCharge ? 30.0 : 15.0;

        Vec3 look = player.getLookAngle().normalize();
        Vec3 origin = player.position();

        // Spawn shadow trail particles along path
        int steps = (int)(distance * 2);
        for (int i = 1; i <= steps; i++) {
            double t = (double) i / steps * distance;
            double tx = origin.x + look.x * t;
            double ty = origin.y + look.y * t + player.getBbHeight() * 0.5;
            double tz = origin.z + look.z * t;
            level.sendParticles(ParticleTypes.PORTAL,
                    tx, ty, tz, 3, 0.15, 0.15, 0.15, 0.0);
            level.sendParticles(ParticleTypes.SMOKE,
                    tx, ty, tz, 2, 0.1, 0.1, 0.1, 0.02);
        }

        // Departure burst
        level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                origin.x, origin.y + 1.0, origin.z, 30, 0.3, 0.5, 0.3, 0.1);
        level.playSound(null, player.blockPosition(),
                SoundEvents.CHORUS_FRUIT_TELEPORT,
                SoundSource.PLAYERS, 1.0f, 0.6f);

        // Calculate safe destination (step-back from walls)
        Vec3 dest = findSafeTeleport(player, look, distance, level);
        player.teleportTo(dest.x, dest.y, dest.z);

        // Arrival burst
        level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                dest.x, dest.y + 1.0, dest.z, 40, 0.4, 0.6, 0.4, 0.15);
        level.sendParticles(ParticleTypes.SQUID_INK,
                dest.x, dest.y + 1.0, dest.z, 20, 0.3, 0.3, 0.3, 0.05);
        level.playSound(null, player.blockPosition(),
                SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS, 1.0f, 0.8f);

        if (fullCharge) {
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
        }
    }

    private static Vec3 findSafeTeleport(Player player, Vec3 look, double maxDist, ServerLevel level) {
        Vec3 origin = player.position();
        for (double d = maxDist; d >= 1.0; d -= 0.5) {
            double tx = origin.x + look.x * d;
            double ty = origin.y + look.y * d;
            double tz = origin.z + look.z * d;
            // Check if destination block column is open (2 blocks tall)
            net.minecraft.core.BlockPos feet = new net.minecraft.core.BlockPos(
                    (int)Math.floor(tx), (int)Math.floor(ty), (int)Math.floor(tz));
            net.minecraft.core.BlockPos head = feet.above();
            if (level.getBlockState(feet).isAir() && level.getBlockState(head).isAir()) {
                return new Vec3(tx, ty, tz);
            }
        }
        return origin;
    }

    /** Ambient passive: void particles drift off body when ability is ready. */
    public static void doPassive(Player player, ServerLevel level, long tick) {
        if (tick % 4 != 0) return;
        double x = player.getX() + (level.random.nextDouble() - 0.5) * 0.6;
        double y = player.getY() + level.random.nextDouble() * player.getBbHeight();
        double z = player.getZ() + (level.random.nextDouble() - 0.5) * 0.6;
        level.sendParticles(ParticleTypes.PORTAL, x, y, z, 1, 0.0, 0.05, 0.0, 0.0);
        if (tick % 40 == 0) {
            level.playSound(null, player.blockPosition(),
                    SoundEvents.ENDERMAN_AMBIENT,
                    SoundSource.PLAYERS, 0.08f, 2.0f);
        }
    }
}

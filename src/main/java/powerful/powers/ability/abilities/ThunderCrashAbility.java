package powerful.powers.ability.abilities;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import powerful.powers.ability.AbilityData;

import java.util.List;

/**
 * THUNDER CRASH
 * Tap: strike all enemies within 8 blocks with lightning.
 * Full charge: 8-block AoE shockwave knockback + lightning.
 */
public class ThunderCrashAbility {

    public static void execute(Player player, AbilityData data, ServerLevel level) {
        boolean fullCharge = data.getChargeRatio() >= 1.0f;
        double radius = 8.0;

        // Ground shockwave ring
        for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 20) {
            double px = player.getX() + Math.cos(angle) * radius;
            double pz = player.getZ() + Math.sin(angle) * radius;
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    px, player.getY() + 0.2, pz, 2, 0.0, 0.1, 0.0, 0.0);
        }
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                player.getX(), player.getY() + 0.5, player.getZ(),
                60, 0.3, 0.1, 0.3, 0.3);
        level.playSound(null, player.blockPosition(),
                SoundEvents.LIGHTNING_BOLT_THUNDER,
                SoundSource.PLAYERS, 1.0f, 1.0f);
        level.playSound(null, player.blockPosition(),
                SoundEvents.LIGHTNING_BOLT_IMPACT,
                SoundSource.PLAYERS, 1.0f, 0.9f);

        List<LivingEntity> targets = level.getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(radius),
                e -> e != player && e.isAlive()
        );

        for (LivingEntity target : targets) {
            // Summon lightning bolt at target
            net.minecraft.world.entity.LightningBolt bolt =
                    new net.minecraft.world.entity.LightningBolt(
                            net.minecraft.world.entity.EntityType.LIGHTNING_BOLT, level);
            bolt.moveTo(target.getX(), target.getY(), target.getZ());
            bolt.setCause(player instanceof net.minecraft.server.level.ServerPlayer sp ? sp : null);
            bolt.setVisualOnly(false);
            level.addFreshEntity(bolt);

            if (fullCharge) {
                // Knockback away from player
                Vec3 knockDir = target.position().subtract(player.position()).normalize();
                target.push(knockDir.x * 1.8, 0.6, knockDir.z * 1.8);
                target.hurtMarked = true;
            }
        }
    }

    /** Ambient passive: crackling yellow sparks on hands (around player). */
    public static void doPassive(Player player, ServerLevel level, long tick) {
        if (tick % 3 != 0) return;
        double x = player.getX() + (level.random.nextDouble() - 0.5) * 0.4;
        double y = player.getY() + 0.8 + level.random.nextDouble() * 0.8;
        double z = player.getZ() + (level.random.nextDouble() - 0.5) * 0.4;
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
        if (tick % 30 == 0) {
            level.playSound(null, player.blockPosition(),
                    SoundEvents.LIGHTNING_BOLT_THUNDER,
                    SoundSource.PLAYERS, 0.04f, 2.0f);
        }
    }
}

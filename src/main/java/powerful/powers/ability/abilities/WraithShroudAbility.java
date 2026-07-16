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

import java.util.Comparator;
import java.util.List;

/**
 * WRAITH SHROUD
 * Tap: invisibility + Speed II for 5s.
 * Full charge: 10s + nausea on nearest enemy.
 */
public class WraithShroudAbility {

    public static void execute(Player player, AbilityData data, ServerLevel level) {
        boolean fullCharge = data.getChargeRatio() >= 1.0f;
        int duration = fullCharge ? 200 : 100;

        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, duration, 0));
        // MOVEMENT_SPEED was renamed to SPEED in 1.20.5+
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, duration, 1));

        level.sendParticles(ParticleTypes.CLOUD,
                player.getX(), player.getY() + 1.0, player.getZ(),
                30, 0.5, 0.7, 0.5, 0.08);
        level.sendParticles(ParticleTypes.ENCHANT,
                player.getX(), player.getY() + 1.0, player.getZ(),
                20, 0.4, 0.6, 0.4, 0.1);
        level.playSound(null, player.blockPosition(), SoundEvents.WITCH_AMBIENT,
                SoundSource.PLAYERS, 0.9f, 1.4f);
        level.playSound(null, player.blockPosition(), SoundEvents.AMBIENT_CAVE.value(),
                SoundSource.PLAYERS, 0.3f, 1.8f);

        if (fullCharge) {
            List<LivingEntity> nearby = level.getEntitiesOfClass(
                    LivingEntity.class, player.getBoundingBox().inflate(16),
                    e -> e != player && e.isAlive());
            nearby.stream()
                    .min(Comparator.comparingDouble(e -> e.distanceToSqr(player)))
                    .ifPresent(target -> {
                        // CONFUSION was renamed to NAUSEA in 1.20.5+
                        target.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 120, 0));
                        level.sendParticles(ParticleTypes.WITCH,
                                target.getX(), target.getY() + 1.0, target.getZ(),
                                15, 0.3, 0.5, 0.3, 0.0);
                    });
        }
    }

    public static void doPassive(Player player, ServerLevel level, long tick) {
        if (tick % 6 != 0) return;
        double x = player.getX() + (level.random.nextDouble() - 0.5) * 0.8;
        double y = player.getY() + level.random.nextDouble() * player.getBbHeight();
        double z = player.getZ() + (level.random.nextDouble() - 0.5) * 0.8;
        level.sendParticles(ParticleTypes.CLOUD, x, y, z, 1, 0.0, 0.03, 0.0, 0.0);
        if (tick % 70 == 0)
            level.playSound(null, player.blockPosition(), SoundEvents.WITCH_AMBIENT,
                    SoundSource.PLAYERS, 0.06f, 1.8f);
    }
}

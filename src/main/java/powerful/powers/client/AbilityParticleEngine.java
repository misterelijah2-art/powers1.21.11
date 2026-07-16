package powerful.powers.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.*;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import powerful.powers.ability.AbilityType;
import powerful.powers.powers;

/**
 * Spawns client-side passive aura particles around the local player.
 * Each ability has a distinct visual signature.
 */
@EventBusSubscriber(modid = powers.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class AbilityParticleEngine {

    @SubscribeEvent
    public static void onClientTick(LevelTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (!ClientAbilityData.hasAbility() || !ClientAbilityData.hudUnlocked) return;
        if (ClientTitleRevealState.isRevealActive()) return;

        Player player = mc.player;
        ClientLevel level = mc.level;
        AbilityType type = ClientAbilityData.ability;
        int tick = (int)(mc.level.getGameTime() & Integer.MAX_VALUE);
        int interval = intervalFor(type);

        if (tick % interval != 0) return;

        double x = player.getX();
        double y = player.getY() + 1.0;
        double z = player.getZ();
        double angle = tick * 0.2;
        double r = 0.6;
        double ox = Math.cos(angle) * r;
        double oz = Math.sin(angle) * r;

        switch (type) {
            case VOIDSTEP -> {
                level.addParticle(ParticleTypes.PORTAL,
                        x + ox, y, z + oz, 0, 0.05, 0);
                level.addParticle(ParticleTypes.PORTAL,
                        x - ox, y, z - oz, 0, 0.05, 0);
            }
            case SOULFLARE -> {
                level.addParticle(ParticleTypes.FLAME,
                        x + ox * 0.5, y - 0.2, z + oz * 0.5, 0, 0.03, 0);
                level.addParticle(ParticleTypes.SOUL_FIRE_FLAME,
                        x + ox * 0.3, y + 0.3, z + oz * 0.3, 0, 0.02, 0);
            }
            case GLACIAL_PULSE -> {
                for (int i = 0; i < 3; i++) {
                    double a2 = angle + i * (Math.PI * 2 / 3);
                    level.addParticle(ParticleTypes.SNOWFLAKE,
                            x + Math.cos(a2) * r, y - 0.3, z + Math.sin(a2) * r,
                            0, 0.01, 0);
                }
            }
            case WRAITH_SHROUD -> {
                level.addParticle(ParticleTypes.CLOUD,
                        x + ox * 0.4, y + 0.5, z + oz * 0.4,
                        0, 0.01, 0);
                level.addParticle(ParticleTypes.WITCH,
                        x + ox, y + 0.8, z + oz, 0, 0.02, 0);
            }
            case THUNDER_CRASH -> {
                level.addParticle(ParticleTypes.ELECTRIC_SPARK,
                        x + ox, y + 0.4, z + oz, 0, 0.05, 0);
            }
        }
    }

    private static int intervalFor(AbilityType type) {
        return switch (type) {
            case VOIDSTEP      -> 3;
            case SOULFLARE     -> 2;
            case GLACIAL_PULSE -> 4;
            case WRAITH_SHROUD -> 3;
            case THUNDER_CRASH -> 2;
        };
    }
}

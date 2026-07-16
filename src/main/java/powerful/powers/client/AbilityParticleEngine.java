package powerful.powers.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import powerful.powers.ability.AbilityType;
import powerful.powers.powers;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Client-side particle bursts triggered by AbilityFxPacket.
 * Runs completely on the render thread.
 */
@EventBusSubscriber(modid = powers.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class AbilityParticleEngine {

    private record Burst(AbilityType type, double x, double y, double z, int life, int max) {}

    private static final List<Burst> active = new ArrayList<>();

    public static void addBurst(AbilityType type, double x, double y, double z) {
        int maxLife = switch (type) {
            case VOIDSTEP      -> 40;
            case SOULFLARE     -> 60;
            case GLACIAL_PULSE -> 50;
            case WRAITH_SHROUD -> 20;
            case THUNDER_CRASH -> 35;
        };
        active.add(new Burst(type, x, y, z, 0, maxLife));
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (!(mc.level instanceof ClientLevel level)) return;

        Iterator<Burst> it = active.iterator();
        List<Burst> next = new ArrayList<>();
        while (it.hasNext()) {
            Burst b = it.next();
            if (b.life() >= b.max()) { it.remove(); continue; }
            tickBurst(b, level);
            next.add(new Burst(b.type(), b.x(), b.y(), b.z(), b.life() + 1, b.max()));
        }
        active.clear();
        active.addAll(next);
    }

    private static void tickBurst(Burst b, ClientLevel level) {
        switch (b.type()) {
            case VOIDSTEP      -> tickVoidstep(b, level);
            case SOULFLARE     -> tickSoulflare(b, level);
            case GLACIAL_PULSE -> tickGlacialPulse(b, level);
            case WRAITH_SHROUD -> tickWraithShroud(b, level);
            case THUNDER_CRASH -> tickThunderCrash(b, level);
        }
    }

    private static void tickVoidstep(Burst b, ClientLevel level) {
        if (b.life() % 2 != 0) return;
        double angle = b.life() * 30.0 * Math.PI / 180.0;
        for (int i = 0; i < 6; i++) {
            double a = angle + i * Math.PI / 3;
            double r = 1.0 + b.life() * 0.12;
            level.addParticle(ParticleTypes.PORTAL,
                    b.x() + Math.cos(a)*r, b.y() + 0.5, b.z() + Math.sin(a)*r,
                    0, 0.05, 0);
        }
    }

    private static void tickSoulflare(Burst b, ClientLevel level) {
        level.addParticle(ParticleTypes.FLAME,
                b.x() + (Math.random()-0.5)*1.5, b.y() + Math.random()*2, b.z() + (Math.random()-0.5)*1.5,
                0, 0.08, 0);
        if (b.life() % 3 == 0)
            level.addParticle(ParticleTypes.LAVA, b.x(), b.y()+1, b.z(), 0, 0, 0);
    }

    private static void tickGlacialPulse(Burst b, ClientLevel level) {
        if (b.life() % 2 != 0) return;
        double r = b.life() * 0.25;
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4;
            level.addParticle(ParticleTypes.SNOWFLAKE,
                    b.x() + Math.cos(a)*r, b.y() + 0.1, b.z() + Math.sin(a)*r,
                    0, 0.03, 0);
        }
    }

    private static void tickWraithShroud(Burst b, ClientLevel level) {
        level.addParticle(ParticleTypes.CLOUD,
                b.x() + (Math.random()-0.5)*2, b.y() + Math.random()*2, b.z() + (Math.random()-0.5)*2,
                0, 0.05, 0);
    }

    private static void tickThunderCrash(Burst b, ClientLevel level) {
        level.addParticle(ParticleTypes.ELECTRIC_SPARK,
                b.x() + (Math.random()-0.5)*3, b.y() + Math.random(), b.z() + (Math.random()-0.5)*3,
                (Math.random()-0.5)*0.3, Math.random()*0.3, (Math.random()-0.5)*0.3);
    }
}

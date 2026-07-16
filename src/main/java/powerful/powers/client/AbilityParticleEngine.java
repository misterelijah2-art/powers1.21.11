package powerful.powers.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ShriekParticleOption;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ClientTickEvent;
import powerful.powers.ability.AbilityType;
import powerful.powers.powers;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.Random;

/**
 * Client-side multi-frame particle engine.
 *
 * DustParticleOptions in MC 1.21.11 takes (int argbColor, float size).
 * Pack colour with: (0xFF << 24) | (r << 16) | (g << 8) | b
 * DustColorTransitionOptions takes (int fromArgb, int toArgb, float size).
 *
 * EventBusSubscriber without a bus value defaults to the MOD bus.
 * For the GAME bus (forge events like ClientTickEvent) we must use
 * NeoForge.EVENT_BUS.register() at runtime, OR use the value attribute.
 * In NeoForge 21.11 the correct annotation form is:
 *   @EventBusSubscriber(modid = ..., value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
 * BUT Bus enum was removed — the default (no bus param) IS the game bus.
 * So: no bus param = game bus. MOD bus requires Bus.MOD explicitly.
 */
@EventBusSubscriber(modid = powers.MODID)
public class AbilityParticleEngine {

    private record Burst(AbilityType type, double x, double y, double z, int ticksLeft) {}

    private static final Queue<Burst> QUEUE = new ArrayDeque<>();
    private static final Random RNG = new Random();

    // Helpers: pack float RGB (0-1) into ARGB int
    private static int rgb(float r, float g, float b) {
        return (0xFF << 24) | ((int)(r * 255) << 16) | ((int)(g * 255) << 8) | (int)(b * 255);
    }

    public static void schedule(AbilityType type, double x, double y, double z) {
        int frames = switch (type) {
            case NULL_RIFT     -> 40;
            case MAGMA_CAGE    -> 60;
            case DEATH_MARK    -> 50;
            case SHADOW_STRIKE -> 20;
            case STORM_CHAIN   -> 35;
        };
        QUEUE.add(new Burst(type, x, y, z, frames));
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || QUEUE.isEmpty()) return;

        Queue<Burst> next = new ArrayDeque<>();
        int processed = 0;
        for (Burst b : QUEUE) {
            if (processed < 3) {
                tickBurst(b, level);
                processed++;
                if (b.ticksLeft() > 1)
                    next.add(new Burst(b.type(), b.x(), b.y(), b.z(), b.ticksLeft() - 1));
            } else {
                next.add(b);
            }
        }
        QUEUE.clear();
        QUEUE.addAll(next);
    }

    private static void tickBurst(Burst b, ClientLevel level) {
        switch (b.type()) {
            case NULL_RIFT     -> tickNullRift(b, level);
            case MAGMA_CAGE    -> tickMagmaCage(b, level);
            case DEATH_MARK    -> tickDeathMark(b, level);
            case SHADOW_STRIKE -> tickShadowStrike(b, level);
            case STORM_CHAIN   -> tickStormChain(b, level);
        }
    }

    // ── NULL RIFT ──────────────────────────────────────────────────────────────
    private static void tickNullRift(Burst b, ClientLevel level) {
        int frame = b.ticksLeft();
        boolean imploding = frame > 20;
        double radius = imploding ? (frame / 40.0) * 4.0 : ((20 - frame) / 20.0) * 5.0;
        int count = imploding ? 6 : 10;

        // bright violet -> near black
        int voidFrom = rgb(0.48f, 0f, 1f);
        int voidTo   = rgb(0.1f,  0f, 0.1f);
        DustColorTransitionOptions voidDust = new DustColorTransitionOptions(voidFrom, voidTo, imploding ? 1.2f : 1.8f);
        DustParticleOptions rimDust = new DustParticleOptions(rgb(0.55f, 0f, 0.9f), 0.8f);

        for (int i = 0; i < count; i++) {
            double angle = (i / (double) count) * Math.PI * 2 + (frame * 0.15);
            double px = b.x() + Math.cos(angle) * radius + (RNG.nextDouble() - 0.5) * 0.4;
            double py = b.y() + 1 + (RNG.nextDouble() - 0.5) * (imploding ? 1.5 : 2.5);
            double pz = b.z() + Math.sin(angle) * radius + (RNG.nextDouble() - 0.5) * 0.4;
            level.addParticle(voidDust, px, py, pz, 0, 0, 0);
            if (i % 2 == 0) level.addParticle(rimDust, px, py + 0.3, pz, 0, 0.05, 0);
        }
        if (frame == 19) {
            for (int i = 0; i < 6; i++)
                level.addParticle(new ShriekParticleOption(i * 3), b.x(), b.y() + 1, b.z(), 0, 0.2, 0);
        }
    }

    // ── MAGMA CAGE ─────────────────────────────────────────────────────────────
    private static void tickMagmaCage(Burst b, ClientLevel level) {
        int frame = b.ticksLeft();
        double pulse = 2.5 + Math.sin(frame * 0.25) * 1.0;

        // molten orange -> hot yellow
        int magmaFrom = rgb(1f, 0.42f, 0f);
        int magmaTo   = rgb(1f, 0.84f, 0f);
        DustColorTransitionOptions magma = new DustColorTransitionOptions(magmaFrom, magmaTo, 1.5f);
        DustParticleOptions lavaCore = new DustParticleOptions(rgb(0.8f, 0.13f, 0f), 1.0f);

        for (int r = 0; r < 3; r++) {
            double height = b.y() + 0.5 + r * 0.9;
            double ringR  = pulse * (0.8 + r * 0.15);
            for (int i = 0; i < 8; i++) {
                double angle = (i / 8.0) * Math.PI * 2 + (frame * 0.08) + (r * 0.5);
                double px = b.x() + Math.cos(angle) * ringR;
                double pz = b.z() + Math.sin(angle) * ringR;
                level.addParticle(magma, px, height, pz, 0, RNG.nextFloat() * 0.05f, 0);
                if (RNG.nextInt(4) == 0)
                    level.addParticle(lavaCore, px, height + 0.2, pz, 0, 0.08, 0);
            }
        }
        if (frame % 5 == 0) {
            for (int i = 0; i < 5; i++) {
                double angle = RNG.nextDouble() * Math.PI * 2;
                level.addParticle(lavaCore,
                        b.x() + Math.cos(angle) * pulse, b.y() + 2.7,
                        b.z() + Math.sin(angle) * pulse, 0, -0.12, 0);
            }
        }
    }

    // ── DEATH MARK ─────────────────────────────────────────────────────────────
    private static void tickDeathMark(Burst b, ClientLevel level) {
        int frame = b.ticksLeft();

        // poison green -> bile yellow-green
        int toxFrom = rgb(0f, 1f, 0.27f);
        int toxTo   = rgb(0.67f, 1f, 0f);
        DustColorTransitionOptions toxin = new DustColorTransitionOptions(toxFrom, toxTo, 1.3f);
        DustParticleOptions necro = new DustParticleOptions(rgb(0f, 0.2f, 0f), 1.1f);

        for (int s = 0; s < 4; s++) {
            double baseAngle = (s / 4.0) * Math.PI * 2;
            double t = frame * 0.12;
            double radius = 1.5 + Math.sin(t + s) * 0.6;
            double angle  = baseAngle + t;
            double px = b.x() + Math.cos(angle) * radius;
            double py = b.y() + 1 + ((50 - frame) / 50.0) * 3.0;
            double pz = b.z() + Math.sin(angle) * radius;
            level.addParticle(toxin, px, py, pz, 0, 0.04, 0);
            level.addParticle(necro, px + RNG.nextDouble() * 0.3, py + 0.4, pz + RNG.nextDouble() * 0.3, 0, 0.02, 0);
        }
        if (frame == 50 || frame == 25) {
            for (int i = 0; i < 8; i++) {
                double a = (i / 8.0) * Math.PI * 2;
                level.addParticle(new ShriekParticleOption(i * 2),
                        b.x() + Math.cos(a) * 2, b.y() + 1.2, b.z() + Math.sin(a) * 2, 0, 0.1, 0);
            }
        }
    }

    // ── SHADOW STRIKE ──────────────────────────────────────────────────────────
    private static void tickShadowStrike(Burst b, ClientLevel level) {
        int frame = b.ticksLeft();

        // silver-blue -> midnight blue
        int bladeFrom = rgb(0.75f, 0.75f, 1f);
        int bladeTo   = rgb(0f, 0f, 0.4f);
        DustColorTransitionOptions blade = new DustColorTransitionOptions(bladeFrom, bladeTo, 0.9f);
        DustParticleOptions flash = new DustParticleOptions(rgb(1f, 1f, 1f), 1.4f);

        int count = frame > 10 ? 12 : 6;
        for (int i = 0; i < count; i++) {
            double px = b.x() + (RNG.nextDouble() - 0.5) * 1.5;
            double py = b.y() + 0.5 + (i / (double) count) * 2.5 * 0.4;
            double pz = b.z() + (RNG.nextDouble() - 0.5) * 1.5;
            level.addParticle(blade, px, py, pz, 0, -0.05, 0);
            if (i % 3 == 0) level.addParticle(flash, px, py, pz, 0, 0, 0);
        }
        if (frame == 15) {
            for (int i = 0; i < 16; i++) {
                double a = (i / 16.0) * Math.PI * 2;
                level.addParticle(flash,
                        b.x() + Math.cos(a) * 0.8, b.y() + 1.0, b.z() + Math.sin(a) * 0.8,
                        Math.cos(a) * 0.3, 0, Math.sin(a) * 0.3);
            }
        }
    }

    // ── STORM CHAIN ────────────────────────────────────────────────────────────
    private static void tickStormChain(Burst b, ClientLevel level) {
        int frame = b.ticksLeft();

        // electric cyan -> white
        int arcFrom = rgb(0f, 1f, 1f);
        int arcTo   = rgb(1f, 1f, 1f);
        DustColorTransitionOptions arc = new DustColorTransitionOptions(arcFrom, arcTo, 1.0f);
        DustParticleOptions core = new DustParticleOptions(rgb(0f, 0.4f, 1f), 0.7f);

        double[] angles = {0, Math.PI / 2, Math.PI, 3 * Math.PI / 2};
        for (double baseAngle : angles) {
            double reach = Math.min((35 - frame) / 35.0 * 5.0, 5.0);
            for (int s = 0; s < 7; s++) {
                double t = s / 7.0;
                double jitter = (RNG.nextDouble() - 0.5) * 0.6;
                double angle  = baseAngle + jitter * 0.4;
                double px = b.x() + Math.cos(angle) * t * reach + (RNG.nextDouble() - 0.5) * 0.3;
                double py = b.y() + 1 + jitter * 0.5;
                double pz = b.z() + Math.sin(angle) * t * reach + (RNG.nextDouble() - 0.5) * 0.3;
                level.addParticle(arc, px, py, pz, 0, 0, 0);
                if (s % 2 == 0) level.addParticle(core, px, py, pz, 0, 0.03, 0);
            }
        }
        if (frame % 5 == 0) {
            DustParticleOptions bolt = new DustParticleOptions(rgb(0.8f, 0.95f, 1f), 1.5f);
            for (int i = 0; i < 6; i++) {
                double a = (i / 6.0) * Math.PI * 2;
                level.addParticle(bolt,
                        b.x() + Math.cos(a) * 0.5, b.y() + 1.0, b.z() + Math.sin(a) * 0.5,
                        Math.cos(a) * 0.2, 0.05, Math.sin(a) * 0.2);
            }
        }
    }
}

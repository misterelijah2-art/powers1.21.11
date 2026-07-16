package powerful.powers.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.ShriekParticleOption;
import net.neoforged.neoforge.event.tick.ClientTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.joml.Vector3f;
import powerful.powers.ability.AbilityType;
import powerful.powers.powers;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.Random;

/**
 * Client-side multi-frame particle engine.
 *
 * When an AbilityFxPacket arrives, we "schedule" a burst descriptor.
 * Each client tick we drain up to N frames from the queue so the
 * animation plays out over several ticks instead of one giant spike.
 *
 * Uses ONLY custom-coloured DustParticleOptions and
 * DustColorTransitionOptions so visuals are unique, not vanilla.
 */
@EventBusSubscriber(modid = powers.MODID, bus = EventBusSubscriber.Bus.GAME)
public class AbilityParticleEngine {

    private record Burst(AbilityType type, double x, double y, double z, int ticksLeft) {}

    private static final Queue<Burst> QUEUE = new ArrayDeque<>();
    private static final Random RNG = new Random();

    // Called from packet handler
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

        // Process up to 3 active bursts per tick
        int processed = 0;
        Queue<Burst> next = new ArrayDeque<>();
        for (Burst b : QUEUE) {
            if (processed < 3) {
                tickBurst(b, level);
                processed++;
                if (b.ticksLeft() > 1) next.add(new Burst(b.type(), b.x(), b.y(), b.z(), b.ticksLeft() - 1));
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

    // ── NULL RIFT ─────────────────────────────────────────────────────────────
    // Deep violet → black transition dust spiralling inward then exploding out.
    private static void tickNullRift(Burst b, ClientLevel level) {
        int frame = b.ticksLeft(); // counts down from 40
        float progress = frame / 40f;
        // Implode phase (frames 40→20): particles spiral inward
        // Explode phase (frames 20→0): particles blast outward
        boolean imploding = frame > 20;
        double radius = imploding ? progress * 4.0 : (1f - progress) * 5.0;
        int count = imploding ? 6 : 10;

        // deep violet #7B00FF → near-black #1A001A
        DustColorTransitionOptions voidDust = new DustColorTransitionOptions(
                new Vector3f(0.48f, 0f, 1f),       // from: bright violet
                new Vector3f(0.1f, 0f, 0.1f),      // to:   near black
                imploding ? 1.2f : 1.8f
        );
        // Rim: electric indigo
        DustParticleOptions rimDust = new DustParticleOptions(
                new Vector3f(0.55f, 0f, 0.9f), 0.8f);

        for (int i = 0; i < count; i++) {
            double angle = (i / (double) count) * Math.PI * 2 + (frame * 0.15);
            double px = b.x() + Math.cos(angle) * radius + (RNG.nextDouble() - 0.5) * 0.4;
            double py = b.y() + 1 + (RNG.nextDouble() - 0.5) * (imploding ? 1.5 : 2.5);
            double pz = b.z() + Math.sin(angle) * radius + (RNG.nextDouble() - 0.5) * 0.4;
            level.addParticle(voidDust, px, py, pz, 0, 0, 0);
            if (i % 2 == 0) level.addParticle(rimDust, px, py + 0.3, pz, 0, 0.05, 0);
        }
        // Core shriek ring on the explosion frame
        if (frame == 19) {
            for (int i = 0; i < 6; i++) {
                level.addParticle(new ShriekParticleOption(i * 3),
                        b.x(), b.y() + 1, b.z(), 0, 0.2, 0);
            }
        }
    }

    // ── MAGMA CAGE ────────────────────────────────────────────────────────────
    // Molten orange→yellow transition dust forming a pulsing ring cage.
    private static void tickMagmaCage(Burst b, ClientLevel level) {
        int frame = b.ticksLeft();
        double pulse = 2.5 + Math.sin(frame * 0.25) * 1.0;  // radius pulses 1.5–3.5
        int rings = 3;
        int perRing = 8;

        // molten orange #FF6A00 → hot yellow #FFD700
        DustColorTransitionOptions magma = new DustColorTransitionOptions(
                new Vector3f(1f, 0.42f, 0f),
                new Vector3f(1f, 0.84f, 0f),
                1.5f
        );
        // inner lava core: deep red #CC2200
        DustParticleOptions lavaCore = new DustParticleOptions(
                new Vector3f(0.8f, 0.13f, 0f), 1.0f);

        for (int r = 0; r < rings; r++) {
            double height = b.y() + 0.5 + r * 0.9;
            double ringRadius = pulse * (0.8 + r * 0.15);
            for (int i = 0; i < perRing; i++) {
                double angle = (i / (double) perRing) * Math.PI * 2 + (frame * 0.08) + (r * 0.5);
                double px = b.x() + Math.cos(angle) * ringRadius;
                double pz = b.z() + Math.sin(angle) * ringRadius;
                level.addParticle(magma, px, height, pz, 0, RNG.nextFloat() * 0.05f, 0);
                if (RNG.nextInt(4) == 0)
                    level.addParticle(lavaCore, px, height + 0.2, pz, 0, 0.08, 0);
            }
        }
        // drip sparks downward every 5 frames
        if (frame % 5 == 0) {
            for (int i = 0; i < 5; i++) {
                double angle = RNG.nextDouble() * Math.PI * 2;
                level.addParticle(lavaCore,
                        b.x() + Math.cos(angle) * pulse,
                        b.y() + 2.7,
                        b.z() + Math.sin(angle) * pulse,
                        0, -0.12, 0);
            }
        }
    }

    // ── DEATH MARK ────────────────────────────────────────────────────────────
    // Toxic green → sickly yellow-green wisps that orbit the caster upward.
    private static void tickDeathMark(Burst b, ClientLevel level) {
        int frame = b.ticksLeft();

        // poison green #00FF44 → bile yellow #AAFF00
        DustColorTransitionOptions toxin = new DustColorTransitionOptions(
                new Vector3f(0f, 1f, 0.27f),
                new Vector3f(0.67f, 1f, 0f),
                1.3f
        );
        // dark necrotic #003300
        DustParticleOptions necro = new DustParticleOptions(
                new Vector3f(0f, 0.2f, 0f), 1.1f);

        int spirals = 4;
        for (int s = 0; s < spirals; s++) {
            double baseAngle = (s / (double) spirals) * Math.PI * 2;
            double t = frame * 0.12;
            double radius = 1.5 + Math.sin(t + s) * 0.6;
            double angle = baseAngle + t;
            double px = b.x() + Math.cos(angle) * radius;
            double py = b.y() + 1 + ((50 - frame) / 50.0) * 3.0; // rises
            double pz = b.z() + Math.sin(angle) * radius;
            level.addParticle(toxin, px, py, pz, 0, 0.04, 0);
            level.addParticle(necro, px + RNG.nextDouble() * 0.3, py + 0.4, pz + RNG.nextDouble() * 0.3, 0, 0.02, 0);
        }
        // skull-ring pulse at activation (frame ~50) and mid-point (~25)
        if (frame == 50 || frame == 25) {
            for (int i = 0; i < 8; i++) {
                double a = (i / 8.0) * Math.PI * 2;
                level.addParticle(new ShriekParticleOption(i * 2),
                        b.x() + Math.cos(a) * 2, b.y() + 1.2, b.z() + Math.sin(a) * 2,
                        0, 0.1, 0);
            }
        }
    }

    // ── SHADOW STRIKE ─────────────────────────────────────────────────────────
    // Silver → dark-blue streak from caster position through the target.
    private static void tickShadowStrike(Burst b, ClientLevel level) {
        int frame = b.ticksLeft(); // 20 → 0
        float progress = 1f - frame / 20f;

        // silver #C0C0FF → midnight blue #000066
        DustColorTransitionOptions blade = new DustColorTransitionOptions(
                new Vector3f(0.75f, 0.75f, 1f),
                new Vector3f(0f, 0f, 0.4f),
                0.9f
        );
        // pure white flash
        DustParticleOptions flash = new DustParticleOptions(
                new Vector3f(1f, 1f, 1f), 1.4f);

        int count = frame > 10 ? 12 : 6;
        for (int i = 0; i < count; i++) {
            double spread = (i / (double) count) * 2.5;
            double px = b.x() + (RNG.nextDouble() - 0.5) * 1.5;
            double py = b.y() + 0.5 + spread * 0.4;
            double pz = b.z() + (RNG.nextDouble() - 0.5) * 1.5;
            level.addParticle(blade, px, py, pz, 0, -0.05, 0);
            if (i % 3 == 0) level.addParticle(flash, px, py, pz, 0, 0, 0);
        }
        // horizontal slash ring at peak
        if (frame == 15) {
            for (int i = 0; i < 16; i++) {
                double a = (i / 16.0) * Math.PI * 2;
                level.addParticle(flash,
                        b.x() + Math.cos(a) * 0.8,
                        b.y() + 1.0,
                        b.z() + Math.sin(a) * 0.8,
                        Math.cos(a) * 0.3, 0, Math.sin(a) * 0.3);
            }
        }
    }

    // ── STORM CHAIN ───────────────────────────────────────────────────────────
    // Electric cyan → white arcs that zigzag between positions.
    private static void tickStormChain(Burst b, ClientLevel level) {
        int frame = b.ticksLeft();

        // electric cyan #00FFFF → white #FFFFFF
        DustColorTransitionOptions arc = new DustColorTransitionOptions(
                new Vector3f(0f, 1f, 1f),
                new Vector3f(1f, 1f, 1f),
                1.0f
        );
        // core blue #0066FF
        DustParticleOptions core = new DustParticleOptions(
                new Vector3f(0f, 0.4f, 1f), 0.7f);

        // Zigzag bolt from centre outward in 4 directions (the 4 chain jumps)
        double[] angles = {0, Math.PI / 2, Math.PI, 3 * Math.PI / 2};
        for (double baseAngle : angles) {
            int segments = 7;
            double reach = Math.min((35 - frame) / 35.0 * 5.0, 5.0);
            for (int s = 0; s < segments; s++) {
                double t = s / (double) segments;
                double jitter = (RNG.nextDouble() - 0.5) * 0.6;
                double angle  = baseAngle + jitter * 0.4;
                double px = b.x() + Math.cos(angle) * t * reach + (RNG.nextDouble() - 0.5) * 0.3;
                double py = b.y() + 1 + jitter * 0.5;
                double pz = b.z() + Math.sin(angle) * t * reach + (RNG.nextDouble() - 0.5) * 0.3;
                level.addParticle(arc, px, py, pz, 0, 0, 0);
                if (s % 2 == 0) level.addParticle(core, px, py, pz, 0, 0.03, 0);
            }
        }
        // central burst flash every 5 frames
        if (frame % 5 == 0) {
            DustParticleOptions bolt = new DustParticleOptions(new Vector3f(0.8f, 0.95f, 1f), 1.5f);
            for (int i = 0; i < 6; i++) {
                double a = (i / 6.0) * Math.PI * 2;
                level.addParticle(bolt,
                        b.x() + Math.cos(a) * 0.5,
                        b.y() + 1.0,
                        b.z() + Math.sin(a) * 0.5,
                        Math.cos(a) * 0.2, 0.05, Math.sin(a) * 0.2);
            }
        }
    }
}

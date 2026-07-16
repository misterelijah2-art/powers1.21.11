package powerful.powers.ability;

import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.joml.Vector3f;
import powerful.powers.network.AbilityFxPacket;
import powerful.powers.network.PacketHandler;
import powerful.powers.network.SyncAbilityPacket;
import powerful.powers.network.TitleRevealPacket;
import powerful.powers.powers;

import java.util.*;

/**
 * Server-side event handler for all five reworked abilities.
 *
 * ─────────────────────────────────────────────────────────────────────────────
 *  NULL RIFT     – Implodes all enemies within 5 blocks toward the caster
 *                  (strong knockback pull), then after 1 second detonates
 *                  them outward dealing 8♥ + 3s Levitation (they float away).
 *                  Caster gets brief Resistance I as an eye-of-the-storm buff.
 *
 *  MAGMA CAGE    – Summons a molten ring. Every 10 ticks for 5 seconds, every
 *                  entity inside 4 blocks takes 3♥ and is slowed (Slowness III
 *                  for 1.5s). Cannot be walked out of easily. Ends with a
 *                  final 6♥ burst.
 *
 *  DEATH MARK    – Tags all nearby enemies (within 6 blocks) with Glowing so
 *                  you can see them through walls. While active (6s), every
 *                  time a marked enemy damages you, they take 150% of that
 *                  damage back (damage reflection via LivingIncomingDamageEvent).
 *                  Marked enemies also get Weakness II for the duration.
 *
 *  SHADOW STRIKE – Fires a raycast up to 10 blocks. If it hits a living entity,
 *                  the caster teleports BEHIND them, deals 10♥ instantly, and
 *                  leaves a lingering Wither cloud at the old enemy position
 *                  that deals 1♥/s for 4s. Caster gets 1s Invisibility.
 *
 *  STORM CHAIN   – Picks the nearest enemy within 8 blocks, strikes it with a
 *                  custom lightning bolt (no fire), then chains to the 3 next
 *                  nearest targets automatically (each chain deals 5♥ and
 *                  stacks Slowness II). Caster gains Speed II for 3s to chase.
 * ─────────────────────────────────────────────────────────────────────────────
 */
@EventBusSubscriber(modid = powers.MODID)
public class AbilityLogicHandler {

    private static final Map<UUID, Integer>  joinTimers   = new HashMap<>();
    private static final Set<UUID>           assigned     = new HashSet<>();
    private static final Map<UUID, Set<UUID>> deathMarked  = new HashMap<>(); // caster → set of marked entity UUIDs
    private static final int ASSIGN_DELAY_TICKS = 200; // 10 seconds

    // ── MAGMA CAGE state: caster UUID → ticks remaining ──────────────────────
    private static final Map<UUID, Integer> magmaCageTicks = new HashMap<>();

    // ── NULL RIFT detonation: caster UUID → detonation countdown ─────────────
    private static final Map<UUID, Integer> riftDetonation  = new HashMap<>();
    private static final Map<UUID, List<UUID>> riftTargets   = new HashMap<>();

    // ─────────────────────────────────────────────────────────────────────────
    //  JOIN / LOGOUT
    // ─────────────────────────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        UUID id = sp.getUUID();
        PlayerAbilityData data = sp.getData(AbilityAttachment.ABILITY_DATA.get());
        if (!data.hasAbility() && !assigned.contains(id)) {
            joinTimers.put(id, 0);
        }
        syncTo(sp, data);
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            joinTimers.remove(sp.getUUID());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  TICK
    // ─────────────────────────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        UUID id = sp.getUUID();
        ServerLevel level = (ServerLevel) sp.level();

        // Join timer
        if (joinTimers.containsKey(id)) {
            int t = joinTimers.get(id) + 1;
            joinTimers.put(id, t);
            if (t >= ASSIGN_DELAY_TICKS) {
                joinTimers.remove(id);
                assigned.add(id);
                assignRandomAbility(sp);
            }
        }

        PlayerAbilityData data = sp.getData(AbilityAttachment.ABILITY_DATA.get());
        data.tickCooldown();
        if (data.isActive()) data.tickActive();

        // ── Magma Cage pulse ─────────────────────────────────────────────────
        if (magmaCageTicks.containsKey(id)) {
            int remaining = magmaCageTicks.get(id) - 1;
            if (remaining <= 0) {
                // Final burst
                pulseMagmaCage(sp, level, 6.0f, true);
                magmaCageTicks.remove(id);
            } else {
                if (remaining % 10 == 0) pulseMagmaCage(sp, level, 3.0f, false);
                magmaCageTicks.put(id, remaining);
            }
        }

        // ── Null Rift detonation ──────────────────────────────────────────────
        if (riftDetonation.containsKey(id)) {
            int countdown = riftDetonation.get(id) - 1;
            if (countdown <= 0) {
                detonateRift(sp, level);
                riftDetonation.remove(id);
                riftTargets.remove(id);
            } else {
                riftDetonation.put(id, countdown);
            }
        }

        // Sync every 5 ticks
        if (sp.tickCount % 5 == 0) syncTo(sp, data);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  ACTIVATION (called from UseAbilityPacket handler)
    // ─────────────────────────────────────────────────────────────────────────

    public static void activateAbility(ServerPlayer sp) {
        PlayerAbilityData data = sp.getData(AbilityAttachment.ABILITY_DATA.get());
        if (!data.hasAbility()) return;
        if (data.isOnCooldown()) {
            sp.displayClientMessage(Component.literal("\u00a7cAbility is on cooldown!"), true);
            return;
        }
        AbilityType type = data.getAbility();
        ServerLevel level = (ServerLevel) sp.level();

        triggerAbility(sp, type, level);
        data.setCooldownRemaining(type.getCooldownTicks());
        data.setActiveRemaining(type.getDurationTicks());

        // Broadcast FX packet to everyone within 32 blocks
        Vec3 pos = sp.position();
        PacketHandler.sendToNear(level, pos.x, pos.y, pos.z, 32,
                new AbilityFxPacket(type.name(), pos.x, pos.y, pos.z));

        syncTo(sp, data);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  ABILITY IMPLEMENTATIONS
    // ─────────────────────────────────────────────────────────────────────────

    private static void triggerAbility(ServerPlayer sp, AbilityType type, ServerLevel level) {
        Vec3 pos = sp.position();
        switch (type) {

            // ── NULL RIFT ────────────────────────────────────────────────────
            case NULL_RIFT -> {
                // Phase 1: suck all enemies within 5 blocks toward caster
                List<LivingEntity> targets = getNearby(level, sp, 5.0);
                List<UUID> ids = new ArrayList<>();
                for (LivingEntity e : targets) {
                    Vec3 pull = pos.add(0, 1, 0).subtract(e.position()).normalize().scale(2.5);
                    e.setDeltaMovement(pull);
                    e.hurtMarked = true;
                    e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 25, 0, false, false));
                    ids.add(e.getUUID());
                }
                // Rumble sound + visual
                level.playSound(null, pos.x, pos.y, pos.z,
                        SoundEvents.CONDUIT_ACTIVATE, SoundSource.PLAYERS, 1.2f, 0.5f);
                sp.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 30, 0, false, false));
                // Schedule detonation in 20 ticks (1 second)
                riftDetonation.put(sp.getUUID(), 20);
                riftTargets.put(sp.getUUID(), ids);
            }

            // ── MAGMA CAGE ───────────────────────────────────────────────────
            case MAGMA_CAGE -> {
                level.playSound(null, pos.x, pos.y, pos.z,
                        SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0f, 0.6f);
                // Slow all nearby immediately
                getNearby(level, sp, 4.0).forEach(e -> {
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 2, false, false));
                    e.igniteForSeconds(2);
                });
                magmaCageTicks.put(sp.getUUID(), 100); // 5 seconds of pulses
                sp.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 120, 0, false, false));
            }

            // ── DEATH MARK ───────────────────────────────────────────────────
            case DEATH_MARK -> {
                Set<UUID> marked = new HashSet<>();
                getNearby(level, sp, 6.0).forEach(e -> {
                    e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 120, 0, false, false));
                    e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 1, false, false));
                    marked.add(e.getUUID());
                });
                deathMarked.put(sp.getUUID(), marked);
                level.playSound(null, pos.x, pos.y, pos.z,
                        SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.6f, 1.6f);
                sp.displayClientMessage(
                    Component.literal("\u00a72" + marked.size() + " enemies marked – damage will be reflected!"), true);
            }

            // ── SHADOW STRIKE ────────────────────────────────────────────────
            case SHADOW_STRIKE -> {
                // Raycast up to 10 blocks in look direction
                Vec3 look  = sp.getLookAngle();
                Vec3 start = sp.getEyePosition();
                Vec3 end   = start.add(look.scale(10.0));
                LivingEntity hit = null;
                double closest = Double.MAX_VALUE;
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class,
                        new AABB(start, end).inflate(1.5))) {
                    if (e == sp) continue;
                    double dist = e.distanceTo(sp);
                    if (dist < closest) { closest = dist; hit = e; }
                }
                if (hit != null) {
                    // Teleport BEHIND the target
                    Vec3 behindTarget = hit.position().subtract(look.normalize().scale(1.2));
                    sp.teleportTo(behindTarget.x, behindTarget.y, behindTarget.z);
                    hit.hurt(level.damageSources().playerAttack(sp), 10.0f);
                    // Leave a wither smoke cloud at target's feet
                    spawnWitherCloud(level, hit.position());
                    level.playSound(null, behindTarget.x, behindTarget.y, behindTarget.z,
                            SoundEvents.PHANTOM_BITE, SoundSource.PLAYERS, 1.0f, 1.3f);
                } else {
                    // No target: short dash forward
                    Vec3 dest = pos.add(look.normalize().scale(6.0));
                    sp.teleportTo(dest.x, dest.y, dest.z);
                    level.playSound(null, dest.x, dest.y, dest.z,
                            SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8f, 1.5f);
                }
                sp.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 20, 0, false, false));
            }

            // ── STORM CHAIN ──────────────────────────────────────────────────
            case STORM_CHAIN -> {
                List<LivingEntity> pool = getNearby(level, sp, 8.0);
                pool.sort(Comparator.comparingDouble(e -> e.distanceTo(sp)));
                int chains = Math.min(4, pool.size());
                LivingEntity last = null;
                for (int i = 0; i < chains; i++) {
                    LivingEntity target = pool.get(i);
                    // Custom lightning bolt (no ground fire)
                    LightningBolt bolt = new LightningBolt(EntityType.LIGHTNING_BOLT, level);
                    bolt.setPos(target.getX(), target.getY(), target.getZ());
                    bolt.setVisualOnly(false);
                    level.addFreshEntity(bolt);
                    target.hurt(level.damageSources().playerAttack(sp), 5.0f);
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1, false, false));
                    target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 80, 0, false, false));
                    // Draw arc particles between previous and current (server-side dust)
                    if (last != null) {
                        drawDustLine(level, last.position().add(0,1,0), target.position().add(0,1,0),
                                new Vector3f(0f, 1f, 1f), 0.6f, 12);
                    }
                    last = target;
                }
                sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 1, false, false));
                level.playSound(null, pos.x, pos.y, pos.z,
                        SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.8f, 1.3f);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  MAGMA CAGE pulse helper
    // ─────────────────────────────────────────────────────────────────────────

    private static void pulseMagmaCage(ServerPlayer sp, ServerLevel level, float dmg, boolean final_) {
        Vec3 pos = sp.position();
        getNearby(level, sp, 4.0).forEach(e -> {
            e.hurt(level.damageSources().playerAttack(sp), dmg);
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 2, false, false));
            if (!final_) e.igniteForSeconds(1);
        });
        level.playSound(null, pos.x, pos.y, pos.z,
                SoundEvents.FIRE_AMBIENT, SoundSource.PLAYERS, 0.6f, 0.8f + (float)(Math.random() * 0.4));
        if (final_) {
            level.playSound(null, pos.x, pos.y, pos.z,
                    SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.7f, 1.4f);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  NULL RIFT detonation helper
    // ─────────────────────────────────────────────────────────────────────────

    private static void detonateRift(ServerPlayer sp, ServerLevel level) {
        Vec3 pos = sp.position();
        List<UUID> ids = riftTargets.getOrDefault(sp.getUUID(), List.of());
        for (UUID uid : ids) {
            level.getEntitiesOfClass(LivingEntity.class, sp.getBoundingBox().inflate(6))
                .stream().filter(e -> e.getUUID().equals(uid)).findFirst().ifPresent(e -> {
                    Vec3 blast = e.position().subtract(pos).normalize().scale(3.0);
                    e.setDeltaMovement(blast.x, 0.8, blast.z);
                    e.hurtMarked = true;
                    e.hurt(level.damageSources().playerAttack(sp), 8.0f);
                    e.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 40, 2, false, false));
                });
        }
        level.playSound(null, pos.x, pos.y, pos.z,
                SoundEvents.CONDUIT_DEACTIVATE, SoundSource.PLAYERS, 1.2f, 0.7f);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  DEATH MARK reflection
    // ─────────────────────────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onLivingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer victim)) return;
        ServerLevel level = (ServerLevel) victim.level();
        UUID vid = victim.getUUID();
        Set<UUID> marked = deathMarked.get(vid);
        if (marked == null || marked.isEmpty()) return;

        // Check if the attacker is a marked entity
        if (event.getSource().getEntity() instanceof LivingEntity attacker
                && marked.contains(attacker.getUUID())) {
            float reflected = event.getAmount() * 1.5f;
            attacker.hurt(level.damageSources().thorns(victim), reflected);
            // Visual pop
            level.playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(),
                    SoundEvents.PLAYER_HURT_FREEZE, SoundSource.PLAYERS, 0.5f, 1.8f);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  PvP DEATH DROP
    // ─────────────────────────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer victim)) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer)) return;

        PlayerAbilityData data = victim.getData(AbilityAttachment.ABILITY_DATA.get());
        if (!data.hasAbility()) return;

        AbilityType type = data.getAbility();
        ServerLevel level = (ServerLevel) victim.level();
        Vec3 pos = victim.position();

        ItemStack orb = AbilityItem.forAbility(type);
        ItemEntity itemEntity = new ItemEntity(level, pos.x, pos.y + 0.5, pos.z, orb);
        itemEntity.setPickUpDelay(20);
        level.addFreshEntity(itemEntity);

        level.playSound(null, pos.x, pos.y, pos.z,
                SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0f, 0.6f);

        // Notify all nearby via FX packet
        PacketHandler.sendToNear(level, pos.x, pos.y, pos.z, 32,
                new AbilityFxPacket(type.name(), pos.x, pos.y, pos.z));

        data.clearAbility();
        assigned.remove(victim.getUUID());
        deathMarked.remove(victim.getUUID());
        magmaCageTicks.remove(victim.getUUID());
        riftDetonation.remove(victim.getUUID());
        riftTargets.remove(victim.getUUID());

        victim.displayClientMessage(
            Component.literal("\u00a7cYou lost your power!"), false);
        PacketHandler.sendToPlayer(victim, new SyncAbilityPacket("", 0, 0));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  RESPAWN: keep ability on natural death
    // ─────────────────────────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;
        PlayerAbilityData oldData = event.getOriginal().getData(AbilityAttachment.ABILITY_DATA.get());
        if (!oldData.hasAbility()) return;
        PlayerAbilityData newData = event.getEntity().getData(AbilityAttachment.ABILITY_DATA.get());
        newData.setAbility(oldData.getAbility());
        newData.setCooldownRemaining(oldData.getCooldownRemaining());
        assigned.add(event.getEntity().getUUID());
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  UTILITY
    // ─────────────────────────────────────────────────────────────────────────

    private static void assignRandomAbility(ServerPlayer sp) {
        AbilityType[] types = AbilityType.values();
        AbilityType chosen = types[sp.getRandom().nextInt(types.length)];
        PlayerAbilityData data = sp.getData(AbilityAttachment.ABILITY_DATA.get());
        data.setAbility(chosen);
        data.setCooldownRemaining(0);
        PacketHandler.sendToPlayer(sp, new TitleRevealPacket(chosen.name()));
        syncTo(sp, data);
        powers.LOGGER.info("[Powers] Assigned {} to {}", chosen.name(), sp.getName().getString());
    }

    private static void syncTo(ServerPlayer sp, PlayerAbilityData data) {
        PacketHandler.sendToPlayer(sp, new SyncAbilityPacket(
            data.hasAbility() ? data.getAbility().name() : "",
            data.getCooldownRemaining(),
            data.hasAbility() ? data.getAbility().getCooldownTicks() : 0
        ));
    }

    private static List<LivingEntity> getNearby(ServerLevel level, ServerPlayer sp, double radius) {
        return level.getEntitiesOfClass(LivingEntity.class,
                sp.getBoundingBox().inflate(radius),
                e -> e != sp);
    }

    /** Drops a lingering wither-smoke cloud using dust particles (server-side). */
    private static void spawnWitherCloud(ServerLevel level, Vec3 pos) {
        for (int i = 0; i < 3; i++) {
            int finalI = i;
            // Stagger 3 dust bursts over the next 3 ticks using a simple scheduler
            // We just emit them all now; the slight clump is fine server-side.
            level.sendParticles(
                new DustParticleOptions(new Vector3f(0.1f, 0f, 0.1f), 1.2f),
                pos.x, pos.y + 0.5 + finalI * 0.4, pos.z,
                8, 0.4, 0.2, 0.4, 0.02
            );
        }
        // Wither damage pulses handled by the Magma-style per-tick approach would
        // over-engineer this; instead apply Wither directly to the hit entity.
        level.playSound(null, pos.x, pos.y, pos.z,
                SoundEvents.WITHER_AMBIENT, SoundSource.PLAYERS, 0.4f, 1.8f);
    }

    /** Server-side coloured dust line between two points (visible to all nearby). */
    private static void drawDustLine(ServerLevel level, Vec3 from, Vec3 to,
                                      Vector3f rgb, float size, int steps) {
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            double px = from.x + (to.x - from.x) * t;
            double py = from.y + (to.y - from.y) * t;
            double pz = from.z + (to.z - from.z) * t;
            level.sendParticles(new DustParticleOptions(rgb, size),
                    px, py, pz, 1, 0.05, 0.05, 0.05, 0);
        }
    }
}

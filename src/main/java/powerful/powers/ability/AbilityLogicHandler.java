package powerful.powers.ability;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import powerful.powers.network.AbilityFxPacket;
import powerful.powers.network.PacketHandler;
import powerful.powers.network.SyncAbilityPacket;
import powerful.powers.network.TitleRevealPacket;
import powerful.powers.powers;

import java.util.*;

@EventBusSubscriber(modid = powers.MODID)
public class AbilityLogicHandler {

    private static final Map<UUID, Integer>    joinTimers      = new HashMap<>();
    private static final Set<UUID>             assigned        = new HashSet<>();
    private static final Map<UUID, Set<UUID>>  deathMarked     = new HashMap<>();
    private static final Map<UUID, Integer>    magmaCageTicks  = new HashMap<>();
    private static final Map<UUID, Integer>    riftDetonation  = new HashMap<>();
    private static final Map<UUID, List<UUID>> riftTargets     = new HashMap<>();
    private static final Map<UUID, Integer>    hudUnlockTimers = new HashMap<>();
    private static final int ASSIGN_DELAY_TICKS = 200;

    // Particle colours per ability (ARGB packed - using org.joml.Vector4f via DustParticleOptions int)
    private static final int COLOR_VOIDSTEP      = (0xFF << 24) | 0x6600CC;
    private static final int COLOR_SOULFLARE     = (0xFF << 24) | 0xFF6600;
    private static final int COLOR_GLACIAL_PULSE = (0xFF << 24) | 0x44FFFF;
    private static final int COLOR_WRAITH_SHROUD = (0xFF << 24) | 0xCCCCCC;
    private static final int COLOR_THUNDER_CRASH = (0xFF << 24) | 0xFFFF00;

    // ── JOIN / LOGOUT ────────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        UUID id = sp.getUUID();
        PlayerAbilityData data = sp.getData(AbilityAttachment.ABILITY_DATA.get());
        if (!data.hasAbility() && !assigned.contains(id)) joinTimers.put(id, 0);
        syncTo(sp, data);
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) joinTimers.remove(sp.getUUID());
    }

    // ── TICK ─────────────────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        UUID id = sp.getUUID();
        ServerLevel level = (ServerLevel) sp.level();

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
        if (data.isCharging()) data.tickCharge();

        tickHudUnlock(sp);

        if (magmaCageTicks.containsKey(id)) {
            int rem = magmaCageTicks.get(id) - 1;
            if (rem <= 0) {
                pulseMagmaCage(sp, level, 6.0f, true);
                magmaCageTicks.remove(id);
            } else {
                if (rem % 10 == 0) pulseMagmaCage(sp, level, 3.0f, false);
                magmaCageTicks.put(id, rem);
            }
        }

        if (riftDetonation.containsKey(id)) {
            int cd = riftDetonation.get(id) - 1;
            if (cd <= 0) {
                detonateRift(sp, level);
                riftDetonation.remove(id);
                riftTargets.remove(id);
            } else {
                riftDetonation.put(id, cd);
            }
        }

        if (data.hasAbility() && !data.isOnCooldown() && data.isHudUnlocked()) {
            spawnAmbientAura(level, sp, data.getAbility());
        }

        if (sp.tickCount % 5 == 0) syncTo(sp, data);
    }

    // ── CHARGE API ───────────────────────────────────────────────────────────

    public static void beginCharge(ServerPlayer sp) {
        PlayerAbilityData data = sp.getData(AbilityAttachment.ABILITY_DATA.get());
        if (!data.hasAbility() || data.isOnCooldown() || data.isCharging()) return;
        data.resetCharge();
        data.setCharging(true);
        Vec3 pos = sp.position();
        sp.level().playSound(null, pos.x, pos.y, pos.z,
                SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 0.4f, 1.6f);
        syncTo(sp, data);
    }

    public static void releaseCharge(ServerPlayer sp) {
        PlayerAbilityData data = sp.getData(AbilityAttachment.ABILITY_DATA.get());
        if (!data.hasAbility() || !data.isCharging()) return;
        int charged = data.getChargeTicks();
        data.resetCharge();
        activateAbility(sp, charged);
    }

    public static void activateAbility(ServerPlayer sp) { activateAbility(sp, 40); }

    public static void activateAbility(ServerPlayer sp, int chargeTicks) {
        PlayerAbilityData data = sp.getData(AbilityAttachment.ABILITY_DATA.get());
        if (!data.hasAbility()) return;
        if (data.isOnCooldown()) {
            sp.displayClientMessage(Component.literal("\u00a7cAbility on cooldown!"), true);
            return;
        }
        AbilityType type = data.getAbility();
        ServerLevel level = (ServerLevel) sp.level();
        float charge = 0.5f + 0.5f * Math.min(1f, chargeTicks / (float) type.chargeMaxTicks);
        triggerAbility(sp, type, level, charge);
        data.setCooldownRemaining(type.getCooldownTicks());
        data.setActiveRemaining(type.getDurationTicks());
        Vec3 pos = sp.position();
        PacketHandler.sendToNear(level, pos.x, pos.y, pos.z, 32,
                new AbilityFxPacket(type.name(), pos.x, pos.y, pos.z));
        syncTo(sp, data);
    }

    // ── ABILITY IMPLEMENTATIONS ───────────────────────────────────────────────

    private static void triggerAbility(ServerPlayer sp, AbilityType type,
                                        ServerLevel level, float charge) {
        Vec3 pos = sp.position();
        switch (type) {

            case VOIDSTEP -> {
                double radius = 5.0 * charge;
                List<LivingEntity> targets = getNearby(level, sp, radius);
                List<UUID> ids = new ArrayList<>();
                for (LivingEntity e : targets) {
                    Vec3 pull = pos.add(0, 1, 0).subtract(e.position()).normalize().scale(2.5 * charge);
                    e.setDeltaMovement(pull);
                    e.hurtMarked = true;
                    e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 25, 0, false, false));
                    ids.add(e.getUUID());
                }
                level.playSound(null, pos.x, pos.y, pos.z,
                        SoundEvents.CONDUIT_ACTIVATE, SoundSource.PLAYERS, 1.2f, 0.5f);
                sp.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 30, 0, false, false));
                riftDetonation.put(sp.getUUID(), 20);
                riftTargets.put(sp.getUUID(), ids);
            }

            case SOULFLARE -> {
                level.playSound(null, pos.x, pos.y, pos.z,
                        SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0f, 0.6f);
                getNearby(level, sp, 4.0 * charge).forEach(e -> {
                    e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 30, 2, false, false));
                    e.igniteForSeconds((int)(2 * charge));
                });
                magmaCageTicks.put(sp.getUUID(), (int)(100 * charge));
                sp.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 120, 0, false, false));
            }

            case GLACIAL_PULSE -> {
                Set<UUID> marked = new HashSet<>();
                getNearby(level, sp, 6.0 * charge).forEach(e -> {
                    e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 120, 0, false, false));
                    e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 1, false, false));
                    marked.add(e.getUUID());
                });
                deathMarked.put(sp.getUUID(), marked);
                level.playSound(null, pos.x, pos.y, pos.z,
                        SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.6f, 1.6f);
                sp.displayClientMessage(
                        Component.literal("\u00a72" + marked.size() + " enemies marked!"), true);
            }

            case WRAITH_SHROUD -> {
                Vec3 look  = sp.getLookAngle();
                Vec3 start = sp.getEyePosition();
                double range = 10.0 * charge;
                LivingEntity hit = null;
                double closest = Double.MAX_VALUE;
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class,
                        new AABB(start, start.add(look.scale(range))).inflate(1.5))) {
                    if (e == sp) continue;
                    double dist = e.distanceTo(sp);
                    if (dist < closest) { closest = dist; hit = e; }
                }
                if (hit != null) {
                    Vec3 behind = hit.position().subtract(look.normalize().scale(1.2));
                    sp.teleportTo(behind.x, behind.y, behind.z);
                    hit.hurt(level.damageSources().playerAttack(sp), 10.0f * charge);
                    hit.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 1, false, false));
                    level.playSound(null, behind.x, behind.y, behind.z,
                            SoundEvents.PHANTOM_BITE, SoundSource.PLAYERS, 1.0f, 1.3f);
                } else {
                    double blinkRange = 6.0 * charge;
                    Vec3 dest = pos.add(look.normalize().scale(blinkRange));
                    sp.teleportTo(dest.x, dest.y, dest.z);
                    level.playSound(null, dest.x, dest.y, dest.z,
                            SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8f, 1.5f);
                }
                sp.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 20, 0, false, false));
            }

            case THUNDER_CRASH -> {
                List<LivingEntity> pool = getNearby(level, sp, 8.0 * charge);
                pool.sort(Comparator.comparingDouble(e -> e.distanceTo(sp)));
                int chains = Math.min((int)(2 + 2 * charge), pool.size());
                LivingEntity last = null;
                for (int i = 0; i < chains; i++) {
                    LivingEntity target = pool.get(i);
                    LightningBolt bolt = new LightningBolt(EntityType.LIGHTNING_BOLT, level);
                    bolt.setPos(target.getX(), target.getY(), target.getZ());
                    bolt.setVisualOnly(true);
                    level.addFreshEntity(bolt);
                    target.hurt(level.damageSources().playerAttack(sp), 5.0f * charge);
                    target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1, false, false));
                    target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 80, 0, false, false));
                    if (last != null)
                        drawDustLine(level, last.position().add(0,1,0), target.position().add(0,1,0), 12);
                    last = target;
                }
                sp.addEffect(new MobEffectInstance(MobEffects.SPEED, 60, 1, false, false));
                level.playSound(null, pos.x, pos.y, pos.z,
                        SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.8f, 1.3f);
            }
        }
    }

    // ── AMBIENT PASSIVE AURA ─────────────────────────────────────────────────

    private static void spawnAmbientAura(ServerLevel level, ServerPlayer sp, AbilityType type) {
        if (sp.tickCount % 4 != 0) return;
        Vec3 pos = sp.position().add(0, 1.0, 0);
        double angle = (sp.tickCount * 18.0) * Math.PI / 180.0;
        double r = 0.6;
        double ox = Math.cos(angle) * r;
        double oz = Math.sin(angle) * r;
        int color = switch (type) {
            case VOIDSTEP      -> COLOR_VOIDSTEP;
            case SOULFLARE     -> COLOR_SOULFLARE;
            case GLACIAL_PULSE -> COLOR_GLACIAL_PULSE;
            case WRAITH_SHROUD -> COLOR_WRAITH_SHROUD;
            case THUNDER_CRASH -> COLOR_THUNDER_CRASH;
        };
        level.sendParticles(new DustParticleOptions(color, 0.5f),
                pos.x + ox, pos.y, pos.z + oz, 1, 0.05, 0.1, 0.05, 0.0);
        level.sendParticles(new DustParticleOptions(color, 0.5f),
                pos.x - ox, pos.y, pos.z - oz, 1, 0.05, 0.1, 0.05, 0.0);
    }

    // ── MAGMA / SOULFLARE pulse ───────────────────────────────────────────────

    private static void pulseMagmaCage(ServerPlayer sp, ServerLevel level, float dmg, boolean finalBurst) {
        Vec3 pos = sp.position();
        getNearby(level, sp, 4.0).forEach(e -> {
            e.hurt(level.damageSources().playerAttack(sp), dmg);
            e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 30, 2, false, false));
            if (!finalBurst) e.igniteForSeconds(1);
        });
        level.playSound(null, pos.x, pos.y, pos.z,
                SoundEvents.FIRE_AMBIENT, SoundSource.PLAYERS, 0.6f,
                0.8f + (float)(Math.random() * 0.4));
        if (finalBurst)
            level.playSound(null, pos.x, pos.y, pos.z,
                    SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.7f, 1.4f);
    }

    // ── VOIDSTEP detonation ───────────────────────────────────────────────────

    private static void detonateRift(ServerPlayer sp, ServerLevel level) {
        Vec3 pos = sp.position();
        List<UUID> ids = riftTargets.getOrDefault(sp.getUUID(), List.of());
        level.getEntitiesOfClass(LivingEntity.class, sp.getBoundingBox().inflate(8))
            .stream()
            .filter(e -> ids.contains(e.getUUID()))
            .forEach(e -> {
                Vec3 blast = e.position().subtract(pos).normalize().scale(3.0);
                e.setDeltaMovement(blast.x, 0.8, blast.z);
                e.hurtMarked = true;
                e.hurt(level.damageSources().playerAttack(sp), 8.0f);
                e.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 40, 2, false, false));
            });
        level.playSound(null, pos.x, pos.y, pos.z,
                SoundEvents.CONDUIT_DEACTIVATE, SoundSource.PLAYERS, 1.2f, 0.7f);
    }

    // ── DEATH MARK / GLACIAL reflection ──────────────────────────────────────

    @SubscribeEvent
    public static void onLivingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer victim)) return;
        ServerLevel level = (ServerLevel) victim.level();
        Set<UUID> marked = deathMarked.get(victim.getUUID());
        if (marked == null || marked.isEmpty()) return;
        if (event.getSource().getEntity() instanceof LivingEntity attacker
                && marked.contains(attacker.getUUID())) {
            attacker.hurt(level.damageSources().thorns(victim), event.getAmount() * 1.5f);
            level.playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(),
                    SoundEvents.PLAYER_HURT_FREEZE, SoundSource.PLAYERS, 0.5f, 1.8f);
        }
    }

    // ── PvP DEATH DROP ───────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer victim)) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer)) return;
        PlayerAbilityData data = victim.getData(AbilityAttachment.ABILITY_DATA.get());
        if (!data.hasAbility()) return;
        AbilityType type = data.getAbility();
        int cooldownSnapshot = data.getCooldownRemaining();
        ServerLevel level = (ServerLevel) victim.level();
        Vec3 pos = victim.position();
        ItemStack orb = AbilityItem.forAbility(type, cooldownSnapshot);
        ItemEntity ie = new ItemEntity(level, pos.x, pos.y + 0.5, pos.z, orb);
        ie.setPickUpDelay(20);
        level.addFreshEntity(ie);
        level.playSound(null, pos.x, pos.y, pos.z,
                SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0f, 0.6f);
        PacketHandler.sendToNear(level, pos.x, pos.y, pos.z, 32,
                new AbilityFxPacket(type.name(), pos.x, pos.y, pos.z));
        data.clearAbility();
        assigned.remove(victim.getUUID());
        deathMarked.remove(victim.getUUID());
        magmaCageTicks.remove(victim.getUUID());
        riftDetonation.remove(victim.getUUID());
        riftTargets.remove(victim.getUUID());
        victim.displayClientMessage(Component.literal("\u00a7cYou lost your power!"), false);
        PacketHandler.sendToPlayer(victim, new SyncAbilityPacket("", 0, 0, false, 0, false));
    }

    // ── RESPAWN: keep ability on natural death ────────────────────────────────

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;
        PlayerAbilityData old = event.getOriginal().getData(AbilityAttachment.ABILITY_DATA.get());
        if (!old.hasAbility()) return;
        PlayerAbilityData neo = event.getEntity().getData(AbilityAttachment.ABILITY_DATA.get());
        neo.setAbility(old.getAbility());
        neo.setCooldownRemaining(old.getCooldownRemaining());
        if (old.isHudUnlocked()) neo.unlockHud();
        assigned.add(event.getEntity().getUUID());
    }

    // ── HUD UNLOCK ───────────────────────────────────────────────────────────

    public static void unlockHud(ServerPlayer sp) {
        PlayerAbilityData data = sp.getData(AbilityAttachment.ABILITY_DATA.get());
        if (!data.isHudUnlocked()) { data.unlockHud(); syncTo(sp, data); }
    }

    private static void scheduleHudUnlock(ServerPlayer sp, int delayTicks) {
        hudUnlockTimers.put(sp.getUUID(), delayTicks);
    }

    private static void tickHudUnlock(ServerPlayer sp) {
        UUID id = sp.getUUID();
        if (!hudUnlockTimers.containsKey(id)) return;
        int t = hudUnlockTimers.get(id) - 1;
        if (t <= 0) { hudUnlockTimers.remove(id); unlockHud(sp); }
        else hudUnlockTimers.put(id, t);
    }

    // ── UTILS ─────────────────────────────────────────────────────────────────

    private static void assignRandomAbility(ServerPlayer sp) {
        AbilityType[] types = AbilityType.values();
        AbilityType chosen = types[sp.getRandom().nextInt(types.length)];
        PlayerAbilityData data = sp.getData(AbilityAttachment.ABILITY_DATA.get());
        data.setAbility(chosen);
        data.setCooldownRemaining(0);
        PacketHandler.sendToPlayer(sp, new TitleRevealPacket(chosen.name()));
        syncTo(sp, data);
        scheduleHudUnlock(sp, 340);
        powers.LOGGER.info("[Powers] Assigned {} to {}", chosen.name(), sp.getName().getString());
    }

    private static void syncTo(ServerPlayer sp, PlayerAbilityData data) {
        PacketHandler.sendToPlayer(sp, new SyncAbilityPacket(
                data.hasAbility() ? data.getAbility().name() : "",
                data.getCooldownRemaining(),
                data.hasAbility() ? data.getAbility().getCooldownTicks() : 0,
                data.isCharging(),
                data.getChargeTicks(),
                data.isHudUnlocked()));
    }

    private static List<LivingEntity> getNearby(ServerLevel level, ServerPlayer sp, double r) {
        return level.getEntitiesOfClass(LivingEntity.class, sp.getBoundingBox().inflate(r),
                e -> e != sp);
    }

    private static void drawDustLine(ServerLevel level, Vec3 from, Vec3 to, int steps) {
        int cyan = (0xFF << 24) | 0x00FFFF;
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            level.sendParticles(new DustParticleOptions(cyan, 0.6f),
                    from.x + (to.x - from.x) * t,
                    from.y + (to.y - from.y) * t,
                    from.z + (to.z - from.z) * t,
                    1, 0.05, 0.05, 0.05, 0);
        }
    }
}

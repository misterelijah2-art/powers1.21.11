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

    // ── ACTIVATION ───────────────────────────────────────────────────────────

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
        float charge = 0.5f + 0.5f * Math.min(1f, chargeTicks / 40f);
        triggerAbility(sp, type, level, charge);
        data.setCooldownRemaining(type.cooldownTicks);
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
                // Teleport 30 blocks in look direction + blindness to nearby
                Vec3 look = sp.getLookAngle();
                Vec3 dest = pos.add(look.normalize().scale(30.0 * charge));
                sp.teleportTo(dest.x, dest.y, dest.z);
                getNearby(level, sp, 4.0).forEach(e ->
                    e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0, false, false)));
                level.playSound(null, dest.x, dest.y, dest.z,
                        SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.2f, 0.5f);
                sp.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 40, 0, false, false));
            }

            case SOULFLARE -> {
                // 3-fireball spread at nearby targets
                List<LivingEntity> targets = getNearby(level, sp, 16.0 * charge);
                int shots = Math.min(3, targets.isEmpty() ? 1 : targets.size());
                Vec3 look = sp.getLookAngle();
                for (int i = 0; i < shots; i++) {
                    double spread = (i - shots / 2.0) * 0.15;
                    Vec3 dir = targets.isEmpty()
                            ? look.add(spread, 0, spread).normalize()
                            : targets.get(i).position().subtract(pos).normalize().add(spread, 0, spread);
                    LightningBolt bolt = new LightningBolt(EntityType.LIGHTNING_BOLT, level);
                    Vec3 bPos = pos.add(dir.scale(6.0 + i * 3.0));
                    bolt.setPos(bPos.x, bPos.y, bPos.z);
                    bolt.setVisualOnly(false);
                    level.addFreshEntity(bolt);
                }
                level.playSound(null, pos.x, pos.y, pos.z,
                        SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0f, 0.7f);
                sp.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 60, 0, false, false));
            }

            case GLACIAL_PULSE -> {
                // Freeze wave: slowness IV in 12-block radius
                getNearby(level, sp, 12.0 * charge).forEach(e -> {
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 3, false, false));
                    e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 1, false, false));
                });
                level.playSound(null, pos.x, pos.y, pos.z,
                        SoundEvents.POWDER_SNOW_FALL, SoundSource.PLAYERS, 1.2f, 0.4f);
                sp.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 1, false, false));
            }

            case WRAITH_SHROUD -> {
                // 10s invis + speed + nausea on nearest enemy
                sp.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 200, 0, false, false));
                sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 1, false, false));
                List<LivingEntity> near = getNearby(level, sp, 10.0);
                if (!near.isEmpty()) {
                    near.stream()
                        .min(Comparator.comparingDouble(e -> e.distanceTo(sp)))
                        .ifPresent(t -> t.addEffect(
                            new MobEffectInstance(MobEffects.CONFUSION, 120, 0, false, false)));
                }
                level.playSound(null, pos.x, pos.y, pos.z,
                        SoundEvents.WITCH_AMBIENT, SoundSource.PLAYERS, 0.8f, 0.6f);
            }

            case THUNDER_CRASH -> {
                // Knockback shockwave + lightning chain
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
                    Vec3 knockDir = target.position().subtract(pos).normalize();
                    target.setDeltaMovement(knockDir.x * 2.5, 0.6, knockDir.z * 2.5);
                    target.hurtMarked = true;
                    target.hurt(level.damageSources().playerAttack(sp), 6.0f * charge);
                    if (last != null)
                        drawDustLine(level, last.position().add(0,1,0), target.position().add(0,1,0), 12);
                    last = target;
                }
                sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 1, false, false));
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
        switch (type) {
            case VOIDSTEP -> {
                level.sendParticles(new DustParticleOptions(COLOR_VOIDSTEP, 0.5f),
                        pos.x + ox, pos.y, pos.z + oz, 1, 0.05, 0.1, 0.05, 0.0);
                level.sendParticles(new DustParticleOptions(COLOR_VOIDSTEP, 0.5f),
                        pos.x - ox, pos.y, pos.z - oz, 1, 0.05, 0.1, 0.05, 0.0);
            }
            case SOULFLARE -> {
                level.sendParticles(new DustParticleOptions(COLOR_SOULFLARE, 0.6f),
                        pos.x + ox * 0.5, pos.y, pos.z + oz * 0.5, 1, 0.1, 0.05, 0.1, 0.01);
            }
            case GLACIAL_PULSE -> {
                level.sendParticles(new DustParticleOptions(COLOR_GLACIAL_PULSE, 0.5f),
                        pos.x + ox, pos.y - 0.5, pos.z + oz, 1, 0.02, 0.02, 0.02, 0.0);
            }
            case WRAITH_SHROUD -> {
                level.sendParticles(new DustParticleOptions(COLOR_WRAITH_SHROUD, 0.4f),
                        pos.x + ox * 0.4, pos.y + 0.5, pos.z + oz * 0.4, 1, 0.02, 0.15, 0.02, 0.0);
            }
            case THUNDER_CRASH -> {
                level.sendParticles(new DustParticleOptions(COLOR_THUNDER_CRASH, 0.5f),
                        pos.x + ox, pos.y + 0.3, pos.z + oz, 1, 0.05, 0.05, 0.05, 0.02);
            }
        }
    }

    // ── MAGMA-STYLE cage pulse (soulflare after-burn) ─────────────────────────

    private static void pulseMagmaCage(ServerPlayer sp, ServerLevel level, float dmg, boolean finalBurst) {
        Vec3 pos = sp.position();
        getNearby(level, sp, 4.0).forEach(e -> {
            e.hurt(level.damageSources().playerAttack(sp), dmg);
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 2, false, false));
            if (!finalBurst) e.igniteForSeconds(1);
        });
        level.playSound(null, pos.x, pos.y, pos.z,
                SoundEvents.FIRE_AMBIENT, SoundSource.PLAYERS, 0.6f,
                0.8f + (float)(Math.random() * 0.4));
        if (finalBurst)
            level.playSound(null, pos.x, pos.y, pos.z,
                    SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.7f, 1.4f);
    }

    // ── NULL RIFT detonation (now used for Voidstep aftershock) ──────────────

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

    // ── DEATH MARK reflection (still used internally for future expansion) ───

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
        if (!data.isHudUnlocked()) {
            data.unlockHud();
            syncTo(sp, data);
        }
    }

    private static void scheduleHudUnlock(ServerPlayer sp, int delayTicks) {
        hudUnlockTimers.put(sp.getUUID(), delayTicks);
    }

    private static void tickHudUnlock(ServerPlayer sp) {
        UUID id = sp.getUUID();
        if (!hudUnlockTimers.containsKey(id)) return;
        int t = hudUnlockTimers.get(id) - 1;
        if (t <= 0) {
            hudUnlockTimers.remove(id);
            unlockHud(sp);
        } else {
            hudUnlockTimers.put(id, t);
        }
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
                data.hasAbility() ? data.getAbility().cooldownTicks : 0,
                data.isCharging(),
                data.getChargeTicks(),
                data.isHudUnlocked()));
    }

    private static List<LivingEntity> getNearby(ServerLevel level, ServerPlayer sp, double r) {
        return level.getEntitiesOfClass(LivingEntity.class, sp.getBoundingBox().inflate(r),
                e -> e != sp);
    }

    private static void spawnWitherCloud(ServerLevel level, Vec3 pos) {
        int darkPurple = (0xFF << 24) | 0x1A001A;
        for (int i = 0; i < 3; i++) {
            level.sendParticles(new DustParticleOptions(darkPurple, 1.2f),
                    pos.x, pos.y + 0.5 + i * 0.4, pos.z, 8, 0.4, 0.2, 0.4, 0.02);
        }
        level.playSound(null, pos.x, pos.y, pos.z,
                SoundEvents.WITHER_AMBIENT, SoundSource.PLAYERS, 0.4f, 1.8f);
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

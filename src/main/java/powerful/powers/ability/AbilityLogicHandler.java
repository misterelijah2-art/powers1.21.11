package powerful.powers.ability;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import powerful.powers.network.PacketHandler;
import powerful.powers.network.SyncAbilityPacket;
import powerful.powers.network.TitleRevealPacket;
import powerful.powers.powers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Server-side event handler:
 * - 10-second delayed ability assignment on join
 * - Ability activation (via packet from client)
 * - Ability ticking (cooldown + active effects)
 * - PvP death ability drop
 * - Respawn persistence (natural death keeps ability)
 */
@EventBusSubscriber(modid = powers.MODID, bus = EventBusSubscriber.Bus.GAME)
public class AbilityLogicHandler {

    // Tracks how many ticks since a player joined (for 10-second delay)
    private static final Map<UUID, Integer> joinTimers = new HashMap<>();
    // Prevent double-assigning on rapid re-joins
    private static final java.util.Set<UUID> assigned = new java.util.HashSet<>();

    private static final int ASSIGN_DELAY_TICKS = 200; // 10 seconds

    // ---- JOIN: start the 10-second countdown ----

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            UUID id = sp.getUUID();
            PlayerAbilityData data = sp.getData(AbilityAttachment.ABILITY_DATA.get());
            if (!data.hasAbility() && !assigned.contains(id)) {
                joinTimers.put(id, 0);
            }
            // Always sync ability to client on join
            PacketHandler.sendToPlayer(sp, new SyncAbilityPacket(
                    data.hasAbility() ? data.getAbility().name() : "",
                    data.getCooldownRemaining(),
                    data.getAbility() != null ? data.getAbility().getCooldownTicks() : 0
            ));
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            joinTimers.remove(sp.getUUID());
        }
    }

    // ---- TICK: countdown, ability effects, cooldown ----

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        UUID id = sp.getUUID();
        ServerLevel level = sp.serverLevel();

        // --- Assignment countdown ---
        if (joinTimers.containsKey(id)) {
            int ticks = joinTimers.get(id) + 1;
            joinTimers.put(id, ticks);
            if (ticks >= ASSIGN_DELAY_TICKS) {
                joinTimers.remove(id);
                assigned.add(id);
                assignRandomAbility(sp);
            }
        }

        // --- Active ability effect tick ---
        PlayerAbilityData data = sp.getData(AbilityAttachment.ABILITY_DATA.get());
        data.tickCooldown();
        if (data.isActive()) {
            applyActiveEffect(sp, data.getAbility(), level);
            data.tickActive();
        }

        // Sync cooldown to client every 5 ticks to keep HUD smooth
        if (sp.tickCount % 5 == 0) {
            PacketHandler.sendToPlayer(sp, new SyncAbilityPacket(
                    data.hasAbility() ? data.getAbility().name() : "",
                    data.getCooldownRemaining(),
                    data.hasAbility() ? data.getAbility().getCooldownTicks() : 0
            ));
        }
    }

    // ---- ABILITY ACTIVATION (called from server when client sends UseAbilityPacket) ----

    public static void activateAbility(ServerPlayer sp) {
        PlayerAbilityData data = sp.getData(AbilityAttachment.ABILITY_DATA.get());
        if (!data.hasAbility()) return;
        if (data.isOnCooldown()) {
            sp.sendSystemMessage(Component.literal("§cAbility is on cooldown!"));
            return;
        }
        AbilityType type = data.getAbility();
        ServerLevel level = sp.serverLevel();

        // Trigger the ability
        triggerAbility(sp, type, level);

        // Start cooldown and active duration
        data.setCooldownRemaining(type.getCooldownTicks());
        data.setActiveRemaining(type.getDurationTicks());

        // Sync immediately
        PacketHandler.sendToPlayer(sp, new SyncAbilityPacket(
                type.name(),
                data.getCooldownRemaining(),
                type.getCooldownTicks()
        ));
    }

    // ---- ABILITY EFFECTS ----

    private static void triggerAbility(ServerPlayer sp, AbilityType type, ServerLevel level) {
        Vec3 pos = sp.position();
        switch (type) {
            case VOID_STEP -> {
                // Teleport 8 blocks in look direction, leave shadow particles
                Vec3 look = sp.getLookAngle().normalize().scale(8.0);
                Vec3 dest = pos.add(look);
                spawnParticles(level, pos, ParticleTypes.PORTAL, 30);
                sp.teleportTo(dest.x, dest.y, dest.z);
                spawnParticles(level, dest, ParticleTypes.PORTAL, 30);
                level.playSound(null, dest.x, dest.y, dest.z,
                        SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 0.8f);
                sp.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 40, 0, false, false));
            }
            case INFERNO_BURST -> {
                // Damage + fire nearby entities, engulf self in flame particles
                spawnParticles(level, pos, ParticleTypes.FLAME, 60);
                spawnParticles(level, pos, ParticleTypes.LAVA, 20);
                level.playSound(null, pos.x, pos.y, pos.z,
                        SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0f, 0.7f);
                level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,
                        sp.getBoundingBox().inflate(5.0))
                    .stream()
                    .filter(e -> e != sp)
                    .forEach(e -> {
                        e.hurt(level.damageSources().playerAttack(sp), 6.0f);
                        e.setSecondsOnFire(5);
                    });
                sp.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 100, 0, false, false));
            }
            case SOUL_ANCHOR -> {
                // Creates a protective barrier: high resistance + regeneration
                spawnParticles(level, pos, ParticleTypes.SOUL, 50);
                spawnParticles(level, pos, ParticleTypes.SOUL_FIRE_FLAME, 20);
                level.playSound(null, pos.x, pos.y, pos.z,
                        SoundEvents.AMBIENT_SOUL_SAND_VALLEY_MOOD, SoundSource.PLAYERS, 1.0f, 1.2f);
                sp.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 80 * 20, 2, false, false));
                sp.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40 * 20, 1, false, false));
            }
            case PHANTOM_LUNGE -> {
                // Launch player forward at extreme speed + brief invisibility
                Vec3 look = sp.getLookAngle().normalize();
                sp.setDeltaMovement(look.x * 3.5, look.y * 1.5 + 0.4, look.z * 3.5);
                sp.hurtMarked = true;
                spawnParticles(level, pos, ParticleTypes.SWEEP_ATTACK, 25);
                spawnParticles(level, pos, ParticleTypes.CLOUD, 15);
                level.playSound(null, pos.x, pos.y, pos.z,
                        SoundEvents.PHANTOM_SWOOP, SoundSource.PLAYERS, 1.0f, 1.4f);
                sp.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 40, 0, false, false));
            }
            case THUNDER_MARK -> {
                // Strike all nearby enemies with lightning + glowing mark
                spawnParticles(level, pos, ParticleTypes.ELECTRIC_SPARK, 80);
                level.playSound(null, pos.x, pos.y, pos.z,
                        SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 1.0f, 1.0f);
                level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,
                        sp.getBoundingBox().inflate(6.0))
                    .stream()
                    .filter(e -> e != sp)
                    .forEach(e -> {
                        net.minecraft.world.entity.LightningBolt bolt =
                            new net.minecraft.world.entity.LightningBolt(
                                net.minecraft.world.entity.EntityType.LIGHTNING_BOLT, level);
                        bolt.moveTo(e.position());
                        bolt.setVisualOnly(false);
                        level.addFreshEntity(bolt);
                        e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0, false, false));
                    });
                sp.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 60, 1, false, false));
            }
        }
    }

    /** Called every tick while the ability is active. */
    private static void applyActiveEffect(ServerPlayer sp, AbilityType type, ServerLevel level) {
        Vec3 pos = sp.position();
        switch (type) {
            case VOID_STEP      -> spawnParticles(level, pos, ParticleTypes.PORTAL, 3);
            case INFERNO_BURST  -> spawnParticles(level, pos, ParticleTypes.FLAME, 2);
            case SOUL_ANCHOR    -> spawnParticles(level, pos, ParticleTypes.SOUL, 3);
            case PHANTOM_LUNGE  -> spawnParticles(level, pos, ParticleTypes.CLOUD, 2);
            case THUNDER_MARK   -> spawnParticles(level, pos, ParticleTypes.ELECTRIC_SPARK, 4);
        }
    }

    // ---- DEATH: drop orb only on PvP kill ----

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer victim)) return;
        // Only drop if killed by another player
        if (!(event.getSource().getEntity() instanceof ServerPlayer)) return;

        PlayerAbilityData data = victim.getData(AbilityAttachment.ABILITY_DATA.get());
        if (!data.hasAbility()) return;

        AbilityType type = data.getAbility();
        ServerLevel level = victim.serverLevel();
        Vec3 pos = victim.position();

        // Drop the orb item into the world
        ItemStack orb = AbilityItem.forAbility(type);
        ItemEntity itemEntity = new ItemEntity(level, pos.x, pos.y + 0.5, pos.z, orb);
        itemEntity.setPickUpDelay(20); // 1 second pickup delay
        level.addFreshEntity(itemEntity);
        spawnParticles(level, pos, ParticleTypes.TOTEM_OF_UNDYING, 40);
        level.playSound(null, pos.x, pos.y, pos.z,
                SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0f, 0.6f);

        // Remove ability from victim
        data.clearAbility();
        assigned.remove(victim.getUUID());

        victim.sendSystemMessage(Component.literal("§cYou lost your power! It dropped on the ground."));
        PacketHandler.sendToPlayer(victim, new SyncAbilityPacket("", 0, 0));
    }

    // ---- RESPAWN: preserve ability on natural death ----

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            // Only copy if no PvP (pvp death already cleared it above; but clone fires after)
            // We copy whatever is currently stored (may be empty if pvp cleared it)
            PlayerAbilityData oldData = event.getOriginal().getData(AbilityAttachment.ABILITY_DATA.get());
            PlayerAbilityData newData = event.getEntity().getData(AbilityAttachment.ABILITY_DATA.get());
            if (oldData.hasAbility()) {
                newData.setAbility(oldData.getAbility());
                newData.setCooldownRemaining(oldData.getCooldownRemaining());
                // Mark as assigned so they don't get re-assigned
                assigned.add(event.getEntity().getUUID());
            }
        }
    }

    // ---- UTILITY ----

    private static void assignRandomAbility(ServerPlayer sp) {
        AbilityType[] types = AbilityType.values();
        AbilityType chosen = types[sp.getRandom().nextInt(types.length)];
        PlayerAbilityData data = sp.getData(AbilityAttachment.ABILITY_DATA.get());
        data.setAbility(chosen);
        data.setCooldownRemaining(0);

        // Send title reveal packet to client
        PacketHandler.sendToPlayer(sp, new TitleRevealPacket(chosen.name()));
        // Sync ability state
        PacketHandler.sendToPlayer(sp, new SyncAbilityPacket(
                chosen.name(), 0, chosen.getCooldownTicks()));

        powers.LOGGER.info("[Powers] Assigned {} to {}", chosen.name(), sp.getName().getString());
    }

    private static void spawnParticles(ServerLevel level, Vec3 pos,
            net.minecraft.core.particles.SimpleParticleType particle, int count) {
        level.sendParticles(particle, pos.x, pos.y + 1.0, pos.z, count,
                0.5, 0.5, 0.5, 0.15);
    }
}

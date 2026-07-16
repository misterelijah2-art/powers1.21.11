package powerful.powers.ability;

import net.minecraft.core.particles.ParticleTypes;
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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Server-side game event handler.
 * NeoForge 21.11: @EventBusSubscriber defaults to FORGE bus — no Bus.GAME needed.
 */
@EventBusSubscriber(modid = powers.MODID)
public class AbilityLogicHandler {

    private static final Map<UUID, Integer> joinTimers = new HashMap<>();
    private static final Set<UUID> assigned = new HashSet<>();
    private static final int ASSIGN_DELAY_TICKS = 200;

    // ---- JOIN ----

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        UUID id = sp.getUUID();
        PlayerAbilityData data = sp.getData(AbilityAttachment.ABILITY_DATA.get());
        if (!data.hasAbility() && !assigned.contains(id)) {
            joinTimers.put(id, 0);
        }
        PacketHandler.sendToPlayer(sp, new SyncAbilityPacket(
            data.hasAbility() ? data.getAbility().name() : "",
            data.getCooldownRemaining(),
            data.hasAbility() ? data.getAbility().getCooldownTicks() : 0
        ));
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            joinTimers.remove(sp.getUUID());
        }
    }

    // ---- TICK ----

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        UUID id = sp.getUUID();
        ServerLevel level = (ServerLevel) sp.level();

        if (joinTimers.containsKey(id)) {
            int ticks = joinTimers.get(id) + 1;
            joinTimers.put(id, ticks);
            if (ticks >= ASSIGN_DELAY_TICKS) {
                joinTimers.remove(id);
                assigned.add(id);
                assignRandomAbility(sp);
            }
        }

        PlayerAbilityData data = sp.getData(AbilityAttachment.ABILITY_DATA.get());
        data.tickCooldown();
        if (data.isActive()) {
            applyActiveEffect(sp, data.getAbility(), level);
            data.tickActive();
        }

        if (sp.tickCount % 5 == 0) {
            PacketHandler.sendToPlayer(sp, new SyncAbilityPacket(
                data.hasAbility() ? data.getAbility().name() : "",
                data.getCooldownRemaining(),
                data.hasAbility() ? data.getAbility().getCooldownTicks() : 0
            ));
        }
    }

    // ---- ACTIVATION ----

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
        PacketHandler.sendToPlayer(sp, new SyncAbilityPacket(
            type.name(), data.getCooldownRemaining(), type.getCooldownTicks()
        ));
    }

    // ---- ABILITY TRIGGERS ----

    private static void triggerAbility(ServerPlayer sp, AbilityType type, ServerLevel level) {
        Vec3 pos = sp.position();
        switch (type) {
            case VOID_STEP -> {
                Vec3 dest = pos.add(sp.getLookAngle().normalize().scale(8.0));
                spawnParticles(level, pos, ParticleTypes.PORTAL, 30);
                sp.teleportTo(dest.x, dest.y, dest.z);
                spawnParticles(level, dest, ParticleTypes.PORTAL, 30);
                level.playSound(null, dest.x, dest.y, dest.z,
                    SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 0.8f);
                sp.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 40, 0, false, false));
            }
            case INFERNO_BURST -> {
                spawnParticles(level, pos, ParticleTypes.FLAME, 60);
                spawnParticles(level, pos, ParticleTypes.LAVA, 20);
                level.playSound(null, pos.x, pos.y, pos.z,
                    SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0f, 0.7f);
                level.getEntitiesOfClass(LivingEntity.class, sp.getBoundingBox().inflate(5.0))
                    .stream().filter(e -> e != sp).forEach(e -> {
                        e.hurt(level.damageSources().playerAttack(sp), 6.0f);
                        e.igniteForSeconds(5); // 1.21.11: replaces setSecondsOnFire
                    });
                sp.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 100, 0, false, false));
            }
            case SOUL_ANCHOR -> {
                spawnParticles(level, pos, ParticleTypes.SOUL, 50);
                spawnParticles(level, pos, ParticleTypes.SOUL_FIRE_FLAME, 20);
                level.playSound(null, pos.x, pos.y, pos.z,
                    SoundEvents.AMBIENT_SOUL_SAND_VALLEY_MOOD, SoundSource.PLAYERS, 1.0f, 1.2f);
                sp.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 80 * 20, 2, false, false)); // 1.21.11: DAMAGE_RESISTANCE -> RESISTANCE
                sp.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40 * 20, 1, false, false));
            }
            case PHANTOM_LUNGE -> {
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
                spawnParticles(level, pos, ParticleTypes.ELECTRIC_SPARK, 80);
                level.playSound(null, pos.x, pos.y, pos.z,
                    SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 1.0f, 1.0f);
                level.getEntitiesOfClass(LivingEntity.class, sp.getBoundingBox().inflate(6.0))
                    .stream().filter(e -> e != sp).forEach(e -> {
                        LightningBolt bolt = new LightningBolt(EntityType.LIGHTNING_BOLT, level);
                        bolt.moveTo(e.getX(), e.getY(), e.getZ()); // 1.21.11: moveTo(x,y,z) not moveTo(Vec3)
                        bolt.setVisualOnly(false);
                        level.addFreshEntity(bolt);
                        e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0, false, false));
                    });
                sp.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 60, 1, false, false));
            }
        }
    }

    private static void applyActiveEffect(ServerPlayer sp, AbilityType type, ServerLevel level) {
        Vec3 pos = sp.position();
        switch (type) {
            case VOID_STEP     -> spawnParticles(level, pos, ParticleTypes.PORTAL, 3);
            case INFERNO_BURST -> spawnParticles(level, pos, ParticleTypes.FLAME, 2);
            case SOUL_ANCHOR   -> spawnParticles(level, pos, ParticleTypes.SOUL, 3);
            case PHANTOM_LUNGE -> spawnParticles(level, pos, ParticleTypes.CLOUD, 2);
            case THUNDER_MARK  -> spawnParticles(level, pos, ParticleTypes.ELECTRIC_SPARK, 4);
        }
    }

    // ---- PvP DEATH DROP ----

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
        spawnParticles(level, pos, ParticleTypes.TOTEM_OF_UNDYING, 40);
        level.playSound(null, pos.x, pos.y, pos.z,
            SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0f, 0.6f);

        data.clearAbility();
        assigned.remove(victim.getUUID());
        victim.displayClientMessage(Component.literal("\u00a7cYou lost your power! It dropped on the ground."), false);
        PacketHandler.sendToPlayer(victim, new SyncAbilityPacket("", 0, 0));
    }

    // ---- RESPAWN: keep ability on natural death ----

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

    // ---- UTIL ----

    private static void assignRandomAbility(ServerPlayer sp) {
        AbilityType[] types = AbilityType.values();
        AbilityType chosen = types[sp.getRandom().nextInt(types.length)];
        PlayerAbilityData data = sp.getData(AbilityAttachment.ABILITY_DATA.get());
        data.setAbility(chosen);
        data.setCooldownRemaining(0);
        PacketHandler.sendToPlayer(sp, new TitleRevealPacket(chosen.name()));
        PacketHandler.sendToPlayer(sp, new SyncAbilityPacket(
            chosen.name(), 0, chosen.getCooldownTicks()
        ));
        powers.LOGGER.info("[Powers] Assigned {} to {}", chosen.name(), sp.getName().getString());
    }

    private static void spawnParticles(ServerLevel level, Vec3 pos,
            net.minecraft.core.particles.SimpleParticleType particle, int count) {
        level.sendParticles(particle, pos.x, pos.y + 1.0, pos.z, count, 0.5, 0.5, 0.5, 0.15);
    }
}

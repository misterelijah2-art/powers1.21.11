package powerful.powers.ability;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import powerful.powers.ability.abilities.*;
import powerful.powers.item.AbilityOrbItem;
import powerful.powers.network.SyncAbilityPacket;
import powerful.powers.network.UseAbilityPacket;
import powerful.powers.util.AbilityNBTHelper;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AbilityManager {

    // Server-side ability state per player UUID
    private static final Map<UUID, AbilityData> ABILITY_MAP = new ConcurrentHashMap<>();

    // -------------------------------------------------------------------------
    // Assignment
    // -------------------------------------------------------------------------

    public static void assignAbility(Player player, AbilityData data) {
        ABILITY_MAP.put(player.getUUID(), data);
        AbilityNBTHelper.save(player, data);
        if (player instanceof ServerPlayer sp) {
            syncToClient(sp, data);
        }
    }

    public static void assignRandom(Player player) {
        AbilityData data = new AbilityData(AbilityType.random(), 0);
        data.announced = false;
        assignAbility(player, data);
    }

    public static AbilityData get(UUID uuid) {
        return ABILITY_MAP.get(uuid);
    }

    public static AbilityData get(Player player) {
        return ABILITY_MAP.get(player.getUUID());
    }

    public static void markAnnounced(Player player) {
        AbilityData data = get(player);
        if (data != null) {
            data.announced = true;
            if (player instanceof ServerPlayer sp) syncToClient(sp, data);
        }
    }

    // -------------------------------------------------------------------------
    // Server tick (called from PlayerJoinHandler tick via server tick event)
    // -------------------------------------------------------------------------

    public static void serverTick(ServerLevel level) {
        for (ServerPlayer sp : level.players()) {
            AbilityData data = ABILITY_MAP.get(sp.getUUID());
            if (data == null) continue;

            // Tick down cooldown
            if (data.cooldownTicks > 0) {
                data.cooldownTicks--;
                // Sync every 4 ticks to reduce packet spam
                if (data.cooldownTicks % 4 == 0) {
                    syncToClient(sp, data);
                }
                // Sync the exact moment it hits 0
                if (data.cooldownTicks == 0) {
                    syncToClient(sp, data);
                }
            }

            // Passive ambient FX when ready and announced
            if (data.isReady() && data.announced) {
                long tick = level.getGameTime();
                switch (data.type) {
                    case VOIDSTEP       -> VoidstepAbility.doPassive(sp, level, tick);
                    case SOULFLARE      -> SoulflareAbility.doPassive(sp, level, tick);
                    case GLACIAL_PULSE  -> GlacialPulseAbility.doPassive(sp, level, tick);
                    case WRAITH_SHROUD  -> WraithShroudAbility.doPassive(sp, level, tick);
                    case THUNDER_CRASH  -> ThunderCrashAbility.doPassive(sp, level, tick);
                }
            }

            AbilityNBTHelper.save(sp, data);
        }
    }

    // -------------------------------------------------------------------------
    // Use ability (called from UseAbilityPacket handler on server)
    // -------------------------------------------------------------------------

    public static void onUseAbilityPacket(UseAbilityPacket packet,
            net.neoforged.neoforge.network.handling.IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer sp)) return;
            AbilityData data = ABILITY_MAP.get(sp.getUUID());
            if (data == null || !data.announced) return;

            if (packet.pressed()) {
                // Start charging
                data.charging = true;
                data.chargeTicks = 0;
            } else {
                // Key released -> fire if ready
                data.chargeTicks = packet.chargeTicks();
                data.charging = false;
                if (data.isReady()) {
                    fireAbility(sp, data);
                }
            }
        });
    }

    private static void fireAbility(ServerPlayer player, AbilityData data) {
        if (!(player.level() instanceof ServerLevel sl)) return;
        switch (data.type) {
            case VOIDSTEP       -> VoidstepAbility.execute(player, data, sl);
            case SOULFLARE      -> SoulflareAbility.execute(player, data, sl);
            case GLACIAL_PULSE  -> GlacialPulseAbility.execute(player, data, sl);
            case WRAITH_SHROUD  -> WraithShroudAbility.execute(player, data, sl);
            case THUNDER_CRASH  -> ThunderCrashAbility.execute(player, data, sl);
        }
        data.cooldownTicks = data.type.cooldownTicks;
        data.chargeTicks = 0;
        syncToClient(player, data);
    }

    // -------------------------------------------------------------------------
    // Death handling
    // -------------------------------------------------------------------------

    /**
     * Called on player death.
     * If killed by another player -> drop orb with remaining cooldown.
     * Natural death -> retain ability.
     */
    public static void onPlayerDeath(ServerPlayer dead, DamageSource source) {
        AbilityData data = ABILITY_MAP.get(dead.getUUID());
        if (data == null) return;

        // Check if a player delivered the killing blow
        boolean pvpKill = source.getEntity() instanceof Player killer
                && killer != dead;

        if (pvpKill) {
            // Drop orb with remaining cooldown
            net.minecraft.world.item.ItemStack orb =
                    AbilityOrbItem.createOrb(data.type, data.cooldownTicks);

            // Drop at death position
            net.minecraft.world.entity.item.ItemEntity ie =
                    new net.minecraft.world.entity.item.ItemEntity(
                            dead.level(),
                            dead.getX(), dead.getY() + 0.5, dead.getZ(),
                            orb
                    );
            ie.setPickUpDelay(10);
            ie.setNeverPickUp(); // must be right-clicked, not walked-over
            dead.level().addFreshEntity(ie);

            // Remove from dead player
            ABILITY_MAP.remove(dead.getUUID());
            AbilityNBTHelper.clear(dead);
            syncToClient(dead, null);
        }
        // else: natural death - do nothing, ability persists via NBT
    }

    // -------------------------------------------------------------------------
    // Join handling
    // -------------------------------------------------------------------------

    public static void onPlayerJoin(ServerPlayer player) {
        // Try to restore from NBT first (returning player)
        AbilityData saved = AbilityNBTHelper.load(player);
        if (saved != null) {
            ABILITY_MAP.put(player.getUUID(), saved);
            syncToClient(player, saved);
            return;
        }
        // New player -> schedule assignment after 10s (handled in PlayerJoinHandler)
    }

    public static void onPlayerLeave(ServerPlayer player) {
        AbilityData data = ABILITY_MAP.get(player.getUUID());
        if (data != null) {
            AbilityNBTHelper.save(player, data);
        }
        ABILITY_MAP.remove(player.getUUID());
    }

    // -------------------------------------------------------------------------
    // Sync
    // -------------------------------------------------------------------------

    public static void syncToClient(ServerPlayer player, AbilityData data) {
        String name = (data != null && data.type != null) ? data.type.name() : "";
        int cd = (data != null) ? data.cooldownTicks : 0;
        boolean announced = (data != null) && data.announced;
        PacketDistributor.sendToPlayer(player,
                new SyncAbilityPacket(name, cd, announced));
    }
}

package powerful.powers.ability;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import powerful.powers.ability.abilities.*;
import powerful.powers.network.PacketHandler;
import powerful.powers.network.SyncAbilityPacket;
import powerful.powers.network.TitleRevealPacket;
import powerful.powers.network.UseAbilityPacket;
import powerful.powers.util.AbilityNBTHelper;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AbilityManager {

    private static final Map<UUID, AbilityData>  ABILITY_MAP        = new ConcurrentHashMap<>();
    /** Ticks remaining until HUD unlocks after an announcement was triggered. */
    private static final Map<UUID, Integer>      announcementTimers = new ConcurrentHashMap<>();

    // -------------------------------------------------------------------------
    // Assignment
    // -------------------------------------------------------------------------

    public static void assignAbility(Player player, AbilityData data) {
        ABILITY_MAP.put(player.getUUID(), data);
        AbilityNBTHelper.save(player, data);
        if (player instanceof ServerPlayer sp) syncToClient(sp, data);
    }

    public static void assignRandom(Player player) {
        AbilityData data = new AbilityData(AbilityType.random(), 0);
        data.announced = false;
        assignAbility(player, data);
    }

    public static AbilityData get(UUID uuid)    { return ABILITY_MAP.get(uuid); }
    public static AbilityData get(Player player){ return ABILITY_MAP.get(player.getUUID()); }

    /**
     * Sends the TitleReveal packet and schedules HUD unlock after 340 ticks.
     * Called after a player is assigned an ability (join or orb pickup).
     */
    public static void scheduleAnnouncement(ServerPlayer sp) {
        AbilityData data = ABILITY_MAP.get(sp.getUUID());
        if (data == null) return;
        PacketHandler.sendToPlayer(sp, new TitleRevealPacket(data.type.name()));
        announcementTimers.put(sp.getUUID(), 340);
    }

    // -------------------------------------------------------------------------
    // Server tick
    // -------------------------------------------------------------------------

    public static void serverTick(ServerLevel level) {
        for (ServerPlayer sp : level.players()) {
            UUID id = sp.getUUID();
            AbilityData data = ABILITY_MAP.get(id);

            // --- Announcement timer ---
            Integer announceTicks = announcementTimers.get(id);
            if (announceTicks != null) {
                int next = announceTicks - 1;
                if (next <= 0) {
                    announcementTimers.remove(id);
                    if (data != null) {
                        data.announced = true;
                        syncToClient(sp, data);
                    }
                } else {
                    announcementTimers.put(id, next);
                }
            }

            if (data == null) continue;

            // --- Cooldown tick ---
            if (data.cooldownTicks > 0) {
                data.cooldownTicks--;
                if (data.cooldownTicks % 4 == 0 || data.cooldownTicks == 0)
                    syncToClient(sp, data);
            }

            // --- Passive ambient FX ---
            if (data.isReady() && data.announced) {
                long tick = level.getGameTime();
                switch (data.type) {
                    case VOIDSTEP      -> VoidstepAbility.doPassive(sp, level, tick);
                    case SOULFLARE     -> SoulflareAbility.doPassive(sp, level, tick);
                    case GLACIAL_PULSE -> GlacialPulseAbility.doPassive(sp, level, tick);
                    case WRAITH_SHROUD -> WraithShroudAbility.doPassive(sp, level, tick);
                    case THUNDER_CRASH -> ThunderCrashAbility.doPassive(sp, level, tick);
                }
            }

            AbilityNBTHelper.save(sp, data);
        }
    }

    // -------------------------------------------------------------------------
    // Packet handlers (called from ModNetwork)
    // -------------------------------------------------------------------------

    public static void onUseAbilityPacket(UseAbilityPacket packet,
            net.neoforged.neoforge.network.handling.IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer sp)) return;
            AbilityData data = ABILITY_MAP.get(sp.getUUID());
            if (data == null || !data.announced) return;

            if (packet.pressed()) {
                data.charging    = true;
                data.chargeTicks = 0;
            } else {
                data.chargeTicks = packet.chargeTicks();
                data.charging    = false;
                if (data.isReady()) fireAbility(sp, data);
            }
        });
    }

    public static void onChargeStart(ServerPlayer sp) {
        AbilityData data = ABILITY_MAP.get(sp.getUUID());
        if (data == null || !data.announced) return;
        data.charging    = true;
        data.chargeTicks = 0;
    }

    public static void onChargeRelease(ServerPlayer sp) {
        AbilityData data = ABILITY_MAP.get(sp.getUUID());
        if (data == null || !data.announced) return;
        data.charging = false;
        if (data.isReady()) fireAbility(sp, data);
    }

    private static void fireAbility(ServerPlayer player, AbilityData data) {
        if (!(player.level() instanceof ServerLevel sl)) return;
        switch (data.type) {
            case VOIDSTEP      -> VoidstepAbility.execute(player, data, sl);
            case SOULFLARE     -> SoulflareAbility.execute(player, data, sl);
            case GLACIAL_PULSE -> GlacialPulseAbility.execute(player, data, sl);
            case WRAITH_SHROUD -> WraithShroudAbility.execute(player, data, sl);
            case THUNDER_CRASH -> ThunderCrashAbility.execute(player, data, sl);
        }
        data.cooldownTicks = data.type.cooldownTicks;
        data.chargeTicks   = 0;
        syncToClient(player, data);
    }

    // -------------------------------------------------------------------------
    // Death handling
    // -------------------------------------------------------------------------

    public static void onPlayerDeath(ServerPlayer dead, DamageSource source) {
        AbilityData data = ABILITY_MAP.get(dead.getUUID());
        if (data == null) return;

        boolean pvpKill = source.getEntity() instanceof Player killer && killer != dead;
        if (pvpKill) {
            ItemStack orb = AbilityItem.forAbility(data.type, data.cooldownTicks);
            ItemEntity ie = new ItemEntity(
                    dead.level(),
                    dead.getX(), dead.getY() + 0.5, dead.getZ(),
                    orb);
            ie.setPickUpDelay(10);
            ie.setNeverPickUp();
            dead.level().addFreshEntity(ie);

            ABILITY_MAP.remove(dead.getUUID());
            announcementTimers.remove(dead.getUUID());
            AbilityNBTHelper.clear(dead);
            syncToClient(dead, null);
        }
        // Natural death: retain ability via NBT
    }

    // -------------------------------------------------------------------------
    // Join / Leave
    // -------------------------------------------------------------------------

    public static void onPlayerJoin(ServerPlayer player) {
        AbilityData saved = AbilityNBTHelper.load(player);
        if (saved != null) {
            ABILITY_MAP.put(player.getUUID(), saved);
            syncToClient(player, saved);
        }
        // No ability yet -> PlayerJoinHandler schedules assignment after 10 s
    }

    public static void onPlayerLeave(ServerPlayer player) {
        AbilityData data = ABILITY_MAP.get(player.getUUID());
        if (data != null) AbilityNBTHelper.save(player, data);
        ABILITY_MAP.remove(player.getUUID());
        announcementTimers.remove(player.getUUID());
    }

    // -------------------------------------------------------------------------
    // Sync
    // -------------------------------------------------------------------------

    public static void syncToClient(ServerPlayer player, AbilityData data) {
        String  name     = (data != null && data.type != null) ? data.type.name() : "";
        int     cd       = (data != null) ? data.cooldownTicks : 0;
        int     maxCd    = (data != null && data.type != null) ? data.type.cooldownTicks : 0;
        boolean charging = (data != null) && data.charging;
        int     charge   = (data != null) ? data.chargeTicks : 0;
        boolean unlocked = (data != null) && data.announced;
        PacketDistributor.sendToPlayer(player,
                new SyncAbilityPacket(name, cd, maxCd, charging, charge, unlocked));
    }
}

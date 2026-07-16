package powerful.powers.event;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import powerful.powers.ability.AbilityManager;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles:
 * - Player join: 10-second delayed ability assignment for new players.
 * - Server tick: drives AbilityManager ticks and the join countdown.
 */
public class PlayerJoinHandler {

    // UUID -> game-time tick at which the ability should be assigned
    private static final Map<UUID, Long> PENDING_ASSIGN = new ConcurrentHashMap<>();
    private static final int DELAY_TICKS = 200; // 10 seconds

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        AbilityManager.onPlayerJoin(sp);

        // If onPlayerJoin found a saved ability, no need to schedule
        if (AbilityManager.get(sp) == null) {
            long assignAt = sp.serverLevel().getGameTime() + DELAY_TICKS;
            PENDING_ASSIGN.put(sp.getUUID(), assignAt);
        }
    }

    @SubscribeEvent
    public static void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        PENDING_ASSIGN.remove(sp.getUUID());
        AbilityManager.onPlayerLeave(sp);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        // Tick ability manager for each level
        for (net.minecraft.server.level.ServerLevel level :
                event.getServer().getAllLevels()) {
            AbilityManager.serverTick(level);
        }

        // Process pending delayed assignments
        if (PENDING_ASSIGN.isEmpty()) return;
        long now = event.getServer().overworld().getGameTime();
        PENDING_ASSIGN.entrySet().removeIf(entry -> {
            if (now < entry.getValue()) return false;
            ServerPlayer sp = event.getServer().getPlayerList()
                    .getPlayer(entry.getKey());
            if (sp == null) return true; // player left
            AbilityManager.assignRandom(sp);
            return true;
        });
    }
}

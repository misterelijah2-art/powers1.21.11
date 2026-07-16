package powerful.powers.event;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import powerful.powers.ability.AbilityAttachment;
import powerful.powers.ability.PlayerAbilityData;
import powerful.powers.network.PacketHandler;
import powerful.powers.network.SyncAbilityPacket;
import powerful.powers.powers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles the initial 10-second join delay for ability assignment.
 * The actual assignment is done in AbilityLogicHandler; this class
 * only provides supplementary join-event handling if needed.
 */
@EventBusSubscriber(modid = powers.MODID)
public class PlayerJoinHandler {

    private static final int DELAY_TICKS = 200; // 10 seconds
    private static final Map<UUID, Long> assignAt = new HashMap<>();

    @SubscribeEvent
    public static void onJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        // Schedule assignment (AbilityLogicHandler also does this;
        // here we just ensure sync of existing ability on reconnect)
        PlayerAbilityData data = sp.getData(AbilityAttachment.ABILITY_DATA.get());
        if (data.hasAbility()) {
            // Re-sync existing ability to client on reconnect
            PacketHandler.sendToPlayer(sp, new SyncAbilityPacket(
                    data.getAbility().name(),
                    data.getCooldownRemaining(),
                    data.getAbility().getCooldownTicks(),
                    false, 0,
                    data.isHudUnlocked()));
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        PlayerAbilityData data = sp.getData(AbilityAttachment.ABILITY_DATA.get());
        if (data.hasAbility()) {
            PacketHandler.sendToPlayer(sp, new SyncAbilityPacket(
                    data.getAbility().name(),
                    data.getCooldownRemaining(),
                    data.getAbility().getCooldownTicks(),
                    false, 0,
                    data.isHudUnlocked()));
        }
    }
}

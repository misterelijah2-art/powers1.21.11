package powerful.powers.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import powerful.powers.ability.AbilityType;
import powerful.powers.network.SyncAbilityPacket;
import powerful.powers.network.TitleRevealPacket;

/**
 * Static handlers called by {@link powerful.powers.network.ModNetwork}
 * when packets arrive on the client.
 */
@EventBusSubscriber(modid = powerful.powers.powers.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class ClientPacketHandlers {

    private ClientPacketHandlers() {}

    /** Called when a {@link SyncAbilityPacket} arrives. */
    public static void handleSync(SyncAbilityPacket packet) {
        ClientAbilityData.onSync(packet);
    }

    /** Called when a {@link TitleRevealPacket} arrives — starts the reveal animation. */
    public static void handleReveal(TitleRevealPacket packet) {
        AbilityType type = packet.resolveType();
        if (type != null) ClientTitleRevealState.startReveal(type);
    }
}

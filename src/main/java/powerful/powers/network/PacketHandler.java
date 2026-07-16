package powerful.powers.network;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Registers all network payloads and provides a send helper.
 */
public class PacketHandler {

    public static void register(PayloadRegistrar registrar) {
        registrar.playToClient(
            SyncAbilityPacket.TYPE,
            SyncAbilityPacket.STREAM_CODEC,
            SyncAbilityPacket.Handler::handle
        );
        registrar.playToClient(
            TitleRevealPacket.TYPE,
            TitleRevealPacket.STREAM_CODEC,
            TitleRevealPacket.Handler::handle
        );
        registrar.playToServer(
            UseAbilityPacket.TYPE,
            UseAbilityPacket.STREAM_CODEC,
            UseAbilityPacket.Handler::handle
        );
    }

    public static void sendToPlayer(ServerPlayer player, net.minecraft.network.protocol.common.custom.CustomPacketPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }
}

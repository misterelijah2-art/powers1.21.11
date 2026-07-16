package powerful.powers.network;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import powerful.powers.powers;

/**
 * Central networking registration. Called from powers constructor.
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

    public static void sendToPlayer(ServerPlayer player, net.neoforged.neoforge.network.packet.CustomPacketPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }
}

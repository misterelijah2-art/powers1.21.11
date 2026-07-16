package powerful.powers.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Utility wrappers around PacketDistributor.
 * Packet registration is handled entirely by ModNetwork.
 */
public class PacketHandler {

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload packet) {
        PacketDistributor.sendToPlayer(player, packet);
    }

    public static void sendToNear(ServerLevel level, double x, double y, double z,
                                   double range, CustomPacketPayload packet) {
        PacketDistributor.sendToPlayersNear(level, null, x, y, z, range, packet);
    }
}

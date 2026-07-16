package powerful.powers.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import powerful.powers.ability.AbilityLogicHandler;
import powerful.powers.client.ClientPacketHandlers;

public class PacketHandler {

    public static void register(PayloadRegistrar reg) {

        // server -> client
        reg.playToClient(
                SyncAbilityPacket.TYPE,
                SyncAbilityPacket.STREAM_CODEC,
                (pkt, ctx) -> ClientPacketHandlers.handleSyncAbility(pkt, ctx));

        reg.playToClient(
                TitleRevealPacket.TYPE,
                TitleRevealPacket.STREAM_CODEC,
                (pkt, ctx) -> ClientPacketHandlers.handleTitleReveal(pkt, ctx));

        reg.playToClient(
                AbilityFxPacket.TYPE,
                AbilityFxPacket.STREAM_CODEC,
                (pkt, ctx) -> ClientPacketHandlers.handleAbilityFx(pkt, ctx));

        // client -> server
        reg.playToServer(
                UseAbilityPacket.TYPE,
                UseAbilityPacket.STREAM_CODEC,
                (pkt, ctx) -> ctx.enqueueWork(() -> {
                    if (ctx.player() instanceof ServerPlayer sp)
                        AbilityLogicHandler.activateAbility(sp);
                }));

        reg.playToServer(
                ChargeStartPacket.TYPE,
                ChargeStartPacket.STREAM_CODEC,
                (pkt, ctx) -> ctx.enqueueWork(() -> {
                    if (ctx.player() instanceof ServerPlayer sp)
                        AbilityLogicHandler.beginCharge(sp);
                }));

        reg.playToServer(
                ChargeReleasePacket.TYPE,
                ChargeReleasePacket.STREAM_CODEC,
                (pkt, ctx) -> ctx.enqueueWork(() -> {
                    if (ctx.player() instanceof ServerPlayer sp)
                        AbilityLogicHandler.releaseCharge(sp);
                }));
    }

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload packet) {
        PacketDistributor.sendToPlayer(player, packet);
    }

    public static void sendToNear(ServerLevel level, double x, double y, double z,
                                   double range, CustomPacketPayload packet) {
        PacketDistributor.sendToPlayersNear(level, null, x, y, z, range, packet);
    }
}

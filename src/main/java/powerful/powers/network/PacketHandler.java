package powerful.powers.network;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import powerful.powers.client.ClientPacketHandlers;

public class PacketHandler {

    public static void register(IEventBus bus) {
        bus.addListener(PacketHandler::onRegisterPayloads);
    }

    private static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar reg = event.registrar("1");

        // server → client
        reg.playToClient(
                SyncAbilityPacket.TYPE, SyncAbilityPacket.CODEC,
                new DirectionalPayloadHandler<>(
                        ClientPacketHandlers::handleSyncAbility, (p, c) -> {}));

        reg.playToClient(
                TitleRevealPacket.TYPE, TitleRevealPacket.CODEC,
                new DirectionalPayloadHandler<>(
                        ClientPacketHandlers::handleTitleReveal, (p, c) -> {}));

        reg.playToClient(
                AbilityFxPacket.TYPE, AbilityFxPacket.CODEC,
                new DirectionalPayloadHandler<>(
                        ClientPacketHandlers::handleAbilityFx, (p, c) -> {}));

        // client → server
        reg.playToServer(
                UseAbilityPacket.TYPE, UseAbilityPacket.CODEC,
                new DirectionalPayloadHandler<>(
                        (p, c) -> {},
                        (p, c) -> c.player().ifPresent(player -> {
                            if (player instanceof ServerPlayer sp) {
                                powerful.powers.ability.AbilityLogicHandler.activateAbility(sp);
                            }
                        })));
    }

    public static void sendToPlayer(ServerPlayer player, net.minecraft.network.protocol.common.custom.CustomPacketPayload packet) {
        PacketDistributor.sendToPlayer(player, packet);
    }

    public static void sendToNear(net.minecraft.server.level.ServerLevel level, double x, double y, double z, double range,
                                   net.minecraft.network.protocol.common.custom.CustomPacketPayload packet) {
        PacketDistributor.sendToPlayersNear(level, null, x, y, z, range, packet);
    }
}

package powerful.powers.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import powerful.powers.KeyBindings;
import powerful.powers.network.UseAbilityPacket;
import powerful.powers.powers;

@EventBusSubscriber(modid = powers.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class ClientEvents {

    private static boolean wasDown = false;
    private static int holdTicks   = 0;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        boolean isDown = KeyBindings.USE_ABILITY.isDown();

        if (isDown && !wasDown) {
            // Key just pressed
            holdTicks = 0;
            PacketDistributor.sendToServer(new UseAbilityPacket(true, -1));
        } else if (isDown) {
            holdTicks++;
        } else if (!isDown && wasDown) {
            // Key just released
            PacketDistributor.sendToServer(new UseAbilityPacket(false, holdTicks));
            holdTicks = 0;
        }
        wasDown = isDown;
    }
}

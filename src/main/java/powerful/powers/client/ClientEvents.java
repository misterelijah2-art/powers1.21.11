package powerful.powers.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import powerful.powers.network.UseAbilityPacket;
import powerful.powers.powers;

/**
 * Handles keybind press/release and sends {@link UseAbilityPacket} to the server.
 */
@EventBusSubscriber(modid = powers.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class ClientEvents {

    private static boolean keyWasDown  = false;
    private static int     holdCounter = 0;

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (!KeyBindings.USE_ABILITY.isDown() && keyWasDown) {
            // Key was released
            keyWasDown = false;
            PacketDistributor.sendToServer(new UseAbilityPacket(true, holdCounter));
            holdCounter = 0;
        }
    }

    /** Tick on the client level to track how long the key is held. */
    @SubscribeEvent
    public static void onClientTick(LevelTickEvent.Post event) {
        if (net.minecraft.client.Minecraft.getInstance().player == null) return;
        if (KeyBindings.USE_ABILITY.isDown()) {
            if (!keyWasDown) {
                keyWasDown  = true;
                holdCounter = 0;
                PacketDistributor.sendToServer(new UseAbilityPacket(false, 0));
            } else {
                holdCounter++;
            }
        }
    }
}

package powerful.powers.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import powerful.powers.network.ChargeReleasePacket;
import powerful.powers.network.ChargeStartPacket;
import powerful.powers.powers;
import org.lwjgl.glfw.GLFW;

/**
 * Hold-to-charge keybind.
 *
 * Press  → sends ChargeStartPacket  (server begins counting charge ticks)
 * Hold   → server ticks chargeTicks each game tick
 * Release→ sends ChargeReleasePacket (server fires ability at current charge level)
 *
 * MAX_CHARGE_TICKS: full charge threshold (used by HUD charge bar fraction).
 */
@EventBusSubscriber(modid = powers.MODID, value = Dist.CLIENT)
public class AbilityKeybind {

    public static KeyMapping USE_ABILITY_KEY;
    /** Full charge is reached at this many ticks held. */
    public static final int MAX_CHARGE_TICKS = 40; // 2 seconds

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        USE_ABILITY_KEY = new KeyMapping(
            "key.powers.use_ability",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_R),
            KeyMapping.Category.GAMEPLAY
        );
        event.register(USE_ABILITY_KEY);
    }

    @EventBusSubscriber(modid = powers.MODID, value = Dist.CLIENT)
    public static class InputListener {

        /** True while the key is currently held down. */
        private static boolean wasDown = false;

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            if (USE_ABILITY_KEY == null) return;
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player == null || mc.screen != null) {
                // If the screen opens while held, cancel gracefully
                if (wasDown) {
                    ClientPacketDistributor.sendToServer(new ChargeReleasePacket());
                    wasDown = false;
                }
                return;
            }

            boolean isDown = USE_ABILITY_KEY.isDown();

            if (isDown && !wasDown) {
                // Key just pressed — start charge
                ClientPacketDistributor.sendToServer(new ChargeStartPacket());
                wasDown = true;
            } else if (!isDown && wasDown) {
                // Key just released — fire
                ClientPacketDistributor.sendToServer(new ChargeReleasePacket());
                wasDown = false;
            }
        }
    }
}

package powerful.powers.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import powerful.powers.network.UseAbilityPacket;
import powerful.powers.powers;
import org.lwjgl.glfw.GLFW;

/**
 * Registers the "Use Ability" keybind (default R) and listens for presses.
 */
@EventBusSubscriber(modid = powers.MODID, value = Dist.CLIENT)
public class AbilityKeybind {

    public static KeyMapping USE_ABILITY_KEY;

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        USE_ABILITY_KEY = new KeyMapping(
                "key.powers.use_ability",
                KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                "key.categories.powers"
        );
        event.register(USE_ABILITY_KEY);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof net.minecraft.client.player.LocalPlayer)) return;
        if (USE_ABILITY_KEY != null && USE_ABILITY_KEY.consumeClick()) {
            // Send to server
            PacketDistributor.sendToServer(new UseAbilityPacket());
        }
    }
}

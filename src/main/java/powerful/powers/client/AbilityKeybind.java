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
 * Registers the Use Ability keybind (default: R).
 * NeoForge 21.11: KeyMapping constructor takes (String, IKeyConflictContext, Type, InputConstants.Key, Category).
 * Category is a string — pass it via KeyMapping.Category.GAMEPLAY.
 */
@EventBusSubscriber(modid = powers.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class AbilityKeybind {

    public static KeyMapping USE_ABILITY_KEY;

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        USE_ABILITY_KEY = new KeyMapping(
            "key.powers.use_ability",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_R),
            "key.categories.powers"
        );
        event.register(USE_ABILITY_KEY);
    }

    // Listen for key presses on FORGE bus (game events)
    @EventBusSubscriber(modid = powers.MODID, value = Dist.CLIENT)
    public static class InputListener {
        @SubscribeEvent
        public static void onPlayerTick(PlayerTickEvent.Post event) {
            if (!(event.getEntity() instanceof net.minecraft.client.player.LocalPlayer)) return;
            if (USE_ABILITY_KEY != null && USE_ABILITY_KEY.consumeClick()) {
                PacketDistributor.sendToServer(new UseAbilityPacket());
            }
        }
    }
}

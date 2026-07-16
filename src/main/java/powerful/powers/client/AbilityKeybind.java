package powerful.powers.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.event.tick.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import powerful.powers.network.UseAbilityPacket;
import powerful.powers.powers;
import org.lwjgl.glfw.GLFW;

/**
 * Registers the Use Ability keybind (default: R).
 *
 * NeoForge 21.11:
 *   - KeyMapping constructor: (String, IKeyConflictContext, InputConstants.Key, KeyMapping.Category)
 *     The last arg is KeyMapping.Category (enum), not a plain String.
 *     We use VANILLA_OR_CONSTANT categories from KeyMapping.Categories, or create via
 *     KeyMapping.Category.create(translationKey) if available.
 *   - RegisterKeyMappingsEvent is on MOD bus -> use @EventBusSubscriber Bus.MOD
 *   - ClientTickEvent.Post is on GAME (FORGE) bus -> separate class without Bus.MOD
 *   - PacketDistributor.sendToServer(payload) is the correct static method.
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
            // KeyMapping.Category is an enum in 1.21.11 — use GAMEPLAY or create custom
            KeyMapping.Category.GAMEPLAY
        );
        event.register(USE_ABILITY_KEY);
    }

    /**
     * Input listener on the FORGE (game) bus.
     * Note: no 'bus = Bus.MOD' here — default is FORGE bus.
     */
    @EventBusSubscriber(modid = powers.MODID, value = Dist.CLIENT)
    public static class InputListener {
        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            if (USE_ABILITY_KEY == null) return;
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player == null || mc.screen != null) return;
            while (USE_ABILITY_KEY.consumeClick()) {
                PacketDistributor.sendToServer(new UseAbilityPacket());
            }
        }
    }
}

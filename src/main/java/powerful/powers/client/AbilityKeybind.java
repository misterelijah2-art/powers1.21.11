package powerful.powers.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.network.PacketDistributor;
import powerful.powers.network.UseAbilityPacket;
import powerful.powers.powers;
import org.lwjgl.glfw.GLFW;

/**
 * Registers the Use Ability keybind (default: R).
 *
 * NeoForge 21.11 EventBusSubscriber rules:
 *   - The annotation has NO 'bus' element.
 *   - Events that implement IModBusEvent (e.g. RegisterKeyMappingsEvent) auto-route to the MOD bus.
 *   - All other events (e.g. ClientTickEvent) auto-route to the FORGE (game) bus.
 *   - So both classes below use @EventBusSubscriber without a bus= parameter.
 *
 * KeyMapping NeoForge constructor: (String translationKey, IKeyConflictContext, InputConstants.Key, String categoryKey)
 *   - The last argument is a plain translation key String for the category.
 *
 * PacketDistributor in NeoForge 21.11:
 *   - Serverbound packets: PacketDistributor.sendToServer(payload)
 *     (This is a static method on PacketDistributor itself in 21.11 — confirmed in the 21.11 javadoc.)
 */
@EventBusSubscriber(modid = powers.MODID, value = Dist.CLIENT)
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

    /**
     * Separate inner class so both event types can live in the same file.
     * ClientTickEvent is a game bus event, so this also uses no bus= param.
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

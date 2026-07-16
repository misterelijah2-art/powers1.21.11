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
import powerful.powers.network.UseAbilityPacket;
import powerful.powers.powers;
import org.lwjgl.glfw.GLFW;

/**
 * NeoForge 21.11:
 *   - KeyMapping constructor: (String, IKeyConflictContext, InputConstants.Key, KeyMapping.Category)
 *   - Serverbound packets: net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer()
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
            KeyMapping.Category.GAMEPLAY
        );
        event.register(USE_ABILITY_KEY);
    }

    @EventBusSubscriber(modid = powers.MODID, value = Dist.CLIENT)
    public static class InputListener {
        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            if (USE_ABILITY_KEY == null) return;
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player == null || mc.screen != null) return;
            while (USE_ABILITY_KEY.consumeClick()) {
                ClientPacketDistributor.sendToServer(new UseAbilityPacket());
            }
        }
    }
}

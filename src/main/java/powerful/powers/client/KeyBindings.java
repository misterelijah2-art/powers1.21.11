package powerful.powers.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

public class KeyBindings {

    public static final KeyMapping USE_ABILITY = new KeyMapping(
            "key.powers.use_ability",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            "key.categories.powers"
    );

    public static void register(RegisterKeyMappingsEvent event) {
        event.register(USE_ABILITY);
    }
}

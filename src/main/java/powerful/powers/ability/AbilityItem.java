package powerful.powers.ability;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Logical helper for the ability orb ItemStack.
 * We reuse the vanilla egg item (no custom item registration needed).
 * The ability name and cooldown are stored in the stack's custom name
 * via Component data, and a custom tag stored in PersistentData on pickup.
 *
 * Since MC 1.20.5+ removed ItemStack#getOrCreateTag / setTag,
 * we encode the ability info purely into the display name + lore,
 * and pass it through the item's custom-name Component.
 */
public class AbilityItem {

    private static final String ENCODED_PREFIX = "\u00a7r\u00a7r";

    /**
     * Create an ability orb ItemStack that encodes abilityName+cooldown
     * inside the item's custom name so it survives serialisation without
     * needing a custom item or DataComponent.
     */
    public static ItemStack forAbility(AbilityType type, int inheritedCooldown) {
        // Use a spectral arrow as the orb base (glows, looks special)
        ItemStack stack = new ItemStack(net.minecraft.world.item.Items.NETHER_STAR);

        // Encode type + cooldown in the custom name using invisible prefix
        // Format: ENCODED_PREFIX + typeName + "|" + cooldown + "|" + displaySuffix
        String encoded = ENCODED_PREFIX + type.name() + "|" + inheritedCooldown;
        Component name = Component.literal(encoded)
                .append(Component.literal(type.getDisplayName() + " Orb")
                        .withStyle(s -> s.withColor(type.getColor().getColor() != null
                                ? type.getColor().getColor() : 0xFFFFFF)
                                .withItalic(true)
                                .withBold(false)));
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, name);
        return stack;
    }

    /**
     * Attempt to decode the ability type from an orb stack.
     * Returns null if this is not a valid orb.
     */
    public static AbilityType decodeType(ItemStack stack) {
        if (stack.isEmpty()) return null;
        Component name = stack.getComponents().get(net.minecraft.core.component.DataComponents.CUSTOM_NAME);
        if (name == null) return null;
        String raw = net.minecraft.network.chat.Component.Serializer.toJson(name, null);
        // Parse the encoded prefix out of the literal siblings
        String plain = name.getString();
        if (!plain.startsWith(ENCODED_PREFIX)) return null;
        try {
            String payload = plain.substring(ENCODED_PREFIX.length());
            String typeName = payload.split("\\|")[0];
            return AbilityType.valueOf(typeName);
        } catch (Exception e) { return null; }
    }

    /**
     * Attempt to decode the inherited cooldown from an orb stack.
     */
    public static int decodeCooldown(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        Component name = stack.getComponents().get(net.minecraft.core.component.DataComponents.CUSTOM_NAME);
        if (name == null) return 0;
        String plain = name.getString();
        if (!plain.startsWith(ENCODED_PREFIX)) return 0;
        try {
            String payload = plain.substring(ENCODED_PREFIX.length());
            String[] parts = payload.split("\\|");
            return parts.length >= 2 ? Integer.parseInt(parts[1]) : 0;
        } catch (Exception e) { return 0; }
    }
}

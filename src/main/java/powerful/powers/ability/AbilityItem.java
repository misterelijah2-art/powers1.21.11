package powerful.powers.ability;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.DeferredItem;
import powerful.powers.item.ModItems;
import powerful.powers.network.PacketHandler;
import powerful.powers.network.SyncAbilityPacket;

import java.util.List;

/**
 * Factory + helper methods for the ability-orb item.
 * The actual Item class lives in {@link powerful.powers.item.AbilityOrbItem}.
 */
public final class AbilityItem {

    private AbilityItem() {}

    private static final String TAG_TYPE     = "ability_type";
    private static final String TAG_COOLDOWN = "cooldown_remaining";

    /** Build an orb ItemStack carrying the given ability and a cooldown snapshot. */
    public static ItemStack forAbility(AbilityType type, int cooldownSnapshot) {
        ItemStack stack = new ItemStack(ModItems.ABILITY_ORB.get());

        CompoundTag tag = new CompoundTag();
        tag.putString(TAG_TYPE, type.name());
        tag.putInt(TAG_COOLDOWN, cooldownSnapshot);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

        // Display name + lore via standard components
        String suffix = cooldownSnapshot > 0
                ? " \u00a78(" + (cooldownSnapshot / 20) + "s cd)" : "";
        stack.set(DataComponents.CUSTOM_NAME,
                Component.literal(type.displayName + " Orb" + suffix)
                         .withStyle(s -> s.withColor(chatColorToInt(type.color)).withItalic(true)));
        return stack;
    }

    /**
     * Called from {@link powerful.powers.item.AbilityOrbItem#use} when a player right-clicks.
     * Returns true if the ability was successfully absorbed.
     */
    public static boolean tryAbsorb(ItemStack stack, Player player) {
        if (!(player instanceof ServerPlayer sp)) return false;
        if (player.level().isClientSide()) return false;

        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return false;
        CompoundTag tag = customData.copyTag();

        AbilityType type;
        try {
            type = AbilityType.valueOf(tag.getString(TAG_TYPE));
        } catch (IllegalArgumentException e) {
            return false;
        }
        int inheritedCooldown = tag.contains(TAG_COOLDOWN) ? tag.getInt(TAG_COOLDOWN) : 0;

        PlayerAbilityData data = sp.getData(AbilityAttachment.ABILITY_DATA.get());
        data.setAbility(type);
        data.setCooldownRemaining(inheritedCooldown);
        data.unlockHud();

        sp.displayClientMessage(
            Component.literal("\u00a7aYou absorbed: ").append(type.getDisplayComponent()), false);

        PacketHandler.sendToPlayer(sp, new SyncAbilityPacket(
                type.name(), inheritedCooldown, type.cooldownTicks, false, 0, true));
        return true;
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private static int chatColorToInt(net.minecraft.ChatFormatting fmt) {
        Integer c = fmt.getColor();
        return c != null ? c : 0xFFFFFF;
    }
}

package powerful.powers.ability;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

/**
 * The dropped ability orb item.
 * Uses DataComponents (CustomData) for NBT storage — MC 1.21.11 ItemStack API.
 * Right-clicking absorbs the ability and destroys the stack.
 */
public class AbilityItem extends Item {

    private static final String NBT_ABILITY = "AbilityType";

    // Fix: do NOT call any extra methods on props here — the registry
    // already provides a fully-configured Item.Properties. Calling
    // props.stacksTo(1) again (or any descriptor method) before the
    // registry ID is bound causes the "Item id not set" NPE on 1.21.1+.
    public AbilityItem(Properties props) {
        super(props);
    }

    /** Create a stack encoding the given ability type. */
    public static ItemStack forAbility(AbilityType type) {
        ItemStack stack = new ItemStack(AbilityRegistry.ABILITY_ITEM.get());
        CompoundTag tag = new CompoundTag();
        tag.putString(NBT_ABILITY, type.name());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    /** Read the encoded ability from a stack. Returns null if missing or invalid. */
    public static AbilityType getAbilityFromStack(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof AbilityItem)) return null;
        CustomData custom = stack.get(DataComponents.CUSTOM_DATA);
        if (custom == null) return null;
        CompoundTag tag = custom.copyTag();
        if (!tag.contains(NBT_ABILITY)) return null;
        // Fix: CompoundTag.getString() returns String directly in 1.21.1+, not Optional.
        try { return AbilityType.valueOf(tag.getString(NBT_ABILITY)); }
        catch (IllegalArgumentException e) { return null; }
    }

    @Override
    public Component getName(ItemStack stack) {
        AbilityType type = getAbilityFromStack(stack);
        if (type == null) return Component.literal("Unknown Power Orb");
        return Component.literal(type.getDisplayName() + " Orb")
                .withStyle(s -> s.withColor(type.getColor()).withItalic(true));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        AbilityType type = getAbilityFromStack(stack);
        if (type == null) return InteractionResult.PASS;

        if (!level.isClientSide()) {
            PlayerAbilityData data = player.getData(AbilityAttachment.ABILITY_DATA.get());
            data.setAbility(type);
            data.setCooldownRemaining(0);
            player.displayClientMessage(
                Component.literal("You absorbed the power: ").append(type.getTitle()), false
            );
            stack.shrink(1);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.SUCCESS;
    }
}

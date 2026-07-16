package powerful.powers.ability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.ChatFormatting;

/**
 * The dropped ability orb item. Stores which AbilityType it holds in NBT.
 * Right-clicking absorbs the ability into the player and removes the stack.
 */
public class AbilityItem extends Item {

    private static final String NBT_ABILITY = "AbilityType";

    public AbilityItem(Properties props) {
        super(props.stacksTo(1));
    }

    /** Create a stack that encodes which ability it holds. */
    public static ItemStack forAbility(AbilityType type) {
        ItemStack stack = new ItemStack(AbilityRegistry.ABILITY_ITEM.get());
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString(NBT_ABILITY, type.name());
        return stack;
    }

    /** Read the encoded ability from a stack. Returns null if invalid. */
    public static AbilityType getAbilityFromStack(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof AbilityItem)) return null;
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(NBT_ABILITY)) return null;
        try { return AbilityType.valueOf(tag.getString(NBT_ABILITY)); }
        catch (IllegalArgumentException e) { return null; }
    }

    @Override
    public Component getName(ItemStack stack) {
        AbilityType type = getAbilityFromStack(stack);
        if (type == null) return Component.literal("Unknown Power Orb");
        return Component.literal(type.getDisplayName() + " Orb")
                .withStyle(style -> style.withColor(type.getColor()).withItalic(true));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, net.minecraft.world.InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        AbilityType type = getAbilityFromStack(stack);
        if (type == null) return InteractionResultHolder.pass(stack);

        if (!level.isClientSide()) {
            // Give the ability to the player on the server
            PlayerAbilityData data = player.getData(AbilityAttachment.ABILITY_DATA.get());
            data.setAbility(type);
            data.setCooldownRemaining(0);

            player.sendSystemMessage(
                Component.literal("You absorbed the power: ")
                    .append(type.getTitle())
            );

            // Shrink stack to destroy it
            stack.shrink(1);
            return InteractionResultHolder.consume(ItemStack.EMPTY);
        }
        return InteractionResultHolder.success(stack);
    }
}

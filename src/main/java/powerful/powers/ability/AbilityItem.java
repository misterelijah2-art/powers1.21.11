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
import net.minecraft.server.level.ServerPlayer;

/**
 * The dropped ability orb item.
 * Right-click to absorb the ability (with inherited cooldown).
 * Dropped only on PvP death; natural death does NOT drop it.
 */
public class AbilityItem extends Item {

    private static final String NBT_ABILITY  = "AbilityType";
    private static final String NBT_COOLDOWN = "CooldownRemaining";

    public AbilityItem(Properties props) {
        super(props);
    }

    /** Create a stack encoding the given ability type and its current cooldown. */
    public static ItemStack forAbility(AbilityType type, int cooldownRemaining) {
        ItemStack stack = new ItemStack(AbilityRegistry.ABILITY_ITEM.get());
        CompoundTag tag = new CompoundTag();
        tag.putString(NBT_ABILITY, type.name());
        tag.putInt(NBT_COOLDOWN, cooldownRemaining);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide) return InteractionResult.SUCCESS;

        ItemStack stack = player.getItemInHand(hand);
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        String typeName = tag.getString(NBT_ABILITY);
        if (typeName.isEmpty()) return InteractionResult.FAIL;

        try {
            AbilityType type = AbilityType.valueOf(typeName);
            int inheritedCd  = tag.getInt(NBT_COOLDOWN);

            if (player instanceof ServerPlayer sp) {
                AbilityData data = new AbilityData(type, inheritedCd);
                data.announced = false;
                AbilityManager.assignAbility(sp, data);
                AbilityManager.scheduleAnnouncement(sp);
                sp.displayClientMessage(
                    Component.literal("You absorbed: ").append(type.getDisplayComponent()), true);
            }

            stack.shrink(1);
            return InteractionResult.CONSUME;
        } catch (IllegalArgumentException e) {
            return InteractionResult.FAIL;
        }
    }
}

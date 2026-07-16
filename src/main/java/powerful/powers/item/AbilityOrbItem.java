package powerful.powers.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import powerful.powers.ability.AbilityData;
import powerful.powers.ability.AbilityManager;
import powerful.powers.ability.AbilityType;

import java.util.List;

/**
 * Glowing orb dropped on PvP kill.
 * Stores the original owner's ability type and remaining cooldown.
 * Right-clicking absorbs the ability into the new player (keeping cooldown),
 * then the item vanishes.
 */
public class AbilityOrbItem extends Item {

    public static final String TAG_TYPE     = "AbilityType";
    public static final String TAG_COOLDOWN = "RemainingCooldown";

    public AbilityOrbItem() {
        super(new Item.Properties()
                .stacksTo(1)
                .rarity(net.minecraft.world.item.Rarity.EPIC)
        );
    }

    /** Called by death handler to stamp the orb with ability data. */
    public static ItemStack createOrb(AbilityType type, int remainingCooldown) {
        ItemStack stack = new ItemStack(ModItems.ABILITY_ORB.get());
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString(TAG_TYPE, type.name());
        tag.putInt(TAG_COOLDOWN, remainingCooldown);
        stack.setTag(tag);
        return stack;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.pass(stack);

        CompoundTag tag = stack.getTag();
        if (tag == null) return InteractionResultHolder.fail(stack);

        try {
            AbilityType type     = AbilityType.valueOf(tag.getString(TAG_TYPE));
            int remainingCooldown = tag.getInt(TAG_COOLDOWN);

            // Give player the stolen ability (inheriting cooldown)
            AbilityData data = new AbilityData(type, remainingCooldown);
            data.announced = false; // triggers announcement sequence for new owner
            AbilityManager.assignAbility(player, data);

            // Play absorption sound
            level.playSound(null, player.blockPosition(),
                    net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME,
                    net.minecraft.sounds.SoundSource.PLAYERS, 1.2f, 0.7f);

            // Particles burst
            if (level instanceof net.minecraft.server.level.ServerLevel sl) {
                sl.sendParticles(
                    net.minecraft.core.particles.ParticleTypes.PORTAL,
                    player.getX(), player.getY() + 1.0, player.getZ(),
                    40, 0.5, 0.8, 0.5, 0.1
                );
            }

            // Shrink to 0 = item vanishes
            stack.shrink(stack.getCount());
            return InteractionResultHolder.consume(stack);

        } catch (IllegalArgumentException e) {
            return InteractionResultHolder.fail(stack);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx,
                                List<Component> tooltip, TooltipFlag flag) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(TAG_TYPE)) {
            try {
                AbilityType type = AbilityType.valueOf(tag.getString(TAG_TYPE));
                tooltip.add(Component.literal("Ability: ")
                        .append(Component.literal(type.displayName).withStyle(type.color)));
                int cd = tag.getInt(TAG_COOLDOWN);
                if (cd > 0) {
                    tooltip.add(Component.literal("Cooldown: "
                            + String.format("%.1f", cd / 20f) + "s remaining")
                            .withStyle(net.minecraft.ChatFormatting.GRAY));
                } else {
                    tooltip.add(Component.literal("Ready to use!")
                            .withStyle(net.minecraft.ChatFormatting.GREEN));
                }
            } catch (IllegalArgumentException ignored) {}
        }
        tooltip.add(Component.literal("Right-click to absorb").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true; // always has enchantment glint
    }
}

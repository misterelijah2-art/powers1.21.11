package powerful.powers.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import powerful.powers.ability.AbilityItem;

/**
 * The physical ability orb item dropped on PvP death.
 * Right-clicking absorbs the ability stored in the item's custom data.
 */
public class AbilityOrbItem extends Item {

    public AbilityOrbItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) return InteractionResultHolder.pass(stack);

        boolean absorbed = AbilityItem.tryAbsorb(stack, player);
        if (absorbed) {
            level.playSound(null,
                    player.getX(), player.getY(), player.getZ(),
                    SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.2f);
            stack.shrink(1);
            return InteractionResultHolder.consume(stack);
        }
        return InteractionResultHolder.fail(stack);
    }
}

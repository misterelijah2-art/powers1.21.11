package powerful.powers.item;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import powerful.powers.ability.AbilityAttachment;
import powerful.powers.ability.AbilityItem;
import powerful.powers.ability.AbilityType;
import powerful.powers.ability.PlayerAbilityData;
import powerful.powers.network.PacketHandler;
import powerful.powers.network.SyncAbilityPacket;
import powerful.powers.network.TitleRevealPacket;

/**
 * Right-click to absorb a dropped ability orb.
 * The orb type/cooldown is encoded in the item's custom name component.
 */
public class AbilityOrbItem extends Item {

    public AbilityOrbItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) return InteractionResultHolder.pass(stack);
        if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.fail(stack);

        AbilityType type = AbilityItem.decodeType(stack);
        if (type == null) return InteractionResultHolder.fail(stack);
        int inheritedCooldown = AbilityItem.decodeCooldown(stack);

        PlayerAbilityData data = sp.getData(AbilityAttachment.ABILITY_DATA.get());
        // Overwrite any existing ability
        data.setAbility(type);
        data.setCooldownRemaining(inheritedCooldown);
        data.unlockHud();

        // Remove the orb from inventory
        stack.shrink(1);

        // Announce and sync
        PacketHandler.sendToPlayer(sp, new TitleRevealPacket(type.name()));
        PacketHandler.sendToPlayer(sp, new SyncAbilityPacket(
                type.name(), inheritedCooldown, type.getCooldownTicks(),
                false, 0, true));

        sp.displayClientMessage(
                Component.literal("\u00a7aYou absorbed the power: ").append(type.getTitle()), false);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        // Glowing enchantment-style shimmer on the orb
        return true;
    }
}

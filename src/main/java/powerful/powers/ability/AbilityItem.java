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
import powerful.powers.network.PacketHandler;
import powerful.powers.network.SyncAbilityPacket;
import powerful.powers.network.TitleRevealPacket;

/**
 * The dropped ability orb item.
 * NBT now also stores the cooldown so a stolen ability inherits its remaining cooldown.
 */
public class AbilityItem extends Item {

    private static final String NBT_ABILITY   = "AbilityType";
    private static final String NBT_COOLDOWN  = "CooldownRemaining";

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

    /** @deprecated use forAbility(type, cooldown) — kept for any call-sites that don't have cooldown. */
    public static ItemStack forAbility(AbilityType type) {
        return forAbility(type, 0);
    }

    /** Read the encoded ability from a stack. Returns null if missing or invalid. */
    public static AbilityType getAbilityFromStack(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof AbilityItem)) return null;
        CustomData custom = stack.get(DataComponents.CUSTOM_DATA);
        if (custom == null) return null;
        CompoundTag tag = custom.copyTag();
        if (!tag.contains(NBT_ABILITY)) return null;
        String raw = tag.getString(NBT_ABILITY).orElse("");
        if (raw.isEmpty()) return null;
        try { return AbilityType.valueOf(raw); }
        catch (IllegalArgumentException e) { return null; }
    }

    /** Read the encoded cooldown from a stack. Returns 0 if absent. */
    public static int getCooldownFromStack(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof AbilityItem)) return 0;
        CustomData custom = stack.get(DataComponents.CUSTOM_DATA);
        if (custom == null) return 0;
        return custom.copyTag().getInt(NBT_COOLDOWN).orElse(0);
    }

    @Override
    public Component getName(ItemStack stack) {
        AbilityType type = getAbilityFromStack(stack);
        if (type == null) return Component.literal("Unknown Power Orb");
        int cd = getCooldownFromStack(stack);
        String suffix = cd > 0
                ? String.format(" (%.1fs cd)", cd / 20.0f)
                : " (ready)";
        return Component.literal(type.getDisplayName() + " Orb" + suffix)
                .withStyle(s -> s.withColor(type.getColor()).withItalic(true));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        AbilityType type = getAbilityFromStack(stack);
        if (type == null) return InteractionResult.PASS;

        if (!level.isClientSide()) {
            ServerPlayer sp = (ServerPlayer) player;
            PlayerAbilityData data = player.getData(AbilityAttachment.ABILITY_DATA.get());
            int inheritedCooldown = getCooldownFromStack(stack);

            data.setAbility(type);
            data.setCooldownRemaining(inheritedCooldown);
            // HUD stays locked — the title reveal will unlock it
            // hudUnlocked resets to false via clearAbility, leave it false until reveal done

            // Show the title reveal so the player knows what they absorbed
            PacketHandler.sendToPlayer(sp, new TitleRevealPacket(type.name()));
            PacketHandler.sendToPlayer(sp, new SyncAbilityPacket(
                    type.name(), inheritedCooldown, type.getCooldownTicks(),
                    false, 0, false));

            player.displayClientMessage(
                Component.literal("You absorbed the power: ").append(type.getTitle()), false
            );
            stack.shrink(1);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.SUCCESS;
    }
}

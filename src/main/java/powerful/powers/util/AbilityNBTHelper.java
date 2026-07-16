package powerful.powers.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import powerful.powers.ability.AbilityData;
import powerful.powers.ability.AbilityType;

public class AbilityNBTHelper {

    private static final String KEY_ROOT     = "PowersMod";
    private static final String KEY_TYPE     = "AbilityType";
    private static final String KEY_COOLDOWN = "Cooldown";

    /** Save ability type + cooldown into the player's persistent data. */
    public static void save(Player player, AbilityData data) {
        CompoundTag root = player.getPersistentData()
                .getCompound(Player.PERSISTED_NBT_TAG);
        CompoundTag tag = new CompoundTag();
        if (data != null && data.type != null) {
            tag.putString(KEY_TYPE, data.type.name());
            tag.putInt(KEY_COOLDOWN, Math.max(0, data.cooldownTicks));
        }
        root.put(KEY_ROOT, tag);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, root);
    }

    /** Load ability data from the player's persistent data. Returns null if none saved. */
    public static AbilityData load(Player player) {
        CompoundTag root = player.getPersistentData()
                .getCompound(Player.PERSISTED_NBT_TAG);
        if (!root.contains(KEY_ROOT)) return null;
        CompoundTag tag = root.getCompound(KEY_ROOT);
        if (!tag.contains(KEY_TYPE)) return null;
        try {
            AbilityType type = AbilityType.valueOf(tag.getString(KEY_TYPE));
            int cooldown = tag.getInt(KEY_COOLDOWN);
            AbilityData data = new AbilityData(type, cooldown);
            data.announced = true; // returning player already had announcement
            return data;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Remove all stored ability data from a player. */
    public static void clear(Player player) {
        CompoundTag root = player.getPersistentData()
                .getCompound(Player.PERSISTED_NBT_TAG);
        root.remove(KEY_ROOT);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, root);
    }
}

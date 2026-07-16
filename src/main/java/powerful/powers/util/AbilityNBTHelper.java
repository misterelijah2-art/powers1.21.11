package powerful.powers.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import powerful.powers.ability.AbilityData;
import powerful.powers.ability.AbilityType;

/**
 * Persists ability data to the player's persistent NBT data so it survives
 * natural deaths and server restarts.
 */
public class AbilityNBTHelper {

    private static final String KEY = "PowersAbility";

    public static void save(Player player, AbilityData data) {
        if (data == null || data.type == null) {
            player.getPersistentData().remove(KEY);
            return;
        }
        CompoundTag tag = new CompoundTag();
        tag.putString("type",     data.type.name());
        tag.putInt   ("cd",       data.cooldownTicks);
        tag.putBoolean("announced", data.announced);
        player.getPersistentData().put(KEY, tag);
    }

    public static AbilityData load(Player player) {
        if (!player.getPersistentData().contains(KEY)) return null;
        CompoundTag tag = player.getPersistentData().getCompound(KEY);
        try {
            AbilityType type = AbilityType.valueOf(tag.getString("type"));
            AbilityData d    = new AbilityData(type, tag.getInt("cd"));
            d.announced      = tag.getBoolean("announced");
            return d;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static void clear(Player player) {
        player.getPersistentData().remove(KEY);
    }
}

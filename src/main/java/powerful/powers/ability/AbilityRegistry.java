package powerful.powers.ability;

import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import powerful.powers.powers;

/**
 * Registers the AbilityItem (the dropped power orb).
 */
public class AbilityRegistry {

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(powers.MODID);

    public static final DeferredItem<AbilityItem> ABILITY_ITEM =
            ITEMS.register("ability_orb",
                    () -> new AbilityItem(new Item.Properties().stacksTo(1)));

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}

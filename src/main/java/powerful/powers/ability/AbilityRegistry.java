package powerful.powers.ability;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import powerful.powers.powers;

/**
 * Registers the AbilityItem (the dropped power orb).
 *
 * NeoForge 1.21.1+ IMPORTANT:
 * DeferredRegister.Items.registerItem() automatically injects the correct
 * Item.Properties with the registry ID already bound, avoiding the
 * "Item id not set" NullPointerException that occurs when you call
 * new Item.Properties() manually before the ID is assigned.
 */
public class AbilityRegistry {

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(powers.MODID);

    // registerItem() passes a pre-bound Properties to the constructor — safe on 1.21.1+
    public static final DeferredItem<AbilityItem> ABILITY_ITEM =
            ITEMS.registerItem("ability_orb", AbilityItem::new);

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}

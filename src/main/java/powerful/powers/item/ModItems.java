package powerful.powers.item;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems("powers");

    public static final DeferredItem<AbilityOrbItem> ABILITY_ORB =
            ITEMS.register("ability_orb", AbilityOrbItem::new);
}

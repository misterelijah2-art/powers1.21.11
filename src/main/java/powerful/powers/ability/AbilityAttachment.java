package powerful.powers.ability;

import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.IEventBus;
import java.util.function.Supplier;
import powerful.powers.powers;

/**
 * Registers the AttachmentType that stores PlayerAbilityData on each ServerPlayer.
 */
public class AbilityAttachment {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, powers.MODID);

    public static final Supplier<AttachmentType<PlayerAbilityData>> ABILITY_DATA =
            ATTACHMENT_TYPES.register("ability_data", () ->
                    AttachmentType.builder(PlayerAbilityData::new)
                            .serialize(
                                    data -> data.save(),
                                    (data, tag) -> data.load(tag)
                            )
                            .build()
            );

    public static void register(IEventBus bus) {
        ATTACHMENT_TYPES.register(bus);
    }
}

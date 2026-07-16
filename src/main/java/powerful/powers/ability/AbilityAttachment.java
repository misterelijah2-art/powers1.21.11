package powerful.powers.ability;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import powerful.powers.powers;

/**
 * Registers the AttachmentType for PlayerAbilityData.
 * NeoForge 21.11: Use AttachmentType.builder(supplier).serialize(codec).build()
 */
public class AbilityAttachment {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, powers.MODID);

    public static final java.util.function.Supplier<AttachmentType<PlayerAbilityData>> ABILITY_DATA =
            ATTACHMENT_TYPES.register("ability_data", () ->
                AttachmentType.builder(PlayerAbilityData::new)
                    .serialize(
                        net.minecraft.nbt.CompoundTag.CODEC.xmap(
                            tag -> { PlayerAbilityData d = new PlayerAbilityData(); d.load(tag); return d; },
                            PlayerAbilityData::save
                        )
                    )
                    .build()
            );

    public static void register(IEventBus bus) {
        ATTACHMENT_TYPES.register(bus);
    }
}

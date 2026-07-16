package powerful.powers.ability;

import com.mojang.serialization.MapCodec;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import powerful.powers.powers;

/**
 * Registers the AttachmentType for PlayerAbilityData.
 *
 * NeoForge 21.11: Builder.serialize() requires a MapCodec<T>, not a plain Codec<T>.
 * We convert CompoundTag.CODEC (a Codec) to a MapCodec via .fieldOf("d").
 */
public class AbilityAttachment {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, powers.MODID);

    public static final java.util.function.Supplier<AttachmentType<PlayerAbilityData>> ABILITY_DATA =
            ATTACHMENT_TYPES.register("ability_data", () -> {
                // CompoundTag.CODEC is a Codec<CompoundTag>.
                // xmap converts it to Codec<PlayerAbilityData>.
                // .fieldOf("d") converts that to a MapCodec<PlayerAbilityData> as required.
                MapCodec<PlayerAbilityData> mapCodec = CompoundTag.CODEC
                        .xmap(
                            tag -> { PlayerAbilityData d = new PlayerAbilityData(); d.load(tag); return d; },
                            PlayerAbilityData::save
                        )
                        .fieldOf("d");
                return AttachmentType.builder(PlayerAbilityData::new)
                        .serialize(mapCodec)
                        .build();
            });

    public static void register(IEventBus bus) {
        ATTACHMENT_TYPES.register(bus);
    }
}

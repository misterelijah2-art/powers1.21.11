package powerful.powers;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import powerful.powers.ability.AbilityAttachment;
import powerful.powers.ability.AbilityRegistry;
import powerful.powers.command.AbilityCommand;
import powerful.powers.network.PacketHandler;

@Mod(powers.MODID)
public class powers {

    public static final String MODID = "powers";
    public static final Logger LOGGER = LogUtils.getLogger();

    // Creative tab
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> POWERS_TAB =
            CREATIVE_MODE_TABS.register("powers_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.powers"))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> AbilityRegistry.ABILITY_ITEM.get().getDefaultInstance())
                    .displayItems((params, output) -> {
                        output.accept(AbilityRegistry.ABILITY_ITEM.get());
                    })
                    .build());

    public powers(IEventBus modEventBus, ModContainer modContainer) {
        // Register subsystems
        AbilityAttachment.register(modEventBus);
        AbilityRegistry.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);

        // Register network packets
        modEventBus.addListener(this::registerPayloads);

        modEventBus.addListener(this::commonSetup);
        NeoForge.EVENT_BUS.register(this);
        // AbilityCommand registers itself via @EventBusSubscriber + RegisterCommandsEvent
        NeoForge.EVENT_BUS.register(AbilityCommand.class);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(MODID);
        PacketHandler.register(registrar);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("[Powers] Common setup complete.");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("[Powers] Server starting.");
    }
}

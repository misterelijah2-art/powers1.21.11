package powerful.powers;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.common.NeoForge;
import powerful.powers.client.ClientEvents;
import powerful.powers.client.hud.AbilityHudRenderer;
import powerful.powers.client.hud.AnnouncementRenderer;
import powerful.powers.event.PlayerDeathHandler;
import powerful.powers.event.PlayerJoinHandler;
import powerful.powers.item.ModItems;
import powerful.powers.network.ModNetwork;

@Mod("powers")
public class PowersMod {

    public PowersMod(IEventBus modEventBus) {
        // Register items
        ModItems.ITEMS.register(modEventBus);

        // Register network packets
        modEventBus.addListener(ModNetwork::register);

        // Common setup
        modEventBus.addListener(this::commonSetup);

        // Client setup
        modEventBus.addListener(this::clientSetup);

        // Register server-side event handlers
        NeoForge.EVENT_BUS.register(PlayerJoinHandler.class);
        NeoForge.EVENT_BUS.register(PlayerDeathHandler.class);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // Nothing needed here currently
    }

    private void clientSetup(FMLClientSetupEvent event) {
        // Register client-side keybind + tick events
        NeoForge.EVENT_BUS.register(ClientEvents.class);

        // Register HUD overlay using RenderGuiLayerEvent
        NeoForge.EVENT_BUS.addListener(PowersMod::onRenderGuiLayer);
    }

    /**
     * Render our HUD layers on top of everything else.
     * AnnouncementRenderer gates the HUD so they never overlap.
     */
    @SubscribeEvent
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Post event) {
        // We hook after the hotbar layer so our HUD sits above it
        if (!event.getName().equals(
                net.neoforged.neoforge.client.gui.VanillaGuiLayers.HOTBAR)) return;

        net.minecraft.client.gui.GuiGraphics gfx = event.getGuiGraphics();
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(true);

        // Announcement goes first (full-screen, blocks HUD)
        AnnouncementRenderer.render(gfx, partialTick);

        // HUD only renders if announcement is done (guarded internally)
        AbilityHudRenderer.render(gfx, partialTick);
    }
}

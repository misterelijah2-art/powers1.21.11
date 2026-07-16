package powerful.powers.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import powerful.powers.client.hud.AbilityHudRenderer;
import powerful.powers.client.hud.AnnouncementRenderer;
import powerful.powers.powers;

/**
 * Hooks HUD and announcement overlay into the render pipeline.
 * Must be on Bus.FORGE (it's a game event, not a mod-lifecycle event).
 */
@EventBusSubscriber(modid = powers.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.FORGE)
public class HudOverlayEvents {

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        AnnouncementRenderer.render(event.getGuiGraphics(), event.getPartialTick());
        AbilityTitleRenderer.render(event.getGuiGraphics(), event.getPartialTick());
        AbilityHudRenderer.render(event.getGuiGraphics(), event.getPartialTick());
    }
}

package powerful.powers.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import powerful.powers.ability.AbilityType;
import powerful.powers.powers;

/**
 * Renders the ability HUD in the bottom-right corner.
 *
 * Layout:
 *   [Ability Name]      <- colored name label
 *   [■■■■■□□□□□]       <- cooldown bar (fills left-to-right as cooldown expires)
 *   READY / Xs          <- status text
 *
 * Also drives the title reveal animation.
 */
@EventBusSubscriber(modid = powers.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public class AbilityHudRenderer {

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null) return;

        // Tick and render the title reveal
        AbilityTitleRenderer.tick();
        AbilityTitleRenderer.render(event.getGuiGraphics(), event.getPartialTick());

        if (!ClientAbilityData.hasAbility()) return;

        GuiGraphics gfx = event.getGuiGraphics();
        Font font = mc.font;
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();

        AbilityType ability = ClientAbilityData.getCurrentAbility();
        float fraction = ClientAbilityData.getCooldownFraction(); // 0 = ready, 1 = just used

        // Position: bottom-right, 8px from edge
        int barWidth = 80;
        int barHeight = 6;
        int padX = 8;
        int padY = 10;
        int baseX = sw - barWidth - padX - 2;
        int baseY = sh - padY - barHeight - font.lineHeight * 2 - 6;

        // --- Background panel ---
        gfx.fill(baseX - 4, baseY - 4,
                  baseX + barWidth + 4, baseY + barHeight + font.lineHeight * 2 + 10,
                  0x88000000);

        // --- Ability name ---
        int nameColor = ability.getColor().getColor() != null
                ? (0xFF000000 | ability.getColor().getColor()) : 0xFFFFFFFF;
        gfx.drawString(font, ability.getDisplayName(), baseX, baseY, nameColor, true);

        // --- Cooldown bar background (dark) ---
        int barY = baseY + font.lineHeight + 3;
        gfx.fill(baseX, barY, baseX + barWidth, barY + barHeight, 0xFF333333);

        // --- Cooldown bar fill (color based on readiness) ---
        float readyFraction = 1f - fraction; // how much is filled = how ready
        int fillWidth = (int)(readyFraction * barWidth);
        int barColor = fraction <= 0f ? 0xFF44FF44 // green = ready
                     : fraction < 0.5f ? 0xFFFFAA00 // orange = half
                     : 0xFFFF3333; // red = mostly on cooldown
        if (fillWidth > 0) {
            gfx.fill(baseX, barY, baseX + fillWidth, barY + barHeight, barColor);
        }

        // --- Bar border ---
        gfx.hLine(baseX, baseX + barWidth - 1, barY, 0xFF888888);
        gfx.hLine(baseX, baseX + barWidth - 1, barY + barHeight - 1, 0xFF888888);
        gfx.vLine(baseX, barY, barY + barHeight, 0xFF888888);
        gfx.vLine(baseX + barWidth - 1, barY, barY + barHeight, 0xFF888888);

        // --- Status text ---
        int statusY = barY + barHeight + 3;
        String statusText;
        int statusColor;
        if (!ClientAbilityData.isOnCooldown()) {
            statusText  = "READY";
            statusColor = 0xFF44FF44;
        } else {
            int secsLeft = (int)Math.ceil(ClientAbilityData.getCooldownRemaining() / 20.0);
            statusText  = secsLeft + "s";
            statusColor = 0xFFAAAAAA;
        }
        gfx.drawString(font, statusText, baseX, statusY, statusColor, false);

        // Keybind hint
        String hint = "[R]";
        gfx.drawString(font, hint, baseX + barWidth - font.width(hint), statusY, 0xFF666666, false);
    }
}

package powerful.powers.client;

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
 * Renders the ability HUD (bottom-right).
 *
 * HUD is suppressed until ClientAbilityData.isHudUnlocked() returns true.
 * During charging, a second bar shows charge progress above the cooldown bar.
 */
@EventBusSubscriber(modid = powers.MODID, value = Dist.CLIENT)
public class AbilityHudRenderer {

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null) return;

        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);

        // Always tick + render the title (it manages its own visibility)
        AbilityTitleRenderer.tick();
        AbilityTitleRenderer.render(event.getGuiGraphics(), partialTick);

        // HUD is hidden until the reveal animation finishes
        if (!ClientAbilityData.hasAbility()) return;
        if (!ClientAbilityData.isHudUnlocked()) return;

        GuiGraphics gfx = event.getGuiGraphics();
        Font font = mc.font;
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();

        AbilityType ability  = ClientAbilityData.getCurrentAbility();
        float fraction       = ClientAbilityData.getCooldownFraction();
        boolean isCharging   = ClientAbilityData.isCharging();
        float chargeFraction = ClientAbilityData.getChargeFraction();

        int barWidth  = 80;
        int barHeight = 6;

        // Extra height when charging bar is shown
        int chargeBarHeight = isCharging ? barHeight + 3 : 0;
        int baseX = sw - barWidth - 10;
        int baseY = sh - font.lineHeight * 2 - barHeight - 14 - chargeBarHeight;

        // Background panel
        int panelBottom = baseY + barHeight + font.lineHeight * 2 + 10 + chargeBarHeight;
        gfx.fill(baseX - 4, baseY - 4, baseX + barWidth + 4, panelBottom, 0x88000000);

        // Ability name
        int nameColor = ability.getColor().getColor() != null
                ? (0xFF000000 | ability.getColor().getColor()) : 0xFFFFFFFF;
        gfx.drawString(font, ability.getDisplayName(), baseX, baseY, nameColor, true);

        // Charge bar (shown only while holding)
        if (isCharging) {
            int chargeY = baseY + font.lineHeight + 2;
            gfx.fill(baseX, chargeY, baseX + barWidth, chargeY + barHeight, 0xFF222222);
            int chargeFill = (int)(chargeFraction * barWidth);
            if (chargeFill > 0) {
                // Colour interpolates gold -> white as charge fills
                int chargeColor = chargeFraction >= 1f ? 0xFFFFFFFF : 0xFFFFCC00;
                gfx.fill(baseX, chargeY, baseX + chargeFill, chargeY + barHeight, chargeColor);
            }
            // Border
            gfx.hLine(baseX, baseX + barWidth - 1, chargeY, 0xFF888888);
            gfx.hLine(baseX, baseX + barWidth - 1, chargeY + barHeight - 1, 0xFF888888);
            gfx.vLine(baseX, chargeY, chargeY + barHeight, 0xFF888888);
            gfx.vLine(baseX + barWidth - 1, chargeY, chargeY + barHeight, 0xFF888888);
            // Label
            String chargeLabel = chargeFraction >= 1f ? "FULL" : (int)(chargeFraction * 100) + "%";
            gfx.drawString(font, chargeLabel,
                    baseX + barWidth - font.width(chargeLabel), chargeY - font.lineHeight - 1,
                    chargeFraction >= 1f ? 0xFFFFFFFF : 0xFFFFCC00, false);
        }

        // Cooldown bar
        int barY = baseY + font.lineHeight + 3 + chargeBarHeight;
        gfx.fill(baseX, barY, baseX + barWidth, barY + barHeight, 0xFF333333);
        int fillWidth = (int)((1f - fraction) * barWidth);
        int barColor = fraction <= 0f ? 0xFF44FF44 : fraction < 0.5f ? 0xFFFFAA00 : 0xFFFF3333;
        if (fillWidth > 0)
            gfx.fill(baseX, barY, baseX + fillWidth, barY + barHeight, barColor);
        gfx.hLine(baseX, baseX + barWidth - 1, barY, 0xFF888888);
        gfx.hLine(baseX, baseX + barWidth - 1, barY + barHeight - 1, 0xFF888888);
        gfx.vLine(baseX, barY, barY + barHeight, 0xFF888888);
        gfx.vLine(baseX + barWidth - 1, barY, barY + barHeight, 0xFF888888);

        // Status text
        int statusY = barY + barHeight + 3;
        String statusText = ClientAbilityData.isOnCooldown()
                ? (int)Math.ceil(ClientAbilityData.getCooldownRemaining() / 20.0) + "s"
                : isCharging ? "CHARGING" : "READY";
        int statusColor = ClientAbilityData.isOnCooldown() ? 0xFFAAAAAA
                        : isCharging ? 0xFFFFCC00 : 0xFF44FF44;
        gfx.drawString(font, statusText, baseX, statusY, statusColor, false);

        String hint = isCharging ? "[HOLD R]" : "[R]";
        gfx.drawString(font, hint, baseX + barWidth - font.width(hint), statusY, 0xFF666666, false);
    }
}

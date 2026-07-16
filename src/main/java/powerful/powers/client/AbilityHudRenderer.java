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
 * Renders the ability HUD (bottom-right) and drives the title reveal.
 * NeoForge 21.11: RenderGuiEvent.Post.getPartialTick() returns DeltaTracker —
 * we call event.getPartialTick().getGameTimeDeltaPartialTick(false) for the float.
 */
@EventBusSubscriber(modid = powers.MODID, value = Dist.CLIENT)
public class AbilityHudRenderer {

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null) return;

        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);

        AbilityTitleRenderer.tick();
        AbilityTitleRenderer.render(event.getGuiGraphics(), partialTick);

        if (!ClientAbilityData.hasAbility()) return;

        GuiGraphics gfx = event.getGuiGraphics();
        Font font = mc.font;
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();

        AbilityType ability = ClientAbilityData.getCurrentAbility();
        float fraction = ClientAbilityData.getCooldownFraction();

        int barWidth = 80;
        int barHeight = 6;
        int baseX = sw - barWidth - 10;
        int baseY = sh - font.lineHeight * 2 - barHeight - 14;

        // Background panel
        gfx.fill(baseX - 4, baseY - 4, baseX + barWidth + 4, baseY + barHeight + font.lineHeight * 2 + 10, 0x88000000);

        // Ability name
        int nameColor = ability.getColor().getColor() != null
                ? (0xFF000000 | ability.getColor().getColor()) : 0xFFFFFFFF;
        gfx.drawString(font, ability.getDisplayName(), baseX, baseY, nameColor, true);

        // Cooldown bar
        int barY = baseY + font.lineHeight + 3;
        gfx.fill(baseX, barY, baseX + barWidth, barY + barHeight, 0xFF333333);

        int fillWidth = (int)((1f - fraction) * barWidth);
        int barColor = fraction <= 0f ? 0xFF44FF44 : fraction < 0.5f ? 0xFFFFAA00 : 0xFFFF3333;
        if (fillWidth > 0) {
            gfx.fill(baseX, barY, baseX + fillWidth, barY + barHeight, barColor);
        }

        // Bar border
        gfx.hLine(baseX, baseX + barWidth - 1, barY, 0xFF888888);
        gfx.hLine(baseX, baseX + barWidth - 1, barY + barHeight - 1, 0xFF888888);
        gfx.vLine(baseX, barY, barY + barHeight, 0xFF888888);
        gfx.vLine(baseX + barWidth - 1, barY, barY + barHeight, 0xFF888888);

        // Status text
        int statusY = barY + barHeight + 3;
        String statusText  = ClientAbilityData.isOnCooldown()
            ? (int)Math.ceil(ClientAbilityData.getCooldownRemaining() / 20.0) + "s"
            : "READY";
        int statusColor = ClientAbilityData.isOnCooldown() ? 0xFFAAAAAA : 0xFF44FF44;
        gfx.drawString(font, statusText, baseX, statusY, statusColor, false);

        String hint = "[R]";
        gfx.drawString(font, hint, baseX + barWidth - font.width(hint), statusY, 0xFF666666, false);
    }
}

package powerful.powers.client;

import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.event.tick.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import powerful.powers.network.UseAbilityPacket;

public class ClientEvents {

    private static boolean wasDown = false;
    private static int     holdTicks = 0;

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        KeyBindings.register(event);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // ---- Announcement timer ----
        if (ClientAbilityState.showingAnnouncement) {
            if (ClientAbilityState.announcementTimer > 0) {
                ClientAbilityState.announcementTimer--;
            } else {
                // Announcement over -> unlock HUD
                ClientAbilityState.showingAnnouncement = false;
                // Tell server announcement done
                PacketDistributor.sendToServer(new UseAbilityPacket(false, -1));
            }
        }

        // ---- Keybind charge tracking ----
        boolean isDown = KeyBindings.USE_ABILITY.isDown();

        // Only process if player has an announced ability
        if (!ClientAbilityState.announced || ClientAbilityState.type == null) {
            wasDown = isDown;
            return;
        }

        if (isDown && !wasDown) {
            // Key just pressed
            ClientAbilityState.charging = true;
            ClientAbilityState.chargeTicks = 0;
            holdTicks = 0;
            PacketDistributor.sendToServer(new UseAbilityPacket(true, 0));
        }

        if (isDown && ClientAbilityState.charging) {
            holdTicks++;
            ClientAbilityState.chargeTicks = holdTicks;
        }

        if (!isDown && wasDown) {
            // Key just released
            ClientAbilityState.charging = false;
            ClientAbilityState.chargeTicks = 0;
            PacketDistributor.sendToServer(new UseAbilityPacket(false, holdTicks));
            holdTicks = 0;
        }

        wasDown = isDown;

        // Tick down local cooldown mirror (server is authoritative,
        // but this keeps the HUD smooth between syncs)
        if (ClientAbilityState.cooldownTicks > 0) {
            ClientAbilityState.cooldownTicks--;
        }
    }
}

package powerful.powers.client;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import powerful.powers.network.ChargeReleasePacket;
import powerful.powers.network.ChargeStartPacket;
import powerful.powers.network.UseAbilityPacket;
import powerful.powers.powers;

/** Keybind registration lives on the MOD bus; tick handling on the FORGE bus. */
@EventBusSubscriber(modid = powers.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class ClientEvents {

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        KeyBindings.register(event);
    }

    // ── Tick handler — must be on the FORGE bus ───────────────────────────────
    @EventBusSubscriber(modid = powers.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.FORGE)
    public static class TickHandler {

        private static boolean wasDown   = false;
        private static int     holdTicks = 0;

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;

            // Announcement countdown
            if (ClientAbilityState.showingAnnouncement) {
                if (ClientAbilityState.announcementTimer > 0) {
                    ClientAbilityState.announcementTimer--;
                } else {
                    ClientAbilityState.showingAnnouncement = false;
                    ClientAbilityState.announced = true;
                    PacketDistributor.sendToServer(new UseAbilityPacket(false, -1));
                }
            }

            AbilityTitleRenderer.tick();

            boolean isDown = KeyBindings.USE_ABILITY.isDown();

            if (!ClientAbilityState.announced || ClientAbilityState.type == null) {
                wasDown = isDown;
                return;
            }

            if (isDown && !wasDown) {
                ClientAbilityState.charging    = true;
                ClientAbilityState.chargeTicks = 0;
                holdTicks = 0;
                PacketDistributor.sendToServer(new ChargeStartPacket());
            }

            if (isDown && ClientAbilityState.charging) {
                holdTicks++;
                ClientAbilityState.chargeTicks = holdTicks;
            }

            if (!isDown && wasDown && ClientAbilityState.charging) {
                ClientAbilityState.charging    = false;
                ClientAbilityState.chargeTicks = 0;
                PacketDistributor.sendToServer(new ChargeReleasePacket());
                holdTicks = 0;
            }

            wasDown = isDown;

            if (ClientAbilityState.cooldownTicks > 0)
                ClientAbilityState.cooldownTicks--;
        }
    }
}

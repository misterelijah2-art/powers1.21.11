package powerful.powers.event;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import powerful.powers.ability.AbilityManager;

public class PlayerDeathHandler {

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer dead)) return;
        AbilityManager.onPlayerDeath(dead, event.getSource());
    }
}

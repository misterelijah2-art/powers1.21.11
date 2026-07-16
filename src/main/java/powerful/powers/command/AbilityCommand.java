package powerful.powers.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import powerful.powers.ability.AbilityAttachment;
import powerful.powers.ability.AbilityLogicHandler;
import powerful.powers.ability.AbilityType;
import powerful.powers.ability.PlayerAbilityData;
import powerful.powers.network.PacketHandler;
import powerful.powers.network.SyncAbilityPacket;
import powerful.powers.powers;

@EventBusSubscriber(modid = powers.MODID)
public class AbilityCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(
            Commands.literal("ability")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("give")
                    .then(Commands.argument("player", com.mojang.brigadier.arguments.StringArgumentType.word())
                        .then(Commands.argument("type", StringArgumentType.word())
                            .executes(ctx -> giveAbility(ctx.getSource(),
                                    StringArgumentType.getString(ctx, "player"),
                                    StringArgumentType.getString(ctx, "type")))
                    ))
                )
                .then(Commands.literal("revoke")
                    .then(Commands.argument("player", StringArgumentType.word())
                        .executes(ctx -> revokeAbility(ctx.getSource(),
                                StringArgumentType.getString(ctx, "player"))))
                )
                .then(Commands.literal("check")
                    .then(Commands.argument("player", StringArgumentType.word())
                        .executes(ctx -> checkAbility(ctx.getSource(),
                                StringArgumentType.getString(ctx, "player"))))
                )
        );
    }

    private static int giveAbility(CommandSourceStack src, String playerName, String typeName) {
        ServerPlayer target = src.getServer().getPlayerList().getPlayerByName(playerName);
        if (target == null) {
            src.sendFailure(Component.literal("Player not found: " + playerName));
            return 0;
        }
        AbilityType type;
        try {
            type = AbilityType.valueOf(typeName.toUpperCase());
        } catch (IllegalArgumentException e) {
            src.sendFailure(Component.literal("Unknown ability: " + typeName));
            return 0;
        }
        PlayerAbilityData data = target.getData(AbilityAttachment.ABILITY_DATA.get());
        data.setAbility(type);
        data.setCooldownRemaining(0);
        data.unlockHud();
        PacketHandler.sendToPlayer(target, new SyncAbilityPacket(
                type.name(), 0, type.cooldownTicks, false, 0, true));
        src.sendSuccess(() -> Component.literal(
                "\u00a7aGranted " + type.displayName + " to " + target.getName().getString()), true);
        return 1;
    }

    private static int revokeAbility(CommandSourceStack src, String playerName) {
        ServerPlayer target = src.getServer().getPlayerList().getPlayerByName(playerName);
        if (target == null) {
            src.sendFailure(Component.literal("Player not found: " + playerName));
            return 0;
        }
        PlayerAbilityData data = target.getData(AbilityAttachment.ABILITY_DATA.get());
        String old = data.hasAbility() ? data.getAbility().displayName : "none";
        data.clearAbility();
        PacketHandler.sendToPlayer(target, new SyncAbilityPacket("", 0, 0, false, 0, false));
        src.sendSuccess(() -> Component.literal(
                "\u00a7eRevoked " + old + " from " + target.getName().getString()), true);
        return 1;
    }

    private static int checkAbility(CommandSourceStack src, String playerName) {
        ServerPlayer target = src.getServer().getPlayerList().getPlayerByName(playerName);
        if (target == null) {
            src.sendFailure(Component.literal("Player not found: " + playerName));
            return 0;
        }
        PlayerAbilityData data = target.getData(AbilityAttachment.ABILITY_DATA.get());
        if (!data.hasAbility()) {
            src.sendSuccess(() -> Component.literal(target.getName().getString() + " has no ability."), false);
            return 1;
        }
        AbilityType type  = data.getAbility();
        int cdFull = type.cooldownTicks;
        int cdRem  = data.getCooldownRemaining();
        src.sendSuccess(() -> Component.literal(
                target.getName().getString() + " has \u00a7b" + type.displayName
                + "\u00a7r | CD: " + cdRem + "/" + cdFull + " ticks"), false);
        return 1;
    }
}

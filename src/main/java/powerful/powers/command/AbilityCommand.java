package powerful.powers.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
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
import powerful.powers.network.TitleRevealPacket;
import powerful.powers.powers;

@EventBusSubscriber(modid = powers.MODID)
public class AbilityCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();

        d.register(Commands.literal("ability")
            // /ability give <player> <type>
            .then(Commands.literal("give")
                .requires(src -> src.hasPermissions(2))
                .then(Commands.argument("target", EntityArgument.player())
                    .then(Commands.argument("type", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            for (AbilityType t : AbilityType.values())
                                builder.suggest(t.name().toLowerCase());
                            return builder.buildFuture();
                        })
                        .executes(AbilityCommand::giveAbility)
                    )
                )
            )
            // /ability revoke <player>
            .then(Commands.literal("revoke")
                .requires(src -> src.hasPermissions(2))
                .then(Commands.argument("target", EntityArgument.player())
                    .executes(AbilityCommand::revokeAbility)
                )
            )
            // /ability check <player>
            .then(Commands.literal("check")
                .requires(src -> src.hasPermissions(2))
                .then(Commands.argument("target", EntityArgument.player())
                    .executes(AbilityCommand::checkAbility)
                )
            )
            // /ability reload
            .then(Commands.literal("reload")
                .requires(src -> src.hasPermissions(4))
                .executes(ctx -> {
                    ctx.getSource().sendSuccess(() -> Component.literal("\u00a7aConfig reloaded (no config in this mod)."), false);
                    return 1;
                })
            )
        );
    }

    // ── /ability give ─────────────────────────────────────────────────────────

    private static int giveAbility(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
            String typeName = StringArgumentType.getString(ctx, "type").toUpperCase();
            AbilityType type;
            try { type = AbilityType.valueOf(typeName); }
            catch (IllegalArgumentException e) {
                ctx.getSource().sendFailure(Component.literal("\u00a7cUnknown ability: " + typeName));
                return 0;
            }

            PlayerAbilityData data = target.getData(AbilityAttachment.ABILITY_DATA.get());
            data.setAbility(type);
            data.setCooldownRemaining(0);
            data.unlockHud();

            PacketHandler.sendToPlayer(target, new TitleRevealPacket(type.name()));
            PacketHandler.sendToPlayer(target, new SyncAbilityPacket(
                    type.name(), 0, type.getCooldownTicks(), false, 0, true));

            ctx.getSource().sendSuccess(() -> Component.literal(
                    "\u00a7aGranted " + type.getDisplayName() + "\u00a7a to " +
                    target.getName().getString()), true);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("\u00a7cError: " + e.getMessage()));
            return 0;
        }
    }

    // ── /ability revoke ───────────────────────────────────────────────────────

    private static int revokeAbility(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
            PlayerAbilityData data = target.getData(AbilityAttachment.ABILITY_DATA.get());
            if (!data.hasAbility()) {
                ctx.getSource().sendFailure(Component.literal(target.getName().getString() + " has no ability."));
                return 0;
            }
            String old = data.getAbility().getDisplayName();
            data.clearAbility();
            PacketHandler.sendToPlayer(target, new SyncAbilityPacket("", 0, 0, false, 0, false));
            ctx.getSource().sendSuccess(() -> Component.literal(
                    "\u00a7eRevoked " + old + "\u00a7e from " + target.getName().getString()), true);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("\u00a7cError: " + e.getMessage()));
            return 0;
        }
    }

    // ── /ability check ────────────────────────────────────────────────────────

    private static int checkAbility(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
            PlayerAbilityData data = target.getData(AbilityAttachment.ABILITY_DATA.get());
            if (!data.hasAbility()) {
                ctx.getSource().sendSuccess(() -> Component.literal(
                        target.getName().getString() + " has no ability."), false);
                return 1;
            }
            AbilityType type = data.getAbility();
            int cdFull = type.getCooldownTicks();
            int cdRem  = data.getCooldownRemaining();
            ctx.getSource().sendSuccess(() -> Component.literal(
                    target.getName().getString() + "\u00a7r has \u00a7b" + type.getDisplayName() +
                    "\u00a7r (CD: " + cdRem + "/" + cdFull + " ticks)"), false);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("\u00a7cError: " + e.getMessage()));
            return 0;
        }
    }
}

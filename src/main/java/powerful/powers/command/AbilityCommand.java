package powerful.powers.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import powerful.powers.ability.AbilityAttachment;
import powerful.powers.ability.AbilityLogicHandler;
import powerful.powers.ability.AbilityType;
import powerful.powers.ability.PlayerAbilityData;
import powerful.powers.network.PacketHandler;
import powerful.powers.network.SyncAbilityPacket;
import powerful.powers.network.TitleRevealPacket;
import powerful.powers.powers;

import java.util.Arrays;
import java.util.Collection;

@EventBusSubscriber(modid = powers.MODID)
public class AbilityCommand {

    // Suggestion provider: lists all AbilityType names in lowercase
    private static final SuggestionProvider<CommandSourceStack> ABILITY_SUGGESTIONS =
            (ctx, builder) -> {
                for (AbilityType type : AbilityType.values())
                    builder.suggest(type.name().toLowerCase());
                return builder.buildFuture();
            };

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(
            Commands.literal("ability")
                .requires(src -> src.hasPermission(2)) // OP level 2

                // /ability give <targets> <ability>
                .then(Commands.literal("give")
                    .then(Commands.argument("targets", EntityArgument.players())
                        .then(Commands.argument("ability", StringArgumentType.word())
                            .suggests(ABILITY_SUGGESTIONS)
                            .executes(ctx -> giveAbility(
                                    ctx,
                                    EntityArgument.getPlayers(ctx, "targets"),
                                    StringArgumentType.getString(ctx, "ability")
                            ))
                        )
                    )
                )

                // /ability clear <targets>
                .then(Commands.literal("clear")
                    .then(Commands.argument("targets", EntityArgument.players())
                        .executes(ctx -> clearAbility(
                                ctx,
                                EntityArgument.getPlayers(ctx, "targets")
                        ))
                    )
                )

                // /ability check <target>
                .then(Commands.literal("check")
                    .then(Commands.argument("target", EntityArgument.player())
                        .executes(ctx -> checkAbility(
                                ctx,
                                EntityArgument.getPlayer(ctx, "target")
                        ))
                    )
                )

                // /ability list  — lists all available ability names
                .then(Commands.literal("list")
                    .executes(AbilityCommand::listAbilities)
                )
        );
    }

    // ── GIVE ─────────────────────────────────────────────────────────────────

    private static int giveAbility(CommandContext<CommandSourceStack> ctx,
                                   Collection<ServerPlayer> targets,
                                   String abilityName) {
        AbilityType type;
        try {
            type = AbilityType.valueOf(abilityName.toUpperCase());
        } catch (IllegalArgumentException e) {
            ctx.getSource().sendFailure(Component.literal(
                    "\u00a7cUnknown ability: \"" + abilityName + "\". Use /ability list."));
            return 0;
        }

        int count = 0;
        for (ServerPlayer sp : targets) {
            PlayerAbilityData data = sp.getData(AbilityAttachment.ABILITY_DATA.get());
            data.setAbility(type);
            data.setCooldownRemaining(0);

            // Send title reveal packet so the player sees the assignment screen
            PacketHandler.sendToPlayer(sp, new TitleRevealPacket(type.name()));
            // Sync HUD
            PacketHandler.sendToPlayer(sp, new SyncAbilityPacket(
                    type.name(), 0, type.getCooldownTicks()));

            ctx.getSource().sendSuccess(() -> Component.literal(
                    "\u00a7aGranted " + type.getDisplayName() + "\u00a7a to " +
                    sp.getName().getString() + "."), true);
            powers.LOGGER.info("[Powers] Admin gave {} to {}",
                    type.name(), sp.getName().getString());
            count++;
        }
        return count;
    }

    // ── CLEAR ─────────────────────────────────────────────────────────────────

    private static int clearAbility(CommandContext<CommandSourceStack> ctx,
                                    Collection<ServerPlayer> targets) {
        int count = 0;
        for (ServerPlayer sp : targets) {
            PlayerAbilityData data = sp.getData(AbilityAttachment.ABILITY_DATA.get());
            if (!data.hasAbility()) {
                ctx.getSource().sendSuccess(() -> Component.literal(
                        "\u00a7e" + sp.getName().getString() + " has no ability to clear."), false);
                continue;
            }
            String old = data.getAbility().getDisplayName();
            data.clearAbility();
            PacketHandler.sendToPlayer(sp, new SyncAbilityPacket("", 0, 0));
            sp.displayClientMessage(
                    Component.literal("\u00a7cYour ability has been removed by an admin."), false);
            ctx.getSource().sendSuccess(() -> Component.literal(
                    "\u00a7aCleared " + old + "\u00a7a from " + sp.getName().getString() + "."), true);
            powers.LOGGER.info("[Powers] Admin cleared ability from {}", sp.getName().getString());
            count++;
        }
        return count;
    }

    // ── CHECK ─────────────────────────────────────────────────────────────────

    private static int checkAbility(CommandContext<CommandSourceStack> ctx,
                                    ServerPlayer target) {
        PlayerAbilityData data = target.getData(AbilityAttachment.ABILITY_DATA.get());
        if (!data.hasAbility()) {
            ctx.getSource().sendSuccess(() -> Component.literal(
                    "\u00a7e" + target.getName().getString() + " has no ability."), false);
        } else {
            AbilityType type = data.getAbility();
            int cdRem  = data.getCooldownRemaining();
            int cdFull = type.getCooldownTicks();
            String cdStr = cdRem > 0
                    ? String.format("%.1fs cooldown remaining", cdRem / 20.0f)
                    : "ready";
            ctx.getSource().sendSuccess(() -> Component.literal(
                    "\u00a76" + target.getName().getString() +
                    "\u00a7r has \u00a7b" + type.getDisplayName() +
                    "\u00a7r (" + cdStr + ", full cd: " +
                    String.format("%.1fs", cdFull / 20.0f) + ")"), false);
        }
        return 1;
    }

    // ── LIST ─────────────────────────────────────────────────────────────────

    private static int listAbilities(CommandContext<CommandSourceStack> ctx) {
        StringBuilder sb = new StringBuilder("\u00a76Available abilities:\u00a7r ");
        AbilityType[] types = AbilityType.values();
        for (int i = 0; i < types.length; i++) {
            sb.append("\u00a7b").append(types[i].name().toLowerCase());
            if (i < types.length - 1) sb.append("\u00a7r, ");
        }
        ctx.getSource().sendSuccess(() -> Component.literal(sb.toString()), false);
        return types.length;
    }
}

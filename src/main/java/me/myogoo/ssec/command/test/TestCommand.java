package me.myogoo.ssec.command.test;

import me.myogoo.ssec.api.SSECDebug;
import me.myogoo.ssec.api.command.*;
import me.myogoo.ssec.api.command.permission.PermissionLevel;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

@SSCommand("ssec")
@SSCAlias({ "ssc", "스섹" })
@SSCPermission(permission = PermissionLevel.GAME_MASTER)
@SSECDebug
public class TestCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestCommand.class);

    @SSCAlias("/s")
    @SSCommand(value = "say", parent = TestCommand.class)
    public static class SubCommand {

        @SSCExecute
        public static void execute(CommandContext<CommandSourceStack> ctx, @SSCArgument("msg") String msg) {
            ctx.getSource().sendSuccess(() -> Component.literal("Subcommand msg: " + msg), false);
        }

        @SSCommand(value = "number", parent = TestCommand.SubCommand.class)
        @SSCAlias({ "/ssec/ssub" })
        public static class SubSubCommand {
            @SSCExecute
            public static void execute(CommandContext<CommandSourceStack> ctx, @SSCArgument("number") int number) {
                ctx.getSource().sendSuccess(() -> Component.literal("SubSubCommand number: " + number), false);
            }
        }
    }

    @SSCommand(value = "tp", parent = TestCommand.class)
    @SSCAlias({ "티피", "순가이동", "ttp" })
    public static class TPCommand {
        @SSCExecute
        public static void execute(CommandContext<CommandSourceStack> ctx,
                @SSCArgument("vec") Vec3 vec) {
            ctx.getSource().sendSuccess(
                    () -> Component.literal(String.format("Teleporting to: (%.2f, %.2f, %.2f)", vec.x, vec.y, vec.z)),
                    false);
        }
    }

    @SSCommand(value = "alias", parent = TestCommand.class)
    @SSCAlias({ "얼라", "aaa" })
    public static class AliasCommand {
        @SSCExecute
        public static void excute(CommandContext<CommandSourceStack> ctx) {
            ctx.getSource().sendSuccess(() -> Component.literal("sdssd"), false);
        }
    }
}

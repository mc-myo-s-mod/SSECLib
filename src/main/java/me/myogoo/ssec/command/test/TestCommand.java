package me.myogoo.ssec.command.test;

import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mojang.brigadier.context.CommandContext;

import me.myogoo.ssec.api.command.SSCAlias;
import me.myogoo.ssec.api.command.SSCExecute;
import me.myogoo.ssec.api.command.SSCommand;
import me.myogoo.ssec.api.command.SSCArgument;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

@SSCommand("ssec")
@SSCAlias({ "ssc" })
public class TestCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestCommand.class);

    @SSCExecute
    public static void execute(CommandContext<CommandSourceStack> ctx,
            @SSCArgument("amount") int amount,
            @SSCArgument("name") String name) {
        LOGGER.info("Executed test command with amount: {} and name: {}", amount, name);
        ctx.getSource().sendSuccess(() -> Component.literal("hi! amount=" + amount + ", name=" + name), false);
    }

    @SSCommand(value = "say", parent = TestCommand.class)
    public static class SubCommand {

        @SSCExecute
        public static void execute(CommandContext<CommandSourceStack> ctx, @SSCArgument("msg") String msg) {
            ctx.getSource().sendSuccess(() -> Component.literal("Subcommand msg: " + msg), false);
        }

        @SSCommand(value = "number", parent = SubCommand.class)
        @SSCAlias({ "seec.ssub" })
        public static class SubSubCommand {
            @SSCExecute
            public static void execute(CommandContext<CommandSourceStack> ctx, @SSCArgument("number") int number) {
                ctx.getSource().sendSuccess(() -> Component.literal("SubSubCommand number: " + number), false);
            }
        }
    }

    @SSCommand(value = "tp", parent = TestCommand.class)
    public static class TPCommand {
        @SSCExecute
        public static void execute(CommandContext<CommandSourceStack> ctx,
                @SSCArgument("vec") Vec3 vec) {
            ctx.getSource().sendSuccess(
                    () -> Component.literal(String.format("Teleporting to: (%.2f, %.2f, %.2f)", vec.x, vec.y, vec.z)),
                    false);
        }
    }
}

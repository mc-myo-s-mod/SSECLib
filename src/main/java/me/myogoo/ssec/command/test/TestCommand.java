package me.myogoo.ssec.command.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mojang.brigadier.context.CommandContext;

import me.myogoo.ssec.api.command.SSCAlias;
import me.myogoo.ssec.api.command.SSCExecute;
import me.myogoo.ssec.api.command.SSCommand;
import me.myogoo.ssec.api.command.SSCArg;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

@SSCommand("newt")
@SSCAlias({ "newtest", "t" })
public class TestCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestCommand.class);

    @SSCExecute
    public static void execute(CommandContext<CommandSourceStack> ctx,
            @SSCArg("amount") int amount,
            @SSCArg("name") String name) {
        LOGGER.info("Executed test command with amount: {} and name: {}", amount, name);
        ctx.getSource().sendSuccess(() -> Component.literal("hi! amount=" + amount + ", name=" + name), false);
    }

    @SSCommand(value = "fuck", parent = TestCommand.class)
    public static class SubCommand {

        @SSCExecute
        public static void execute(CommandContext<CommandSourceStack> ctx, @SSCArg("msg") String msg) {
            ctx.getSource().sendSuccess(() -> Component.literal("Subcommand msg: " + msg), false);
        }
    }
}

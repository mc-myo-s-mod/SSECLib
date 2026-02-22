package me.myogoo.ssec.command.test;

import com.mojang.brigadier.context.CommandContext;
import me.myogoo.ssec.api.command.SSCExecute;
import me.myogoo.ssec.api.command.SSCommand;
import net.minecraft.commands.CommandSourceStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SSCommand(value = "other", parent = TestCommand.class)
public class TestSubCommand {
    private final static Logger LOGGER = LoggerFactory.getLogger(TestSubCommand.class);
    @SSCExecute
    public void execute(CommandContext<CommandSourceStack> ctx) {
        LOGGER.info("Executed other subcommand");
        ctx.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal("Executed other subcommand"), false);
    }
}

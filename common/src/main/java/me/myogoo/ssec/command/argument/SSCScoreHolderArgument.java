package me.myogoo.ssec.command.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import me.myogoo.ssec.api.command.argument.SSCArgumentAdapter;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.ScoreHolderArgument;
import net.minecraft.world.scores.ScoreHolder;

public class SSCScoreHolderArgument implements SSCArgumentAdapter<ScoreHolder> {
    @Override
    public ScoreHolder value(CommandContext<CommandSourceStack> ctx, String name) {
        try {
            return ScoreHolderArgument.getName(ctx, name);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ArgumentType<?> argumentType() {
        return ScoreHolderArgument.scoreHolder();
    }
}

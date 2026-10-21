package me.myogoo.ssec.command.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import me.myogoo.ssec.api.command.argument.SSCArgumentAdapter;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.ScoreboardSlotArgument;
import net.minecraft.world.scores.DisplaySlot;

public class SSCDisplaySlotArgument implements SSCArgumentAdapter<DisplaySlot> {
    @Override
    public DisplaySlot value(CommandContext<CommandSourceStack> ctx, String name) {
        try {
            return ScoreboardSlotArgument.getDisplaySlot(ctx, name);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ArgumentType<?> argumentType() {
        return ScoreboardSlotArgument.displaySlot();
    }
}

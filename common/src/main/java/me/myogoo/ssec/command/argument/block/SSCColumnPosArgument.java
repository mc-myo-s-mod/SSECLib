package me.myogoo.ssec.command.argument.block;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import me.myogoo.ssec.api.command.argument.SSCArgumentAdapter;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.coordinates.ColumnPosArgument;
import net.minecraft.server.level.ColumnPos;

public class SSCColumnPosArgument implements SSCArgumentAdapter<ColumnPos> {
    @Override
    public ColumnPos value(CommandContext<CommandSourceStack> ctx, String name) {
        try {
            return ColumnPosArgument.getColumnPos(ctx, name);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ArgumentType<?> argumentType() {
        return ColumnPosArgument.columnPos();
    }
}

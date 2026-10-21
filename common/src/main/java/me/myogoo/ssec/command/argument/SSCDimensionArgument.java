package me.myogoo.ssec.command.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import me.myogoo.ssec.api.command.argument.SSCArgumentAdapter;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.server.level.ServerLevel;

public class SSCDimensionArgument implements SSCArgumentAdapter<ServerLevel> {
    @Override
    public ServerLevel value(CommandContext<CommandSourceStack> ctx, String name) {
        try {
            return DimensionArgument.getDimension(ctx, name);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ArgumentType<?> argumentType() {
        return DimensionArgument.dimension();
    }
}

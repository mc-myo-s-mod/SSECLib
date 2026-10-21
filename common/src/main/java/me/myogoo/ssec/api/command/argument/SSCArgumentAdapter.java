package me.myogoo.ssec.api.command.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;

public interface SSCArgumentAdapter<T> {
    T value(CommandContext<CommandSourceStack> ctx, String name) throws Exception;

    default ArgumentType<?> argumentType() {
        throw new UnsupportedOperationException("This argument adapter requires CommandBuildContext.");
    }

    default ArgumentType<?> argumentType(CommandBuildContext buildContext) {
        return argumentType();
    }
}

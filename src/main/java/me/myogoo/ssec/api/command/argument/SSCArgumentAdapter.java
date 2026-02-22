package me.myogoo.ssec.api.command.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;

public interface SSCArgumentAdapter<T> {
    T value(CommandContext<?> ctx, String name);

    ArgumentType<?> argumentType();
}

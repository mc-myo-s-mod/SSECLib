package me.myogoo.ssec.api.command.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;

public interface SSCArgumentAdapter<T> {
    T value(CommandContext<CommandSourceStack> ctx, String name);

    ArgumentType<?> argumentType();
}

package me.myogoo.ssec.command.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import me.myogoo.ssec.api.command.argument.SSCArgumentAdapter;

public class SSCIntArgument implements SSCArgumentAdapter<Integer> {
    @Override
    public Integer value(CommandContext<?> ctx, String name) {
        return IntegerArgumentType.getInteger(ctx, name);
    }

    @Override
    public ArgumentType<?> argumentType() {
        return IntegerArgumentType.integer();
    }
}

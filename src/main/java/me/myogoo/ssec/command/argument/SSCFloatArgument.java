package me.myogoo.ssec.command.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.context.CommandContext;
import me.myogoo.ssec.api.command.argument.SSCArgumentAdapter;

public class SSCFloatArgument implements SSCArgumentAdapter<Float> {
    @Override
    public Float value(CommandContext<?> ctx, String name) {
        return FloatArgumentType.getFloat(ctx, name);
    }

    @Override
    public ArgumentType<?> argumentType() {
        return FloatArgumentType.floatArg();
    }
}

package me.myogoo.ssec.command.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import net.minecraft.commands.CommandSourceStack;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import me.myogoo.ssec.api.command.argument.SSCArgumentAdapter;

public class SSCDoubleArgument implements SSCArgumentAdapter<Double> {
    @Override
    public Double value(CommandContext<CommandSourceStack> ctx, String name) {
        return DoubleArgumentType.getDouble(ctx, name);
    }

    @Override
    public ArgumentType<?> argumentType() {
        return DoubleArgumentType.doubleArg();
    }
}

package me.myogoo.ssec.command.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import net.minecraft.commands.CommandSourceStack;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import me.myogoo.ssec.api.command.argument.SSCArgumentAdapter;

public class SSCIntArgument implements SSCArgumentAdapter<Integer> {
    @Override
    public Integer value(CommandContext<CommandSourceStack> ctx, String name) {
        return IntegerArgumentType.getInteger(ctx, name);
    }

    @Override
    public ArgumentType<?> argumentType() {
        return IntegerArgumentType.integer();
    }
}

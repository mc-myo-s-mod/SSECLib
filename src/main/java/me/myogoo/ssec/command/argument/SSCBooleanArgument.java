package me.myogoo.ssec.command.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import net.minecraft.commands.CommandSourceStack;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import me.myogoo.ssec.api.command.argument.SSCArgumentAdapter;

public class SSCBooleanArgument implements SSCArgumentAdapter<Boolean> {
    @Override
    public Boolean value(CommandContext<CommandSourceStack> ctx, String name) {
        return BoolArgumentType.getBool(ctx, name);
    }

    @Override
    public ArgumentType<?> argumentType() {
        return BoolArgumentType.bool();
    }
}

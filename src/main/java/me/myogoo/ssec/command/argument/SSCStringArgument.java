package me.myogoo.ssec.command.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import me.myogoo.ssec.api.command.argument.SSCArgumentAdapter;

public class SSCStringArgument implements SSCArgumentAdapter<String> {
    @Override
    public String value(CommandContext<?> ctx, String name) {
        return StringArgumentType.getString(ctx, name);
    }

    @Override
    public ArgumentType<?> argumentType() {
        return StringArgumentType.string();
    }
}

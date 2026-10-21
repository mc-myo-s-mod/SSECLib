package me.myogoo.ssec.command.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import me.myogoo.ssec.api.command.argument.SSCArgumentAdapter;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.StyleArgument;
import net.minecraft.network.chat.Style;

public class SSCStyleArgument implements SSCArgumentAdapter<Style> {
    @Override
    public Style value(CommandContext<CommandSourceStack> ctx, String name) {
        try {
            return StyleArgument.getStyle(ctx, name);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ArgumentType<?> argumentType(CommandBuildContext buildContext) {
        return StyleArgument.style(buildContext);
    }
}

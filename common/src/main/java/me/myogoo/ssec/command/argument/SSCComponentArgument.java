package me.myogoo.ssec.command.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import me.myogoo.ssec.api.command.argument.SSCArgumentAdapter;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.ComponentArgument;
import net.minecraft.network.chat.Component;

public class SSCComponentArgument implements SSCArgumentAdapter<Component> {
    @Override
    public Component value(CommandContext<CommandSourceStack> ctx, String name) {
        try {
            return ComponentArgument.getResolvedComponent(ctx, name);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ArgumentType<?> argumentType(CommandBuildContext buildContext) {
        return ComponentArgument.textComponent(buildContext);
    }
}

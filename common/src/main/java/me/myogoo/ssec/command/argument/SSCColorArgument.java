package me.myogoo.ssec.command.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import me.myogoo.ssec.api.command.argument.SSCArgumentAdapter;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.ColorArgument;

public class SSCColorArgument implements SSCArgumentAdapter<ChatFormatting> {
    @Override
    public ChatFormatting value(CommandContext<CommandSourceStack> ctx, String name) {
        try {
            return ColorArgument.getColor(ctx, name);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ArgumentType<?> argumentType() {
        return ColorArgument.color();
    }
}

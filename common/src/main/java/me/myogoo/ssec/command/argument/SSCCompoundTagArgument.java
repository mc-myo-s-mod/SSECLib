package me.myogoo.ssec.command.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import me.myogoo.ssec.api.command.argument.SSCArgumentAdapter;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.CompoundTagArgument;
import net.minecraft.nbt.CompoundTag;

public class SSCCompoundTagArgument implements SSCArgumentAdapter<CompoundTag> {
    @Override
    public CompoundTag value(CommandContext<CommandSourceStack> ctx, String name) {
        try {
            return CompoundTagArgument.getCompoundTag(ctx, name);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ArgumentType<?> argumentType() {
        return CompoundTagArgument.compoundTag();
    }
}

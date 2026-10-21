package me.myogoo.ssec.command.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import me.myogoo.ssec.api.command.argument.SSCArgumentAdapter;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.NbtPathArgument;

public class SSCNbtPathArgument implements SSCArgumentAdapter<NbtPathArgument.NbtPath> {
    @Override
    public NbtPathArgument.NbtPath value(CommandContext<CommandSourceStack> ctx, String name) {
        try {
            return NbtPathArgument.getPath(ctx, name);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ArgumentType<?> argumentType() {
        return NbtPathArgument.nbtPath();
    }
}

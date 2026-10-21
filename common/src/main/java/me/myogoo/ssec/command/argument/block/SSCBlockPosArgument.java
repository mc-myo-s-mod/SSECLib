package me.myogoo.ssec.command.argument.block;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import me.myogoo.ssec.api.command.argument.SSCArgumentAdapter;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;

public class SSCBlockPosArgument implements SSCArgumentAdapter<BlockPos> {
    @Override
    public BlockPos value(CommandContext<CommandSourceStack> ctx, String name) {
        return BlockPosArgument.getBlockPos(ctx, name);
    }

    @Override
    public ArgumentType<?> argumentType() {
        return BlockPosArgument.blockPos();
    }
}

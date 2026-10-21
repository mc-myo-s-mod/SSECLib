package me.myogoo.ssec.command.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import me.myogoo.ssec.api.command.argument.SSCArgumentAdapter;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.commands.arguments.blocks.BlockStateArgument;

public class SSCBlockInputArgument implements SSCArgumentAdapter<BlockInput> {
    @Override
    public BlockInput value(CommandContext<CommandSourceStack> ctx, String name) {
        try {
            return BlockStateArgument.getBlock(ctx, name);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ArgumentType<?> argumentType(CommandBuildContext buildContext) {
        return BlockStateArgument.block(buildContext);
    }
}

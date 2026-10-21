package me.myogoo.ssec.command.argument.math;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import me.myogoo.ssec.api.command.argument.SSCArgumentAdapter;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityAnchorArgument;

public class SSCEntityAnchorArgument implements SSCArgumentAdapter<EntityAnchorArgument.Anchor> {
    @Override
    public EntityAnchorArgument.Anchor value(CommandContext<CommandSourceStack> ctx, String name) {
        try {
            return EntityAnchorArgument.getAnchor(ctx, name);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ArgumentType<?> argumentType() {
        return EntityAnchorArgument.anchor();
    }
}

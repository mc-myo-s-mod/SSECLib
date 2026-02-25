package me.myogoo.ssec.command.argument.math;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import me.myogoo.ssec.api.command.argument.SSCArgumentAdapter;
import me.myogoo.ssec.mixin.argument.AngleArgumentAccessor;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.AngleArgument;


public class SSCAngleRelativeArgument implements SSCArgumentAdapter<AngleArgument.SingleAngle> {
    @Override
    public AngleArgument.SingleAngle value(CommandContext<CommandSourceStack> ctx, String name) {
        return AngleArgumentAccessor.create(AngleArgument.getAngle(ctx, name), true);
    }

    @Override
    public ArgumentType<?> argumentType() {
        return AngleArgument.angle();
    }

    public interface AngleFactory {
        AngleArgument.SingleAngle makeInstance(float angle);
    }
}


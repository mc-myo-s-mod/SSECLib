package me.myogoo.ssec.command.argument.math;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import me.myogoo.ssec.api.command.argument.SSCArgumentAdapter;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.coordinates.Vec2Argument;
import net.minecraft.world.phys.Vec2;

public class SSCVec2Argument implements SSCArgumentAdapter<Vec2> {
    @Override
    public Vec2 value(CommandContext<CommandSourceStack> ctx, String name) {
        return Vec2Argument.getVec2(ctx, name);
    }

    @Override
    public ArgumentType<?> argumentType() {
        return Vec2Argument.vec2();
    }
}

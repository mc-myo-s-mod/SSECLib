package me.myogoo.ssec.command.argument.math;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import me.myogoo.ssec.api.command.argument.SSCArgumentAdapter;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.world.phys.Vec3;

public class SSCVec3Argument implements SSCArgumentAdapter<Vec3> {
    @Override
    public Vec3 value(CommandContext<CommandSourceStack> ctx, String name) {
        return Vec3Argument.getVec3((CommandContext<CommandSourceStack>) ctx, name);
    }

    @Override
    public ArgumentType<?> argumentType() {
        return Vec3Argument.vec3();
    }
}

package me.myogoo.ssec.command.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import me.myogoo.ssec.api.command.argument.SSCArgumentAdapter;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.TemplateRotationArgument;
import net.minecraft.world.level.block.Rotation;

public class SSCRotationTemplateArgument implements SSCArgumentAdapter<Rotation> {
    @Override
    public Rotation value(CommandContext<CommandSourceStack> ctx, String name) {
        try {
            return TemplateRotationArgument.getRotation(ctx, name);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ArgumentType<?> argumentType() {
        return TemplateRotationArgument.templateRotation();
    }
}

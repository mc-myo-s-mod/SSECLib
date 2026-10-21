package me.myogoo.ssec.command.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import me.myogoo.ssec.api.command.argument.SSCArgumentAdapter;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.TemplateMirrorArgument;
import net.minecraft.world.level.block.Mirror;

public class SSCMirrorArgument implements SSCArgumentAdapter<Mirror> {
    @Override
    public Mirror value(CommandContext<CommandSourceStack> ctx, String name) {
        try {
            return TemplateMirrorArgument.getMirror(ctx, name);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ArgumentType<?> argumentType() {
        return TemplateMirrorArgument.templateMirror();
    }
}

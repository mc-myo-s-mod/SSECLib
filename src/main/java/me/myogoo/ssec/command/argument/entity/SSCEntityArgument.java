package me.myogoo.ssec.command.argument.entity;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import me.myogoo.ssec.api.command.argument.SSCArgumentAdapter;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.world.entity.Entity;

public class SSCEntityArgument implements SSCArgumentAdapter<Entity> {
    @SuppressWarnings("unchecked")
    @Override
    public Entity value(CommandContext<?> ctx, String name) {
        try {
            return EntityArgument.getEntity((CommandContext<CommandSourceStack>) ctx, name);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ArgumentType<?> argumentType() {
        return EntityArgument.entity();
    }
}

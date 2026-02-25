package me.myogoo.ssec.command.argument.entity;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import me.myogoo.ssec.api.command.argument.SSCArgumentAdapter;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.world.entity.Entity;

import java.util.Collection;

public class SSCEntitiesArgument implements SSCArgumentAdapter<Entity[]> {
    @Override
    public Entity[] value(CommandContext<CommandSourceStack> ctx, String name) {
        try {
            Collection<? extends Entity> entities = EntityArgument.getEntities(ctx,
                    name);
            return entities.toArray(new Entity[0]);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ArgumentType<?> argumentType() {
        return EntityArgument.entities();
    }
}

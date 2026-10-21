package me.myogoo.ssec.command.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import java.util.UUID;
import me.myogoo.ssec.api.command.argument.SSCArgumentAdapter;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.UuidArgument;

public class SSCUUIDArgument implements SSCArgumentAdapter<UUID> {
    @Override
    public UUID value(CommandContext<CommandSourceStack> ctx, String name) {
        try {
            return UuidArgument.getUuid(ctx, name);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ArgumentType<?> argumentType() {
        return UuidArgument.uuid();
    }
}

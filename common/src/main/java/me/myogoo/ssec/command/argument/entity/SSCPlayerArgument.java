package me.myogoo.ssec.command.argument.entity;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import me.myogoo.ssec.api.command.argument.SSCArgumentAdapter;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;

public class SSCPlayerArgument implements SSCArgumentAdapter<ServerPlayer> {
    @Override
    public ServerPlayer value(CommandContext<CommandSourceStack> ctx, String name) {
        try {
            return EntityArgument.getPlayer(ctx, name);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ArgumentType<?> argumentType() {
        return EntityArgument.player();
    }
}

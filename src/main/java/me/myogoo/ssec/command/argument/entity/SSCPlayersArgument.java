package me.myogoo.ssec.command.argument.entity;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import me.myogoo.ssec.api.command.argument.SSCArgumentAdapter;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;

public class SSCPlayersArgument implements SSCArgumentAdapter<ServerPlayer[]> {
    @SuppressWarnings("unchecked")
    @Override
    public ServerPlayer[] value(CommandContext<?> ctx, String name) {
        try {
            Collection<ServerPlayer> players = EntityArgument.getPlayers((CommandContext<CommandSourceStack>) ctx,
                    name);
            return players.toArray(new ServerPlayer[0]);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ArgumentType<?> argumentType() {
        return EntityArgument.players();
    }
}

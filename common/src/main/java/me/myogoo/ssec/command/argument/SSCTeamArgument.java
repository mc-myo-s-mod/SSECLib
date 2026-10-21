package me.myogoo.ssec.command.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import me.myogoo.ssec.api.command.argument.SSCArgumentAdapter;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.TeamArgument;
import net.minecraft.world.scores.PlayerTeam;

public class SSCTeamArgument implements SSCArgumentAdapter<PlayerTeam> {
    @Override
    public PlayerTeam value(CommandContext<CommandSourceStack> ctx, String name) {
        try {
            return TeamArgument.getTeam(ctx, name);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ArgumentType<?> argumentType() {
        return TeamArgument.team();
    }
}

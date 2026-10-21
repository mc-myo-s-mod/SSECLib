package me.myogoo.ssec.command.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import me.myogoo.ssec.api.command.argument.SSCArgumentAdapter;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.HeightmapTypeArgument;
import net.minecraft.world.level.levelgen.Heightmap;

public class SSCHeightmapTypeArgument implements SSCArgumentAdapter<Heightmap.Types> {
    @Override
    public Heightmap.Types value(CommandContext<CommandSourceStack> ctx, String name) {
        try {
            return HeightmapTypeArgument.getHeightmap(ctx, name);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ArgumentType<?> argumentType() {
        return HeightmapTypeArgument.heightmap();
    }
}

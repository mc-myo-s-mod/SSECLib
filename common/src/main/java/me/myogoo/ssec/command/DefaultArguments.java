package me.myogoo.ssec.command;

import me.myogoo.ssec.command.argument.SSCColorArgument;
import me.myogoo.ssec.command.argument.SSCCompoundTagArgument;
import me.myogoo.ssec.command.argument.SSCComponentArgument;
import me.myogoo.ssec.command.argument.SSCDimensionArgument;
import me.myogoo.ssec.command.argument.SSCDisplaySlotArgument;
import me.myogoo.ssec.command.argument.SSCGameModeArgument;
import me.myogoo.ssec.command.argument.SSCHeightmapTypeArgument;
import me.myogoo.ssec.command.argument.SSCMirrorArgument;
import me.myogoo.ssec.command.argument.SSCNbtPathArgument;
import me.myogoo.ssec.command.argument.SSCRotationTemplateArgument;
import me.myogoo.ssec.command.argument.SSCScoreHolderArgument;
import me.myogoo.ssec.command.argument.SSCSlotRangeArgument;
import me.myogoo.ssec.command.argument.SSCStyleArgument;
import me.myogoo.ssec.command.argument.SSCTeamArgument;
import me.myogoo.ssec.command.argument.SSCUUIDArgument;
import me.myogoo.ssec.command.argument.math.SSCAngleArgument;
import me.myogoo.ssec.command.argument.math.SSCEntityAnchorArgument;
import me.myogoo.ssec.command.argument.SSCBlockInputArgument;
import me.myogoo.ssec.command.argument.block.SSCBlockPosArgument;
import me.myogoo.ssec.command.argument.block.SSCColumnPosArgument;
import me.myogoo.ssec.command.argument.math.SSCVec2Argument;
import me.myogoo.ssec.command.argument.math.SSCVec3Argument;
import me.myogoo.ssec.command.argument.entity.SSCEntityArgument;
import me.myogoo.ssec.command.argument.entity.SSCEntitiesArgument;
import me.myogoo.ssec.command.argument.entity.SSCPlayerArgument;
import me.myogoo.ssec.command.argument.entity.SSCPlayersArgument;

final class DefaultArguments {
    private DefaultArguments() { }

    static void register() {
        CommandArguments.registerAdapter(net.minecraft.ChatFormatting.class, new SSCColorArgument());
        CommandArguments.registerAdapter(net.minecraft.nbt.CompoundTag.class, new SSCCompoundTagArgument());
        CommandArguments.registerAdapter(net.minecraft.network.chat.Component.class, new SSCComponentArgument());
        CommandArguments.registerAdapter(net.minecraft.server.level.ServerLevel.class, new SSCDimensionArgument());
        CommandArguments.registerAdapter(net.minecraft.world.scores.DisplaySlot.class, new SSCDisplaySlotArgument());
        CommandArguments.registerAdapter(net.minecraft.world.level.GameType.class, new SSCGameModeArgument());
        CommandArguments.registerAdapter(net.minecraft.world.level.levelgen.Heightmap.Types.class, new SSCHeightmapTypeArgument());
        CommandArguments.registerAdapter(net.minecraft.world.level.block.Mirror.class, new SSCMirrorArgument());
        CommandArguments.registerAdapter(net.minecraft.commands.arguments.NbtPathArgument.NbtPath.class, new SSCNbtPathArgument());
        CommandArguments.registerAdapter(net.minecraft.world.level.block.Rotation.class, new SSCRotationTemplateArgument());
        CommandArguments.registerAdapter(net.minecraft.world.scores.ScoreHolder.class, new SSCScoreHolderArgument());
        CommandArguments.registerAdapter(net.minecraft.world.inventory.SlotRange.class, new SSCSlotRangeArgument());
        CommandArguments.registerAdapter(net.minecraft.network.chat.Style.class, new SSCStyleArgument());
        CommandArguments.registerAdapter(net.minecraft.world.scores.PlayerTeam.class, new SSCTeamArgument());
        CommandArguments.registerAdapter(java.util.UUID.class, new SSCUUIDArgument());
        CommandArguments.registerAdapter(net.minecraft.commands.arguments.AngleArgument.SingleAngle.class, new SSCAngleArgument());
        CommandArguments.registerAdapter(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.class, new SSCEntityAnchorArgument());
        CommandArguments.registerAdapter(net.minecraft.commands.arguments.blocks.BlockInput.class, new SSCBlockInputArgument());
        CommandArguments.registerAdapter(net.minecraft.core.BlockPos.class, new SSCBlockPosArgument());
        CommandArguments.registerAdapter(net.minecraft.server.level.ColumnPos.class, new SSCColumnPosArgument());
        CommandArguments.registerAdapter(net.minecraft.world.phys.Vec2.class, new SSCVec2Argument());
        CommandArguments.registerAdapter(net.minecraft.world.phys.Vec3.class, new SSCVec3Argument());
        CommandArguments.registerAdapter(net.minecraft.world.entity.Entity.class, new SSCEntityArgument());
        CommandArguments.registerAdapter(net.minecraft.world.entity.Entity[].class, new SSCEntitiesArgument());
        CommandArguments.registerAdapter(net.minecraft.server.level.ServerPlayer.class, new SSCPlayerArgument());
        CommandArguments.registerAdapter(net.minecraft.server.level.ServerPlayer[].class, new SSCPlayersArgument());
    }
}

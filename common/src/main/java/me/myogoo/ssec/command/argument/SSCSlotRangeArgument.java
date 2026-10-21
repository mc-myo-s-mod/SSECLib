package me.myogoo.ssec.command.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import me.myogoo.ssec.api.command.argument.SSCArgumentAdapter;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.SlotsArgument;
import net.minecraft.world.inventory.SlotRange;

public class SSCSlotRangeArgument implements SSCArgumentAdapter<SlotRange> {
    @Override
    public SlotRange value(CommandContext<CommandSourceStack> ctx, String name) {
        try {
            return SlotsArgument.getSlots(ctx, name);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ArgumentType<?> argumentType() {
        return SlotsArgument.slots();
    }
}

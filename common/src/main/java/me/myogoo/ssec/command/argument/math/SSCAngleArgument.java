package me.myogoo.ssec.command.argument.math;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.myogoo.ssec.api.command.argument.SSCArgumentAdapter;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.AngleArgument;
import java.math.BigDecimal;

public class SSCAngleArgument implements SSCArgumentAdapter<AngleArgument.SingleAngle> {
    @Override
    public AngleArgument.SingleAngle value(CommandContext<CommandSourceStack> ctx, String name) {
        float angle = AngleArgument.getAngle(ctx, name);
        // Brigadier's numeric reader does not consume exponent notation.
        String value = Float.toString(angle);
        if (value.indexOf('E') >= 0) {
            value = new BigDecimal(value).toPlainString();
        }
        try {
            return AngleArgument.angle().parse(new StringReader(value));
        } catch (CommandSyntaxException e) {
            throw new IllegalStateException("Could not reconstruct resolved command angle", e);
        }
    }

    @Override
    public ArgumentType<?> argumentType() {
        return AngleArgument.angle();
    }

    public interface AngleFactory {
        AngleArgument.SingleAngle makeInstance(float angle);
    }
}

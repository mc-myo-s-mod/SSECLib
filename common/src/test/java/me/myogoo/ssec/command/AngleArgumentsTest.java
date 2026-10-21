package me.myogoo.ssec.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.context.ParsedArgument;
import me.myogoo.ssec.command.argument.math.SSCAngleArgument;
import me.myogoo.ssec.command.argument.math.SSCAngleRelativeArgument;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.AngleArgument;
import net.minecraft.world.phys.Vec2;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AngleArgumentsTest {
    @Test
    void adaptersRetainResolvedAbsoluteAndRelativeContractsWithoutMixins() throws Exception {
        var source = mock(CommandSourceStack.class);
        when(source.getRotation()).thenReturn(new Vec2(0, 30));
        var other = mock(CommandSourceStack.class);
        when(other.getRotation()).thenReturn(new Vec2(0, 100));
        var dispatcher = new CommandDispatcher<CommandSourceStack>();
        var parsed = AngleArgument.angle().parse(new StringReader("~10"));
        var context = new CommandContextBuilder<>(dispatcher, source, dispatcher.getRoot(), 0)
                .withArgument("angle", new ParsedArgument<>(0, 3, parsed)).build("~10");
        assertEquals(40, new SSCAngleArgument().value(context, "angle").getAngle(other));
        assertEquals(140, new SSCAngleRelativeArgument().value(context, "angle").getAngle(other));
    }

    @Test
    void adaptersPreserveSmallAnglesThatFloatFormatsWithAnExponent() throws Exception {
        var source = mock(CommandSourceStack.class);
        when(source.getRotation()).thenReturn(new Vec2(0, 0));
        var dispatcher = new CommandDispatcher<CommandSourceStack>();
        var parsed = AngleArgument.angle().parse(new StringReader("0.0000001"));
        var context = new CommandContextBuilder<>(dispatcher, source, dispatcher.getRoot(), 0)
                .withArgument("angle", new ParsedArgument<>(0, 9, parsed)).build("0.0000001");
        assertEquals(0.0000001F, new SSCAngleArgument().value(context, "angle").getAngle(source));
        assertEquals(0.0000001F, new SSCAngleRelativeArgument().value(context, "angle").getAngle(source));
    }
}

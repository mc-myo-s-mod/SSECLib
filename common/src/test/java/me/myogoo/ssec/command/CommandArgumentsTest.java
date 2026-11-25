package me.myogoo.ssec.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.context.ParsedArgument;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import me.myogoo.ssec.api.command.argument.SSCArgumentAdapter;
import me.myogoo.ssec.api.command.permission.PermissionLevel;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class CommandArgumentsTest {
    @Test
    void primitiveAndBoxedAdaptersParseAndRejectMalformedInput() throws Exception {
        check(int.class, Integer.class, "42", 42, "oops");
        check(double.class, Double.class, "1.5", 1.5D, "oops");
        check(float.class, Float.class, "1.5", 1.5F, "oops");
        check(boolean.class, Boolean.class, "true", true, "maybe");
        check(String.class, String.class, "\"hello world\"", "hello world", "\"unterminated");
    }

    private static void check(Class<?> primitive, Class<?> boxed, String input, Object expected, String invalid) throws Exception {
        for (Class<?> type : new Class<?>[]{primitive, boxed}) {
            var adapter = CommandArguments.getAdapter(type);
            assertNotNull(adapter);
            var argument = adapter.argumentType();
            Object parsed = argument.parse(new StringReader(input));
            var source = mock(CommandSourceStack.class);
            var dispatcher = new CommandDispatcher<CommandSourceStack>();
            var context = new CommandContextBuilder<>(dispatcher, source, dispatcher.getRoot(), 0)
                    .withArgument("value", new ParsedArgument<>(0, input.length(), parsed)).build(input);
            assertEquals(expected, adapter.value(context, "value"));
            assertThrows(CommandSyntaxException.class, () -> argument.parse(new StringReader(invalid)));
        }
    }

    @Test
    void defaultMappingsAndContextInjection() throws Exception {
        assertNotNull(CommandArguments.getAdapter(UUID.class));
        assertNotNull(CommandArguments.getAdapter(BlockPos.class));
        assertNotNull(CommandArguments.getAdapter(Component.class));
        assertNull(CommandArguments.getAdapter(Unregistered.class));
        var source = mock(CommandSourceStack.class);
        var dispatcher = new CommandDispatcher<CommandSourceStack>();
        var context = new CommandContextBuilder<>(dispatcher, source, dispatcher.getRoot(), 0).build("");
        assertSame(context, CommandArguments.ParameterMapping.context(0, com.mojang.brigadier.context.CommandContext.class).value(context));
        assertSame(source, CommandArguments.ParameterMapping.context(1, CommandSourceStack.class).value(context));
        assertTrue(PermissionLevel.NONE.check(source));
    }

    @Test
    void wrappedSyntaxExceptionPreservesItsMessageAndCursor() {
        var reader = new StringReader("invalid input");
        reader.setCursor(7);
        var syntax = new SimpleCommandExceptionType(new LiteralMessage("invalid argument")).createWithContext(reader);
        var mapping = CommandArguments.ParameterMapping.argument(0, "value", (context, name) -> {
            throw new RuntimeException(syntax);
        });
        assertSame(syntax, assertThrows(CommandSyntaxException.class, () -> mapping.value(null)));
    }

    @Test
    void overridesAreVisibleAcrossThreads() throws Exception {
        SSCArgumentAdapter<Object> first = (context, name) -> "first";
        SSCArgumentAdapter<Object> second = (context, name) -> "second";
        CommandArguments.registerAdapter(OverrideType.class, first);
        var executor = Executors.newSingleThreadExecutor();
        try {
            assertSame(first, executor.submit(() -> CommandArguments.getAdapter(OverrideType.class)).get(5, TimeUnit.SECONDS));
            executor.submit(() -> CommandArguments.registerAdapter(OverrideType.class, second)).get(5, TimeUnit.SECONDS);
            assertSame(second, CommandArguments.getAdapter(OverrideType.class));
        } finally {
            executor.shutdownNow();
        }
    }

    private static final class Unregistered { }
    private static final class OverrideType { }
}



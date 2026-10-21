package me.myogoo.ssec.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.myogoo.ssec.api.SSECDebug;
import me.myogoo.ssec.api.command.SSCAlias;
import me.myogoo.ssec.api.command.SSCArgument;
import me.myogoo.ssec.api.command.SSCExecute;
import me.myogoo.ssec.api.command.SSCommand;
import me.myogoo.ssec.api.command.SSCPermission;
import me.myogoo.ssec.api.command.argument.SSCArgumentAdapter;
import me.myogoo.ssec.api.command.permission.PermissionLevel;
import me.myogoo.ssec.api.command.permission.SSCPermissionChecker;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class CommandRegistrarTest {
    private static final CommandSourceStack ALLOWED = mock(CommandSourceStack.class);
    private static final CommandSourceStack DENIED = mock(CommandSourceStack.class);

    @Test
    void pathsAliasesAndContextInjectionExecute() throws Exception {
        var dispatcher = register(Paths.class, true);
        assertEquals(9, dispatcher.execute("paths set value 9", ALLOWED));
        assertEquals(7, dispatcher.execute("short", ALLOWED));
        assertEquals(7, dispatcher.execute("paths leafAlias", ALLOWED));
        assertEquals(7, dispatcher.execute("rootAlias leaf", ALLOWED));
    }

    @Test
    void absoluteAliasesRetainPropagatedAncestorPermissions() throws Exception {
        var dispatcher = register(Secured.class, true);
        assertFalse(dispatcher.getRoot().getChild("secured").canUse(DENIED));
        assertFalse(dispatcher.getRoot().getChild("secureShortcut").canUse(DENIED));
        assertTrue(dispatcher.getRoot().getChild("secureShortcut").canUse(ALLOWED));
        assertThrows(CommandSyntaxException.class, () -> dispatcher.execute("secureShortcut", DENIED));
        assertEquals(7, dispatcher.execute("secureShortcut", ALLOWED));
    }

    @Test
    void localPermissionsOnlyGuardExecutionAndMethodPropagationCombinesRequirements() throws Exception {
        var dispatcher = register(Local.class, true);
        assertTrue(dispatcher.getRoot().getChild("local").canUse(DENIED));
        assertEquals(0, dispatcher.execute("local", DENIED));
        assertEquals(7, dispatcher.execute("local child", DENIED));
        assertEquals(0, dispatcher.execute("localAlias", DENIED));
        assertEquals(7, dispatcher.execute("localAlias child", DENIED));
        var combined = register(Combined.class, true);
        assertFalse(combined.getRoot().getChild("combined").canUse(DENIED));
        assertTrue(combined.getRoot().getChild("combined").canUse(ALLOWED));
        assertEquals(7, combined.execute("combined", ALLOWED));
    }

    @Test
    void invalidPermissionsAndMalformedMethodsFailClosed() throws Exception {
        var dispatcher = new CommandDispatcher<CommandSourceStack>();
        CommandRegistrar.registerAll(dispatcher, null,
                List.of(EmptyPermission.class, ConflictingPermission.class, BrokenChecker.class,
                        NamedPermission.class, Malformed.class), true);
        assertNull(dispatcher.getRoot().getChild("empty"));
        assertNull(dispatcher.getRoot().getChild("emptyAlias"));
        assertNull(dispatcher.getRoot().getChild("conflict"));
        assertNull(dispatcher.getRoot().getChild("brokenChecker"));
        assertNull(dispatcher.getRoot().getChild("named"));
        var malformed = dispatcher.getRoot().getChild("malformed");
        assertNull(malformed.getChild("missing"));
        assertNull(malformed.getChild("unsupported"));
        assertNull(malformed.getChild("duplicate"));
        assertNull(malformed.getChild("instance"));
        assertNull(malformed.getChild("invalidPermission"));
        assertEquals(7, dispatcher.execute("malformed valid", ALLOWED));
    }

    @Test
    void nestedDebugCommandsAreFilteredAndRegistrationsCanBeRebuilt() throws Exception {
        var production = register(DebugRoot.class, false);
        assertNull(production.getRoot().getChild("debugRoot").getChild("debug"));
        assertNull(production.getRoot().getChild("debugShortcut"));
        var development = register(DebugRoot.class, true);
        assertEquals(7, development.execute("debugRoot debug", ALLOWED));
        assertEquals(7, development.execute("debugShortcut", ALLOWED));
        assertNull(register(DebugRoot.class, false).getRoot().getChild("debugShortcut"));
    }

    @Test
    void aliasCollisionCannotOverwriteAnExistingUnrestrictedCommand() throws Exception {
        var dispatcher = new CommandDispatcher<CommandSourceStack>();
        dispatcher.register(LiteralArgumentBuilder.<CommandSourceStack>literal("secureShortcut").executes(context -> 42));
        CommandRegistrar.register(dispatcher, Secured.class);
        assertEquals(42, dispatcher.execute("secureShortcut", DENIED));
    }

    @Test
    void aliasesCannotCreateUnrestrictedPlaceholdersForLaterRoots() throws Exception {
        var dispatcher = new CommandDispatcher<CommandSourceStack>();
        CommandRegistrar.registerAll(dispatcher, null, List.of(EarlyAlias.class, Secured.class), true);
        assertFalse(dispatcher.getRoot().getChild("secured").canUse(DENIED));
        assertThrows(CommandSyntaxException.class, () -> dispatcher.execute("secured extra", DENIED));
        assertEquals(7, dispatcher.execute("secured extra", ALLOWED));
    }

    @Test
    void aliasPlacedUnderItsTargetCannotCreateACyclicCommandTree() throws Exception {
        var dispatcher = register(DescendantAlias.class, true);
        assertEquals(7, dispatcher.execute("descendant child again child", ALLOWED));
        assertDoesNotThrow(() -> dispatcher.getAllUsage(dispatcher.getRoot(), ALLOWED, false));
    }

    @Test
    void adapterOverwriteContextAndCheckedExceptionsArePreserved() throws Exception {
        var context = mock(CommandBuildContext.class);
        var firstAdapter = adapter(context, 11);
        CommandRegistrar.registerAdapter(Token.class, firstAdapter);
        var first = new CommandDispatcher<CommandSourceStack>();
        CommandRegistrar.register(first, context, AdapterCommand.class);
        CommandRegistrar.registerAdapter(Token.class, adapter(context, 22));
        var second = new CommandDispatcher<CommandSourceStack>();
        CommandRegistrar.register(second, context, AdapterCommand.class);
        assertEquals(11, first.execute("adapter token", ALLOWED));
        assertEquals(22, second.execute("adapter token", ALLOWED));
        assertEquals(0, second.execute("adapter fail", ALLOWED));
    }

    private static SSCArgumentAdapter<Token> adapter(CommandBuildContext expected, int result) {
        return new SSCArgumentAdapter<>() {
            @Override
            public ArgumentType<?> argumentType(CommandBuildContext context) {
                assertSame(expected, context);
                return StringArgumentType.word();
            }

            @Override
            public Token value(CommandContext<CommandSourceStack> context, String name) throws Exception {
                if (StringArgumentType.getString(context, name).equals("fail")) {
                    throw new Exception("expected adapter failure");
                }
                return new Token(result);
            }
        };
    }

    private static CommandDispatcher<CommandSourceStack> register(Class<?> clazz, boolean developmentMode) {
        var dispatcher = new CommandDispatcher<CommandSourceStack>();
        CommandRegistrar.registerAll(dispatcher, null, List.of(clazz), developmentMode);
        return dispatcher;
    }

    public static class AllowedChecker implements SSCPermissionChecker {
        public boolean check(CommandSourceStack source) { return source == ALLOWED; }
    }

    public static class AlwaysChecker implements SSCPermissionChecker {
        public boolean check(CommandSourceStack source) { return true; }
    }

    public static class NoConstructorChecker implements SSCPermissionChecker {
        private NoConstructorChecker() { }
        public boolean check(CommandSourceStack source) { return true; }
    }

    @SSCommand("earlyAlias") @SSCAlias("/secured/extra")
    static class EarlyAlias { @SSCExecute static int execute() { return 7; } }

    @SSCommand("descendant") @SSCAlias("/descendant/child/again")
    static class DescendantAlias {
        @SSCommand(value = "child", parent = DescendantAlias.class)
        static class Child { @SSCExecute static int execute() { return 7; } }
    }

    @SSCommand("paths") @SSCAlias("rootAlias")
    static class Paths {
        @SSCExecute("set/value")
        static int execute(CommandContext<CommandSourceStack> context, CommandSourceStack source,
                           @SSCArgument("number") int number) {
            assertSame(source, context.getSource());
            return number;
        }
        @SSCommand(value = "leaf", parent = Paths.class) @SSCAlias({"/short", "leafAlias"})
        static class Leaf { @SSCExecute static int execute() { return 7; } }
    }

    @SSCommand("secured") @SSCPermission(custom = AllowedChecker.class, propagate = true)
    static class Secured {
        @SSCommand(value = "child", parent = Secured.class) @SSCAlias("/secureShortcut")
        static class Child { @SSCExecute static int execute() { return 7; } }
    }

    @SSCommand("local") @SSCAlias("localAlias") @SSCPermission(custom = AllowedChecker.class)
    static class Local {
        @SSCExecute static int execute() { return 7; }
        @SSCommand(value = "child", parent = Local.class)
        static class Child { @SSCExecute static int execute() { return 7; } }
    }

    @SSCommand("combined") @SSCPermission(custom = AllowedChecker.class, propagate = true)
    static class Combined {
        @SSCExecute @SSCPermission(custom = AlwaysChecker.class, propagate = true)
        static int execute() { return 7; }
    }

    @SSCommand("empty") @SSCAlias("emptyAlias") @SSCPermission
    static class EmptyPermission { }
    @SSCommand("conflict") @SSCPermission(permission = PermissionLevel.ADMIN, custom = AllowedChecker.class)
    static class ConflictingPermission { }
    @SSCommand("brokenChecker") @SSCPermission(custom = NoConstructorChecker.class)
    static class BrokenChecker { }
    @SSCommand("named") @SSCPermission("ssec.test")
    static class NamedPermission { }

    @SSCommand("malformed")
    static class Malformed {
        @SSCExecute("missing") static int missing(int number) { return number; }
        @SSCExecute("unsupported") static int unsupported(@SSCArgument("value") Object value) { return 1; }
        @SSCExecute("duplicate")
        static int duplicate(@SSCArgument("same") int first, @SSCArgument("same") int second) { return 1; }
        @SSCExecute("instance") int instance() { return 1; }
        @SSCExecute("invalidPermission") @SSCPermission static int invalidPermission() { return 1; }
        @SSCExecute("valid") static int valid() { return 7; }
    }

    @SSCommand("debugRoot")
    static class DebugRoot {
        @SSCommand(value = "debug", parent = DebugRoot.class) @SSECDebug @SSCAlias("/debugShortcut")
        static class Debug { @SSCExecute static int execute() { return 7; } }
    }

    record Token(int result) { }
    @SSCommand("adapter")
    static class AdapterCommand {
        @SSCExecute static int execute(@SSCArgument("value") Token value) { return value.result(); }
    }
}

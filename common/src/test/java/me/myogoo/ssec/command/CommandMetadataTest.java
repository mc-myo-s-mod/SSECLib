package me.myogoo.ssec.command;

import me.myogoo.ssec.api.SSECDebug;
import me.myogoo.ssec.api.command.SSCAlias;
import me.myogoo.ssec.api.command.SSCArgument;
import me.myogoo.ssec.api.command.SSCExecute;
import me.myogoo.ssec.api.command.SSCommand;
import me.myogoo.ssec.api.command.SSCPermission;
import me.myogoo.ssec.api.command.permission.PermissionLevel;
import me.myogoo.ssec.api.command.permission.SSCPermissionChecker;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CommandMetadataTest {
    @Test
    void resolvesNativeAnnotationsAndPaths() throws Exception {
        assertEquals(new CommandMetadata.CommandInfo("root", void.class), CommandMetadata.getCommandInfo(Root.class));
        assertArrayEquals(new String[]{"/shortcut"}, CommandMetadata.getAliasInfo(Root.class).values());
        assertTrue(CommandMetadata.isDebugOnly(Root.class));
        var method = Root.class.getDeclaredMethod("execute", int.class);
        assertEquals(new CommandMetadata.ExecuteInfo("set/value"), CommandMetadata.getExecuteInfo(method));
        assertEquals("number", CommandMetadata.getArgumentName(method.getParameters()[0]));
        var permission = CommandMetadata.getPermissionInfo(Root.class);
        assertEquals(PermissionLevel.ADMIN, permission.level());
        assertEquals(SSCPermissionChecker.class, permission.customChecker());
        assertTrue(permission.propagate());
        assertArrayEquals(new String[0], permission.nodes());
        assertArrayEquals(new String[]{"ssec.node"}, CommandMetadata.getPermissionInfo(method).nodes());
        assertEquals(List.of("a", "b", "c"), CommandPaths.split(" /a b//c "));
        assertEquals(List.of(), CommandPaths.split(null));
        assertEquals(List.of("root", "child"), CommandPaths.resolveCommandPath(Child.class, Root.class));
        assertNull(CommandPaths.resolveCommandPath(Root.class, Child.class));
        assertEquals(List.of("global", "run"), CommandPaths.resolveAlias("/global run", List.of("root")));
        assertEquals(List.of("root", "run"), CommandPaths.resolveAlias("run", List.of("root")));
        assertEquals(List.of(), CommandPaths.resolveAlias(" ", List.of("root")));
        assertNull(CommandMetadata.getCommandInfo(String.class));
    }

    @SSCommand("root")
    @SSCAlias("/shortcut")
    @SSCPermission(permission = PermissionLevel.ADMIN, propagate = true)
    @SSECDebug
    static class Root {
        @SSCExecute("set/value")
        @SSCPermission("ssec.node")
        static int execute(@SSCArgument("number") int number) {
            return number;
        }
    }

    @SSCommand(value = "child", parent = Root.class)
    static class Child {
    }
}

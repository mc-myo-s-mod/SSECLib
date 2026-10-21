package me.myogoo.ssec.command;

import me.myogoo.ssec.api.command.SSCommand;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CommandPathsTest {
    @Test
    void splitHandlesNullBlankSlashAndWhitespaceSeparatedPaths() {
        assertEquals(List.of(), CommandPaths.split(null));
        assertEquals(List.of(), CommandPaths.split("  "));
        assertEquals(List.of("alpha", "beta", "gamma"),
                CommandPaths.split(" /alpha beta//gamma "));
    }

    @Test
    void resolveAliasUsesAbsolutePathWhenAliasStartsWithSlash() {
        assertEquals(List.of("global", "reload"),
                CommandPaths.resolveAlias("/global reload", List.of("myotus", "config")));
    }

    @Test
    void resolveAliasAppendsRelativePathToParentPath() {
        assertEquals(List.of("myotus", "config", "set", "value"),
                CommandPaths.resolveAlias("set value", List.of("myotus", "config")));
    }

    @Test
    void resolveAliasReturnsEmptyPathForBlankAlias() {
        assertEquals(List.of(), CommandPaths.resolveAlias(null, List.of("myotus")));
        assertEquals(List.of(), CommandPaths.resolveAlias(" ", List.of("myotus")));
    }

    @Test
    void resolveCommandPathWalksAnnotatedParentChain() {
        assertEquals(List.of("root", "child", "leaf"),
                CommandPaths.resolveCommandPath(LeafCommand.class, RootCommand.class));
    }

    @Test
    void resolveCommandPathReturnsNullForBrokenOrMismatchedChains() {
        assertNull(CommandPaths.resolveCommandPath(BrokenCommand.class, RootCommand.class));
        assertNull(CommandPaths.resolveCommandPath(PlainCommand.class, RootCommand.class));
        assertNull(CommandPaths.resolveCommandPath(ChildCommand.class, LeafCommand.class));
    }

    @SSCommand("root")
    private static final class RootCommand {
    }

    @SSCommand(value = "child", parent = RootCommand.class)
    private static final class ChildCommand {
    }

    @SSCommand(value = "leaf", parent = ChildCommand.class)
    private static final class LeafCommand {
    }

    @SSCommand(value = "broken", parent = PlainCommand.class)
    private static final class BrokenCommand {
    }

    private static final class PlainCommand {
    }
}

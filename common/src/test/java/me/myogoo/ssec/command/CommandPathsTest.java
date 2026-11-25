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
                CommandPaths.resolveAlias("/global reload", List.of("example", "config")));
    }

    @Test
    void resolveAliasAppendsRelativePathToParentPath() {
        assertEquals(List.of("example", "config", "set", "value"),
                CommandPaths.resolveAlias("set value", List.of("example", "config")));
    }

    @Test
    void resolveAliasReturnsEmptyPathForBlankAlias() {
        assertEquals(List.of(), CommandPaths.resolveAlias(null, List.of("example")));
        assertEquals(List.of(), CommandPaths.resolveAlias(" ", List.of("example")));
        assertEquals(List.of(), CommandPaths.resolveAlias(" /// ", List.of("example")));
        assertEquals(List.of("other"), CommandPaths.resolveAlias(" /other", List.of("example")));
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
        assertNull(CommandPaths.resolveCommandPath(CyclicCommand.class, RootCommand.class));
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

    @SSCommand(value = "cyclic", parent = CyclicCommand.class)
    private static final class CyclicCommand {
    }
}

package me.myogoo.ssec.command;

import me.myogoo.ssec.command.CommandMetadata.CommandInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

final class CommandPaths {
    private CommandPaths() {
    }

    static List<String> split(String path) {
        List<String> result = new ArrayList<>();
        if (path == null || path.isBlank()) {
            return result;
        }
        for (String part : path.trim().split("[/\\s]+")) {
            if (!part.isBlank()) {
                result.add(part);
            }
        }
        return result;
    }

    static List<String> resolveAlias(String alias, List<String> parentPath) {
        if (alias == null || alias.isBlank()) {
            return List.of();
        }
        if (alias.startsWith("/")) {
            return split(alias.substring(1));
        }

        StringJoiner joiner = new StringJoiner("/");
        for (String segment : parentPath) {
            joiner.add(segment);
        }
        joiner.add(alias);
        return split(joiner.toString());
    }

    static List<String> resolveCommandPath(Class<?> childClass, Class<?> rootClass) {
        List<String> path = new ArrayList<>();
        Class<?> current = childClass;
        int maxDepth = 32;
        while (current != null && maxDepth-- > 0) {
            CommandInfo command = CommandMetadata.getCommandInfo(current);
            if (command == null) {
                return null;
            }
            path.add(0, command.value());
            if (current == rootClass) {
                return path;
            }
            current = command.parent();
            if (current == void.class) {
                return null;
            }
        }
        return null;
    }
}

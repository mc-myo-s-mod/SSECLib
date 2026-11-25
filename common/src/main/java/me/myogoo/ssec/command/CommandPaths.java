package me.myogoo.ssec.command;

import me.myogoo.ssec.command.CommandMetadata.CommandInfo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        List<String> segments = split(alias);
        if (segments.isEmpty() || alias.stripLeading().startsWith("/")) {
            return segments;
        }
        List<String> path = new ArrayList<>(parentPath);
        path.addAll(segments);
        return path;
    }

    static List<String> resolveCommandPath(Class<?> childClass, Class<?> rootClass) {
        List<String> path = new ArrayList<>();
        Class<?> current = childClass;
        Set<Class<?>> visited = new HashSet<>();
        while (current != null && visited.add(current)) {
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

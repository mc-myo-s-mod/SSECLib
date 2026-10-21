package me.myogoo.ssec.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import me.myogoo.ssec.command.CommandMetadata.AliasInfo;
import net.minecraft.commands.CommandSourceStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

final class CommandAliasRegistrar {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandAliasRegistrar.class);

    private CommandAliasRegistrar() {
    }

    static void registerAliases(CommandDispatcher<CommandSourceStack> dispatcher, Class<?> rootClass,
                                CommandNode<CommandSourceStack> rootNode, Set<Class<?>> builtClasses) {
        for (Class<?> clazz : builtClasses) {
            try {
                AliasInfo aliases = CommandMetadata.getAliasInfo(clazz);
                if (aliases == null) {
                    continue;
                }
                List<String> path = CommandPaths.resolveCommandPath(clazz, rootClass);
                if (path == null || path.isEmpty()) {
                    continue;
                }
                CommandNode<CommandSourceStack> target = clazz == rootClass ? rootNode : findNode(dispatcher, path);
                if (target == null) {
                    continue;
                }
                Predicate<CommandSourceStack> ancestors = ancestorRequirement(dispatcher, path);
                for (String alias : aliases.values()) {
                    registerAlias(dispatcher, alias, target, path.subList(0, path.size() - 1), ancestors);
                }
            } catch (RuntimeException | LinkageError e) {
                LOGGER.error("Could not register SSEC aliases for {}", clazz.getName(), e);
            }
        }
    }

    private static void registerAlias(CommandDispatcher<CommandSourceStack> dispatcher, String alias,
                                      CommandNode<CommandSourceStack> target, List<String> parentPath,
                                      Predicate<CommandSourceStack> ancestors) {
        List<String> segments = CommandPaths.resolveAlias(alias, parentPath);
        if (segments.isEmpty()) {
            return;
        }
        // Brigadier merges commands but retains the old requirement, which could bypass alias permissions.
        if (findNode(dispatcher, segments) != null) {
            LOGGER.warn("Skipping SSEC alias '{}' because that command path already exists.", alias);
            return;
        }
        LiteralArgumentBuilder<CommandSourceStack> current = LiteralArgumentBuilder.<CommandSourceStack>literal(segments.get(segments.size() - 1));
        current.requires(ancestors.and(target.getRequirement()));
        if (target.getCommand() != null) {
            current.executes(target.getCommand());
        }
        for (CommandNode<CommandSourceStack> child : target.getChildren()) {
            current.then(copyNode(child));
        }
        for (int i = segments.size() - 2; i >= 1; i--) {
            LiteralArgumentBuilder<CommandSourceStack> parent = LiteralArgumentBuilder.<CommandSourceStack>literal(segments.get(i));
            parent.then(current);
            current = parent;
        }
        if (segments.size() == 1) {
            dispatcher.register(current);
            return;
        }
        CommandNode<CommandSourceStack> root = dispatcher.getRoot().getChild(segments.get(0));
        if (root != null) {
            root.addChild(current.build());
        } else {
            LiteralArgumentBuilder<CommandSourceStack> newRoot = LiteralArgumentBuilder.<CommandSourceStack>literal(segments.get(0));
            newRoot.then(current);
            dispatcher.register(newRoot);
        }
    }

    private static CommandNode<CommandSourceStack> copyNode(CommandNode<CommandSourceStack> node) {
        ArgumentBuilder<CommandSourceStack, ?> copy = node.createBuilder();
        for (CommandNode<CommandSourceStack> child : node.getChildren()) {
            copy.then(copyNode(child));
        }
        return copy.build();
    }

    private static Predicate<CommandSourceStack> ancestorRequirement(
            CommandDispatcher<CommandSourceStack> dispatcher, List<String> path) {
        Predicate<CommandSourceStack> combined = source -> true;
        CommandNode<CommandSourceStack> node = dispatcher.getRoot();
        for (int i = 0; i < path.size() - 1; i++) {
            node = node.getChild(path.get(i));
            if (node == null) {
                return source -> false;
            }
            combined = combined.and(node.getRequirement());
        }
        return combined;
    }

    private static CommandNode<CommandSourceStack> findNode(CommandDispatcher<CommandSourceStack> dispatcher,
                                                           List<String> path) {
        CommandNode<CommandSourceStack> node = dispatcher.getRoot();
        for (String segment : path) {
            node = node.getChild(segment);
            if (node == null) {
                return null;
            }
        }
        return node;
    }
}

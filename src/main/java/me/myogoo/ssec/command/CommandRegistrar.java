package me.myogoo.ssec.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import me.myogoo.ssec.api.command.SSCExecute;
import me.myogoo.ssec.api.command.SSCPermission;
import me.myogoo.ssec.api.command.SSCommand;
import me.myogoo.ssec.api.command.SSCArgument;
import me.myogoo.ssec.api.command.SSCAlias;
import me.myogoo.ssec.api.command.permission.SSCPermissionChecker;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.minecraft.commands.arguments.EntityArgument;

import me.myogoo.ssec.api.command.argument.SSCArgumentAdapter;
import me.myogoo.ssec.api.command.permission.PermissionLevel;
import me.myogoo.ssec.command.argument.SSCIntArgument;
import me.myogoo.ssec.command.argument.SSCDoubleArgument;
import me.myogoo.ssec.command.argument.SSCFloatArgument;
import me.myogoo.ssec.command.argument.SSCBooleanArgument;
import me.myogoo.ssec.command.argument.SSCStringArgument;
import me.myogoo.ssec.command.argument.math.SSCVec3Argument;
import me.myogoo.ssec.command.argument.entity.SSCEntitiesArgument;
import me.myogoo.ssec.command.argument.entity.SSCEntityArgument;
import me.myogoo.ssec.command.argument.entity.SSCPlayerArgument;
import me.myogoo.ssec.command.argument.entity.SSCPlayersArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permission;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommandRegistrar {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandRegistrar.class);

    private static final Map<Class<?>, SSCArgumentAdapter<?>> ADAPTER_REGISTRY = new HashMap<>();

    static {
        registerAdapter(int.class, new SSCIntArgument());
        registerAdapter(Integer.class, new SSCIntArgument());
        registerAdapter(double.class, new SSCDoubleArgument());
        registerAdapter(Double.class, new SSCDoubleArgument());
        registerAdapter(float.class, new SSCFloatArgument());
        registerAdapter(Float.class, new SSCFloatArgument());
        registerAdapter(boolean.class, new SSCBooleanArgument());
        registerAdapter(Boolean.class, new SSCBooleanArgument());
        registerAdapter(String.class, new SSCStringArgument());
        registerAdapter(Vec3.class, new SSCVec3Argument());
        registerAdapter(Entity.class, new SSCEntityArgument());
        registerAdapter(Entity[].class, new SSCEntitiesArgument());
        registerAdapter(ServerPlayer.class, new SSCPlayerArgument());
        registerAdapter(ServerPlayer[].class, new SSCPlayersArgument());
    }

    /**
     * Registers a custom ArgumentAdapter mapping for a specific Java class.
     * 
     * @param clazz   The Java class
     * @param adapter The adapter instance
     */
    public static void registerAdapter(Class<?> clazz, SSCArgumentAdapter<?> adapter) {
        ADAPTER_REGISTRY.put(clazz, adapter);
    }

    /**
     * Registers a command class to the dispatcher (legacy: inner-class only).
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, Class<?> clazz) {
        register(dispatcher, clazz, List.of());
    }

    /**
     * Registers a command class to the dispatcher, with all scanned command classes
     * available for cross-file parent resolution.
     *
     * @param dispatcher        The CommandDispatcher
     * @param clazz             The root class annotated with @SSCommand
     * @param allCommandClasses All classes annotated with @SSCommand (for
     *                          cross-file child lookup)
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, Class<?> clazz,
            List<Class<?>> allCommandClasses) {
        LOGGER.debug("Attempting to register class: {}", clazz.getName());
        if (!clazz.isAnnotationPresent(SSCommand.class)) {
            LOGGER.debug("Class {} missing @SSCommand", clazz.getName());
            return;
        }

        LiteralArgumentBuilder<CommandSourceStack> builder = buildCommandNode(clazz, allCommandClasses);
        if (builder != null) {
            LiteralCommandNode<CommandSourceStack> registeredNode = dispatcher.register(builder);
            LOGGER.info("Successfully registered root node: {}", registeredNode.getName());

            // Handle root aliases
            if (clazz.isAnnotationPresent(SSCAlias.class)) {
                // Pass permission information to apply to alias
                SSCPermission aliasPerm = clazz.isAnnotationPresent(SSCPermission.class)
                        ? clazz.getAnnotation(SSCPermission.class)
                        : null;
                SSCAlias aliasAnnotation = clazz.getAnnotation(SSCAlias.class);
                for (String alias : aliasAnnotation.value()) {
                    registerRootAlias(dispatcher, alias, registeredNode, aliasPerm);
                }
            }

            // Handle subcommand aliases (inner + external classes)
            registerSubcommandAliases(dispatcher, clazz, allCommandClasses);

        } else {
            LOGGER.warn("Builder was null for class: {}", clazz.getName());
        }
    }

    /**
     * Iterates all subcommand classes and if @SSCAlias is present,
     * traces the full path to find the node in the dispatcher and registers the
     * alias.
     *
     * Alias path rules:
     * - Starts with "/" → absolute path (from root)
     * - Does not start with "/" → relative path (parent path prefix auto-prepended)
     */
    private static void registerSubcommandAliases(CommandDispatcher<CommandSourceStack> dispatcher,
            Class<?> rootClass, List<Class<?>> allCommandClasses) {
        // Collect all command classes under the root class (inner + external)
        List<Class<?>> allChildren = new ArrayList<>();
        collectAllChildren(rootClass, allCommandClasses, allChildren);

        for (Class<?> childClass : allChildren) {
            if (!childClass.isAnnotationPresent(SSCAlias.class))
                continue;

            // Trace the full path: childClass → parent → ... → rootClass
            List<String> path = resolveCommandPath(childClass, rootClass);
            if (path == null || path.isEmpty()) {
                LOGGER.warn("Could not resolve command path for alias on: {}", childClass.getName());
                continue;
            }

            // Find node from dispatcher
            CommandNode<CommandSourceStack> targetNode = findNode(dispatcher, path);
            if (targetNode == null) {
                LOGGER.warn("Could not find dispatcher node for path: {}", path);
                continue;
            }

            // Build the parent path prefix (all segments except the last = the subcommand
            // itself)
            List<String> parentPath = path.subList(0, path.size() - 1);

            SSCAlias aliasAnnotation = childClass.getAnnotation(SSCAlias.class);
            for (String alias : aliasAnnotation.value()) {
                LOGGER.info("[SSEC] Processing alias '{}' for class {} (path={}, parentPath={})",
                        alias, childClass.getSimpleName(), path, parentPath);
                registerSubcommandAlias(dispatcher, alias, targetNode, parentPath);
            }
        }
    }

    /**
     * Recursively collects all command classes under the rootClass.
     */
    private static void collectAllChildren(Class<?> parentClass, List<Class<?>> allCommandClasses,
            List<Class<?>> result) {
        // inner classes
        for (Class<?> inner : parentClass.getDeclaredClasses()) {
            if (inner.isAnnotationPresent(SSCommand.class)) {
                SSCommand ann = inner.getAnnotation(SSCommand.class);
                if (ann.parent() == parentClass) {
                    result.add(inner);
                    collectAllChildren(inner, allCommandClasses, result);
                }
            }
        }
        // external classes
        for (Class<?> candidate : allCommandClasses) {
            if (candidate.isAnnotationPresent(SSCommand.class)) {
                SSCommand ann = candidate.getAnnotation(SSCommand.class);
                if (ann.parent() == parentClass && candidate.getDeclaringClass() != parentClass) {
                    result.add(candidate);
                    collectAllChildren(candidate, allCommandClasses, result);
                }
            }
        }
    }

    /**
     * Traces the command path from childClass to rootClass.
     * Example: SubSubCommand("number") → SubCommand("say") → TestCommand("ssec")
     * Returns: ["ssec", "say", "number"]
     */
    private static List<String> resolveCommandPath(Class<?> childClass, Class<?> rootClass) {
        List<String> path = new ArrayList<>();
        Class<?> current = childClass;
        int maxDepth = 20; // Prevent infinite loop
        while (current != null && maxDepth-- > 0) {
            SSCommand ann = current.getAnnotation(SSCommand.class);
            if (ann == null)
                return null;
            path.add(0, ann.value());
            if (current == rootClass)
                break;
            current = ann.parent();
            if (current == void.class)
                return null; // Failed to reach root
        }
        return path;
    }

    /**
     * Finds the command node for the given path from the dispatcher.
     */
    private static CommandNode<CommandSourceStack> findNode(CommandDispatcher<CommandSourceStack> dispatcher,
            List<String> path) {
        CommandNode<CommandSourceStack> node = dispatcher.getRoot();
        for (String segment : path) {
            node = node.getChild(segment);
            if (node == null)
                return null;
        }
        return node;
    }

    /**
     * Register root-level alias (redirect approach).
     * Handles "/" path separator.
     * - Starts with "/" → absolute path (leading "/" stripped before processing)
     * - Otherwise → simple alias
     *
     * Applies the same permission of the original command to the alias.
     */
    private static void registerRootAlias(CommandDispatcher<CommandSourceStack> dispatcher,
            String alias, CommandNode<CommandSourceStack> targetNode, SSCPermission perm) {
        // Resolve absolute path: strip leading "/"
        String resolved = alias.startsWith("/") ? alias.substring(1) : alias;

        if (!resolved.contains("/")) {
            // Simple single-segment alias
            LiteralArgumentBuilder<CommandSourceStack> aliasBuilder = Commands.literal(resolved).redirect(targetNode);
            applyPermissionToBuilder(aliasBuilder, perm);
            dispatcher.register(aliasBuilder);
            LOGGER.info("Registered alias: /{} → {}", resolved, targetNode.getName());
            return;
        }

        // Multi-segment alias (e.g. "/newroot/sub" → parts = ["newroot", "sub"])
        String[] parts = resolved.split("/");

        if (alias.startsWith("/")) {
            // Absolute path: the root part must already exist in the dispatcher
            CommandNode<CommandSourceStack> existingRoot = dispatcher.getRoot().getChild(parts[0]);
            if (existingRoot == null) {
                throw new IllegalStateException(
                        String.format("[SSEC] Failed to register alias '/%s': root command '/%s' is not registered! " +
                                "Please register the @SSCommand(\"%s\") root command first.",
                                resolved, parts[0], parts[0]));
            }

            // Build nested chain and add to existing root
            LiteralArgumentBuilder<CommandSourceStack> innermost = Commands.literal(parts[parts.length - 1])
                    .redirect(targetNode);
            for (int i = parts.length - 2; i >= 1; i--) {
                LiteralArgumentBuilder<CommandSourceStack> wrapper = Commands.literal(parts[i]);
                wrapper.then(innermost);
                innermost = wrapper;
            }
            existingRoot.addChild(innermost.build());
            LOGGER.info("Registered absolute alias: /{} → {} (added to existing /{})",
                    resolved, targetNode.getName(), parts[0]);
        } else {
            // Non-absolute multi-segment: register as a new root tree
            LiteralArgumentBuilder<CommandSourceStack> innermost = Commands.literal(parts[parts.length - 1])
                    .redirect(targetNode);
            for (int i = parts.length - 2; i >= 1; i--) {
                LiteralArgumentBuilder<CommandSourceStack> wrapper = Commands.literal(parts[i]);
                wrapper.then(innermost);
                innermost = wrapper;
            }
            LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(parts[0]);
            root.then(innermost);
            applyPermissionToBuilder(root, perm);
            dispatcher.register(root);
            LOGGER.info("Registered multi-segment alias: /{} → {}", resolved, targetNode.getName());
        }
    }

    /**
     * Configures requires() based on SSCPermission for the LiteralArgumentBuilder.
     */
    private static void applyPermissionToBuilder(LiteralArgumentBuilder<CommandSourceStack> builder,
            SSCPermission perm) {
        if (perm == null || !validatePermission(perm, "alias"))
            return;

        if (perm.permission() != PermissionLevel.NONE) {
            final Permission requiredLevel = perm.permission().getPermission();
            builder.requires(source -> {
                try {
                    return (boolean) source.permissions().hasPermission(requiredLevel);
                } catch (Exception e) {
                    return false;
                }
            });
        } else {
            builder.requires(source -> checkPermission(source, perm));
        }
    }

    /**
     * Register Subcommand Alias (redirect approach).
     *
     * Path rules:
     * - Starts with "/" → absolute path (from root)
     * - Otherwise → relative path (parent command path prefix auto-prepended)
     *
     * @param parentPath the command path segments of the parent (e.g. ["ssec",
     *                   "say"])
     */
    private static void registerSubcommandAlias(CommandDispatcher<CommandSourceStack> dispatcher,
            String alias, CommandNode<CommandSourceStack> targetNode, List<String> parentPath) {

        String resolved;
        boolean isAbsolute = alias.startsWith("/");

        if (isAbsolute) {
            // Absolute path: strip leading "/" and use as-is
            resolved = alias.substring(1);
        } else {
            // Relative path: prepend parent path
            StringJoiner joiner = new StringJoiner("/");
            for (String seg : parentPath) {
                joiner.add(seg);
            }
            joiner.add(alias);
            resolved = joiner.toString();
        }

        String[] parts = resolved.split("/");

        LOGGER.info("[SSEC] registerSubcommandAlias: alias='{}', isAbsolute={}, resolved='{}', parts={}",
                alias, isAbsolute, resolved, java.util.Arrays.toString(parts));

        if (parts.length < 2) {
            // Single segment: register as a top-level redirect
            dispatcher.register(Commands.literal(parts[0]).redirect(targetNode));
            LOGGER.info("Registered subcommand alias: /{} -> {}", parts[0], targetNode.getName());
            return;
        }

        // Multi-segment: the root command must already exist
        CommandNode<CommandSourceStack> existingRoot = dispatcher.getRoot().getChild(parts[0]);
        if (existingRoot == null) {
            throw new IllegalStateException(
                    String.format("[SSEC] Failed to register alias '%s': root command '/%s' is not registered! " +
                            "Please register the @SSCommand(\"%s\") root command first.",
                            alias, parts[0], parts[0]));
        }

        // Create the last segment as a redirect
        LiteralArgumentBuilder<CommandSourceStack> innermost = Commands.literal(parts[parts.length - 1])
                .redirect(targetNode);

        // Wrap if there are intermediate segments
        for (int i = parts.length - 2; i >= 1; i--) {
            LiteralArgumentBuilder<CommandSourceStack> wrapper = Commands.literal(parts[i]);
            wrapper.then(innermost);
            innermost = wrapper;
        }

        // Add directly as a child to the already registered parent command
        existingRoot.addChild(innermost.build());
        LOGGER.info("Registered alias: /{} -> {} (added to existing /{})",
                resolved, targetNode.getName(), parts[0]);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildCommandNode(Class<?> clazz,
            List<Class<?>> allCommandClasses) {
        SSCommand cmdAnnotation = clazz.getAnnotation(SSCommand.class);
        if (cmdAnnotation == null)
            return null;
        return buildCommandNodeWithName(cmdAnnotation.value(), clazz, allCommandClasses);
    }

    /**
     * Builds the command node with the specified name (overrideName).
     * When registering an alias, it can be built with the alias segment name
     * instead of the original command name.
     */
    private static LiteralArgumentBuilder<CommandSourceStack> buildCommandNodeWithName(
            String overrideName, Class<?> clazz, List<Class<?>> allCommandClasses) {

        LiteralArgumentBuilder<CommandSourceStack> node = Commands.literal(overrideName);

        // Handle permissions
        SSCPermission permAnnotation = null;
        if (clazz.isAnnotationPresent(SSCPermission.class)) {
            permAnnotation = clazz.getAnnotation(SSCPermission.class);
            if (validatePermission(permAnnotation, clazz.getSimpleName())) {
                // Always apply requires() to the command node for visibility/access control
                if (permAnnotation.permission() != PermissionLevel.NONE) {
                    final Permission requiredLevel = permAnnotation.permission().getPermission();
                    node.requires(source -> {
                        try {
                            return (boolean) source.permissions().hasPermission(requiredLevel);
                        } catch (Exception e) {
                            return false;
                        }
                    });
                } else {
                    final SSCPermission finalPerm = permAnnotation;
                    node.requires(source -> checkPermission(source, finalPerm));
                }

                if (permAnnotation.propagate()) {
                    // propagate=true: already handled by requires(), no execute-time check
                    permAnnotation = null;
                }
                // propagate=false: Keep permAnnotation → also check in execute handler
            } else {
                permAnnotation = null; // validation failed
            }
        }

        // Handle executes
        final SSCPermission executePermission = permAnnotation; // non-null only when propagate=false
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(SSCExecute.class)) {
                if (!Modifier.isStatic(method.getModifiers())) {
                    LOGGER.error("Method {} in {} must be static to act as an execution method.", method.getName(),
                            clazz.getName());
                    continue;
                }

                Parameter[] parameters = method.getParameters();
                List<ParameterMapping> mappings = new ArrayList<>();
                List<RequiredArgumentBuilder<CommandSourceStack, ?>> argBuilders = new ArrayList<>();

                for (int i = 0; i < parameters.length; i++) {
                    Parameter param = parameters[i];

                    // Always allow CommandContext or CommandSourceStack as non-annotated params
                    if (param.getType() == CommandContext.class || param.getType() == CommandSourceStack.class) {
                        mappings.add(new ParameterMapping(null, param.getType()));
                        continue;
                    }

                    String argName = null;
                    if (param.isAnnotationPresent(SSCArgument.class)) {
                        argName = param.getAnnotation(SSCArgument.class).value();
                    }

                    if (argName == null) {
                        LOGGER.error("Parameter {} in method {} is missing an argument annotation.", param.getName(),
                                method.getName());
                        continue;
                    }

                    ArgumentType<?> argType = getArgumentTypeForClass(param.getType());

                    if (argType == null) {
                        LOGGER.error("Unsupported argument type {} in method {}", param.getType().getName(),
                                method.getName());
                        continue;
                    }

                    mappings.add(new ParameterMapping(argName, param.getType()));
                    RequiredArgumentBuilder<CommandSourceStack, ?> argBuilder = Commands.argument(argName, argType);
                    argBuilders.add(argBuilder);
                }

                Command<CommandSourceStack> commandExecutor = context -> {
                    try {
                        // Check permissions before execution when propagate=false
                        if (executePermission != null) {
                            boolean hasPermission;
                            if (executePermission.permission() != PermissionLevel.NONE) {
                                try {
                                    hasPermission = (boolean) context.getSource().permissions()
                                            .hasPermission(executePermission.permission().getPermission());
                                } catch (Exception e) {
                                    hasPermission = false;
                                }
                            } else {
                                hasPermission = checkPermission(context.getSource(), executePermission);
                            }
                            if (!hasPermission) {
                                context.getSource().sendFailure(
                                        Component.literal("You do not have permission to execute this command."));
                                return 0;
                            }
                        }

                        Object[] args = new Object[parameters.length];
                        for (int i = 0; i < mappings.size(); i++) {
                            ParameterMapping map = mappings.get(i);
                            if (map.name == null) { // Contextual
                                if (map.type == CommandContext.class) {
                                    args[i] = context;
                                } else if (map.type == CommandSourceStack.class) {
                                    args[i] = context.getSource();
                                }
                            } else {
                                SSCArgumentAdapter<?> adapter = ADAPTER_REGISTRY.get(map.type);
                                if (adapter != null) {
                                    args[i] = adapter.value(context, map.name);
                                } else if (map.type.getName().contains("ServerPlayer")) {
                                    args[i] = EntityArgument.getPlayer(context, map.name);
                                } else {
                                    args[i] = context.getArgument(map.name, map.type);
                                }
                            }
                        }
                        method.invoke(null, args);
                        return 1; // Success
                    } catch (Exception e) {
                        LOGGER.error("Error executing command", e);
                        return 0;
                    }
                };

                if (!argBuilders.isEmpty()) {
                    argBuilders.get(argBuilders.size() - 1).executes(commandExecutor);
                    for (int j = argBuilders.size() - 1; j > 0; j--) {
                        argBuilders.get(j - 1).then(argBuilders.get(j));
                    }
                    node.then(argBuilders.get(0));
                } else {
                    node = node.executes(commandExecutor);
                }
            }
        }

        // Handle subcommands: inner classes
        for (Class<?> innerClass : clazz.getDeclaredClasses()) {
            if (innerClass.isAnnotationPresent(SSCommand.class)) {
                SSCommand innerCmdAnnotation = innerClass.getAnnotation(SSCommand.class);
                if (innerCmdAnnotation.parent() == clazz) {
                    LiteralArgumentBuilder<CommandSourceStack> subNode = buildCommandNode(innerClass,
                            allCommandClasses);
                    if (subNode != null) {
                        node = node.then(subNode);

                        // Register aliases for subcommands too
                        // (aliases for inner subcommands are handled at registration time)
                    }
                }
            }
        }

        // Handle subcommands: external classes (cross-file parent support)
        for (Class<?> candidate : allCommandClasses) {
            if (candidate.isAnnotationPresent(SSCommand.class)) {
                SSCommand candidateAnnotation = candidate.getAnnotation(SSCommand.class);
                // External class with parent == this class and NOT an inner class of this class
                if (candidateAnnotation.parent() == clazz && candidate.getDeclaringClass() != clazz) {
                    LiteralArgumentBuilder<CommandSourceStack> subNode = buildCommandNode(candidate,
                            allCommandClasses);
                    if (subNode != null) {
                        node = node.then(subNode);
                        LOGGER.info("Registered cross-file subcommand: {} under {}", candidateAnnotation.value(),
                                overrideName);
                    }
                }
            }
        }

        return node;
    }

    private static ArgumentType<?> getArgumentTypeForClass(Class<?> clazz) {
        if (ADAPTER_REGISTRY.containsKey(clazz)) {
            return ADAPTER_REGISTRY.get(clazz).argumentType();
        }
        if (clazz.getName().contains("ServerPlayer"))
            return EntityArgument.player();
        return null;
    }

    /**
     * Validates that only one of the three options in @SSCPermission is used.
     * Logs an error and returns false if multiple are specified.
     */
    private static boolean validatePermission(SSCPermission perm, String className) {
        int count = 0;
        if (perm.permission() != PermissionLevel.NONE)
            count++;
        if (perm.value().length > 0)
            count++;
        if (perm.custom() != SSCPermissionChecker.class)
            count++;

        if (count == 0) {
            LOGGER.warn("[SSEC] @SSCPermission on {} has no value set. Ignoring.", className);
            return false;
        }
        if (count > 1) {
            LOGGER.error("[SSEC] @SSCPermission on {} has multiple values set (level/value/custom). " +
                    "Only one is allowed at a time!", className);
            return false;
        }
        return true;
    }

    private static boolean checkPermission(CommandSourceStack source, SSCPermission perm) {
        if (perm == null)
            return true;

        // 1. Custom permission checker
        Class<? extends SSCPermissionChecker> customClass = perm.custom();
        if (customClass != null && customClass != SSCPermissionChecker.class) {
            try {
                SSCPermissionChecker checker = customClass.getDeclaredConstructor().newInstance();
                return checker.check(source);
            } catch (Exception e) {
                LOGGER.error("Failed to instantiate custom permission checker", e);
                return false;
            }
        }

        // 2. LuckPerms/Fabric Permissions API check
        String[] permissions = perm.value();
        if (permissions != null && permissions.length > 0) {
            try {
                Class<?> permsClass = Class.forName("me.lucko.fabric.api.permissions.v0.Permissions");
                Method checkMethod = permsClass.getMethod("check", Object.class, String.class, int.class);
                for (String p : permissions) {
                    boolean has = (boolean) checkMethod.invoke(null, source, p, 2);
                    if (has)
                        return true;
                }
            } catch (Exception ignored) {
            }
        }

        return false;
    }

    private static class ParameterMapping {
        String name;
        Class<?> type;

        ParameterMapping(String name, Class<?> type) {
            this.name = name;
            this.type = type;
        }
    }
}

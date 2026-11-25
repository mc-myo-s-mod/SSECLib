package me.myogoo.ssec.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import me.myogoo.ssec.command.CommandArguments.ParameterMapping;
import me.myogoo.ssec.command.CommandMetadata.CommandInfo;
import me.myogoo.ssec.command.CommandMetadata.ExecuteInfo;
import me.myogoo.ssec.command.CommandMetadata.PermissionInfo;
import me.myogoo.ssec.api.command.argument.SSCArgumentAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

final class CommandNodeBuilder {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandNodeBuilder.class);
    private CommandNodeBuilder() {
    }

    static LiteralArgumentBuilder<CommandSourceStack> build(CommandBuildContext buildContext, Class<?> clazz,
            List<Class<?>> allCommandClasses, Set<Class<?>> builtClasses,
            Set<Class<?>> ancestors, boolean developmentMode) {
        if (!ancestors.add(clazz)) {
            LOGGER.error("Cyclic command parent chain at {}", clazz.getName());
            return null;
        }
        try {
            return buildNode(buildContext, clazz, allCommandClasses, builtClasses, ancestors, developmentMode);
        } catch (RuntimeException | LinkageError e) {
            LOGGER.error("Could not build SSEC command class {}", clazz.getName(), e);
            return null;
        } finally {
            ancestors.remove(clazz);
        }
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildNode(CommandBuildContext buildContext,
            Class<?> clazz, List<Class<?>> allCommandClasses,
            Set<Class<?>> builtClasses, Set<Class<?>> ancestors, boolean developmentMode) {
        CommandInfo command = CommandMetadata.getCommandInfo(clazz);
        if (command == null) {
            return null;
        }
        if (command.value().isBlank() || command.value().chars().anyMatch(c -> Character.isWhitespace(c) || c == '/')) {
            LOGGER.error("Invalid command literal on {}: {}", clazz.getName(), command.value());
            return null;
        }

        LiteralArgumentBuilder<CommandSourceStack> node = LiteralArgumentBuilder.<CommandSourceStack>literal(command.value());
        PermissionInfo permission = CommandMetadata.getPermissionInfo(clazz);
        PermissionInfo localExecutePermission = null;
        if (permission != null) {
            if (!CommandPermissions.validate(permission, clazz.getName())) {
                return null;
            }
            if (permission.propagate()) {
                CommandPermissions.apply(node, permission);
            } else {
                localExecutePermission = permission;
            }
        }

        Method[] methods = clazz.getDeclaredMethods();
        Arrays.sort(methods, Comparator.comparing(Method::toGenericString));
        for (Method method : methods) {
            try {
                ExecuteInfo execute = CommandMetadata.getExecuteInfo(method);
                if (execute == null || (!developmentMode && CommandMetadata.isDebugOnly(method))) {
                    continue;
                }
                if (!execute.path().isBlank() && CommandPaths.split(execute.path()).isEmpty()) {
                    LOGGER.error("Invalid execution path on {}.{}", clazz.getName(), method.getName());
                    continue;
                }
                if (!Modifier.isStatic(method.getModifiers())) {
                    LOGGER.error("Command execute method must be static: {}.{}", clazz.getName(), method.getName());
                    continue;
                }

                PermissionInfo methodPermission = CommandMetadata.getPermissionInfo(method);
                if (methodPermission != null && !CommandPermissions.validate(methodPermission,
                        clazz.getName() + "." + method.getName())) {
                    continue;
                }

                List<PermissionInfo> executePermissions = new ArrayList<>(2);
                if (localExecutePermission != null) {
                    executePermissions.add(localExecutePermission);
                }
                if (methodPermission != null) {
                    executePermissions.add(methodPermission);
                }
                buildExecutionBranch(buildContext, clazz, method, execute,
                        List.copyOf(executePermissions), methodPermission, node);
            } catch (RuntimeException | LinkageError e) {
                LOGGER.error("Skipping SSEC execute method {}.{}", clazz.getName(), method.getName(), e);
            }
        }

        for (Class<?> candidate : allCommandClasses) {
            CommandInfo childCommand;
            try {
                childCommand = CommandMetadata.getCommandInfo(candidate);
            } catch (RuntimeException | LinkageError e) {
                LOGGER.error("Could not resolve child command metadata {}", candidate.getName(), e);
                continue;
            }
            if (childCommand != null && childCommand.parent() == clazz) {
                if (node.getArguments().stream().anyMatch(existing -> existing.getName().equals(childCommand.value()))) {
                    LOGGER.warn("Skipping colliding SSEC child command {}", candidate.getName());
                    continue;
                }
                LiteralArgumentBuilder<CommandSourceStack> child = build(buildContext, candidate,
                        allCommandClasses, builtClasses, ancestors, developmentMode);
                if (child != null) {
                    node.then(child);
                }
            }
        }
        if (node.getCommand() == null && node.getArguments().isEmpty()) {
            LOGGER.warn("Skipping SSEC command with no executable branches: {}", clazz.getName());
            return null;
        }
        builtClasses.add(clazz);
        return node;
    }

    private static void buildExecutionBranch(CommandBuildContext buildContext,
                                             Class<?> owner,
                                             Method method,
                                             ExecuteInfo execute,
                                             List<PermissionInfo> executePermissions,
                                             PermissionInfo methodPermission,
                                             LiteralArgumentBuilder<CommandSourceStack> node) {
        Parameter[] parameters = method.getParameters();
        List<ParameterMapping> mappings = new ArrayList<>();
        List<RequiredArgumentBuilder<CommandSourceStack, ?>> argumentBuilders = new ArrayList<>();
        Set<String> argumentNames = new HashSet<>();

        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            Class<?> parameterType = parameter.getType();
            if (parameterType == CommandContext.class || parameterType == CommandSourceStack.class) {
                if (CommandMetadata.getArgumentName(parameter) != null) {
                    LOGGER.error("Injected parameter in {}.{} must not have @SSCArgument.", owner.getName(), method.getName());
                    return;
                }
                mappings.add(ParameterMapping.context(i, parameterType));
                continue;
            }

            String argumentName = CommandMetadata.getArgumentName(parameter);
            if (argumentName == null || argumentName.isBlank()
                    || argumentName.chars().anyMatch(c -> Character.isWhitespace(c) || c == '/')
                    || !argumentNames.add(argumentName)) {
                LOGGER.error("Parameter {} in {}.{} has a missing, blank or duplicate @SSCArgument name.",
                        parameter.getName(), owner.getName(), method.getName());
                return;
            }

            SSCArgumentAdapter<?> adapter = CommandArguments.getAdapter(parameterType);
            if (adapter == null) {
                LOGGER.error("Unsupported command argument type {} in {}.{}",
                        parameterType.getName(), owner.getName(), method.getName());
                return;
            }

            mappings.add(ParameterMapping.argument(i, argumentName, adapter));
            ArgumentType<?> argumentType = adapter.argumentType(buildContext);
            RequiredArgumentBuilder<CommandSourceStack, ?> argument = RequiredArgumentBuilder.argument(argumentName, argumentType);
            argumentBuilders.add(argument);
        }

        method.setAccessible(true);
        CommandNode<CommandSourceStack> existing = node.build();
        List<String> names = new ArrayList<>(CommandPaths.split(execute.path()));
        for (RequiredArgumentBuilder<CommandSourceStack, ?> argument : argumentBuilders) {
            names.add(argument.getName());
        }
        for (String name : names) {
            existing = existing.getChild(name);
            if (existing == null) {
                break;
            }
        }
        if (existing != null && (existing.getCommand() != null
                || (!names.isEmpty() && methodPermission != null && methodPermission.propagate()))) {
            LOGGER.warn("Skipping colliding SSEC execute method {}.{}", owner.getName(), method.getName());
            return;
        }
        Command<CommandSourceStack> executor = context -> invokeExecute(method, parameters, mappings,
                executePermissions, context);
        attachExecution(node, execute.path(), argumentBuilders, executor,
                methodPermission != null && methodPermission.propagate() ? methodPermission : null);
    }

    private static int invokeExecute(Method method,
                                     Parameter[] parameters,
                                     List<ParameterMapping> mappings,
                                     List<PermissionInfo> executePermissions,
                                     CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        try {
            for (PermissionInfo permission : executePermissions) {
                if (!CommandPermissions.check(context.getSource(), permission)) {
                    context.getSource().sendFailure(
                            Component.literal("You do not have permission to execute this command."));
                    return 0;
                }
            }

            Object[] args = new Object[parameters.length];
            for (ParameterMapping mapping : mappings) {
                args[mapping.index()] = mapping.value(context);
            }

            Object result = method.invoke(null, args);
            if (result instanceof Number number) {
                return number.intValue();
            }
            if (result instanceof Boolean bool) {
                return bool ? Command.SINGLE_SUCCESS : 0;
            }
            return Command.SINGLE_SUCCESS;
        } catch (CommandSyntaxException e) {
            throw e;
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof CommandSyntaxException syntaxException) {
                throw syntaxException;
            }
            LOGGER.error("Error executing SSEC command method {}", method.getName(), e);
            return 0;
        } catch (Exception | LinkageError e) {
            LOGGER.error("Error executing SSEC command method {}", method.getName(), e);
            return 0;
        }
    }

    private static void attachExecution(LiteralArgumentBuilder<CommandSourceStack> node,
                                        String path,
                                        List<RequiredArgumentBuilder<CommandSourceStack, ?>> argumentBuilders,
                                        Command<CommandSourceStack> executor,
                                        PermissionInfo nodePermission) {
        ArgumentBuilder<CommandSourceStack, ?> branch = null;
        if (!argumentBuilders.isEmpty()) {
            RequiredArgumentBuilder<CommandSourceStack, ?> leaf = argumentBuilders.get(argumentBuilders.size() - 1);
            leaf.executes(executor);
            if (nodePermission != null) {
                CommandPermissions.apply(leaf, nodePermission);
            }
            for (int i = argumentBuilders.size() - 1; i > 0; i--) {
                argumentBuilders.get(i - 1).then(argumentBuilders.get(i));
            }
            branch = argumentBuilders.get(0);
        }

        List<String> segments = CommandPaths.split(path);
        if (segments.isEmpty()) {
            if (branch == null) {
                node.executes(executor);
                if (nodePermission != null) {
                    CommandPermissions.apply(node, nodePermission);
                }
            } else {
                node.then(branch);
            }
            return;
        }

        LiteralArgumentBuilder<CommandSourceStack> leaf = LiteralArgumentBuilder.<CommandSourceStack>literal(segments.get(segments.size() - 1));
        if (branch == null) {
            leaf.executes(executor);
            if (nodePermission != null) {
                CommandPermissions.apply(leaf, nodePermission);
            }
        } else {
            leaf.then(branch);
        }

        LiteralArgumentBuilder<CommandSourceStack> current = leaf;
        for (int i = segments.size() - 2; i >= 0; i--) {
            LiteralArgumentBuilder<CommandSourceStack> parent = LiteralArgumentBuilder.<CommandSourceStack>literal(segments.get(i));
            parent.then(current);
            current = parent;
        }
        node.then(current);
    }

}

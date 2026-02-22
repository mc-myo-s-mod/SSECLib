package me.myogoo.ssec.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import me.myogoo.ssec.api.command.SSCExecute;
import me.myogoo.ssec.api.command.SSCPermission;
import me.myogoo.ssec.api.command.SSCommand;
import me.myogoo.ssec.api.command.SSCDocument;
import me.myogoo.ssec.api.command.SSCArgument;
import me.myogoo.ssec.api.command.SSCPermissionChecker;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.minecraft.commands.arguments.EntityArgument;

import me.myogoo.ssec.api.command.argument.SSCArgumentAdapter;
import me.myogoo.ssec.command.argument.SSCIntArgument;
import me.myogoo.ssec.command.argument.SSCDoubleArgument;
import me.myogoo.ssec.command.argument.SSCFloatArgument;
import me.myogoo.ssec.command.argument.SSCBooleanArgument;
import me.myogoo.ssec.command.argument.SSCStringArgument;
import me.myogoo.ssec.command.argument.SSCVec3Argument;
import me.myogoo.ssec.command.argument.SSCEntityArgument;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.Entity;

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
     * Registers a command class to the dispatcher.
     * 
     * @param dispatcher The CommandDispatcher
     * @param clazz      The class annotated with @SSCommand
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, Class<?> clazz) {
        LOGGER.debug("Attempting to register class: {}", clazz.getName());
        if (!clazz.isAnnotationPresent(SSCommand.class)) {
            LOGGER.debug("Class {} missing @SSCommand", clazz.getName());
            return;
        }

        LiteralArgumentBuilder<CommandSourceStack> builder = buildCommandNode(clazz);
        if (builder != null) {
            LiteralCommandNode<CommandSourceStack> registeredNode = dispatcher
                    .register(builder);
            LOGGER.info("Successfully registered root node: {}", registeredNode.getName());

            if (clazz.isAnnotationPresent(me.myogoo.ssec.api.command.SSCAlias.class)) {
                me.myogoo.ssec.api.command.SSCAlias aliasAnnotation = clazz
                        .getAnnotation(me.myogoo.ssec.api.command.SSCAlias.class);
                for (String alias : aliasAnnotation.value()) {
                    dispatcher.register(Commands.literal(alias).redirect(registeredNode));
                    LOGGER.info("Registered alias: {} for {}", alias, registeredNode.getName());
                }
            }
        } else {
            LOGGER.warn("Builder was null for class: {}", clazz.getName());
        }
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildCommandNode(Class<?> clazz) {
        SSCommand cmdAnnotation = clazz.getAnnotation(SSCommand.class);
        if (cmdAnnotation == null) {
            return null;
        }

        LiteralArgumentBuilder<CommandSourceStack> node = Commands.literal(cmdAnnotation.value());

        // Handle document
        if (clazz.isAnnotationPresent(SSCDocument.class)) {
            SSCDocument docAnnotation = clazz.getAnnotation(SSCDocument.class);
            LOGGER.info("Registered Command [{}] Document: {}", cmdAnnotation.value(), docAnnotation.value());
        }

        // Handle permissions
        if (clazz.isAnnotationPresent(SSCPermission.class)) {
            SSCPermission permAnnotation = clazz.getAnnotation(SSCPermission.class);
            node.requires(source -> checkPermission(source, permAnnotation));
        }

        // Handle executes
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

        // Handle subcommands
        for (Class<?> innerClass : clazz.getDeclaredClasses()) {
            if (innerClass.isAnnotationPresent(SSCommand.class)) {
                SSCommand innerCmdAnnotation = innerClass.getAnnotation(SSCommand.class);
                if (innerCmdAnnotation.parent() == clazz) {
                    LiteralArgumentBuilder<CommandSourceStack> subNode = buildCommandNode(innerClass);
                    if (subNode != null) {
                        node = node.then(subNode);
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

    private static boolean checkPermission(CommandSourceStack source, SSCPermission perm) {
        if (perm == null)
            return true;

        // 1. Vanilla operator level bypass
        try {
            boolean hasVanilla = (boolean) source.getClass().getMethod("hasPermission", int.class).invoke(source,
                    perm.level());
            if (hasVanilla)
                return true;
        } catch (Exception e) {
            try {
                boolean hasVanilla = (boolean) source.getClass().getMethod("hasPermissionLevel", int.class)
                        .invoke(source, perm.level());
                if (hasVanilla)
                    return true;
            } catch (Exception ex) {
            }
        }

        // 2. Custom permission checker
        Class<? extends SSCPermissionChecker> customClass = perm.custom();
        if (customClass != null && customClass != SSCPermissionChecker.class) {
            try {
                SSCPermissionChecker checker = customClass.getDeclaredConstructor().newInstance();
                if (checker.check(source))
                    return true;
            } catch (Exception e) {
                LOGGER.error("Failed to instantiate custom permission checker", e);
            }
        }

        // 3. Try LuckPerms/Fabric Permissions API check
        String[] permissions = perm.value();
        if (permissions != null && permissions.length > 0) {
            try {
                Class<?> permsClass = Class.forName("me.lucko.fabric.api.permissions.v0.Permissions");
                Method checkMethod = permsClass.getMethod("check", Object.class, String.class, int.class);
                for (String p : permissions) {
                    boolean has = (boolean) checkMethod.invoke(null, source, p, perm.level());
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

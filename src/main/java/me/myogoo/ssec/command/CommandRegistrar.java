package me.myogoo.ssec.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import me.myogoo.ssec.api.command.SSCExecute;
import me.myogoo.ssec.api.command.PermissionLevel;
import me.myogoo.ssec.api.command.SSCPermission;
import me.myogoo.ssec.api.command.SSCommand;
import me.myogoo.ssec.api.command.SSCDocument;
import me.myogoo.ssec.api.command.SSCArgument;
import me.myogoo.ssec.api.command.SSCAlias;
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
import me.myogoo.ssec.command.argument.entity.SSCEntitiesArgument;
import me.myogoo.ssec.command.argument.entity.SSCEntityArgument;
import me.myogoo.ssec.command.argument.entity.SSCPlayerArgument;
import me.myogoo.ssec.command.argument.entity.SSCPlayersArgument;
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
                SSCAlias aliasAnnotation = clazz.getAnnotation(SSCAlias.class);
                for (String alias : aliasAnnotation.value()) {
                    registerAlias(dispatcher, alias, registeredNode);
                }
            }

            // Handle subcommand aliases (inner + external classes)
            registerSubcommandAliases(dispatcher, clazz, allCommandClasses);
        } else {
            LOGGER.warn("Builder was null for class: {}", clazz.getName());
        }
    }

    /**
     * 모든 서브커맨드 클래스를 순회하며 @SSCAlias가 있으면
     * 전체 경로를 추적하여 dispatcher에서 노드를 찾아 alias를 등록합니다.
     */
    private static void registerSubcommandAliases(CommandDispatcher<CommandSourceStack> dispatcher,
            Class<?> rootClass, List<Class<?>> allCommandClasses) {
        // root 클래스 산하의 모든 커맨드 클래스를 수집 (inner + external)
        List<Class<?>> allChildren = new ArrayList<>();
        collectAllChildren(rootClass, allCommandClasses, allChildren);

        for (Class<?> childClass : allChildren) {
            if (!childClass.isAnnotationPresent(SSCAlias.class))
                continue;

            // 전체 경로 추적: childClass → parent → ... → rootClass
            List<String> path = resolveCommandPath(childClass, rootClass);
            if (path == null || path.isEmpty()) {
                LOGGER.warn("Could not resolve command path for alias on: {}", childClass.getName());
                continue;
            }

            // dispatcher에서 노드 찾기
            CommandNode<CommandSourceStack> targetNode = findNode(dispatcher, path);
            if (targetNode == null) {
                LOGGER.warn("Could not find dispatcher node for path: {}", path);
                continue;
            }

            SSCAlias aliasAnnotation = childClass.getAnnotation(SSCAlias.class);
            for (String alias : aliasAnnotation.value()) {
                registerSubcommandAlias(dispatcher, alias, targetNode);
            }
        }
    }

    /**
     * rootClass 산하의 모든 커맨드 클래스를 재귀적으로 수집합니다.
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
     * childClass부터 rootClass까지의 커맨드 경로를 추적합니다.
     * 예: SubSubCommand("number") → SubCommand("say") → TestCommand("ssec")
     * 반환: ["ssec", "say", "number"]
     */
    private static List<String> resolveCommandPath(Class<?> childClass, Class<?> rootClass) {
        List<String> path = new ArrayList<>();
        Class<?> current = childClass;
        int maxDepth = 20; // 무한 루프 방지
        while (current != null && maxDepth-- > 0) {
            SSCommand ann = current.getAnnotation(SSCommand.class);
            if (ann == null)
                return null;
            path.add(0, ann.value());
            if (current == rootClass)
                break;
            current = ann.parent();
            if (current == void.class)
                return null; // root에 도달하지 못함
        }
        return path;
    }

    /**
     * dispatcher에서 주어진 경로의 커맨드 노드를 찾습니다.
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
     * Alias 등록 (redirect 방식 - root alias 전용).
     * "." 이 포함되면 계층 리터럴 트리로 분리하여 등록합니다.
     */
    private static void registerAlias(CommandDispatcher<CommandSourceStack> dispatcher,
            String alias, CommandNode<CommandSourceStack> targetNode) {
        if (!alias.contains(".")) {
            dispatcher.register(Commands.literal(alias).redirect(targetNode));
            LOGGER.info("Registered alias: {} → {}", alias, targetNode.getName());
            return;
        }

        String[] parts = alias.split("\\.");
        if (parts.length < 2) {
            dispatcher.register(Commands.literal(alias).redirect(targetNode));
            LOGGER.info("Registered alias: {} → {}", alias, targetNode.getName());
            return;
        }

        LiteralArgumentBuilder<CommandSourceStack> innermost = Commands.literal(parts[parts.length - 1])
                .redirect(targetNode);
        for (int i = parts.length - 2; i >= 1; i--) {
            LiteralArgumentBuilder<CommandSourceStack> wrapper = Commands.literal(parts[i]);
            wrapper.then(innermost);
            innermost = wrapper;
        }
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(parts[0]);
        root.then(innermost);
        dispatcher.register(root);
        LOGGER.info("Registered dot-alias: {} → {}", alias, targetNode.getName());
    }

    /**
     * 서브커맨드 alias 등록 (redirect 방식).
     * alias "." 기준 분리 후, 이미 등록된 상위 커맨드 노드에 redirect 자식을 추가합니다.
     */
    private static void registerSubcommandAlias(CommandDispatcher<CommandSourceStack> dispatcher,
            String alias, CommandNode<CommandSourceStack> targetNode) {
        if (!alias.contains(".")) {
            // 단순 alias: 최상위로 redirect
            dispatcher.register(Commands.literal(alias).redirect(targetNode));
            LOGGER.info("Registered subcommand alias: /{} -> {}", alias, targetNode.getName());
            return;
        }

        String[] parts = alias.split("\\.");

        // 상위 커맨드가 dispatcher에 등록되어 있는지 확인
        CommandNode<CommandSourceStack> existingRoot = dispatcher.getRoot().getChild(parts[0]);
        if (existingRoot == null) {
            LOGGER.error("[SSEC] Alias '{}' 등록 실패: 상위 커맨드 '/{}' 가 등록되어 있지 않습니다! " +
                    "먼저 @SSCommand(\"{}\") 루트 커맨드를 등록해 주세요.", alias, parts[0], parts[0]);
            return;
        }

        // 마지막 세그먼트를 redirect로 생성
        LiteralArgumentBuilder<CommandSourceStack> innermost = Commands.literal(parts[parts.length - 1])
                .redirect(targetNode);

        // 중간 세그먼트가 있으면 감싸기
        for (int i = parts.length - 2; i >= 1; i--) {
            LiteralArgumentBuilder<CommandSourceStack> wrapper = Commands.literal(parts[i]);
            wrapper.then(innermost);
            innermost = wrapper;
        }

        // 이미 등록된 상위 커맨드에 자식으로 직접 추가
        existingRoot.addChild(innermost.build());
        LOGGER.info("Registered dot-alias: {} -> {} (added to existing /{})",
                alias, targetNode.getName(), parts[0]);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildCommandNode(Class<?> clazz,
            List<Class<?>> allCommandClasses) {
        SSCommand cmdAnnotation = clazz.getAnnotation(SSCommand.class);
        if (cmdAnnotation == null)
            return null;
        return buildCommandNodeWithName(cmdAnnotation.value(), clazz, allCommandClasses);
    }

    /**
     * 커맨드 노드를 지정된 이름(overrideName)으로 빌드합니다.
     * alias 등록 시 원래 커맨드명 대신 alias 세그먼트 이름으로 빌드할 수 있습니다.
     */
    private static LiteralArgumentBuilder<CommandSourceStack> buildCommandNodeWithName(
            String overrideName, Class<?> clazz, List<Class<?>> allCommandClasses) {

        LiteralArgumentBuilder<CommandSourceStack> node = Commands.literal(overrideName);

        // Handle document
        if (clazz.isAnnotationPresent(SSCDocument.class)) {
            SSCDocument docAnnotation = clazz.getAnnotation(SSCDocument.class);
            LOGGER.info("Registered Command [{}] Document: {}", overrideName, docAnnotation.value());
        }

        // Handle permissions
        if (clazz.isAnnotationPresent(SSCPermission.class)) {
            SSCPermission permAnnotation = clazz.getAnnotation(SSCPermission.class);
            if (validatePermission(permAnnotation, clazz.getSimpleName())) {
                // level이 지정되면 requires에 OP 레벨 설정
                if (permAnnotation.level() != PermissionLevel.NONE) {
                    final int requiredLevel = permAnnotation.level().getLevel();
                    node.requires(source -> {
                        try {
                            return (boolean) source.getClass().getMethod("hasPermission", int.class)
                                    .invoke(source, requiredLevel);
                        } catch (Exception e) {
                            return false;
                        }
                    });
                } else {
                    node.requires(source -> checkPermission(source, permAnnotation));
                }
            }
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
     * @SSCPermission의 3개 옵션 중 하나만 사용했는지 검증합니다.
     *                 복수 지정 시 에러 로그를 출력하고 false를 반환합니다.
     */
    private static boolean validatePermission(SSCPermission perm, String className) {
        int count = 0;
        if (perm.level() != PermissionLevel.NONE)
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

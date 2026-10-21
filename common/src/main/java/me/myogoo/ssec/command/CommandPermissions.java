package me.myogoo.ssec.command;

import com.mojang.brigadier.builder.ArgumentBuilder;
import me.myogoo.ssec.api.command.permission.PermissionLevel;
import me.myogoo.ssec.api.command.permission.SSCPermissionChecker;
import me.myogoo.ssec.command.CommandMetadata.PermissionInfo;
import net.minecraft.commands.CommandSourceStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class CommandPermissions {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandPermissions.class);
    private static final Map<Class<? extends SSCPermissionChecker>, SSCPermissionChecker> CHECKERS =
            new ConcurrentHashMap<>();

    private CommandPermissions() {
    }

    static void apply(ArgumentBuilder<CommandSourceStack, ?> builder, PermissionInfo permission) {
        builder.requires(builder.getRequirement().and(source -> check(source, permission)));
    }

    static boolean check(CommandSourceStack source, PermissionInfo permission) {
        if (permission == null) {
            return true;
        }
        try {
            if (permission.level() != null && permission.level() != PermissionLevel.NONE) {
                return permission.level().check(source);
            }
            if (permission.customChecker() != SSCPermissionChecker.class) {
                return checker(permission.customChecker()).check(source);
            }
            if (permission.nodes() != null && permission.nodes().length > 0) {
                Method check = namedPermissionCheck();
                for (String node : permission.nodes()) {
                    if (Boolean.TRUE.equals(check.invoke(null, source, node, 2))) {
                        return true;
                    }
                }
            }
        } catch (Exception | LinkageError e) {
            LOGGER.error("SSEC permission evaluation failed; access was denied.", e);
        }
        return false;
    }

    static boolean validate(PermissionInfo permission, String owner) {
        int count = 0;
        if (permission.level() != null && permission.level() != PermissionLevel.NONE) {
            count++;
        }
        if (permission.customChecker() != SSCPermissionChecker.class) {
            count++;
        }
        if (permission.nodes() != null && permission.nodes().length > 0) {
            count++;
        }
        if (count != 1) {
            LOGGER.error("Permission on {} must specify exactly one of level, custom checker or named nodes. "
                    + "Command registration was denied.", owner);
            return false;
        }
        try {
            if (permission.customChecker() != SSCPermissionChecker.class) {
                checker(permission.customChecker());
            }
            if (permission.nodes() != null && permission.nodes().length > 0) {
                for (String node : permission.nodes()) {
                    if (node == null || node.isBlank()) {
                        throw new IllegalArgumentException("Permission node must not be blank");
                    }
                }
                namedPermissionCheck();
            }
            return true;
        } catch (Exception | LinkageError e) {
            LOGGER.error("Permission on {} could not be initialized (named nodes require a supported "
                    + "Fabric Permissions API bridge). Command registration was denied.", owner, e);
            return false;
        }
    }

    private static SSCPermissionChecker checker(Class<? extends SSCPermissionChecker> checkerClass) {
        return CHECKERS.computeIfAbsent(checkerClass, type -> {
            try {
                return type.getConstructor().newInstance();
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Custom permission checker must expose a public no-argument "
                        + "constructor: " + type.getName(), e);
            }
        });
    }

    private static Method namedPermissionCheck() throws ReflectiveOperationException {
        Class<?> bridge = Class.forName("me.lucko.fabric.api.permissions.v0.Permissions");
        try {
            return bridge.getMethod("check", CommandSourceStack.class, String.class, int.class);
        } catch (NoSuchMethodException e) {
            return bridge.getMethod("check", Object.class, String.class, int.class);
        }
    }
}

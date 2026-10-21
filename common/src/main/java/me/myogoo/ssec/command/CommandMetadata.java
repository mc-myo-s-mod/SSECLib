package me.myogoo.ssec.command;

import me.myogoo.ssec.api.SSECDebug;
import me.myogoo.ssec.api.command.SSCAlias;
import me.myogoo.ssec.api.command.SSCArgument;
import me.myogoo.ssec.api.command.SSCExecute;
import me.myogoo.ssec.api.command.SSCommand;
import me.myogoo.ssec.api.command.SSCPermission;
import me.myogoo.ssec.api.command.permission.PermissionLevel;
import me.myogoo.ssec.api.command.permission.SSCPermissionChecker;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

final class CommandMetadata {
    private CommandMetadata() {
    }

    static CommandInfo getCommandInfo(Class<?> clazz) {
        SSCommand command = clazz.getAnnotation(SSCommand.class);
        return command == null ? null : new CommandInfo(command.value(), command.parent());
    }

    static ExecuteInfo getExecuteInfo(Method method) {
        SSCExecute execute = method.getAnnotation(SSCExecute.class);
        return execute == null ? null : new ExecuteInfo(execute.value());
    }

    static String getArgumentName(Parameter parameter) {
        SSCArgument argument = parameter.getAnnotation(SSCArgument.class);
        return argument == null ? null : argument.value();
    }

    static AliasInfo getAliasInfo(Class<?> clazz) {
        SSCAlias alias = clazz.getAnnotation(SSCAlias.class);
        return alias == null ? null : new AliasInfo(alias.value());
    }

    static PermissionInfo getPermissionInfo(AnnotatedElement element) {
        SSCPermission permission = element.getAnnotation(SSCPermission.class);
        return permission == null ? null : new PermissionInfo(permission.permission(), permission.custom(),
                permission.propagate(), permission.value());
    }

    static boolean isDebugOnly(Class<?> clazz) {
        return clazz.isAnnotationPresent(SSECDebug.class);
    }

    record CommandInfo(String value, Class<?> parent) {
    }

    record ExecuteInfo(String path) {
    }

    record AliasInfo(String[] values) {
    }

    record PermissionInfo(PermissionLevel level, Class<? extends SSCPermissionChecker> customChecker,
                                 boolean propagate, String[] nodes) {
    }
}

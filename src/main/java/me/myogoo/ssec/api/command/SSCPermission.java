package me.myogoo.ssec.api.command;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import me.myogoo.ssec.api.command.permission.PermissionLevel;
import me.myogoo.ssec.api.command.permission.SSCPermissionChecker;

/**
 * Annotation defining permissions required to execute a command or access
 * specific features.
 * 
 * <pre>
 * // Usage Example
 * &#64;SSCommand("admincmd")
 * &#64;SSCPermission(permission = PermissionLevel.ADMIN)
 * public class AdminCommand { ... }
 * 
 * &#64;SSCommand("pluginfeature")
 * &#64;SSCPermission(value = "ssec.admin")
 * public class PluginCommand { ... }
 * </pre>
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface SSCPermission {

    /**
     * LuckPerms/Fabric Permissions API permission node.
     * Example: "ssec.admin"
     */
    String[] value() default {};

    /**
     * Vanilla OP level. The default NONE denotes an "unspecified" state.
     */
    PermissionLevel permission() default PermissionLevel.NONE;

    /**
     * A custom dynamic permission checker class.
     */
    Class<? extends SSCPermissionChecker> custom() default SSCPermissionChecker.class;

    /**
     * If true, this permission requirement cascades to subcommands.
     * If false (default), it applies only to this command's execute.
     */
    boolean propagate() default false;
}

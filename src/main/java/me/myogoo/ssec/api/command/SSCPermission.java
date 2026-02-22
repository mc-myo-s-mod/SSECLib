package me.myogoo.ssec.api.command;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface SSCPermission {

    String[] value() default {};

    /**
     * The vanilla operator level required to bypass other checks.
     * Default is 2 (typical for commands).
     */
    int level() default 2;

    /**
     * A custom dynamic permission checker class.
     */
    Class<? extends SSCPermissionChecker> custom() default SSCPermissionChecker.class;
}

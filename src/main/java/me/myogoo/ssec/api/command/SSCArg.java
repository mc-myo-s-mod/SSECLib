package me.myogoo.ssec.api.command;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a method parameter should be parsed as a command argument.
 * The type of the argument will be automatically inferred from the parameter's
 * Java type.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface SSCArg {
    /**
     * The name of the argument as it will appear in the command tree.
     */
    String value();
}

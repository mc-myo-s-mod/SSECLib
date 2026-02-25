package me.myogoo.ssec.api.command;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for registering a method as a command.
 * <p>
 * Methods annotated with this will be registered into the system as an
 * executable command.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SSCExecute {
    /**
     * Specifies the name (path) of the command.
     * If not specified, default behavior will be executed.
     *
     * @return the name of the command
     */
    String value() default "";
}

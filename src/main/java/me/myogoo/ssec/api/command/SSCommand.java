package me.myogoo.ssec.api.command;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to define a command or a subcommand.
 *
 * <pre>
 * // Usage Example: Root Command
 * &#64;SSCommand("test")
 * public class TestCommand {
 *     &#64;SSCExecute
 *     public static void run(CommandContext&lt;CommandSourceStack&gt; context) {
 *         // ...
 *     }
 * }
 *
 * // Usage Example: Sub Command
 * &#64;SSCommand(value = "sub", parent = TestCommand.class)
 * public class TestSubCommand {
 *     // ...
 * }
 * </pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface SSCommand {
    /**
     * @return The name of the command (e.g., "test" for /test)
     */
    String value();

    /**
     * @return The parent command class. If not specified, it is treated as a root
     *         command.
     */
    Class<?> parent() default void.class;
}

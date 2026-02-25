package me.myogoo.ssec.api.command;

//use redirect
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to define aliases for a command or subcommand.
 * Uses redirect under the hood.
 *
 * <b>Path Rules</b>
 * <ul>
 * <li>Starts with {@code "/"} → <b>absolute path</b> (from root)</li>
 * <li>Does not start with {@code "/"} → <b>relative path</b> (relative to
 * parent)</li>
 * <li>Uses {@code "/"} as path separator</li>
 * <li>Throws if the root command referenced by an absolute path does not
 * exist</li>
 * </ul>
 *
 * <pre>
 * // 1. Simple root alias: /tp, /goto → equivalent to /teleport
 * &#64;SSCommand("teleport")
 * &#64;SSCAlias({ "tp", "goto" })
 * public class TeleportCommand { ... }
 *
 * // 2. Absolute path alias: "/newroot/sub" → accessible via /newroot sub
 * //    (newroot command must already exist)
 * &#64;SSCommand(value = "subcmd", parent = MyRootCommand.class)
 * &#64;SSCAlias("/newroot/sub")
 * public class MySubCommand { ... }
 *
 * // 3. Relative path alias: when parent is "test"
 * //    "asd/fgh" → /test asd fgh
 * &#64;SSCommand(value = "subcmd", parent = TestCommand.class)
 * &#64;SSCAlias("asd/fgh")
 * public class MySubCommand2 { ... }
 *
 * // 4. Simple relative alias: when parent is "test"
 * //    "asd" → /test asd
 * &#64;SSCommand(value = "subcmd", parent = TestCommand.class)
 * &#64;SSCAlias("asd")
 * public class MySubCommand3 { ... }
 * </pre>
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface SSCAlias {
    /**
     * @return An array of alias strings for the command
     */
    String[] value();
}

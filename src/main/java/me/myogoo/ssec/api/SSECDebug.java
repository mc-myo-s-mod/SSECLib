package me.myogoo.ssec.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Event classes or command classes annotated with this annotation
 * are only active in a development environment
 * (FabricLoader.isDevelopmentEnvironment()).
 *
 * <p>
 * If applied at the class level, the entire class becomes dev-mode only.
 * If applied at the method level, only that specific event handler becomes
 * dev-mode only.
 * </p>
 *
 * <pre>
 * // Designate the entire class as development mode exclusively
 * &#64;SSECDebug
 * public class DebugEvents {
 *     &#64;SSEvent(ServerLifecycleEvents.ServerStarting.class)
 *     public static void onServerStart(MinecraftServer server) { ... }
 * }
 *
 * // Designate only specific methods for development mode
 * public class MixedEvents {
 *     &#64;SSECDebug
 *     &#64;SSEvent(ServerLifecycleEvents.ServerStarting.class)
 *     public static void debugOnlyEvent(MinecraftServer server) { ... }
 *
 *     &#64;SSEvent(ServerLifecycleEvents.ServerStarted.class)
 *     public static void alwaysActiveEvent(MinecraftServer server) { ... }
 * }
 *
 * // Designate a command exclusively for development mode
 * &#64;SSECDebug
 * &#64;SSCommand("debugcmd")
 * public class DebugCommand { ... }
 * </pre>
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface SSECDebug {
}
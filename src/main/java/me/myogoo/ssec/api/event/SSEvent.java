package me.myogoo.ssec.api.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation instructing the framework to register the specified method
 * as a listener for a target Fabric event.
 *
 * <pre>
 * // Usage Example
 * public class MyEvents {
 *     &#64;SSEvent(ServerLifecycleEvents.ServerStarting.class)
 *     public static void onServerStart(MinecraftServer server) {
 *         // ...
 *     }
 *
 *     &#64;SSEvent(value = ServerChunkEvents.Load.class, priority = 10)
 *     public void onChunkLoad(ServerLevel world, LevelChunk chunk) {
 *         // ...
 *     }
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SSEvent {
    /**
     * @return The target Fabric event callback interface to register
     *         (e.g., ServerLifecycleEvents.ServerStarting.class)
     */
    Class<?> value();

    /**
     * Event priority. Lower numbers are executed first.
     *
     * @return The priority (default: 0)
     */
    int priority() default 0;
}

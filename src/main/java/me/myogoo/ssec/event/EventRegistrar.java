package me.myogoo.ssec.event;

import me.myogoo.ssec.api.SSECDebug;
import me.myogoo.ssec.api.event.SSEvent;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Registrar that automatically registers methods annotated with {@link SSEvent}
 * to the Fabric Event system.
 *
 * <p>
 * Supports both static and instance methods,
 * determining the registration order based on the {@code priority} value.
 * </p>
 */
public class EventRegistrar {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventRegistrar.class);

    /**
     * Scans and registers events from static methods of a class only.
     * Used when instance creation is unnecessary.
     *
     * @param clazz The class to process events from
     */
    public static void registerStatic(Class<?> clazz) {
        List<MethodEntry> entries = collectMethods(clazz);
        entries.sort(Comparator.comparingInt(e -> e.priority));

        for (MethodEntry entry : entries) {
            if (!Modifier.isStatic(entry.method.getModifiers())) {
                LOGGER.warn("[SSEC] Skipping non-static method {} in static-only registration for {}",
                        entry.method.getName(), clazz.getSimpleName());
                continue;
            }
            try {
                registerEvent(null, entry.method, entry.listenerClass);
                LOGGER.debug("[SSEC] Registered static event: {}.{} -> {}",
                        clazz.getSimpleName(), entry.method.getName(), entry.listenerClass.getSimpleName());
            } catch (Exception e) {
                LOGGER.error("[SSEC] Failed to register static event for method: {}.{}",
                        clazz.getSimpleName(), entry.method.getName(), e);
            }
        }
    }

    /**
     * Inspects methods of a given object and automatically registers methods
     * annotated with {@link SSEvent} to the Fabric Event system.
     * <p>
     * Both static and instance methods are registered.
     * </p>
     *
     * @param target The object handling the events
     */
    public static void register(Object target) {
        Class<?> targetClass = target.getClass();
        List<MethodEntry> entries = collectMethods(targetClass);
        entries.sort(Comparator.comparingInt(e -> e.priority));

        for (MethodEntry entry : entries) {
            try {
                Object invokeTarget = Modifier.isStatic(entry.method.getModifiers()) ? null : target;
                registerEvent(invokeTarget, entry.method, entry.listenerClass);
                LOGGER.debug("[SSEC] Registered event: {}.{} -> {} (priority={})",
                        targetClass.getSimpleName(), entry.method.getName(),
                        entry.listenerClass.getSimpleName(), entry.priority);
            } catch (Exception e) {
                LOGGER.error("[SSEC] Failed to register event for method: {}.{}",
                        targetClass.getSimpleName(), entry.method.getName(), e);
            }
        }
    }

    /**
     * Collects methods annotated with @SSEvent from the class.
     */
    private static List<MethodEntry> collectMethods(Class<?> clazz) {
        boolean isDev = FabricLoader.getInstance().isDevelopmentEnvironment();
        List<MethodEntry> entries = new ArrayList<>();
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(SSEvent.class)) {
                // Only register @SSECDebug methods in development mode
                if (method.isAnnotationPresent(SSECDebug.class) && !isDev) {
                    LOGGER.debug("[SSEC] Skipping debug event method: {}.{} (not in dev mode)",
                            clazz.getSimpleName(), method.getName());
                    continue;
                }
                SSEvent annotation = method.getAnnotation(SSEvent.class);
                entries.add(new MethodEntry(method, annotation.value(), annotation.priority()));
            }
        }
        return entries;
    }

    /**
     * Registers a single method as a proxy to a Fabric Event.
     *
     * @param target        The instance to call the method on (null if static)
     * @param method        The method to register
     * @param listenerClass The Fabric event callback interface
     */
    private static void registerEvent(Object target, Method method, Class<?> listenerClass) throws Exception {
        Event<?> eventInstance = findEventInstance(listenerClass);
        if (eventInstance == null) {
            throw new IllegalArgumentException(
                    "Cannot find Event<?> field for listener class: " + listenerClass.getName());
        }

        method.setAccessible(true);

        Object proxyInstance = Proxy.newProxyInstance(
                listenerClass.getClassLoader(),
                new Class<?>[] { listenerClass },
                (proxy, proxyMethod, args) -> {
                    // Delegate Object class methods as-is
                    if (proxyMethod.getDeclaringClass() == Object.class) {
                        if (target != null) {
                            return proxyMethod.invoke(target, args);
                        }
                        return proxyMethod.invoke(proxy, args);
                    }

                    if (method.getParameterCount() != proxyMethod.getParameterCount()) {
                        throw new IllegalArgumentException(
                                "Parameter count mismatch for event method " + method.getName()
                                        + ": expected " + proxyMethod.getParameterCount()
                                        + " but got " + method.getParameterCount());
                    }

                    return method.invoke(target, args);
                });

        @SuppressWarnings("unchecked")
        Event<Object> genericEvent = (Event<Object>) eventInstance;
        genericEvent.register(proxyInstance);
    }

    /**
     * Finds the corresponding {@link Event} instance for the given listener
     * interface.
     *
     * <p>
     * Search order:
     * </p>
     * <ol>
     * <li>The static {@code Event<?>} field of the listener class itself</li>
     * <li>An {@code Event<?>} field with a matching generic type in the listener's
     * enclosing class</li>
     * <li>An {@code Event<?>} field whose name matches the listener name in the
     * listener's enclosing class</li>
     * </ol>
     */
    private static Event<?> findEventInstance(Class<?> listenerClass) throws IllegalAccessException {
        // 1. Search for Event<?> field in the listener class itself
        for (Field field : listenerClass.getDeclaredFields()) {
            if (Event.class.isAssignableFrom(field.getType())) {
                field.setAccessible(true);
                return (Event<?>) field.get(null);
            }
        }

        // 2. Search in the Enclosing class (common Fabric pattern)
        Class<?> enclosingClass = listenerClass.getEnclosingClass();
        if (enclosingClass != null) {
            // 2a. Generic type matching
            for (Field field : enclosingClass.getDeclaredFields()) {
                if (Event.class.isAssignableFrom(field.getType())) {
                    Type genericType = field.getGenericType();
                    if (genericType instanceof ParameterizedType) {
                        ParameterizedType pt = (ParameterizedType) genericType;
                        Type[] typeArgs = pt.getActualTypeArguments();
                        if (typeArgs.length > 0 && typeArgs[0].equals(listenerClass)) {
                            field.setAccessible(true);
                            return (Event<?>) field.get(null);
                        }
                    }
                }
            }

            // 2b. Field name matching (fallback)
            for (Field field : enclosingClass.getDeclaredFields()) {
                if (Event.class.isAssignableFrom(field.getType())) {
                    String fieldNameNormalized = field.getName().toUpperCase().replace("_", "");
                    String listenerNameNormalized = listenerClass.getSimpleName().toUpperCase();
                    if (fieldNameNormalized.contains(listenerNameNormalized)) {
                        field.setAccessible(true);
                        return (Event<?>) field.get(null);
                    }
                }
            }
        }

        return null;
    }

    /**
     * Method entry (for priority sorting)
     */
    private static class MethodEntry {
        final Method method;
        final Class<?> listenerClass;
        final int priority;

        MethodEntry(Method method, Class<?> listenerClass, int priority) {
            this.method = method;
            this.listenerClass = listenerClass;
            this.priority = priority;
        }
    }
}

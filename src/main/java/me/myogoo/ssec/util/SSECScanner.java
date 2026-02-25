package me.myogoo.ssec.util;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import me.myogoo.ssec.api.SSECDebug;
import me.myogoo.ssec.api.command.SSCommand;
import me.myogoo.ssec.command.CommandRegistrar;
import me.myogoo.ssec.event.EventRegistrar;
import me.myogoo.ssec.api.event.SSEvent;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.util.LinkedHashSet;
import java.util.Set;

public class SSECScanner {

    private static final Logger LOGGER = LoggerFactory.getLogger(SSECScanner.class);

    private static final String DEFAULT_PACKAGE = "me.myogoo.ssec";

    private static final Set<String> packagesToScan = new LinkedHashSet<>();

    static {
        packagesToScan.add(DEFAULT_PACKAGE);
    }

    public static void addPackages(String[] packages) {
        for (String packageName : packages) {
            addPackage(packageName);
        }
    }

    /**
     * Adds a package to scan.
     * Must be called before initialize().
     *
     * @param packageName The package name to add (e.g., "com.example.mymod")
     */
    public static void addPackage(String packageName) {
        packagesToScan.add(packageName);
        LOGGER.info("[SSEC] Added package to scan: {}", packageName);
    }

    public static void initialize() {
        String[] packages = packagesToScan.toArray(new String[0]);
        LOGGER.info("[SSEC] Starting annotation scan in packages: {}", packagesToScan);

        try (ScanResult scanResult = new ClassGraph()
                .enableAllInfo()
                .acceptPackages(packages)
                .scan()) {

            registerEvents(scanResult);
            registerCommands(scanResult);

        } catch (Exception e) {
            LOGGER.error("[SSEC] Failed during annotation scan", e);
        }

        LOGGER.info("[SSEC] Annotation scan completed.");
    }

    private static void registerEvents(ScanResult scanResult) {
        LOGGER.info("[SSEC] Scanning for @SSEvent...");
        int count = 0;

        boolean isDev = FabricLoader.getInstance().isDevelopmentEnvironment();

        for (ClassInfo classInfo : scanResult.getClassesWithMethodAnnotation(SSEvent.class.getName())) {
            Class<?> clazz = classInfo.loadClass();

            // Exclude interfaces and abstract classes
            if (clazz.isInterface() || java.lang.reflect.Modifier.isAbstract(clazz.getModifiers())) {
                continue;
            }

            // Only register @SSECDebug classes in development mode
            if (clazz.isAnnotationPresent(SSECDebug.class) && !isDev) {
                LOGGER.debug("[SSEC] Skipping debug event class: {} (not in dev mode)", clazz.getSimpleName());
                continue;
            }

            try {
                // Determine if there are only static methods
                boolean hasInstanceMethods = false;
                for (java.lang.reflect.Method m : clazz.getDeclaredMethods()) {
                    if (m.isAnnotationPresent(SSEvent.class)
                            && !java.lang.reflect.Modifier.isStatic(m.getModifiers())) {
                        hasInstanceMethods = true;
                        break;
                    }
                }

                if (hasInstanceMethods) {
                    // Instance methods exist -> no-args constructor required
                    Constructor<?>[] constructors = clazz.getDeclaredConstructors();
                    boolean hasNoArgsConstructor = false;
                    for (Constructor<?> c : constructors) {
                        if (c.getParameterCount() == 0) {
                            hasNoArgsConstructor = true;
                            break;
                        }
                    }

                    if (hasNoArgsConstructor) {
                        Object instance = clazz.getDeclaredConstructor().newInstance();
                        EventRegistrar.register(instance);
                    } else {
                        LOGGER.warn("[SSEC] Event class {} has instance @SSEvent methods but no no-args constructor.",
                                clazz.getSimpleName());
                        // Register static methods at least
                        EventRegistrar.registerStatic(clazz);
                    }
                } else {
                    // Only static methods -> instance not required
                    EventRegistrar.registerStatic(clazz);
                }

                count++;
                LOGGER.info("[SSEC] Registered event class: {}", clazz.getSimpleName());
            } catch (Exception e) {
                LOGGER.error("[SSEC] Failed to register event class: " + clazz.getName(), e);
            }
        }

        LOGGER.info("[SSEC] Total {} event classes registered.", count);
    }

    private static void registerCommands(ScanResult scanResult) {
        LOGGER.info("[SSEC] Scanning for @SSCommand...");

        // 1. Collect all @SSCommand classes
        boolean isDev = FabricLoader.getInstance().isDevelopmentEnvironment();

        java.util.List<Class<?>> allCommandClasses = new java.util.ArrayList<>();
        for (ClassInfo classInfo : scanResult.getClassesWithAnnotation(SSCommand.class.getName())) {
            Class<?> clazz = classInfo.loadClass();

            // Only register @SSECDebug commands in development mode
            if (clazz.isAnnotationPresent(SSECDebug.class) && !isDev) {
                LOGGER.debug("[SSEC] Skipping debug command class: {} (not in dev mode)", clazz.getSimpleName());
                continue;
            }

            allCommandClasses.add(clazz);
        }

        LOGGER.info("[SSEC] Found {} total @SSCommand classes.", allCommandClasses.size());

        // 2. Filter and register only root commands (passing allCommandClasses
        // entirely)
        int count = 0;
        for (Class<?> clazz : allCommandClasses) {
            SSCommand annotation = clazz.getAnnotation(SSCommand.class);

            // Determine as a root command only if parent() is void.class (default)
            if (annotation.parent() == void.class) {
                CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
                    CommandRegistrar.register(dispatcher, clazz, allCommandClasses);
                });
                count++;
                LOGGER.info("[SSEC] Registered root command class: {}", clazz.getSimpleName());
            }
        }

        LOGGER.info("[SSEC] Total {} root command classes registered.", count);
    }
}

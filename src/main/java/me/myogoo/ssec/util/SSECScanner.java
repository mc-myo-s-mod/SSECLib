package me.myogoo.ssec.util;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import me.myogoo.ssec.api.command.SSCommand;
import me.myogoo.ssec.command.CommandRegistrar;
import me.myogoo.ssec.event.EventRegistrar;
import me.myogoo.ssec.api.event.SSEvent;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;

public class SSECScanner {

    private static final Logger LOGGER = LoggerFactory.getLogger(SSECScanner.class);

    private static final String DEFAULT_PACKAGE_TO_SCAN = "me.myogoo.ssec";

    public static void initialize() {
        initialize(DEFAULT_PACKAGE_TO_SCAN);
    }

    public static void initialize(String packageToScan) {
        LOGGER.info("[SSEC] Starting annotation scan in package: {}", packageToScan);

        try (ScanResult scanResult = new ClassGraph()
                .enableAllInfo()
                .acceptPackages(packageToScan)
                .scan()) {

            registerEvents(scanResult);
            registerCommands(scanResult);

        } catch (Exception e) {
            LOGGER.error("[SSEC] Failed during annotation scan", e);
        }

        LOGGER.info("[SSEC] Annotation scan completed.");
    }

    private static void registerEvents(ScanResult scanResult) {
        LOGGER.info("[SSEC] Scanning for @SSECEvent...");
        int count = 0;

        for (ClassInfo classInfo : scanResult.getClassesWithMethodAnnotation(SSEvent.class.getName())) {
            Class<?> clazz = classInfo.loadClass();

            // 인스턴스화가 가능한지 확인 (인터페이스, 추상 클래스 등 제외)
            if (clazz.isInterface() || java.lang.reflect.Modifier.isAbstract(clazz.getModifiers())) {
                continue;
            }

            try {
                // 기본 생성자가 있는지 확인
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
                    count++;
                    LOGGER.info("[SSEC] Registered event class: {}", clazz.getSimpleName());
                } else {
                    LOGGER.warn("[SSEC] Event class {} must have a no-args constructor.", clazz.getSimpleName());
                }
            } catch (Exception e) {
                LOGGER.error("[SSEC] Failed to register event class: " + clazz.getName(), e);
            }
        }

        LOGGER.info("[SSEC] Total {} event classes registered.", count);
    }

    private static void registerCommands(ScanResult scanResult) {
        LOGGER.info("[SSEC] Scanning for @SSCommand...");

        // 1. 모든 @SSCommand 클래스를 수집
        java.util.List<Class<?>> allCommandClasses = new java.util.ArrayList<>();
        for (ClassInfo classInfo : scanResult.getClassesWithAnnotation(SSCommand.class.getName())) {
            allCommandClasses.add(classInfo.loadClass());
        }

        LOGGER.info("[SSEC] Found {} total @SSCommand classes.", allCommandClasses.size());

        // 2. root 커맨드만 필터링하여 등록 (allCommandClasses 전체를 함께 전달)
        int count = 0;
        for (Class<?> clazz : allCommandClasses) {
            SSCommand annotation = clazz.getAnnotation(SSCommand.class);

            // parent()가 void.class (기본값)인 경우만 최상위 커맨드로 판단하여 등록
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

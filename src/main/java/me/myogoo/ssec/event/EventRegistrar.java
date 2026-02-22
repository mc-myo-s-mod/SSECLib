package me.myogoo.ssec.event;

import me.myogoo.ssec.api.event.SSECEvent;
import net.fabricmc.fabric.api.event.Event;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.lang.reflect.ParameterizedType;

public class EventRegistrar {

    /**
     * 특정 객체의 메소드를 탐색하여 @SSECEvent 어노테이션이 붙은 메소드를
     * Fabric Event 시스템에 자동으로 등록합니다.
     *
     * @param target 이벤트를 처리할 객체
     */
    public static void register(Object target) {
        Class<?> targetClass = target.getClass();

        for (Method method : targetClass.getDeclaredMethods()) {
            if (method.isAnnotationPresent(SSECEvent.class)) {
                SSECEvent annotation = method.getAnnotation(SSECEvent.class);
                Class<?> listenerClass = annotation.value();

                try {
                    registerEvent(target, method, listenerClass);
                } catch (Exception e) {
                    System.err.println("Failed to register event for method: " + method.getName() + " in class "
                            + targetClass.getName());
                    e.printStackTrace();
                }
            }
        }
    }

    private static void registerEvent(Object target, Method method, Class<?> listenerClass) throws Exception {
        Event<?> eventInstance = findEventInstance(listenerClass);
        if (eventInstance == null) {
            throw new IllegalArgumentException(
                    "Cannot find Event<?> field for listener class: " + listenerClass.getName());
        }

        Object proxyInstance = Proxy.newProxyInstance(
                listenerClass.getClassLoader(),
                new Class<?>[] { listenerClass },
                (proxy, proxyMethod, args) -> {
                    if (proxyMethod.getDeclaringClass() == Object.class) {
                        return proxyMethod.invoke(target, args);
                    }

                    method.setAccessible(true);

                    if (method.getParameterCount() != proxyMethod.getParameterCount()) {
                        throw new IllegalArgumentException(
                                "Parameter count mismatch for event method " + method.getName());
                    }

                    return method.invoke(target, args);
                });

        @SuppressWarnings("unchecked")
        Event<Object> genericEvent = (Event<Object>) eventInstance;
        genericEvent.register(proxyInstance);
    }

    private static Event<?> findEventInstance(Class<?> listenerClass) throws IllegalAccessException {
        for (Field field : listenerClass.getDeclaredFields()) {
            if (Event.class.isAssignableFrom(field.getType())) {
                field.setAccessible(true);
                return (Event<?>) field.get(null);
            }
        }

        Class<?> enclosingClass = listenerClass.getEnclosingClass();
        if (enclosingClass != null) {
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
                    } else if (field.getName().toUpperCase().replace("_", "")
                            .contains(listenerClass.getSimpleName().toUpperCase())) {
                        field.setAccessible(true);
                        return (Event<?>) field.get(null);
                    }
                }
            }
        }

        return null;
    }
}

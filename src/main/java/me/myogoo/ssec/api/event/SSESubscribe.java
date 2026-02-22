package me.myogoo.ssec.api.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SSESubscribe {
    /**
     * @return 등록할 주 대상인 Fabric의 인터페이스 (예:
     *         ServerLifecycleEvents.ServerStarting.class)
     */
    Class<?> value();
}

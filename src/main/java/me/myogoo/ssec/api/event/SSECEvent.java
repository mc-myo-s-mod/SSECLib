package me.myogoo.ssec.api.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 프레임워크가 특정 메소드를 Fabric 타겟 이벤트의 리스너로 등록하도록 지시하는 어노테이션입니다.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SSECEvent {
    /**
     * @return 등록할 주 대상인 Fabric의 인터페이스 (예:
     *         ServerLifecycleEvents.ServerStarting.class)
     */
    Class<?> value();
}

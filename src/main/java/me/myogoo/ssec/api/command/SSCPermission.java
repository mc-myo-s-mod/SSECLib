package me.myogoo.ssec.api.command;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface SSCPermission {

    /**
     * LuckPerms/Fabric Permissions API 권한 노드.
     * 예: "ssec.admin"
     */
    String[] value() default {};

    /**
     * Vanilla OP 레벨. 기본값 NONE은 "미지정" 상태.
     */
    PermissionLevel level() default PermissionLevel.NONE;

    /**
     * 커스텀 동적 퍼미션 체커 클래스.
     */
    Class<? extends SSCPermissionChecker> custom() default SSCPermissionChecker.class;
}

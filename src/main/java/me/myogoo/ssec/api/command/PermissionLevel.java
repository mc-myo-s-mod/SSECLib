package me.myogoo.ssec.api.command;

/**
 * 마인크래프트 서버의 OP(Operator) 권한 레벨을 정의합니다.
 */
public enum PermissionLevel {
    /** 기본값 (권한 체크 안 함) */
    NONE(0),
    /** 스폰 보호 우회 가능 */
    MODERATOR(1),
    /** 치트 커맨드 사용 가능 (gamemode, give 등) - 기본값 */
    GAME_MASTER(2),
    /** 멀티플레이어 관리 (kick, ban, op 등) */
    ADMIN(3),
    /** 서버 관리 (stop, save 등) */
    OWNER(4);

    private final int level;

    PermissionLevel(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }
}

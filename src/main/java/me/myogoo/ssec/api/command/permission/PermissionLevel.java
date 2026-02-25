package me.myogoo.ssec.api.command.permission;

import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.Permissions;

public enum PermissionLevel {
    NONE(null),
    MODERATOR(Permissions.COMMANDS_MODERATOR),
    GAME_MASTER(Permissions.COMMANDS_GAMEMASTER),
    ADMIN(Permissions.COMMANDS_ADMIN),
    OWNER(Permissions.COMMANDS_OWNER);

    private final Permission permission;

    PermissionLevel(Permission permission) {
        this.permission = permission;
    }

    public Permission getPermission() {
        return permission;
    }
}

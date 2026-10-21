package me.myogoo.ssec.api.command.permission;

import net.minecraft.commands.CommandSourceStack;

public enum PermissionLevel {
    NONE(0), MODERATOR(1), GAME_MASTER(2), ADMIN(3), OWNER(4);

    private final int level;

    PermissionLevel(int level) { this.level = level; }

    public boolean check(CommandSourceStack source) { return this == NONE || source.hasPermission(level); }
}

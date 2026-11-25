package me.myogoo.ssec.command;

import me.myogoo.ssec.api.command.permission.PermissionLevel;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.permissions.Permissions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PermissionLevelTest {
    @Test
    void eachLevelChecksItsMinecraftPermission() {
        var source = mock(CommandSourceStack.class, RETURNS_DEEP_STUBS);
        var levels = new PermissionLevel[]{
                PermissionLevel.MODERATOR, PermissionLevel.GAME_MASTER, PermissionLevel.ADMIN, PermissionLevel.OWNER
        };
        var permissions = java.util.List.of(Permissions.COMMANDS_MODERATOR, Permissions.COMMANDS_GAMEMASTER,
                Permissions.COMMANDS_ADMIN, Permissions.COMMANDS_OWNER);
        assertTrue(PermissionLevel.NONE.check(source));
        for (int granted = 0; granted < levels.length; granted++) {
            when(source.permissions().hasPermission(permissions.get(granted))).thenReturn(true);
            for (int i = 0; i < levels.length; i++) {
                assertEquals(i == granted, levels[i].check(source), levels[i].name());
            }
            when(source.permissions().hasPermission(permissions.get(granted))).thenReturn(false);
        }
    }
}

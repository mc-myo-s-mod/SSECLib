package me.myogoo.ssec.command;

import me.myogoo.ssec.api.command.permission.PermissionLevel;
import net.minecraft.commands.CommandSourceStack;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PermissionLevelTest {
    @Test
    void eachLevelChecksItsMinecraftPermission() {
        var source = mock(CommandSourceStack.class);
        var levels = new PermissionLevel[]{
                PermissionLevel.MODERATOR, PermissionLevel.GAME_MASTER, PermissionLevel.ADMIN, PermissionLevel.OWNER
        };
        assertTrue(PermissionLevel.NONE.check(source));
        for (int granted = 0; granted < levels.length; granted++) {
            when(source.hasPermission(granted + 1)).thenReturn(true);
            for (int i = 0; i < levels.length; i++) {
                assertEquals(i == granted, levels[i].check(source), levels[i].name());
            }
            when(source.hasPermission(granted + 1)).thenReturn(false);
        }
    }
}

package me.myogoo.ssec.api.command;

import net.minecraft.commands.CommandSourceStack;

/**
 * Interface for custom permission checks in @SSCPermission annotations.
 */
public interface SSCPermissionChecker {
    /**
     * Checks if the given CommandSourceStack satisfies the custom condition.
     * 
     * @param source The command source stack
     * @return true if permission is granted, false otherwise.
     */
    boolean check(CommandSourceStack source);
}

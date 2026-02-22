package me.myogoo.ssec.event.test;

import me.myogoo.ssec.api.event.SSEvent;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.Commands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestEvent {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestEvent.class);

    @SSEvent(ServerLifecycleEvents.ServerStarting.class)
    public static void onServerStart(MinecraftServer server) {
        LOGGER.debug("[SSEC] Super Sexy Event Lib - Server is starting! (Registered via Annotation)");
    }

    @SSEvent(CommandRegistrationCallback.class)
    public void onCommandRegister(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess,
            Commands.CommandSelection environment) {
        LOGGER.debug(
                "[SSEC] Super Sexy Event Lib - Command registration event triggered! (Registered via Annotation)");
    }
}

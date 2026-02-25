package me.myogoo.ssec.event.test;

import me.myogoo.ssec.api.SSECDebug;
import me.myogoo.ssec.api.event.SSEvent;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test event class for @SSEvent annotation.
 * Since only static methods are used, it is registered without instance
 * creation.
 */
@SSECDebug
public class TestEvent {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestEvent.class);

    @SSEvent(ServerLifecycleEvents.ServerStarted.class)
    public static void onServerStart(MinecraftServer server) {
        LOGGER.debug("[SSEC] Super Sexy Event Lib - Server is starting! (Registered via @SSEvent)");
    }

    @SSEvent(value = ServerLifecycleEvents.ServerStarted.class, priority = 10)
    public static void onServerStarted(MinecraftServer server) {
        LOGGER.debug("[SSEC] Super Sexy Event Lib - Server started! (priority=10, Registered via @SSEvent)");
    }
}

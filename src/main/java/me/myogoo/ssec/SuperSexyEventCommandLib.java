package me.myogoo.ssec;

import me.myogoo.ssec.api.SSECInitializer;
import me.myogoo.ssec.util.SSECScanner;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SuperSexyEventCommandLib implements ModInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(SuperSexyEventCommandLib.class);

    @Override
    public void onInitialize() {
        // Load "ssec" entrypoints from other mods to register packages
        for (SSECInitializer initializer : FabricLoader.getInstance()
                .getEntrypoints("ssec", SSECInitializer.class)) {
            try {
                LOGGER.info("[SSEC] Loading entrypoint: {}", initializer.getClass().getName());
                initializer.onInitializeSSEC();
                SSECScanner.addPackages(initializer.getPackagesToScan());
            } catch (Exception e) {
                LOGGER.error("[SSEC] Failed to load entrypoint: {}", initializer.getClass().getName(), e);
            }
        }

        // Execute scan after all packages are registered
        SSECScanner.initialize();
    }
}

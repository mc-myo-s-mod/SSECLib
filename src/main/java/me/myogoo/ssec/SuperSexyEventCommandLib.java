package me.myogoo.ssec;

import me.myogoo.ssec.util.SSECScanner;
import net.fabricmc.api.ModInitializer;

public class SuperSexyEventCommandLib implements ModInitializer {

    @Override
    public void onInitialize() {
        SSECScanner.initialize();
    }
}

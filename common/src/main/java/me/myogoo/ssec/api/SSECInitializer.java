package me.myogoo.ssec.api;

/**
 * A custom Fabric entrypoint interface for SSECLib.
 * <p>
 * If you implement this interface in another mod and register it under the
 * "ssec" key in the entrypoints of your fabric.mod.json, it will be called
 * automatically during SSECLib initialization.
 * </p>
 *
 * <pre>
 * // Usage Example
 * public class MyModSSEC implements SSECInitializer {
 *     &#64;Override
 *     public void onInitializeSSEC() {
 *         SSECScanner.addPackage("com.example.mymod");
 *     }
 * }
 * </pre>
 */
public interface SSECInitializer {

    /**
     * Called when SSECLib initializes.
     * Register packages to scan using {@code SSECScanner.addPackage()} here.
     */
    void onInitializeSSEC();

    String[] getPackagesToScan();
}

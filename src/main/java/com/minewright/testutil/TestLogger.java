package com.minewright.testutil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test-friendly logger provider that doesn't require Minecraft Forge initialization.
 *
 * <p>This class provides a way to get SLF4J loggers without triggering the
 * initialization of {@code MineWrightMod}, which requires Minecraft's registries
 * to be bootstrapped. Use this in test code and in production code that needs to
 * log before Minecraft is fully initialized.</p>
 *
 * <p><b>Usage in production code:</b></p>
 * <pre>{@code
 * private static final Logger LOGGER = TestLogger.getLogger(MyClass.class);
 * }</pre>
 *
 * <p><b>Usage in tests:</b></p>
 * <pre>{@code
 * @BeforeEach
 * void setUp() {
 *     TestLogger.initForTesting();
 *     // ... test setup
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
public final class TestLogger {

    private TestLogger() {
        // Utility class - prevent instantiation
    }

    /**
     * Gets a logger for the given class.
     *
     * <p>This method creates a standard SLF4J logger without touching
     * Minecraft Forge classes. Use this instead of {@code LogUtils.getLogger()}
     * in code that might run before Minecraft is initialized or in tests.</p>
     *
     * @param clazz The class to create a logger for
     * @return A SLF4J logger instance
     */
    public static Logger getLogger(Class<?> clazz) {
        return LoggerFactory.getLogger(clazz);
    }

    /**
     * Gets a logger with the given name.
     *
     * @param name The name for the logger
     * @return A SLF4J logger instance
     */
    public static Logger getLogger(String name) {
        return LoggerFactory.getLogger(name);
    }

    /**
     * Initializes logging for test environments.
     *
     * <p>This method can be called in test setup to ensure logging is properly
     * configured. It's safe to call multiple times.</p>
     *
     * <p>In most cases, you don't need to call this explicitly - the first
     * call to {@link #getLogger(Class)} will initialize logging automatically.
     * However, calling this explicitly can make test intent clearer.</p>
     */
    public static void initForTesting() {
        // Logger initialization is handled by SLF4J automatically
        // This method exists for explicit intent signaling in tests
    }
}

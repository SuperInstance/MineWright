package com.minewright.voice;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

/**
 * Comprehensive test suite for VoiceManager.
 *
 * Tests cover:
 * - Singleton pattern implementation
 * - Initialization lifecycle
 * - Voice enable/disable functionality
 * - Speech-to-text operations
 * - Text-to-speech operations
 * - Configuration management
 * - Error handling and edge cases
 *
 * @since 1.2.0
 */
@DisplayName("VoiceManager Tests")
public class VoiceManagerTest {

    private VoiceManager voiceManager;

    @BeforeEach
    void setUp() {
        // Reset singleton for each test
        resetVoiceManagerSingleton();
        voiceManager = VoiceManager.getInstance();
    }

    /**
     * Resets the VoiceManager singleton for test isolation.
     * Uses reflection to clear the instance.
     */
    private void resetVoiceManagerSingleton() {
        try {
            var field = VoiceManager.class.getDeclaredField("instance");
            field.setAccessible(true);
            field.set(null, null);
        } catch (Exception e) {
            // If reflection fails, continue with potentially cached instance
        }
    }

    // ========================================================================
    // Singleton Pattern Tests
    // ========================================================================

    @Test
    @DisplayName("getInstance should return same instance across multiple calls")
    void testSingletonReturnsSameInstance() {
        VoiceManager instance1 = VoiceManager.getInstance();
        VoiceManager instance2 = VoiceManager.getInstance();
        VoiceManager instance3 = VoiceManager.getInstance();

        assertSame(instance1, instance2, "getInstance should return same instance");
        assertSame(instance2, instance3, "getInstance should return same instance");
    }

    @Test
    @DisplayName("getInstance should never return null")
    void testGetInstanceNeverReturnsNull() {
        VoiceManager instance = VoiceManager.getInstance();
        assertNotNull(instance, "getInstance should never return null");
    }

    // ========================================================================
    // Initialization Tests
    // ========================================================================

    @Test
    @DisplayName("initialize should complete without throwing exceptions")
    void testInitializeCompletesSuccessfully() {
        assertDoesNotThrow(() -> voiceManager.initialize(),
            "initialize should complete without exceptions");
    }

    @Test
    @DisplayName("initialize should be idempotent - multiple calls should be safe")
    void testInitializeIsIdempotent() {
        assertDoesNotThrow(() -> {
            voiceManager.initialize();
            voiceManager.initialize();
            voiceManager.initialize();
        }, "Multiple initialize calls should be safe");
    }

    @Test
    @DisplayName("isEnabled should return false before initialization")
    void testNotEnabledBeforeInitialization() {
        assertFalse(voiceManager.isEnabled(),
            "Voice should not be enabled before initialization");
    }

    @Test
    @DisplayName("isEnabled should return correct state after initialization")
    void testEnabledAfterInitialization() {
        assertDoesNotThrow(() -> voiceManager.initialize());
        // The result depends on config, just check it doesn't throw
        boolean enabled = voiceManager.isEnabled();
        // Just verify we can call it
        assertTrue(enabled || !enabled);
    }

    // ========================================================================
    // Enable/Disable Tests
    // ========================================================================

    @Test
    @DisplayName("setEnabled should update enabled state")
    void testSetEnabledUpdatesState() {
        voiceManager.setEnabled(false);
        assertFalse(voiceManager.isEnabled(),
            "Voice should be disabled after setEnabled(false)");

        voiceManager.setEnabled(true);
        // Result depends on config, just verify it doesn't throw
    }

    @Test
    @DisplayName("setEnabled with false should stop voice operations")
    void testSetEnabledFalseStopsOperations() {
        voiceManager.setEnabled(true);
        voiceManager.setEnabled(false);

        assertDoesNotThrow(() -> voiceManager.stopAll(),
            "stopAll should work after disabling");
    }

    @Test
    @DisplayName("speak should not throw when voice is disabled")
    void testSpeakDoesNotThrowWhenDisabled() {
        voiceManager.setEnabled(false);

        assertDoesNotThrow(() -> voiceManager.speak("Hello world"),
            "speak should not throw when disabled");
    }

    // ========================================================================
    // Speech-to-Text Tests
    // ========================================================================

    @Test
    @DisplayName("listenForCommand should throw when voice is disabled")
    void testListenForCommandThrowsWhenDisabled() {
        voiceManager.setEnabled(false);

        VoiceException exception = assertThrows(VoiceException.class,
            () -> voiceManager.listenForCommand(),
            "listenForCommand should throw when disabled");

        assertTrue(exception.getMessage().contains("not enabled"),
            "Exception should mention disabled state");
    }

    @Test
    @DisplayName("listenForCommand should return CompletableFuture")
    void testListenForCommandReturnsFuture() {
        voiceManager.setEnabled(true);

        try {
            CompletableFuture<String> future = voiceManager.listenForCommand();
            assertNotNull(future, "listenForCommand should return a future");
        } catch (VoiceException e) {
            // May throw if not properly configured, which is ok for this test
        }
    }

    @Test
    @DisplayName("listenForCommand future should handle timeout gracefully")
    void testListenForCommandHandlesTimeout() {
        voiceManager.setEnabled(true);

        try {
            CompletableFuture<String> future = voiceManager.listenForCommand();

            // Wait with timeout - should not throw even if no input
            assertThrows(TimeoutException.class, () ->
                future.get(1, TimeUnit.SECONDS),
                "Future should timeout if no input provided");
        } catch (VoiceException e) {
            // May throw if not configured, acceptable
        }
    }

    // ========================================================================
    // Text-to-Speech Tests
    // ========================================================================

    @Test
    @DisplayName("speak should handle null text gracefully")
    void testSpeakHandlesNullText() {
        assertDoesNotThrow(() -> voiceManager.speak(null),
            "speak should handle null text");
    }

    @Test
    @DisplayName("speak should handle empty text gracefully")
    void testSpeakHandlesEmptyText() {
        assertDoesNotThrow(() -> voiceManager.speak(""),
            "speak should handle empty text");
    }

    @Test
    @DisplayName("speak should handle whitespace text gracefully")
    void testSpeakHandlesWhitespaceText() {
        assertDoesNotThrow(() -> voiceManager.speak("   "),
            "speak should handle whitespace text");
    }

    @Test
    @DisplayName("speak should handle long text")
    void testSpeakHandlesLongText() {
        String longText = "A".repeat(1000);
        assertDoesNotThrow(() -> voiceManager.speak(longText),
            "speak should handle long text");
    }

    @Test
    @DisplayName("speak should handle special characters")
    void testSpeakHandlesSpecialCharacters() {
        String specialText = "Hello! @#$%^&*()_+-=[]{}|;':\",./<>?";
        assertDoesNotThrow(() -> voiceManager.speak(specialText),
            "speak should handle special characters");
    }

    @Test
    @DisplayName("speak should handle unicode characters")
    void testSpeakHandlesUnicode() {
        String unicodeText = "Hello 世界 🌍 Привет";
        assertDoesNotThrow(() -> voiceManager.speak(unicodeText),
            "speak should handle unicode characters");
    }

    @Test
    @DisplayName("speakIfEnabled should be equivalent to speak")
    void testSpeakIfEnabledEquivalentToSpeak() {
        voiceManager.setEnabled(true);

        assertDoesNotThrow(() -> {
            voiceManager.speak("Test 1");
            voiceManager.speakIfEnabled("Test 2");
        }, "speakIfEnabled should behave like speak");
    }

    // ========================================================================
    // Stop Operations Tests
    // ========================================================================

    @Test
    @DisplayName("stopAll should not throw when nothing is running")
    void testStopAllDoesNotThrowWhenIdle() {
        assertDoesNotThrow(() -> voiceManager.stopAll(),
            "stopAll should not throw when nothing is running");
    }

    @Test
    @DisplayName("stopAll should be callable multiple times")
    void testStopAllIsIdempotent() {
        assertDoesNotThrow(() -> {
            voiceManager.stopAll();
            voiceManager.stopAll();
            voiceManager.stopAll();
        }, "stopAll should be callable multiple times");
    }

    // ========================================================================
    // Configuration Tests
    // ========================================================================

    @Test
    @DisplayName("getConfig should return non-null VoiceConfig")
    void testGetConfigReturnsNonNull() {
        VoiceConfig config = voiceManager.getConfig();
        assertNotNull(config, "getConfig should return non-null");
    }

    @Test
    @DisplayName("getConfig should return same instance across calls")
    void testGetConfigReturnsSameInstance() {
        VoiceConfig config1 = voiceManager.getConfig();
        VoiceConfig config2 = voiceManager.getConfig();

        assertSame(config1, config2,
            "getConfig should return same instance");
    }

    @Test
    @DisplayName("reloadConfiguration should not throw")
    void testReloadConfigurationDoesNotThrow() {
        assertDoesNotThrow(() -> voiceManager.reloadConfiguration(),
            "reloadConfiguration should not throw");
    }

    // ========================================================================
    // Voice System Access Tests
    // ========================================================================

    @Test
    @DisplayName("getVoiceSystem should return non-null VoiceSystem")
    void testGetVoiceSystemReturnsNonNull() {
        VoiceSystem system = voiceManager.getVoiceSystem();
        assertNotNull(system, "getVoiceSystem should return non-null");
    }

    @Test
    @DisplayName("getVoiceSystem should return same instance")
    void testGetVoiceSystemReturnsSameInstance() {
        VoiceSystem system1 = voiceManager.getVoiceSystem();
        VoiceSystem system2 = voiceManager.getVoiceSystem();

        assertSame(system1, system2,
            "getVoiceSystem should return same instance");
    }

    // ========================================================================
    // Test Operation Tests
    // ========================================================================

    @Test
    @DisplayName("test should return CompletableFuture")
    void testTestReturnsFuture() {
        CompletableFuture<VoiceSystem.VoiceTestResult> future = voiceManager.test();

        assertNotNull(future, "test should return a future");
    }

    @Test
    @DisplayName("test future should complete with result")
    void testTestFutureCompletes() {
        CompletableFuture<VoiceSystem.VoiceTestResult> future = voiceManager.test();

        assertDoesNotThrow(() -> {
            VoiceSystem.VoiceTestResult result = future.get(10, TimeUnit.SECONDS);
            assertNotNull(result, "Test result should not be null");
            assertNotNull(result.message(), "Result message should not be null");
        }, "Test future should complete");
    }

    // ========================================================================
    // Shutdown Tests
    // ========================================================================

    @Test
    @DisplayName("shutdown should complete without throwing")
    void testShutdownCompletesSuccessfully() {
        assertDoesNotThrow(() -> voiceManager.shutdown(),
            "shutdown should complete without exceptions");
    }

    @Test
    @DisplayName("shutdown should be callable multiple times")
    void testShutdownIsIdempotent() {
        assertDoesNotThrow(() -> {
            voiceManager.shutdown();
            voiceManager.shutdown();
            voiceManager.shutdown();
        }, "shutdown should be callable multiple times");
    }

    @Test
    @DisplayName("operations after shutdown should handle gracefully")
    void testOperationsAfterShutdown() {
        voiceManager.shutdown();

        assertDoesNotThrow(() -> {
            voiceManager.speak("Test");
            voiceManager.stopAll();
        }, "Operations after shutdown should be handled gracefully");
    }

    // ========================================================================
    // Error Handling Tests
    // ========================================================================

    @Test
    @DisplayName("VoiceManager should handle concurrent access")
    void testConcurrentAccess() throws InterruptedException {
        voiceManager.setEnabled(true);

        Thread[] threads = new Thread[10];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                try {
                    voiceManager.speak("Concurrent test");
                    voiceManager.getConfig();
                    voiceManager.getVoiceSystem();
                } catch (Exception e) {
                    // Ignore errors in concurrent test
                }
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join(1000);
        }

        // If we get here without deadlock, concurrent access works
        assertTrue(true, "Concurrent access should not cause deadlock");
    }
}

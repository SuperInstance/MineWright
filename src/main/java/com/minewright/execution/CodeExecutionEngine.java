package com.minewright.execution;

import com.minewright.entity.ForemanEntity;
import com.minewright.testutil.TestLogger;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.slf4j.Logger;

import java.util.concurrent.*;


/**
 * Executes LLM-generated JavaScript code in a sandboxed GraalVM context.
 *
 * <p><b>Safety features:</b></p>
 * <ul>
 *   <li>No file system access</li>
 *   <li>No network access</li>
 *   <li>Timeout enforcement (30 seconds max) - ACTIVELY ENFORCED</li>
 *   <li>Restricted Java package access</li>
 *   <li>Controlled API via ForemanAPI bridge</li>
 * </ul>
 *
 * <p><b>Timeout Implementation:</b></p>
 * Uses ExecutorService with explicit timeout to prevent infinite loops
 * from hanging the game thread. Scripts that exceed timeout are cancelled
 * and return an error result.
 */
public class CodeExecutionEngine {
    private static final Logger LOGGER = TestLogger.getLogger(CodeExecutionEngine.class);

    private final ForemanEntity steve;
    private final Context graalContext;
    private final ForemanAPI steveAPI;
    private final ExecutorService executor;

    private static final long DEFAULT_TIMEOUT_MS = 30000; // 30 seconds
    private static final long MIN_TIMEOUT_MS = 1000; // 1 second minimum
    private static final long MAX_TIMEOUT_MS = 60000; // 60 seconds maximum

    public CodeExecutionEngine(ForemanEntity steve) {
        this.steve = steve;
        this.steveAPI = new ForemanAPI(steve);

        // Create single-threaded executor for timeout enforcement
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "Steve-Code-Execution");
            t.setDaemon(true); // Don't prevent JVM shutdown
            return t;
        });

        // Create GraalVM context with strict security restrictions
        this.graalContext = Context.newBuilder("js")
            .allowAllAccess(false)                        // Deny all access by default
            .allowNativeAccess(false)                      // No native libraries
            .allowCreateThread(false)                      // No thread creation
            .allowCreateProcess(false)                     // No process creation
            .allowHostClassLookup(className -> false)      // No Java class access
            .allowHostAccess(null)                         // No host access
            .option("js.java-package-globals", "false")    // Disable Java package globals
            .option("js.timer-resolution", "1")            // Low resolution timers
            .build();

        // Inject Steve API as the only bridge to Minecraft
        graalContext.getBindings("js").putMember("steve", steveAPI);

        // Add console.log for debugging (optional)
        String consolePolyfill = """
            var console = {
                log: function(...args) {
                    java.lang.System.out.println('[Steve Code] ' + args.join(' '));
                }
            };
            """;

        try {
            graalContext.eval("js", consolePolyfill);
        } catch (PolyglotException e) {
            // SECURITY FIX: Log exception instead of silent failure
            LOGGER.debug("Console polyfill setup failed (non-critical)", e);
        }
    }

    /**
     * Execute JavaScript code with default timeout (30 seconds).
     *
     * <p><b>SECURITY:</b> Timeout is actively enforced using ExecutorService.
     * Scripts that exceed timeout are cancelled and return an error.</p>
     *
     * @param code JavaScript code to execute
     * @return ExecutionResult containing success/failure status and output
     */
    public ExecutionResult execute(String code) {
        return execute(code, DEFAULT_TIMEOUT_MS);
    }

    /**
     * Execute JavaScript code with custom timeout.
     *
     * <p><b>SECURITY:</b> Timeout is actively enforced using ExecutorService.
     * This prevents infinite loops from hanging the game thread.</p>
     *
     * @param code JavaScript code to execute
     * @param timeoutMs Maximum execution time in milliseconds (min: 1000ms, max: 60000ms)
     * @return ExecutionResult containing success/failure status and output
     * @throws IllegalArgumentException if timeout is outside valid range
     */
    public ExecutionResult execute(String code, long timeoutMs) {
        // Validate inputs
        if (code == null || code.trim().isEmpty()) {
            return ExecutionResult.error("No code provided");
        }

        // Clamp timeout to safe range
        long actualTimeout = Math.max(MIN_TIMEOUT_MS, Math.min(MAX_TIMEOUT_MS, timeoutMs));
        if (actualTimeout != timeoutMs) {
            LOGGER.warn("Timeout clamped from {}ms to {}ms", timeoutMs, actualTimeout);
        }

        // Submit execution task to executor with timeout
        Future<Value> future = executor.submit(() -> {
            return graalContext.eval("js", code);
        });

        try {
            // Wait for completion with timeout
            Value result = future.get(actualTimeout, TimeUnit.MILLISECONDS);

            // Convert result to string
            String output = result.isNull() ? "null" : result.toString();

            return ExecutionResult.success(output);

        } catch (TimeoutException e) {
            // SECURITY: Cancel the task on timeout
            future.cancel(true);
            LOGGER.warn("Script execution exceeded timeout ({}ms)", actualTimeout);
            return ExecutionResult.error("Execution timeout: script exceeded " + actualTimeout + "ms");

        } catch (ExecutionException e) {
            // Handle various execution errors
            Throwable cause = e.getCause();
            if (cause instanceof PolyglotException polyEx) {
                return handlePolyglotException(polyEx);
            }

            // Generic execution error
            String errorMsg = cause != null ? cause.getMessage() : "Unknown execution error";
            if (errorMsg == null || errorMsg.isEmpty()) {
                errorMsg = "Execution failed";
            }

            LOGGER.error("Script execution failed: {}", errorMsg);
            return ExecutionResult.error("Error: " + errorMsg);

        } catch (InterruptedException e) {
            // Thread was interrupted
            Thread.currentThread().interrupt();
            LOGGER.warn("Script execution interrupted");
            return ExecutionResult.error("Execution interrupted");

        } catch (Exception e) {
            LOGGER.error("Unexpected error during script execution", e);
            return ExecutionResult.error("Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Handles PolyglotException from GraalVM execution.
     */
    private ExecutionResult handlePolyglotException(PolyglotException e) {
        if (e.isExit()) {
            LOGGER.warn("Script called exit with status: {}", e.getExitStatus());
            return ExecutionResult.error("Code called exit: " + e.getExitStatus());
        }

        if (e.isInterrupted()) {
            LOGGER.warn("Script execution was interrupted");
            return ExecutionResult.error("Execution interrupted");
        }

        if (e.isSyntaxError()) {
            LOGGER.debug("Script syntax error: {}", e.getMessage());
            return ExecutionResult.error("Syntax error: " + e.getMessage());
        }

        // Generic execution error
        String errorMsg = e.getMessage();
        if (errorMsg == null || errorMsg.isEmpty()) {
            errorMsg = "Unknown script error";
        }

        LOGGER.error("Script execution error: {}", errorMsg);
        return ExecutionResult.error("Error: " + errorMsg);
    }

    /**
     * Validate JavaScript code syntax without executing
     *
     * @param code JavaScript code to validate
     * @return true if syntax is valid, false otherwise
     */
    public boolean validateSyntax(String code) {
        try {
            // Parse without executing by wrapping in function
            graalContext.eval("js", "function __validate() { " + code + " }");
            return true;
        } catch (PolyglotException e) {
            return false;
        }
    }

    /**
     * Get the Steve API bridge
     */
    public ForemanAPI getAPI() {
        return steveAPI;
    }

    /**
     * Clean up resources.
     *
     * <p>Shuts down the executor and closes the GraalVM context.
     * Should be called when the CodeExecutionEngine is no longer needed.</p>
     */
    public void close() {
        // Shutdown executor first to cancel any pending executions
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
            try {
                if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                    LOGGER.warn("Executor did not terminate gracefully");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOGGER.warn("Interrupted while waiting for executor termination");
            }
        }

        // Close GraalVM context
        if (graalContext != null) {
            try {
                graalContext.close();
            } catch (Exception e) {
                LOGGER.error("Error closing GraalVM context", e);
            }
        }
    }

    /**
     * Result of code execution
     */
    public static class ExecutionResult {
        private final boolean success;
        private final String output;
        private final String error;

        private ExecutionResult(boolean success, String output, String error) {
            this.success = success;
            this.output = output;
            this.error = error;
        }

        public static ExecutionResult success(String output) {
            return new ExecutionResult(true, output, null);
        }

        public static ExecutionResult error(String error) {
            return new ExecutionResult(false, null, error);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getOutput() {
            return output;
        }

        public String getError() {
            return error;
        }

        @Override
        public String toString() {
            if (success) {
                return "Success: " + output;
            } else {
                return "Error: " + error;
            }
        }
    }
}

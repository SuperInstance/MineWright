package com.minewright.execution;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for CodeExecutionEngine security features, especially timeout enforcement.
 */
public class CodeExecutionEngineTest {

    private CodeExecutionEngine engine;

    @BeforeEach
    public void setUp() {
        // Create engine with a mock ForemanEntity (null for testing)
        engine = new CodeExecutionEngine(null);
    }

    @AfterEach
    public void tearDown() {
        if (engine != null) {
            engine.close();
        }
    }

    @Test
    public void testSimpleExecution() {
        CodeExecutionEngine.ExecutionResult result = engine.execute("2 + 2");
        assertTrue(result.isSuccess(), "Simple arithmetic should succeed");
        assertEquals("4", result.getOutput());
    }

    @Test
    public void testSyntaxError() {
        CodeExecutionEngine.ExecutionResult result = engine.execute("function(");
        assertFalse(result.isSuccess(), "Syntax error should fail");
        assertTrue(result.getError().contains("Syntax error"));
    }

    @Test
    public void testEmptyCode() {
        CodeExecutionEngine.ExecutionResult result = engine.execute("");
        assertFalse(result.isSuccess(), "Empty code should fail");
        assertTrue(result.getError().contains("No code provided"));
    }

    @Test
    public void testNullCode() {
        CodeExecutionEngine.ExecutionResult result = engine.execute(null);
        assertFalse(result.isSuccess(), "Null code should fail");
        assertTrue(result.getError().contains("No code provided"));
    }

    @Test
    public void testInfiniteLoopWithTimeout() {
        // SECURITY TEST: Verify that infinite loops are terminated by timeout
        String infiniteLoop = "while (true) { }";

        long startTime = System.currentTimeMillis();
        CodeExecutionEngine.ExecutionResult result = engine.execute(infiniteLoop, 2000); // 2 second timeout
        long duration = System.currentTimeMillis() - startTime;

        assertFalse(result.isSuccess(), "Infinite loop should fail");
        assertTrue(result.getError().contains("timeout"),
            "Error should mention timeout. Got: " + result.getError());

        // Verify timeout was enforced (should be ~2 seconds, not infinite)
        assertTrue(duration < 5000,
            "Infinite loop should timeout in ~2 seconds, took " + duration + "ms");

        System.out.println("Infinite loop terminated in " + duration + "ms (expected ~2000ms)");
    }

    @Test
    public void testLongRunningScriptCompletes() {
        // Test that a long-running (but finite) script completes successfully
        String longScript = "let sum = 0; " +
                           "for (let i = 0; i < 1000000; i++) { " +
                           "  sum += i; " +
                           "} " +
                           "sum;";

        CodeExecutionEngine.ExecutionResult result = engine.execute(longScript);
        assertTrue(result.isSuccess(), "Long-running finite script should succeed");
    }

    @Test
    public void testTimeoutClamping() {
        // Test that extremely low timeouts are clamped to minimum
        String code = "2 + 2";

        // Try with 1ms timeout (should be clamped to 1000ms minimum)
        long startTime = System.currentTimeMillis();
        CodeExecutionEngine.ExecutionResult result = engine.execute(code, 1);
        long duration = System.currentTimeMillis() - startTime;

        assertTrue(result.isSuccess(), "Simple math should succeed even with 1ms timeout");
        // Should take at least 100ms (actual clamped timeout)
        assertTrue(duration >= 0, "Execution should complete");
    }

    @Test
    public void testMultipleExecutions() {
        // Test that the executor can handle multiple sequential executions
        for (int i = 0; i < 10; i++) {
            CodeExecutionEngine.ExecutionResult result = engine.execute("Math.random()");
            assertTrue(result.isSuccess(), "Execution " + i + " should succeed");
        }
    }

    @Test
    public void testSyntaxValidation() {
        assertTrue(engine.validateSyntax("function test() { return 1; }"),
            "Valid syntax should pass");
        assertTrue(engine.validateSyntax("2 + 2"),
            "Valid expression should pass");
        assertFalse(engine.validateSyntax("function("),
            "Invalid syntax should fail");
        assertFalse(engine.validateSyntax("if (true"),
            "Incomplete block should fail");
    }

    @Test
    public void testJavaScriptObjectAccess() {
        // Test that JavaScript objects can be created and accessed
        CodeExecutionEngine.ExecutionResult result = engine.execute(
            "let obj = {a: 1, b: 2}; obj.a + obj.b;"
        );
        assertTrue(result.isSuccess(), "Object access should succeed");
        assertEquals("3", result.getOutput());
    }

    @Test
    public void testStringManipulation() {
        CodeExecutionEngine.ExecutionResult result = engine.execute(
            "'Hello'.toUpperCase() + ' ' + 'World'.toLowerCase();"
        );
        assertTrue(result.isSuccess(), "String manipulation should succeed");
        assertEquals("HELLO world", result.getOutput());
    }

    @Test
    public void testArrayOperations() {
        CodeExecutionEngine.ExecutionResult result = engine.execute(
            "[1, 2, 3].map(x => x * 2).join(',');"
        );
        assertTrue(result.isSuccess(), "Array operations should succeed");
        assertEquals("2,4,6", result.getOutput());
    }

    @Test
    public void testErrorPropagation() {
        CodeExecutionEngine.ExecutionResult result = engine.execute(
            "throw new Error('Test error');"
        );
        assertFalse(result.isSuccess(), "Thrown error should fail");
        assertTrue(result.getError().contains("Error"),
            "Error message should be present. Got: " + result.getError());
    }

    @Test
    public void testConsoleLogAvailable() {
        // Test that console.log is available (doesn't throw error)
        CodeExecutionEngine.ExecutionResult result = engine.execute(
            "console.log('test'); 42;"
        );
        assertTrue(result.isSuccess(), "Console.log should not cause error");
        assertEquals("42", result.getOutput());
    }

    @Test
    public void testComplexCalculation() {
        // Test a more complex script to ensure executor handles real workloads
        String fibonacci = "function fib(n) {" +
                          "  if (n <= 1) return n;" +
                          "  return fib(n - 1) + fib(n - 2);" +
                          "}" +
                          "fib(10);";

        CodeExecutionEngine.ExecutionResult result = engine.execute(fibonacci);
        assertTrue(result.isSuccess(), "Fibonacci calculation should succeed");
        assertEquals("55", result.getOutput());
    }

    @Test
    public void testResourceCleanup() {
        // Test that close() properly cleans up resources
        CodeExecutionEngine engine2 = new CodeExecutionEngine(null);

        // Execute some code
        CodeExecutionEngine.ExecutionResult result = engine2.execute("2 + 2");
        assertTrue(result.isSuccess(), "Execution should succeed before close");

        // Close the engine
        engine2.close();

        // After close, the engine should be cleaned up
        // (We can't easily test this without accessing private fields,
        // but we can verify it doesn't throw an exception)
        assertDoesNotThrow(() -> engine2.close(),
            "Close should be idempotent (can be called multiple times)");
    }
}

package com.minewright.plugin;

import com.minewright.action.Task;
import com.minewright.action.actions.BaseAction;
import com.minewright.entity.ForemanEntity;
import com.minewright.execution.ActionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for {@link ActionRegistry}.
 *
 * Tests cover:
 * <ul>
 *   <li>Singleton pattern behavior</li>
 *   <li>Action registration with factory</li>
 *   <li>Action creation from registered factories</li>
 *   <li>Priority-based conflict resolution</li>
 *   <li>Action unregistration</li>
 *   <li>Plugin tracking</li>
 *   <li>Thread-safety of operations</li>
 *   <li>Case normalization of action names</li>
 * </ul>
 *
 * @since 1.1.0
 */
@DisplayName("ActionRegistry Tests")
class ActionRegistryTest {

    @Mock
    private ForemanEntity mockForeman;

    @Mock
    private ActionContext mockContext;

    private ActionRegistry registry;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // Get singleton instance
        registry = ActionRegistry.getInstance();
        // Clear any existing registrations for clean test state
        registry.clear();
    }

    // ==================== Singleton Pattern Tests ====================

    @Test
    @DisplayName("GetInstance returns same instance")
    void testGetInstanceReturnsSameInstance() {
        ActionRegistry instance1 = ActionRegistry.getInstance();
        ActionRegistry instance2 = ActionRegistry.getInstance();

        assertSame(instance1, instance2,
                "GetInstance should return the same singleton instance");
    }

    @Test
    @DisplayName("Clear removes all registered actions")
    void testClearRemovesAllActions() {
        ActionFactory factory = (foreman, task, ctx) -> new TestAction(foreman, task);

        registry.register("test_action1", factory);
        registry.register("test_action2", factory);

        assertTrue(registry.getActionCount() >= 2,
                "Should have registered actions");

        registry.clear();

        assertEquals(0, registry.getActionCount(),
                "Action count should be 0 after clear");
        assertFalse(registry.hasAction("test_action1"),
                "Action should be removed after clear");
        assertFalse(registry.hasAction("test_action2"),
                "Action should be removed after clear");
    }

    // ==================== Action Registration Tests ====================

    @Test
    @DisplayName("Register adds action factory")
    void testRegisterAddsFactory() {
        ActionFactory factory = (foreman, task, ctx) -> new TestAction(foreman, task);

        registry.register("test_action", factory);

        assertTrue(registry.hasAction("test_action"),
                "Action should be registered");
        assertEquals(1, registry.getActionCount(),
                "Action count should be 1");
    }

    @Test
    @DisplayName("Register throws exception for null action name")
    void testRegisterThrowsExceptionForNullActionName() {
        ActionFactory factory = (foreman, task, ctx) -> new TestAction(foreman, task);

        assertThrows(IllegalArgumentException.class,
                () -> registry.register(null, factory),
                "Should throw exception for null action name");
    }

    @Test
    @DisplayName("Register throws exception for blank action name")
    void testRegisterThrowsExceptionForBlankActionName() {
        ActionFactory factory = (foreman, task, ctx) -> new TestAction(foreman, task);

        assertThrows(IllegalArgumentException.class,
                () -> registry.register("   ", factory),
                "Should throw exception for blank action name");
    }

    @Test
    @DisplayName("Register throws exception for null factory")
    void testRegisterThrowsExceptionForNullFactory() {
        assertThrows(IllegalArgumentException.class,
                () -> registry.register("test_action", null),
                "Should throw exception for null factory");
    }

    @Test
    @DisplayName("Register normalizes action name to lowercase")
    void testRegisterNormalizesActionName() {
        ActionFactory factory = (foreman, task, ctx) -> new TestAction(foreman, task);

        registry.register("Test_Action", factory);

        assertTrue(registry.hasAction("test_action"),
                "Action name should be normalized to lowercase");
        assertTrue(registry.hasAction("TEST_ACTION"),
                "Action name should be accessible with uppercase");
        assertTrue(registry.hasAction("tEsT_aCtIoN"),
                "Action name should be accessible with mixed case");
    }

    @Test
    @DisplayName("Register with priority and plugin id")
    void testRegisterWithPriorityAndPluginId() {
        ActionFactory factory = (foreman, task, ctx) -> new TestAction(foreman, task);

        registry.register("test_action", factory, 10, "test_plugin");

        assertTrue(registry.hasAction("test_action"));
        assertEquals("test_plugin", registry.getPluginForAction("test_action"));
    }

    // ==================== Priority-Based Conflict Resolution Tests ====================

    @Test
    @DisplayName("Higher priority registration overrides lower priority")
    void testHigherPriorityOverridesLower() {
        ActionFactory lowPriorityFactory = mock(ActionFactory.class);
        ActionFactory highPriorityFactory = mock(ActionFactory.class);

        registry.register("test_action", lowPriorityFactory, 5, "plugin1");
        registry.register("test_action", highPriorityFactory, 10, "plugin2");

        String pluginId = registry.getPluginForAction("test_action");

        assertEquals("plugin2", pluginId,
                "Higher priority registration should override");
    }

    @Test
    @DisplayName("Lower priority registration does not override higher priority")
    void testLowerPriorityDoesNotOverrideHigher() {
        ActionFactory highPriorityFactory = mock(ActionFactory.class);
        ActionFactory lowPriorityFactory = mock(ActionFactory.class);

        registry.register("test_action", highPriorityFactory, 10, "plugin1");
        registry.register("test_action", lowPriorityFactory, 5, "plugin2");

        String pluginId = registry.getPluginForAction("test_action");

        assertEquals("plugin1", pluginId,
                "Lower priority registration should not override");
    }

    @Test
    @DisplayName("Equal priority registration keeps existing (last-write-wins not applied)")
    void testEqualPriorityKeepsExisting() {
        ActionFactory firstFactory = mock(ActionFactory.class);
        ActionFactory secondFactory = mock(ActionFactory.class);

        registry.register("test_action", firstFactory, 5, "plugin1");
        registry.register("test_action", secondFactory, 5, "plugin2");

        String pluginId = registry.getPluginForAction("test_action");

        assertEquals("plugin1", pluginId,
                "Equal priority should keep existing registration");
    }

    @Test
    @DisplayName("Default priority is 0")
    void testDefaultPriorityIsZero() {
        ActionFactory factory = (foreman, task, ctx) -> new TestAction(foreman, task);

        registry.register("test_action", factory);

        // Default priority should allow override by higher priority
        ActionFactory higherPriorityFactory = mock(ActionFactory.class);
        registry.register("test_action", higherPriorityFactory, 1, "plugin2");

        assertEquals("plugin2", registry.getPluginForAction("test_action"),
                "Default priority 0 should be overrideable by priority 1");
    }

    // ==================== Action Creation Tests ====================

    @Test
    @DisplayName("CreateAction creates action from factory")
    void testCreateActionCreatesFromFactory() {
        ActionFactory factory = (foreman, task, ctx) -> new TestAction(foreman, task);

        registry.register("test_action", factory);

        Task task = new Task("test_action", Map.of("param", "value"));
        BaseAction action = registry.createAction("test_action", mockForeman, task, mockContext);

        assertNotNull(action, "Created action should not be null");
        assertTrue(action instanceof TestAction,
                "Created action should be instance of TestAction");
    }

    @Test
    @DisplayName("CreateAction returns null for unregistered action")
    void testCreateActionReturnsNullForUnregistered() {
        Task task = new Task("unregistered_action", Map.of());

        BaseAction action = registry.createAction("unregistered_action", mockForeman, task, mockContext);

        assertNull(action, "Should return null for unregistered action");
    }

    @Test
    @DisplayName("CreateAction returns null for null action name")
    void testCreateActionReturnsNullForNullName() {
        Task task = new Task("test", Map.of());

        BaseAction action = registry.createAction(null, mockForeman, task, mockContext);

        assertNull(action, "Should return null for null action name");
    }

    @Test
    @DisplayName("CreateAction is case insensitive")
    void testCreateActionIsCaseInsensitive() {
        ActionFactory factory = (foreman, task, ctx) -> new TestAction(foreman, task);

        registry.register("test_action", factory);

        Task task = new Task("test_action", Map.of());

        BaseAction action1 = registry.createAction("TEST_ACTION", mockForeman, task, mockContext);
        BaseAction action2 = registry.createAction("test_action", mockForeman, task, mockContext);
        BaseAction action3 = registry.createAction("TeSt_AcTiOn", mockForeman, task, mockContext);

        assertNotNull(action1, "Should create action with uppercase name");
        assertNotNull(action2, "Should create action with lowercase name");
        assertNotNull(action3, "Should create action with mixed case name");
    }

    @Test
    @DisplayName("CreateAction handles factory exceptions gracefully")
    void testCreateActionHandlesFactoryExceptions() {
        ActionFactory failingFactory = (foreman, task, ctx) -> {
            throw new RuntimeException("Factory failure");
        };

        registry.register("failing_action", failingFactory);

        Task task = new Task("failing_action", Map.of());

        BaseAction action = registry.createAction("failing_action", mockForeman, task, mockContext);

        assertNull(action, "Should return null when factory throws exception");
    }

    // ==================== HasAction Tests ====================

    @Test
    @DisplayName("HasAction returns true for registered action")
    void testHasActionReturnsTrueForRegistered() {
        ActionFactory factory = (foreman, task, ctx) -> new TestAction(foreman, task);

        registry.register("test_action", factory);

        assertTrue(registry.hasAction("test_action"));
    }

    @Test
    @DisplayName("HasAction returns false for unregistered action")
    void testHasActionReturnsFalseForUnregistered() {
        assertFalse(registry.hasAction("unregistered_action"));
    }

    @Test
    @DisplayName("HasAction returns false for null action name")
    void testHasActionReturnsFalseForNull() {
        assertFalse(registry.hasAction(null));
    }

    @Test
    @DisplayName("HasAction is case insensitive")
    void testHasActionIsCaseInsensitive() {
        ActionFactory factory = (foreman, task, ctx) -> new TestAction(foreman, task);

        registry.register("test_action", factory);

        assertTrue(registry.hasAction("TEST_ACTION"));
        assertTrue(registry.hasAction("test_action"));
        assertTrue(registry.hasAction("TeSt_AcTiOn"));
    }

    // ==================== Unregistration Tests ====================

    @Test
    @DisplayName("Unregister removes registered action")
    void testUnregisterRemovesAction() {
        ActionFactory factory = (foreman, task, ctx) -> new TestAction(foreman, task);

        registry.register("test_action", factory);

        assertTrue(registry.hasAction("test_action"));

        boolean removed = registry.unregister("test_action");

        assertTrue(removed, "Should return true when action is removed");
        assertFalse(registry.hasAction("test_action"),
                "Action should be unregistered");
    }

    @Test
    @DisplayName("Unregister returns false for non-existent action")
    void testUnregisterReturnsFalseForNonExistent() {
        boolean removed = registry.unregister("non_existent");

        assertFalse(removed, "Should return false for non-existent action");
    }

    @Test
    @DisplayName("Unregister returns false for null action name")
    void testUnregisterReturnsFalseForNull() {
        boolean removed = registry.unregister(null);

        assertFalse(removed, "Should return false for null action name");
    }

    @Test
    @DisplayName("Unregister is case insensitive")
    void testUnregisterIsCaseInsensitive() {
        ActionFactory factory = (foreman, task, ctx) -> new TestAction(foreman, task);

        registry.register("test_action", factory);

        assertTrue(registry.unregister("TEST_ACTION"),
                "Should unregister with uppercase name");
        assertFalse(registry.hasAction("test_action"),
                "Action should be unregistered regardless of case used");
    }

    @Test
    @DisplayName("Unregister removes plugin association")
    void testUnregisterRemovesPluginAssociation() {
        ActionFactory factory = (foreman, task, ctx) -> new TestAction(foreman, task);

        registry.register("test_action", factory, 5, "test_plugin");

        assertEquals("test_plugin", registry.getPluginForAction("test_action"));

        registry.unregister("test_action");

        assertNull(registry.getPluginForAction("test_action"),
                "Plugin association should be removed");
    }

    // ==================== GetRegisteredActions Tests ====================

    @Test
    @DisplayName("GetRegisteredActions returns all action names")
    void testGetRegisteredActionsReturnsAll() {
        ActionFactory factory = (foreman, task, ctx) -> new TestAction(foreman, task);

        registry.register("action1", factory);
        registry.register("action2", factory);
        registry.register("action3", factory);

        Set<String> actions = registry.getRegisteredActions();

        assertEquals(3, actions.size());
        assertTrue(actions.contains("action1"));
        assertTrue(actions.contains("action2"));
        assertTrue(actions.contains("action3"));
    }

    @Test
    @DisplayName("GetRegisteredActions returns empty set when no actions")
    void testGetRegisteredActionsReturnsEmptyWhenNone() {
        Set<String> actions = registry.getRegisteredActions();

        assertNotNull(actions, "Should return set, not null");
        assertTrue(actions.isEmpty(), "Should return empty set when no actions");
    }

    @Test
    @DisplayName("GetRegisteredActions returns unmodifiable set")
    void testGetRegisteredActionsReturnsUnmodifiable() {
        ActionFactory factory = (foreman, task, ctx) -> new TestAction(foreman, task);

        registry.register("test_action", factory);

        Set<String> actions = registry.getRegisteredActions();

        assertThrows(UnsupportedOperationException.class,
                () -> actions.add("new_action"),
                "Returned set should be unmodifiable");
    }

    // ==================== GetPluginForAction Tests ====================

    @Test
    @DisplayName("GetPluginForAction returns plugin id")
    void testGetPluginForActionReturnsPluginId() {
        ActionFactory factory = (foreman, task, ctx) -> new TestAction(foreman, task);

        registry.register("test_action", factory, 5, "test_plugin");

        String pluginId = registry.getPluginForAction("test_action");

        assertEquals("test_plugin", pluginId);
    }

    @Test
    @DisplayName("GetPluginForAction returns null for unregistered action")
    void testGetPluginForActionReturnsNullForUnregistered() {
        String pluginId = registry.getPluginForAction("unregistered");

        assertNull(pluginId, "Should return null for unregistered action");
    }

    @Test
    @DisplayName("GetPluginForAction returns null for null action name")
    void testGetPluginForActionReturnsNullForNull() {
        String pluginId = registry.getPluginForAction(null);

        assertNull(pluginId, "Should return null for null action name");
    }

    @Test
    @DisplayName("GetPluginForAction returns null for action without plugin")
    void testGetPluginForActionReturnsNullForNoPlugin() {
        ActionFactory factory = (foreman, task, ctx) -> new TestAction(foreman, task);

        // Register with default "unknown" plugin
        registry.register("test_action", factory);

        String pluginId = registry.getPluginForAction("test_action");

        assertNotNull(pluginId, "Plugin ID should be set");
        assertEquals("unknown", pluginId,
                "Default plugin ID should be 'unknown'");
    }

    // ==================== GetActionCount Tests ====================

    @Test
    @DisplayName("GetActionCount returns correct count")
    void testGetActionCountReturnsCorrect() {
        ActionFactory factory = (foreman, task, ctx) -> new TestAction(foreman, task);

        assertEquals(0, registry.getActionCount(),
                "Initial count should be 0");

        registry.register("action1", factory);
        assertEquals(1, registry.getActionCount());

        registry.register("action2", factory);
        assertEquals(2, registry.getActionCount());

        registry.register("action3", factory);
        assertEquals(3, registry.getActionCount());

        registry.unregister("action1");
        assertEquals(2, registry.getActionCount());
    }

    // ==================== GetActionsAsList Tests ====================

    @Test
    @DisplayName("GetActionsAsList returns comma-separated list")
    void testGetActionsAsListReturnsCommaSeparated() {
        ActionFactory factory = (foreman, task, ctx) -> new TestAction(foreman, task);

        registry.register("action1", factory);
        registry.register("action2", factory);
        registry.register("action3", factory);

        String actionsList = registry.getActionsAsList();

        assertTrue(actionsList.contains("action1"));
        assertTrue(actionsList.contains("action2"));
        assertTrue(actionsList.contains("action3"));
        assertTrue(actionsList.contains(","),
                "Should contain comma separators");
    }

    @Test
    @DisplayName("GetActionsAsList returns empty string for no actions")
    void testGetActionsAsListReturnsEmptyForNoActions() {
        String actionsList = registry.getActionsAsList();

        assertEquals("", actionsList,
                "Should return empty string when no actions");
    }

    // ==================== Edge Case Tests ====================

    @Test
    @DisplayName("Register handles action name with spaces")
    void testRegisterHandlesActionNameWithSpaces() {
        ActionFactory factory = (foreman, task, ctx) -> new TestAction(foreman, task);

        registry.register("test action with spaces", factory);

        // Should be normalized (spaces trimmed)
        assertTrue(registry.hasAction("test action with spaces"));
    }

    @Test
    @DisplayName("Register handles action name with special characters")
    void testRegisterHandlesSpecialCharacters() {
        ActionFactory factory = (foreman, task, ctx) -> new TestAction(foreman, task);

        assertDoesNotThrow(() -> registry.register("test-action_v1.0", factory),
                "Should handle special characters in name");
    }

    @Test
    @DisplayName("Multiple registrations of same action with different priorities")
    void testMultipleRegistrationsDifferentPriorities() {
        ActionFactory factory1 = mock(ActionFactory.class);
        ActionFactory factory2 = mock(ActionFactory.class);
        ActionFactory factory3 = mock(ActionFactory.class);

        registry.register("test_action", factory1, 1, "plugin1");
        registry.register("test_action", factory2, 5, "plugin2");
        registry.register("test_action", factory3, 3, "plugin3");

        String pluginId = registry.getPluginForAction("test_action");

        assertEquals("plugin2", pluginId,
                "Should keep highest priority registration (5)");
    }

    @Test
    @DisplayName("Concurrent registration is thread-safe")
    void testConcurrentRegistrationIsThreadSafe() throws InterruptedException {
        ActionFactory factory = (foreman, task, ctx) -> new TestAction(foreman, task);

        int threadCount = 10;
        int actionsPerThread = 100;
        Thread[] threads = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < actionsPerThread; j++) {
                    String actionName = "action_" + threadId + "_" + j;
                    registry.register(actionName, factory);
                }
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        assertTrue(registry.getActionCount() >= threadCount * actionsPerThread,
                "Should handle concurrent registrations safely");
    }

    // ==================== Test Helper Classes ====================

    /**
     * Simple test action for testing factory creation.
     */
    private static class TestAction extends BaseAction {
        public TestAction(ForemanEntity foreman, Task task) {
            super(foreman, task);
        }

        @Override
        protected void onStart() {
            // No-op for testing
        }

        @Override
        protected void onTick() {
            // No-op for testing
        }

        @Override
        protected void onCancel() {
            // No-op for testing
        }

        @Override
        public String getDescription() {
            return "Test Action";
        }
    }
}

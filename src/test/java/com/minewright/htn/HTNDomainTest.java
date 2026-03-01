package com.minewright.htn;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for {@link HTNDomain}.
 *
 * <p>Tests cover:
 * <ul>
 *   <li>Domain creation and initialization</li>
 *   <li>Method registration and retrieval</li>
 *   <li>Precondition-based method filtering</li>
 *   <li>Priority-based method ordering</li>
 *   <li>Method removal and clearing</li>
 *   <li>Domain queries and statistics</li>
 *   <li>Default domain methods</li>
 *   <li>Edge cases and error handling</li>
 * </ul>
 */
@DisplayName("HTNDomain Tests")
class HTNDomainTest {

    private HTNWorldState basicState;
    private HTNWorldState stateWithAxe;
    private HTNWorldState stateWithoutAxe;

    @BeforeEach
    void setUp() {
        basicState = HTNWorldState.createMutable();
        stateWithAxe = HTNWorldState.builder()
            .property("hasAxe", true)
            .property("hasWood", false)
            .build();
        stateWithoutAxe = HTNWorldState.builder()
            .property("hasAxe", false)
            .property("hasWood", false)
            .build();
    }

    @Test
    @DisplayName("Constructor creates empty domain with name")
    void testConstructorCreatesEmptyDomain() {
        HTNDomain domain = new HTNDomain("test_domain");

        assertEquals("test_domain", domain.getDomainName());
        assertEquals(0, domain.getTaskCount());
        assertEquals(0, domain.getMethodCount());
    }

    @Test
    @DisplayName("CreateDefault creates domain with default methods")
    void testCreateDefault() {
        HTNDomain domain = HTNDomain.createDefault();

        assertEquals("minecraft_default", domain.getDomainName());
        assertTrue(domain.getTaskCount() > 0,
            "Default domain should have tasks");
        assertTrue(domain.getMethodCount() > 0,
            "Default domain should have methods");
    }

    @Test
    @DisplayName("AddMethod adds method to domain")
    void testAddMethod() {
        HTNDomain domain = new HTNDomain("test_domain");
        HTNMethod method = HTNMethod.builder("test_method", "test_task")
            .subtask(HTNTask.primitive("mine").build())
            .build();

        domain.addMethod(method);

        assertEquals(1, domain.getTaskCount());
        assertEquals(1, domain.getMethodCount());
    }

    @Test
    @DisplayName("AddMethod ignores null method")
    void testAddMethodNull() {
        HTNDomain domain = new HTNDomain("test_domain");

        domain.addMethod(null);

        assertEquals(0, domain.getTaskCount());
        assertEquals(0, domain.getMethodCount());
    }

    @Test
    @DisplayName("AddMethod adds multiple methods for same task")
    void testAddMethodMultipleForSameTask() {
        HTNDomain domain = new HTNDomain("test_domain");

        HTNMethod method1 = HTNMethod.builder("method1", "gather_wood")
            .subtask(HTNTask.primitive("mine").build())
            .priority(100)
            .build();

        HTNMethod method2 = HTNMethod.builder("method2", "gather_wood")
            .subtask(HTNTask.primitive("mine").build())
            .priority(50)
            .build();

        domain.addMethod(method1);
        domain.addMethod(method2);

        assertEquals(1, domain.getTaskCount(),
            "Should have one task");
        assertEquals(2, domain.getMethodCount(),
            "Should have two methods");
    }

    @Test
    @DisplayName("AddMethods adds collection of methods")
    void testAddMethods() {
        HTNDomain domain = new HTNDomain("test_domain");

        HTNMethod method1 = HTNMethod.builder("method1", "task1")
            .subtask(HTNTask.primitive("mine").build())
            .build();

        HTNMethod method2 = HTNMethod.builder("method2", "task2")
            .subtask(HTNTask.primitive("craft").build())
            .build();

        domain.addMethods(List.of(method1, method2));

        assertEquals(2, domain.getTaskCount());
        assertEquals(2, domain.getMethodCount());
    }

    @Test
    @DisplayName("AddMethods with null collection does nothing")
    void testAddMethodsNull() {
        HTNDomain domain = new HTNDomain("test_domain");

        domain.addMethods(null);

        assertEquals(0, domain.getTaskCount());
    }

    @Test
    @DisplayName("GetMethodsForTask returns all methods for task")
    void testGetMethodsForTask() {
        HTNDomain domain = new HTNDomain("test_domain");

        HTNMethod method1 = HTNMethod.builder("method1", "gather_wood")
            .subtask(HTNTask.primitive("mine").build())
            .build();

        HTNMethod method2 = HTNMethod.builder("method2", "gather_wood")
            .subtask(HTNTask.primitive("craft").build())
            .build();

        HTNMethod method3 = HTNMethod.builder("method3", "build_house")
            .subtask(HTNTask.primitive("place").build())
            .build();

        domain.addMethods(List.of(method1, method2, method3));

        List<HTNMethod> gatherMethods = domain.getMethodsForTask("gather_wood");

        assertEquals(2, gatherMethods.size());
        assertTrue(gatherMethods.contains(method1));
        assertTrue(gatherMethods.contains(method2));
    }

    @Test
    @DisplayName("GetMethodsForTask returns empty list for unknown task")
    void testGetMethodsForTaskUnknown() {
        HTNDomain domain = new HTNDomain("test_domain");

        List<HTNMethod> methods = domain.getMethodsForTask("unknown_task");

        assertNotNull(methods);
        assertTrue(methods.isEmpty());
    }

    @Test
    @DisplayName("GetMethodsForTask returns unmodifiable list")
    void testGetMethodsForTaskUnmodifiable() {
        HTNDomain domain = new HTNDomain("test_domain");

        HTNMethod method = HTNMethod.builder("method1", "test_task")
            .subtask(HTNTask.primitive("mine").build())
            .build();

        domain.addMethod(method);

        List<HTNMethod> methods = domain.getMethodsForTask("test_task");

        assertThrows(UnsupportedOperationException.class,
            () -> methods.add(method),
            "Methods list should be unmodifiable");
    }

    @Test
    @DisplayName("GetApplicableMethods filters by preconditions")
    void testGetApplicableMethods() {
        HTNDomain domain = new HTNDomain("test_domain");

        HTNMethod methodWithAxe = HTNMethod.builder("with_axe", "gather_wood")
            .precondition(state -> state.getBoolean("hasAxe"))
            .subtask(HTNTask.primitive("mine").build())
            .priority(100)
            .build();

        HTNMethod methodWithoutAxe = HTNMethod.builder("without_axe", "gather_wood")
            .precondition(state -> true)
            .subtask(HTNTask.primitive("mine").build())
            .priority(50)
            .build();

        domain.addMethods(List.of(methodWithAxe, methodWithoutAxe));

        // With axe - both methods applicable
        List<HTNMethod> withAxe = domain.getApplicableMethods("gather_wood", stateWithAxe);
        assertEquals(2, withAxe.size());

        // Without axe - only fallback applicable
        List<HTNMethod> withoutAxe = domain.getApplicableMethods("gather_wood", stateWithoutAxe);
        assertEquals(1, withoutAxe.size());
        assertEquals("without_axe", withoutAxe.get(0).getMethodName());
    }

    @Test
    @DisplayName("GetApplicableMethods sorts by priority descending")
    void testGetApplicableMethodsPriorityOrder() {
        HTNDomain domain = new HTNDomain("test_domain");

        HTNMethod methodLow = HTNMethod.builder("low", "test_task")
            .subtask(HTNTask.primitive("mine").build())
            .priority(10)
            .build();

        HTNMethod methodHigh = HTNMethod.builder("high", "test_task")
            .subtask(HTNTask.primitive("craft").build())
            .priority(100)
            .build();

        HTNMethod methodMedium = HTNMethod.builder("medium", "test_task")
            .subtask(HTNTask.primitive("place").build())
            .priority(50)
            .build();

        domain.addMethods(List.of(methodLow, methodHigh, methodMedium));

        List<HTNMethod> methods = domain.getApplicableMethods("test_task", basicState);

        assertEquals(3, methods.size());
        assertEquals("high", methods.get(0).getMethodName());
        assertEquals("medium", methods.get(1).getMethodName());
        assertEquals("low", methods.get(2).getMethodName());
    }

    @Test
    @DisplayName("GetApplicableMethods returns empty list for unknown task")
    void testGetApplicableMethodsUnknownTask() {
        HTNDomain domain = new HTNDomain("test_domain");

        List<HTNMethod> methods = domain.getApplicableMethods("unknown_task", basicState);

        assertNotNull(methods);
        assertTrue(methods.isEmpty());
    }

    @Test
    @DisplayName("GetApplicableMethods handles null state")
    void testGetApplicableMethodsNullState() {
        HTNDomain domain = new HTNDomain("test_domain");

        HTNMethod method = HTNMethod.builder("method1", "test_task")
            .precondition(state -> state != null)
            .subtask(HTNTask.primitive("mine").build())
            .build();

        domain.addMethod(method);

        List<HTNMethod> methods = domain.getApplicableMethods("test_task", null);

        assertTrue(methods.isEmpty(),
            "Null state should result in no applicable methods");
    }

    @Test
    @DisplayName("GetBestMethod returns highest priority applicable method")
    void testGetBestMethod() {
        HTNDomain domain = new HTNDomain("test_domain");

        HTNMethod methodHigh = HTNMethod.builder("high", "test_task")
            .precondition(state -> state.getBoolean("hasAxe"))
            .subtask(HTNTask.primitive("mine").build())
            .priority(100)
            .build();

        HTNMethod methodLow = HTNMethod.builder("low", "test_task")
            .precondition(state -> true)
            .subtask(HTNTask.primitive("mine").build())
            .priority(50)
            .build();

        domain.addMethods(List.of(methodHigh, methodLow));

        // With axe - should get high priority method
        HTNMethod bestWithAxe = domain.getBestMethod("test_task", stateWithAxe);
        assertNotNull(bestWithAxe);
        assertEquals("high", bestWithAxe.getMethodName());

        // Without axe - should get low priority fallback
        HTNMethod bestWithoutAxe = domain.getBestMethod("test_task", stateWithoutAxe);
        assertNotNull(bestWithoutAxe);
        assertEquals("low", bestWithoutAxe.getMethodName());
    }

    @Test
    @DisplayName("GetBestMethod returns null when no applicable methods")
    void testGetBestMethodNoneApplicable() {
        HTNDomain domain = new HTNDomain("test_domain");

        HTNMethod method = HTNMethod.builder("method1", "test_task")
            .precondition(state -> state.getBoolean("impossible"))
            .subtask(HTNTask.primitive("mine").build())
            .build();

        domain.addMethod(method);

        HTNMethod best = domain.getBestMethod("test_task", basicState);

        assertNull(best);
    }

    @Test
    @DisplayName("GetBestMethod returns null for unknown task")
    void testGetBestMethodUnknownTask() {
        HTNDomain domain = new HTNDomain("test_domain");

        HTNMethod best = domain.getBestMethod("unknown_task", basicState);

        assertNull(best);
    }

    @Test
    @DisplayName("HasMethodsFor returns true when methods exist")
    void testHasMethodsForTrue() {
        HTNDomain domain = new HTNDomain("test_domain");

        HTNMethod method = HTNMethod.builder("method1", "test_task")
            .subtask(HTNTask.primitive("mine").build())
            .build();

        domain.addMethod(method);

        assertTrue(domain.hasMethodsFor("test_task"));
    }

    @Test
    @DisplayName("HasMethodsFor returns false when no methods exist")
    void testHasMethodsForFalse() {
        HTNDomain domain = new HTNDomain("test_domain");

        assertFalse(domain.hasMethodsFor("unknown_task"));
    }

    @Test
    @DisplayName("RemoveMethod removes method by name")
    void testRemoveMethod() {
        HTNDomain domain = new HTNDomain("test_domain");

        HTNMethod method1 = HTNMethod.builder("method1", "task1")
            .subtask(HTNTask.primitive("mine").build())
            .build();

        HTNMethod method2 = HTNMethod.builder("method2", "task1")
            .subtask(HTNTask.primitive("craft").build())
            .build();

        domain.addMethods(List.of(method1, method2));

        assertTrue(domain.removeMethod("method1"));
        assertEquals(1, domain.getMethodCount());

        assertFalse(domain.removeMethod("nonexistent"));
        assertEquals(1, domain.getMethodCount());
    }

    @Test
    @DisplayName("RemoveMethodsForTask removes all methods for task")
    void testRemoveMethodsForTask() {
        HTNDomain domain = new HTNDomain("test_domain");

        HTNMethod method1 = HTNMethod.builder("method1", "task1")
            .subtask(HTNTask.primitive("mine").build())
            .build();

        HTNMethod method2 = HTNMethod.builder("method2", "task1")
            .subtask(HTNTask.primitive("craft").build())
            .build();

        HTNMethod method3 = HTNMethod.builder("method3", "task2")
            .subtask(HTNTask.primitive("place").build())
            .build();

        domain.addMethods(List.of(method1, method2, method3));

        List<HTNMethod> removed = domain.removeMethodsForTask("task1");

        assertEquals(2, removed.size());
        assertEquals(1, domain.getTaskCount());
        assertEquals(1, domain.getMethodCount());
        assertTrue(domain.hasMethodsFor("task2"));
        assertFalse(domain.hasMethodsFor("task1"));
    }

    @Test
    @DisplayName("RemoveMethodsForTask returns empty list for unknown task")
    void testRemoveMethodsForTaskUnknown() {
        HTNDomain domain = new HTNDomain("test_domain");

        List<HTNMethod> removed = domain.removeMethodsForTask("unknown_task");

        assertNotNull(removed);
        assertTrue(removed.isEmpty());
    }

    @Test
    @DisplayName("Clear removes all methods")
    void testClear() {
        HTNDomain domain = new HTNDomain("test_domain");

        HTNMethod method = HTNMethod.builder("method1", "task1")
            .subtask(HTNTask.primitive("mine").build())
            .build();

        domain.addMethod(method);
        domain.clear();

        assertEquals(0, domain.getTaskCount());
        assertEquals(0, domain.getMethodCount());
    }

    @Test
    @DisplayName("GetTaskCount returns unique task count")
    void testGetTaskCount() {
        HTNDomain domain = new HTNDomain("test_domain");

        HTNMethod method1 = HTNMethod.builder("method1", "task1")
            .subtask(HTNTask.primitive("mine").build())
            .build();

        HTNMethod method2 = HTNMethod.builder("method2", "task1")
            .subtask(HTNTask.primitive("craft").build())
            .build();

        HTNMethod method3 = HTNMethod.builder("method3", "task2")
            .subtask(HTNTask.primitive("place").build())
            .build();

        domain.addMethods(List.of(method1, method2, method3));

        assertEquals(2, domain.getTaskCount());
    }

    @Test
    @DisplayName("GetMethodCount returns total method count")
    void testGetMethodCount() {
        HTNDomain domain = new HTNDomain("test_domain");

        HTNMethod method1 = HTNMethod.builder("method1", "task1")
            .subtask(HTNTask.primitive("mine").build())
            .build();

        HTNMethod method2 = HTNMethod.builder("method2", "task1")
            .subtask(HTNTask.primitive("craft").build())
            .build();

        HTNMethod method3 = HTNMethod.builder("method3", "task2")
            .subtask(HTNTask.primitive("place").build())
            .build();

        domain.addMethods(List.of(method1, method2, method3));

        assertEquals(3, domain.getMethodCount());
    }

    @Test
    @DisplayName("GetTaskNames returns all task names")
    void testGetTaskNames() {
        HTNDomain domain = new HTNDomain("test_domain");

        HTNMethod method1 = HTNMethod.builder("method1", "task1")
            .subtask(HTNTask.primitive("mine").build())
            .build();

        HTNMethod method2 = HTNMethod.builder("method2", "task2")
            .subtask(HTNTask.primitive("craft").build())
            .build();

        HTNMethod method3 = HTNMethod.builder("method3", "task3")
            .subtask(HTNTask.primitive("place").build())
            .build();

        domain.addMethods(List.of(method1, method2, method3));

        Set<String> taskNames = domain.getTaskNames();

        assertEquals(3, taskNames.size());
        assertTrue(taskNames.contains("task1"));
        assertTrue(taskNames.contains("task2"));
        assertTrue(taskNames.contains("task3"));
    }

    @Test
    @DisplayName("GetTaskNames returns unmodifiable set")
    void testGetTaskNamesUnmodifiable() {
        HTNDomain domain = new HTNDomain("test_domain");

        HTNMethod method = HTNMethod.builder("method1", "task1")
            .subtask(HTNTask.primitive("mine").build())
            .build();

        domain.addMethod(method);

        Set<String> taskNames = domain.getTaskNames();

        assertThrows(UnsupportedOperationException.class,
            () -> taskNames.add("new_task"),
            "Task names set should be unmodifiable");
    }

    @Test
    @DisplayName("GetDomainName returns domain name")
    void testGetDomainName() {
        HTNDomain domain = new HTNDomain("custom_domain");

        assertEquals("custom_domain", domain.getDomainName());
    }

    @Test
    @DisplayName("ToString contains domain information")
    void testToString() {
        HTNDomain domain = new HTNDomain("test_domain");

        HTNMethod method = HTNMethod.builder("method1", "task1")
            .subtask(HTNTask.primitive("mine").build())
            .build();

        domain.addMethod(method);

        String str = domain.toString();

        assertTrue(str.contains("test_domain"));
        assertTrue(str.contains("tasks=1"));
        assertTrue(str.contains("methods=1"));
    }

    @Test
    @DisplayName("ToDetailedString includes task breakdown")
    void testToDetailedString() {
        HTNDomain domain = new HTNDomain("test_domain");

        HTNMethod method1 = HTNMethod.builder("method1", "task1")
            .subtask(HTNTask.primitive("mine").build())
            .build();

        HTNMethod method2 = HTNMethod.builder("method2", "task1")
            .subtask(HTNTask.primitive("craft").build())
            .build();

        HTNMethod method3 = HTNMethod.builder("method3", "task2")
            .subtask(HTNTask.primitive("place").build())
            .build();

        domain.addMethods(List.of(method1, method2, method3));

        String str = domain.toDetailedString();

        assertTrue(str.contains("test_domain"));
        assertTrue(str.contains("tasks=2"));
        assertTrue(str.contains("methods=3"));
        assertTrue(str.contains("task1"));
        assertTrue(str.contains("task2"));
    }

    @Test
    @DisplayName("Default domain has build_house methods")
    void testDefaultDomainBuildHouse() {
        HTNDomain domain = HTNDomain.createDefault();

        assertTrue(domain.hasMethodsFor("build_house"));

        List<HTNMethod> methods = domain.getMethodsForTask("build_house");

        assertTrue(methods.size() >= 2,
            "Should have at least 2 build_house methods (with and without materials)");
    }

    @Test
    @DisplayName("Default domain has gather_wood methods")
    void testDefaultDomainGatherWood() {
        HTNDomain domain = HTNDomain.createDefault();

        assertTrue(domain.hasMethodsFor("gather_wood"));

        List<HTNMethod> methods = domain.getMethodsForTask("gather_wood");

        assertTrue(methods.size() >= 1,
            "Should have gather_wood methods");
    }

    @Test
    @DisplayName("Default domain methods have proper priorities")
    void testDefaultDomainPriorities() {
        HTNDomain domain = HTNDomain.createDefault();

        List<HTNMethod> buildMethods = domain.getMethodsForTask("build_house");

        boolean hasHighPriority = buildMethods.stream()
            .anyMatch(m -> m.getPriority() >= 100);

        assertTrue(hasHighPriority,
            "Default domain should have high-priority methods");
    }

    @Test
    @DisplayName("Method precondition checks world state properties")
    void testMethodPreconditionIntegration() {
        HTNDomain domain = new HTNDomain("test_domain");

        HTNMethod method = HTNMethod.builder("test_method", "test_task")
            .precondition((HTNWorldState state) ->
                state.getBoolean("hasAxe") && state.getInt("woodCount") >= 50)
            .subtask(HTNTask.primitive("mine").build())
            .build();

        domain.addMethod(method);

        HTNWorldState validState = HTNWorldState.builder()
            .property("hasAxe", true)
            .property("woodCount", 64)
            .build();

        HTNWorldState invalidState1 = HTNWorldState.builder()
            .property("hasAxe", false)
            .property("woodCount", 64)
            .build();

        HTNWorldState invalidState2 = HTNWorldState.builder()
            .property("hasAxe", true)
            .property("woodCount", 32)
            .build();

        assertEquals(1, domain.getApplicableMethods("test_task", validState).size());
        assertEquals(0, domain.getApplicableMethods("test_task", invalidState1).size());
        assertEquals(0, domain.getApplicableMethods("test_task", invalidState2).size());
    }

    @Test
    @DisplayName("Multiple tasks with same method names")
    void testSameMethodNamesDifferentTasks() {
        HTNDomain domain = new HTNDomain("test_domain");

        HTNMethod method1 = HTNMethod.builder("primary", "task1")
            .subtask(HTNTask.primitive("mine").build())
            .build();

        HTNMethod method2 = HTNMethod.builder("primary", "task2")
            .subtask(HTNTask.primitive("craft").build())
            .build();

        domain.addMethods(List.of(method1, method2));

        assertEquals(2, domain.getTaskCount());
        assertEquals(2, domain.getMethodCount());

        List<HTNMethod> task1Methods = domain.getMethodsForTask("task1");
        List<HTNMethod> task2Methods = domain.getMethodsForTask("task2");

        assertEquals(1, task1Methods.size());
        assertEquals(1, task2Methods.size());
        assertEquals("task1", task1Methods.get(0).getTaskName());
        assertEquals("task2", task2Methods.get(0).getTaskName());
    }

    @Test
    @DisplayName("Domain can be modified after creation")
    void testDomainModification() {
        HTNDomain domain = new HTNDomain("test_domain");

        assertEquals(0, domain.getTaskCount());

        HTNMethod method1 = HTNMethod.builder("method1", "task1")
            .subtask(HTNTask.primitive("mine").build())
            .build();

        domain.addMethod(method1);
        assertEquals(1, domain.getTaskCount());

        HTNMethod method2 = HTNMethod.builder("method2", "task2")
            .subtask(HTNTask.primitive("craft").build())
            .build();

        domain.addMethod(method2);
        assertEquals(2, domain.getTaskCount());

        domain.removeMethod("method1");
        assertEquals(1, domain.getTaskCount());
    }
}

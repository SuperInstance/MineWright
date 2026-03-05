package com.minewright.di;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link SimpleServiceContainer}.
 *
 * Tests cover:
 * <ul>
 *   <li>Service registration by type and name</li>
 *   <li>Service retrieval by type and name</li>
 *   <li>Optional service finding (safe retrieval)</li>
 *   <li>Service existence checking</li>
 *   <li>Service unregistration</li>
 *   <li>Container clearing and counting</li>
 *   <li>Error handling for edge cases</li>
 * </ul>
 *
 * @see SimpleServiceContainer
 * @see ServiceContainer
 * @since 1.1.0
 */
@DisplayName("SimpleServiceContainer Tests")
class SimpleServiceContainerTest {

    private SimpleServiceContainer container;

    // Test service interfaces and implementations
    interface TestService {}
    interface AnotherService {}

    static class TestServiceImpl implements TestService {}
    static class AnotherServiceImpl implements AnotherService {}

    @BeforeEach
    void setUp() {
        container = new SimpleServiceContainer();
    }

    // ==================== Service Registration Tests ====================

    @Test
    @DisplayName("Register service by type")
    void testRegisterServiceByType() {
        TestService service = new TestServiceImpl();

        container.register(TestService.class, service);

        assertTrue(container.hasService(TestService.class),
                   "Service should be registered");
    }

    @Test
    @DisplayName("Register service by name")
    void testRegisterServiceByName() {
        TestService service = new TestServiceImpl();

        container.register("testService", service);

        assertTrue(container.hasService("testService"),
                   "Named service should be registered");
    }

    @Test
    @DisplayName("Register multiple implementations by name")
    void testRegisterMultipleImplementationsByName() {
        TestService service1 = new TestServiceImpl();
        TestService service2 = new TestServiceImpl();

        container.register("primaryService", service1);
        container.register("secondaryService", service2);

        assertTrue(container.hasService("primaryService"));
        assertTrue(container.hasService("secondaryService"));
        assertSame(service1, container.getService("primaryService", TestService.class));
        assertSame(service2, container.getService("secondaryService", TestService.class));
    }

    @Test
    @DisplayName("Replace existing service by type")
    void testReplaceExistingServiceByType() {
        TestService original = new TestServiceImpl();
        TestService replacement = new TestServiceImpl();

        container.register(TestService.class, original);
        assertSame(original, container.getService(TestService.class));

        container.register(TestService.class, replacement);
        assertSame(replacement, container.getService(TestService.class),
                   "Service should be replaced");
    }

    @Test
    @DisplayName("Replace existing service by name")
    void testReplaceExistingServiceByName() {
        TestService original = new TestServiceImpl();
        TestService replacement = new TestServiceImpl();

        container.register("testService", original);
        assertSame(original, container.getService("testService", TestService.class));

        container.register("testService", replacement);
        assertSame(replacement, container.getService("testService", TestService.class),
                   "Named service should be replaced");
    }

    @Test
    @DisplayName("Register service with null type throws exception")
    void testRegisterWithNullTypeThrowsException() {
        TestService service = new TestServiceImpl();

        assertThrows(IllegalArgumentException.class, () -> {
            container.register((Class<TestService>) null, service);
        }, "Should throw exception for null service type");
    }

    @Test
    @DisplayName("Register service with null instance throws exception")
    void testRegisterWithNullInstanceThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            container.register(TestService.class, null);
        }, "Should throw exception for null service instance");
    }

    @Test
    @DisplayName("Register service with blank name throws exception")
    void testRegisterWithBlankNameThrowsException() {
        TestService service = new TestServiceImpl();

        assertThrows(IllegalArgumentException.class, () -> {
            container.register("   ", service);
        }, "Should throw exception for blank service name");
    }

    @Test
    @DisplayName("Register service with empty name throws exception")
    void testRegisterWithEmptyNameThrowsException() {
        TestService service = new TestServiceImpl();

        assertThrows(IllegalArgumentException.class, () -> {
            container.register("", service);
        }, "Should throw exception for empty service name");
    }

    // ==================== Service Retrieval Tests ====================

    @Test
    @DisplayName("Get service by type")
    void testGetServiceByType() {
        TestService service = new TestServiceImpl();

        container.register(TestService.class, service);
        TestService retrieved = container.getService(TestService.class);

        assertSame(service, retrieved, "Should retrieve the same service instance");
    }

    @Test
    @DisplayName("Get service by name and type")
    void testGetServiceByNameAndType() {
        TestService service = new TestServiceImpl();

        container.register("testService", service);
        TestService retrieved = container.getService("testService", TestService.class);

        assertSame(service, retrieved, "Should retrieve the named service");
    }

    @Test
    @DisplayName("Get non-existent service by type throws exception")
    void testGetNonExistentServiceByTypeThrowsException() {
        assertThrows(ServiceContainer.ServiceNotFoundException.class, () -> {
            container.getService(TestService.class);
        }, "Should throw exception for non-existent service");
    }

    @Test
    @DisplayName("Get non-existent service by name throws exception")
    void testGetNonExistentServiceByNameThrowsException() {
        assertThrows(ServiceContainer.ServiceNotFoundException.class, () -> {
            container.getService("nonexistent", TestService.class);
        }, "Should throw exception for non-existent named service");
    }

    @Test
    @DisplayName("Get service with null type throws exception")
    void testGetServiceWithNullTypeThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            container.getService(null);
        }, "Should throw exception for null type");
    }

    @Test
    @DisplayName("Get named service with null name throws exception")
    void testGetNamedServiceWithNullNameThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            container.getService(null, TestService.class);
        }, "Should throw exception for null name");
    }

    @Test
    @DisplayName("Get named service with blank name throws exception")
    void testGetNamedServiceWithBlankNameThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            container.getService("  ", TestService.class);
        }, "Should throw exception for blank name");
    }

    @Test
    @DisplayName("Get named service with null type throws exception")
    void testGetNamedServiceWithNullTypeThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            container.getService("testService", null);
        }, "Should throw exception for null type");
    }

    @Test
    @DisplayName("Get named service with wrong type throws exception")
    void testGetNamedServiceWithWrongTypeThrowsException() {
        TestService service = new TestServiceImpl();

        container.register("testService", service);

        assertThrows(ClassCastException.class, () -> {
            container.getService("testService", AnotherService.class);
        }, "Should throw exception when service is not of requested type");
    }

    // ==================== Optional Service Finding Tests ====================

    @Test
    @DisplayName("Find existing service by type returns Optional with value")
    void testFindExistingServiceByType() {
        TestService service = new TestServiceImpl();

        container.register(TestService.class, service);
        Optional<TestService> result = container.findService(TestService.class);

        assertTrue(result.isPresent(), "Optional should contain value");
        assertSame(service, result.get(), "Should contain the same service instance");
    }

    @Test
    @DisplayName("Find non-existent service by type returns empty Optional")
    void testFindNonExistentServiceByType() {
        Optional<TestService> result = container.findService(TestService.class);

        assertFalse(result.isPresent(), "Optional should be empty for non-existent service");
    }

    @Test
    @DisplayName("Find service with null type returns empty Optional")
    void testFindServiceWithNullType() {
        Optional<TestService> result = container.findService(null);

        assertFalse(result.isPresent(), "Optional should be empty for null type");
    }

    @Test
    @DisplayName("Find existing named service returns Optional with value")
    void testFindExistingNamedService() {
        TestService service = new TestServiceImpl();

        container.register("testService", service);
        Optional<TestService> result = container.findService("testService", TestService.class);

        assertTrue(result.isPresent());
        assertSame(service, result.get());
    }

    @Test
    @DisplayName("Find non-existent named service returns empty Optional")
    void testFindNonExistentNamedService() {
        Optional<TestService> result = container.findService("nonexistent", TestService.class);

        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Find named service with blank name returns empty Optional")
    void testFindNamedServiceWithBlankName() {
        Optional<TestService> result = container.findService("  ", TestService.class);

        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Find named service with null type returns empty Optional")
    void testFindNamedServiceWithNullType() {
        Optional<TestService> result = container.findService("testService", null);

        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Find named service with wrong type returns empty Optional")
    void testFindNamedServiceWithWrongType() {
        TestService service = new TestServiceImpl();

        container.register("testService", service);
        Optional<AnotherService> result = container.findService("testService", AnotherService.class);

        assertFalse(result.isPresent(),
                   "Optional should be empty when service is not of requested type");
    }

    // ==================== Service Existence Checking Tests ====================

    @Test
    @DisplayName("Has service returns true for registered type")
    void testHasServiceForRegisteredType() {
        TestService service = new TestServiceImpl();

        container.register(TestService.class, service);

        assertTrue(container.hasService(TestService.class));
    }

    @Test
    @DisplayName("Has service returns false for non-existent type")
    void testHasServiceForNonExistentType() {
        assertFalse(container.hasService(TestService.class));
    }

    @Test
    @DisplayName("Has service returns false for null type")
    void testHasServiceForNullType() {
        assertFalse(container.hasService((Class<?>) null));
    }

    @Test
    @DisplayName("Has service returns true for registered name")
    void testHasServiceForRegisteredName() {
        TestService service = new TestServiceImpl();

        container.register("testService", service);

        assertTrue(container.hasService("testService"));
    }

    @Test
    @DisplayName("Has service returns false for non-existent name")
    void testHasServiceForNonExistentName() {
        assertFalse(container.hasService("nonexistent"));
    }

    @Test
    @DisplayName("Has service returns false for null name")
    void testHasServiceForNullName() {
        assertFalse(container.hasService((String) null));
    }

    // ==================== Service Unregistration Tests ====================

    @Test
    @DisplayName("Unregister existing service by type")
    void testUnregisterExistingServiceByType() {
        TestService service = new TestServiceImpl();

        container.register(TestService.class, service);
        assertTrue(container.hasService(TestService.class));

        boolean result = container.unregister(TestService.class);

        assertTrue(result, "Unregister should return true");
        assertFalse(container.hasService(TestService.class),
                   "Service should be removed");
    }

    @Test
    @DisplayName("Unregister non-existent service by type returns false")
    void testUnregisterNonExistentServiceByType() {
        boolean result = container.unregister(TestService.class);

        assertFalse(result, "Unregister should return false for non-existent service");
    }

    @Test
    @DisplayName("Unregister with null type returns false")
    void testUnregisterWithNullType() {
        boolean result = container.unregister((Class<?>) null);

        assertFalse(result, "Unregister should return false for null type");
    }

    @Test
    @DisplayName("Unregister existing service by name")
    void testUnregisterExistingServiceByName() {
        TestService service = new TestServiceImpl();

        container.register("testService", service);
        assertTrue(container.hasService("testService"));

        boolean result = container.unregister("testService");

        assertTrue(result, "Unregister should return true");
        assertFalse(container.hasService("testService"));
    }

    @Test
    @DisplayName("Unregister non-existent service by name returns false")
    void testUnregisterNonExistentServiceByName() {
        boolean result = container.unregister("nonexistent");

        assertFalse(result, "Unregister should return false for non-existent named service");
    }

    @Test
    @DisplayName("Unregister with null name returns false")
    void testUnregisterWithNullName() {
        boolean result = container.unregister((String) null);

        assertFalse(result, "Unregister should return false for null name");
    }

    // ==================== Container Management Tests ====================

    @Test
    @DisplayName("Get service count returns zero for empty container")
    void testGetServiceCountForEmptyContainer() {
        assertEquals(0, container.getServiceCount(),
                   "Empty container should have zero services");
    }

    @Test
    @DisplayName("Get service count returns number of registered services")
    void testGetServiceCount() {
        TestService service1 = new TestServiceImpl();
        AnotherService service2 = new AnotherServiceImpl();
        TestService service3 = new TestServiceImpl();

        container.register(TestService.class, service1);
        container.register(AnotherService.class, service2);
        container.register("namedService", service3);

        assertEquals(3, container.getServiceCount(),
                   "Service count should match number of registered services");
    }

    @Test
    @DisplayName("Clear removes all services")
    void testClearRemovesAllServices() {
        TestService service1 = new TestServiceImpl();
        AnotherService service2 = new AnotherServiceImpl();

        container.register(TestService.class, service1);
        container.register("namedService", service2);

        assertTrue(container.getServiceCount() > 0);

        container.clear();

        assertEquals(0, container.getServiceCount(),
                   "All services should be removed");
        assertFalse(container.hasService(TestService.class));
        assertFalse(container.hasService("namedService"));
    }

    @Test
    @DisplayName("Clear on empty container does not throw exception")
    void testClearOnEmptyContainer() {
        assertDoesNotThrow(() -> container.clear(),
                          "Clear should not throw exception on empty container");
        assertEquals(0, container.getServiceCount());
    }

    // ==================== Debug Info Tests ====================

    @Test
    @DisplayName("Debug info contains registered services")
    void testDebugInfo() {
        TestService service1 = new TestServiceImpl();
        AnotherService service2 = new AnotherServiceImpl();

        container.register(TestService.class, service1);
        container.register(AnotherService.class, service2);
        container.register("namedService", service1);

        String debugInfo = container.debugInfo();

        assertTrue(debugInfo.contains("ServiceContainer"),
                   "Debug info should contain container name");
        assertTrue(debugInfo.contains("Type Registry"),
                   "Debug info should contain type registry section");
        assertTrue(debugInfo.contains("Named Registry"),
                   "Debug info should contain named registry section");
        assertTrue(debugInfo.contains("TestService"),
                   "Debug info should list service types");
        assertTrue(debugInfo.contains("AnotherService"),
                   "Debug info should list all service types");
        assertTrue(debugInfo.contains("namedService"),
                   "Debug info should list named services");
    }

    @Test
    @DisplayName("Debug info for empty container")
    void testDebugInfoForEmptyContainer() {
        String debugInfo = container.debugInfo();

        assertTrue(debugInfo.contains("0 services"),
                   "Debug info should show zero services");
    }

    // ==================== Singleton Scope Tests ====================

    @Test
    @DisplayName("Services are singleton scoped by type")
    void testServicesAreSingletonScopedByType() {
        TestService service = new TestServiceImpl();

        container.register(TestService.class, service);

        TestService retrieved1 = container.getService(TestService.class);
        TestService retrieved2 = container.getService(TestService.class);

        assertSame(retrieved1, retrieved2,
                  "Should return same instance for singleton scope");
    }

    @Test
    @DisplayName("Services are singleton scoped by name")
    void testServicesAreSingletonScopedByName() {
        TestService service = new TestServiceImpl();

        container.register("testService", service);

        TestService retrieved1 = container.getService("testService", TestService.class);
        TestService retrieved2 = container.getService("testService", TestService.class);

        assertSame(retrieved1, retrieved2,
                  "Should return same instance for named singleton scope");
    }

    // ==================== Multi-Type Registration Tests ====================

    @Test
    @DisplayName("Can register multiple different service types")
    void testRegisterMultipleDifferentTypes() {
        TestService service1 = new TestServiceImpl();
        AnotherService service2 = new AnotherServiceImpl();

        container.register(TestService.class, service1);
        container.register(AnotherService.class, service2);

        assertTrue(container.hasService(TestService.class));
        assertTrue(container.hasService(AnotherService.class));
        assertSame(service1, container.getService(TestService.class));
        assertSame(service2, container.getService(AnotherService.class));
    }

    @Test
    @DisplayName("Service type and name registries are independent")
    void testTypeAndNameRegistriesAreIndependent() {
        TestService service1 = new TestServiceImpl();
        TestService service2 = new TestServiceImpl();

        container.register(TestService.class, service1);
        container.register("testService", service2);

        assertSame(service1, container.getService(TestService.class));
        assertSame(service2, container.getService("testService", TestService.class));
        assertEquals(2, container.getServiceCount());
    }

    // ==================== ServiceNotFoundException Tests ====================

    @Test
    @DisplayName("ServiceNotFoundException contains service type name")
    void testServiceNotFoundExceptionContainsTypeName() {
        container.register(TestService.class, new TestServiceImpl());

        ServiceContainer.ServiceNotFoundException exception =
            assertThrows(ServiceContainer.ServiceNotFoundException.class, () -> {
                container.getService(AnotherService.class);
            });

        assertTrue(exception.getMessage().contains(AnotherService.class.getName()),
                   "Exception message should contain missing type name");
    }

    @Test
    @DisplayName("ServiceNotFoundException for named service contains name and type")
    void testServiceNotFoundExceptionForNamedService() {
        container.register(TestService.class, new TestServiceImpl());

        ServiceContainer.ServiceNotFoundException exception =
            assertThrows(ServiceContainer.ServiceNotFoundException.class, () -> {
                container.getService("missingService", TestService.class);
            });

        assertTrue(exception.getMessage().contains("missingService"),
                   "Exception message should contain service name");
        assertTrue(exception.getMessage().contains(TestService.class.getName()),
                   "Exception message should contain service type");
    }

    // ==================== Thread Safety Tests ====================

    @Test
    @DisplayName("Concurrent registration is thread-safe")
    void testConcurrentRegistration() throws InterruptedException {
        int threadCount = 10;
        java.util.concurrent.CountDownLatch startLatch = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch doneLatch = new java.util.concurrent.CountDownLatch(threadCount);
        java.util.concurrent.atomic.AtomicInteger successCount = new java.util.concurrent.atomic.AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            new Thread(() -> {
                try {
                    startLatch.await();
                    container.register("service" + index, new TestServiceImpl());
                    successCount.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(5, java.util.concurrent.TimeUnit.SECONDS));

        assertEquals(threadCount, successCount.get());
        assertEquals(threadCount, container.getServiceCount());
    }

    @Test
    @DisplayName("Concurrent retrieval is thread-safe")
    void testConcurrentRetrieval() throws InterruptedException {
        TestService service = new TestServiceImpl();
        container.register(TestService.class, service);

        int threadCount = 10;
        int retrievalsPerThread = 100;
        java.util.concurrent.CountDownLatch startLatch = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch doneLatch = new java.util.concurrent.CountDownLatch(threadCount);
        java.util.concurrent.atomic.AtomicInteger successCount = new java.util.concurrent.atomic.AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < retrievalsPerThread; j++) {
                        TestService retrieved = container.getService(TestService.class);
                        if (retrieved != null) {
                            successCount.incrementAndGet();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(5, java.util.concurrent.TimeUnit.SECONDS));

        assertEquals(threadCount * retrievalsPerThread, successCount.get());
    }

    @Test
    @DisplayName("Concurrent registration and retrieval")
    void testConcurrentRegistrationAndRetrieval() throws InterruptedException {
        int threadCount = 10;
        java.util.concurrent.CountDownLatch startLatch = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch doneLatch = new java.util.concurrent.CountDownLatch(threadCount);
        java.util.concurrent.atomic.AtomicInteger operationsCount = new java.util.concurrent.atomic.AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            new Thread(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < 50; j++) {
                        if (j % 2 == 0) {
                            container.register("service" + index + "_" + j, new TestServiceImpl());
                        } else {
                            container.findService("service" + index + "_" + (j - 1), TestService.class);
                        }
                        operationsCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    // Should not throw exceptions
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(10, java.util.concurrent.TimeUnit.SECONDS));

        assertEquals(threadCount * 50, operationsCount.get());
    }

    @Test
    @DisplayName("Concurrent clear does not cause issues")
    void testConcurrentClear() throws InterruptedException {
        int threadCount = 5;
        java.util.concurrent.CountDownLatch startLatch = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch doneLatch = new java.util.concurrent.CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            new Thread(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < 10; j++) {
                        container.register("service" + index + "_" + j, new TestServiceImpl());
                        if (j % 3 == 0) {
                            container.clear();
                        }
                    }
                } catch (Exception e) {
                    // Should not throw exceptions
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(5, java.util.concurrent.TimeUnit.SECONDS));

        // Container should still be functional
        container.register("final", new TestServiceImpl());
        assertTrue(container.hasService("final"));
    }
}

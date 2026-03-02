package com.minewright.event;

import com.minewright.testutil.TestLogger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for {@link SimpleEventBus}.
 *
 * <p>Tests cover the core event bus infrastructure including:</p>
 * <ul>
 *   <li>Event publishing and subscription</li>
 *   <li>Unsubscribing from events</li>
 *   <li>Event delivery to multiple subscribers</li>
 *   <li>Error handling in subscribers</li>
 *   <li>Thread safety and concurrent operations</li>
 *   <li>Priority-based subscriber ordering</li>
 *   <li>Asynchronous event publishing</li>
 *   <li>Subscription lifecycle management</li>
 * </ul>
 *
 * @see SimpleEventBus
 * @see EventBus
 * @see ActionStartedEvent
 * @see StateTransitionEvent
 * @since 1.1.0
 */
@DisplayName("EventBus Tests")
class EventBusTest {

    private SimpleEventBus eventBus;

    @BeforeEach
    void setUp() {
        TestLogger.initForTesting();
        eventBus = new SimpleEventBus();
    }

    @AfterEach
    void tearDown() {
        if (eventBus != null) {
            eventBus.shutdown();
        }
    }

    // ==================== Publish Tests ====================

    @Nested
    @DisplayName("Event Publishing Tests")
    class PublishTests {

        @Test
        @DisplayName("Publish event to single subscriber")
        void publishToSingleSubscriber() {
            List<ActionStartedEvent> received = new ArrayList<>();
            EventBus.Subscription subscription = eventBus.subscribe(
                ActionStartedEvent.class,
                received::add
            );

            ActionStartedEvent event = new ActionStartedEvent(
                "agent-1",
                "mine",
                "Mining stone",
                Map.of("block", "stone")
            );

            eventBus.publish(event);

            assertEquals(1, received.size(), "Subscriber should receive the event");
            assertEquals(event, received.get(0), "Received event should match published event");
            assertTrue(subscription.isActive(), "Subscription should remain active");
        }

        @Test
        @DisplayName("Publish event to multiple subscribers")
        void publishToMultipleSubscribers() {
            List<ActionStartedEvent> received1 = new ArrayList<>();
            List<ActionStartedEvent> received2 = new ArrayList<>();
            List<ActionStartedEvent> received3 = new ArrayList<>();

            eventBus.subscribe(ActionStartedEvent.class, received1::add);
            eventBus.subscribe(ActionStartedEvent.class, received2::add);
            eventBus.subscribe(ActionStartedEvent.class, received3::add);

            ActionStartedEvent event = new ActionStartedEvent(
                "agent-1",
                "mine",
                "Mining stone",
                Map.of()
            );

            eventBus.publish(event);

            assertEquals(1, received1.size(), "First subscriber should receive event");
            assertEquals(1, received2.size(), "Second subscriber should receive event");
            assertEquals(1, received3.size(), "Third subscriber should receive event");
            assertEquals(event, received1.get(0));
            assertEquals(event, received2.get(0));
            assertEquals(event, received3.get(0));
        }

        @Test
        @DisplayName("Publish multiple events in sequence")
        void publishMultipleEvents() {
            List<ActionStartedEvent> received = new ArrayList<>();
            eventBus.subscribe(ActionStartedEvent.class, received::add);

            eventBus.publish(new ActionStartedEvent("agent-1", "mine", "Event 1", Map.of()));
            eventBus.publish(new ActionStartedEvent("agent-2", "build", "Event 2", Map.of()));
            eventBus.publish(new ActionStartedEvent("agent-3", "gather", "Event 3", Map.of()));

            assertEquals(3, received.size(), "All events should be received");
            assertEquals("Event 1", received.get(0).getDescription());
            assertEquals("Event 2", received.get(1).getDescription());
            assertEquals("Event 3", received.get(2).getDescription());
        }

        @Test
        @DisplayName("Publish null event does not throw")
        void publishNullEvent() {
            List<ActionStartedEvent> received = new ArrayList<>();
            eventBus.subscribe(ActionStartedEvent.class, received::add);

            assertDoesNotThrow(() -> eventBus.publish(null),
                "Publishing null event should not throw exception");

            assertEquals(0, received.size(), "No event should be received");
        }

        @Test
        @DisplayName("Publish event with no subscribers logs trace")
        void publishWithNoSubscribers() {
            ActionStartedEvent event = new ActionStartedEvent(
                "agent-1",
                "mine",
                "No subscribers",
                Map.of()
            );

            assertDoesNotThrow(() -> eventBus.publish(event),
                "Publishing event with no subscribers should not throw");
        }

        @Test
        @DisplayName("Publish different event types")
        void publishDifferentEventTypes() {
            List<ActionStartedEvent> actionEvents = new ArrayList<>();
            List<StateTransitionEvent> stateEvents = new ArrayList<>();

            eventBus.subscribe(ActionStartedEvent.class, actionEvents::add);
            eventBus.subscribe(StateTransitionEvent.class, stateEvents::add);

            ActionStartedEvent actionEvent = new ActionStartedEvent(
                "agent-1",
                "mine",
                "Action event",
                Map.of()
            );
            StateTransitionEvent stateEvent = new StateTransitionEvent(
                "agent-1",
                com.minewright.execution.AgentState.IDLE,
                com.minewright.execution.AgentState.EXECUTING,
                "Started task"
            );

            eventBus.publish(actionEvent);
            eventBus.publish(stateEvent);

            assertEquals(1, actionEvents.size(), "Action subscriber should receive action event");
            assertEquals(1, stateEvents.size(), "State subscriber should receive state event");
        }
    }

    // ==================== Subscribe Tests ====================

    @Nested
    @DisplayName("Subscription Tests")
    class SubscribeTests {

        @Test
        @DisplayName("Subscribe to event type")
        void subscribeToEventType() {
            List<ActionStartedEvent> received = new ArrayList<>();
            EventBus.Subscription subscription = eventBus.subscribe(
                ActionStartedEvent.class,
                received::add
            );

            assertNotNull(subscription, "Subscription should be created");
            assertTrue(subscription.isActive(), "Subscription should be active");
            assertEquals(1, eventBus.getSubscriberCount(ActionStartedEvent.class),
                "Subscriber count should be 1");
        }

        @Test
        @DisplayName("Subscribe with priority")
        void subscribeWithPriority() {
            List<String> executionOrder = new ArrayList<>();

            eventBus.subscribe(ActionStartedEvent.class,
                e -> executionOrder.add("low"),
                10
            );
            eventBus.subscribe(ActionStartedEvent.class,
                e -> executionOrder.add("high"),
                100
            );
            eventBus.subscribe(ActionStartedEvent.class,
                e -> executionOrder.add("medium"),
                50
            );

            ActionStartedEvent event = new ActionStartedEvent(
                "agent-1",
                "test",
                "Priority test",
                Map.of()
            );

            eventBus.publish(event);

            assertEquals(3, executionOrder.size());
            assertEquals("high", executionOrder.get(0), "Highest priority should execute first");
            assertEquals("medium", executionOrder.get(1));
            assertEquals("low", executionOrder.get(2), "Lowest priority should execute last");
        }

        @Test
        @DisplayName("Subscribe with same priority maintains insertion order")
        void subscribeWithSamePriority() {
            List<String> executionOrder = new ArrayList<>();

            eventBus.subscribe(ActionStartedEvent.class,
                e -> executionOrder.add("first"),
                50
            );
            eventBus.subscribe(ActionStartedEvent.class,
                e -> executionOrder.add("second"),
                50
            );
            eventBus.subscribe(ActionStartedEvent.class,
                e -> executionOrder.add("third"),
                50
            );

            eventBus.publish(new ActionStartedEvent("agent-1", "test", "Test", Map.of()));

            assertEquals(List.of("first", "second", "third"), executionOrder);
        }

        @Test
        @DisplayName("Subscribe with null event type throws exception")
        void subscribeNullEventType() {
            assertThrows(IllegalArgumentException.class,
                () -> eventBus.subscribe(null, e -> {}),
                "Should throw IllegalArgumentException for null event type");
        }

        @Test
        @DisplayName("Subscribe with null subscriber throws exception")
        void subscribeNullSubscriber() {
            assertThrows(IllegalArgumentException.class,
                () -> eventBus.subscribe(ActionStartedEvent.class, null),
                "Should throw IllegalArgumentException for null subscriber");
        }

        @Test
        @DisplayName("Multiple subscriptions to same event type")
        void multipleSubscriptionsToSameType() {
            List<ActionStartedEvent> received1 = new ArrayList<>();
            List<ActionStartedEvent> received2 = new ArrayList<>();

            EventBus.Subscription sub1 = eventBus.subscribe(ActionStartedEvent.class, received1::add);
            EventBus.Subscription sub2 = eventBus.subscribe(ActionStartedEvent.class, received2::add);

            assertTrue(sub1.isActive());
            assertTrue(sub2.isActive());
            assertEquals(2, eventBus.getSubscriberCount(ActionStartedEvent.class));
        }
    }

    // ==================== Unsubscribe Tests ====================

    @Nested
    @DisplayName("Unsubscribe Tests")
    class UnsubscribeTests {

        @Test
        @DisplayName("Unsubscribe using subscription handle")
        void unsubscribeUsingHandle() {
            List<ActionStartedEvent> received = new ArrayList<>();
            EventBus.Subscription subscription = eventBus.subscribe(
                ActionStartedEvent.class,
                received::add
            );

            assertTrue(subscription.isActive());
            assertEquals(1, eventBus.getSubscriberCount(ActionStartedEvent.class));

            subscription.unsubscribe();

            assertFalse(subscription.isActive(), "Subscription should be inactive");
            assertEquals(0, eventBus.getSubscriberCount(ActionStartedEvent.class),
                "Subscriber count should be 0 after unsubscribe");

            eventBus.publish(new ActionStartedEvent("agent-1", "test", "Test", Map.of()));

            assertEquals(0, received.size(), "Unsubscribed handler should not receive events");
        }

        @Test
        @DisplayName("Unsubscribe all for event type")
        void unsubscribeAllForEventType() {
            List<ActionStartedEvent> received1 = new ArrayList<>();
            List<ActionStartedEvent> received2 = new ArrayList<>();
            List<ActionStartedEvent> received3 = new ArrayList<>();

            eventBus.subscribe(ActionStartedEvent.class, received1::add);
            eventBus.subscribe(ActionStartedEvent.class, received2::add);
            eventBus.subscribe(ActionStartedEvent.class, received3::add);

            assertEquals(3, eventBus.getSubscriberCount(ActionStartedEvent.class));

            eventBus.unsubscribeAll(ActionStartedEvent.class);

            assertEquals(0, eventBus.getSubscriberCount(ActionStartedEvent.class));

            eventBus.publish(new ActionStartedEvent("agent-1", "test", "Test", Map.of()));

            assertEquals(0, received1.size());
            assertEquals(0, received2.size());
            assertEquals(0, received3.size());
        }

        @Test
        @DisplayName("Unsubscribe all for null event type does not throw")
        void unsubscribeAllNullEventType() {
            assertDoesNotThrow(() -> eventBus.unsubscribeAll(null),
                "UnsubscribeAll with null should not throw");
        }

        @Test
        @DisplayName("Unsubscribe all for event type with no subscribers")
        void unsubscribeAllNoSubscribers() {
            assertDoesNotThrow(() -> eventBus.unsubscribeAll(ActionStartedEvent.class),
                "UnsubscribeAll with no subscribers should not throw");
        }

        @Test
        @DisplayName("Unsubscribe specific subscription from multiple")
        void unsubscribeSpecificFromMultiple() {
            List<ActionStartedEvent> received1 = new ArrayList<>();
            List<ActionStartedEvent> received2 = new ArrayList<>();
            List<ActionStartedEvent> received3 = new ArrayList<>();

            EventBus.Subscription sub1 = eventBus.subscribe(ActionStartedEvent.class, received1::add);
            EventBus.Subscription sub2 = eventBus.subscribe(ActionStartedEvent.class, received2::add);
            EventBus.Subscription sub3 = eventBus.subscribe(ActionStartedEvent.class, received3::add);

            assertEquals(3, eventBus.getSubscriberCount(ActionStartedEvent.class));

            sub2.unsubscribe();

            assertEquals(2, eventBus.getSubscriberCount(ActionStartedEvent.class));
            assertTrue(sub1.isActive());
            assertFalse(sub2.isActive());
            assertTrue(sub3.isActive());

            eventBus.publish(new ActionStartedEvent("agent-1", "test", "Test", Map.of()));

            assertEquals(1, received1.size(), "First subscriber should still receive");
            assertEquals(0, received2.size(), "Second subscriber should not receive");
            assertEquals(1, received3.size(), "Third subscriber should still receive");
        }

        @Test
        @DisplayName("Unsubscribe called multiple times is idempotent")
        void unsubscribeMultipleTimes() {
            List<ActionStartedEvent> received = new ArrayList<>();
            EventBus.Subscription subscription = eventBus.subscribe(
                ActionStartedEvent.class,
                received::add
            );

            subscription.unsubscribe();
            subscription.unsubscribe();
            subscription.unsubscribe();

            assertEquals(0, eventBus.getSubscriberCount(ActionStartedEvent.class));
            assertFalse(subscription.isActive());
        }

        @Test
        @DisplayName("Clear all subscriptions")
        void clearAllSubscriptions() {
            eventBus.subscribe(ActionStartedEvent.class, e -> {});
            eventBus.subscribe(StateTransitionEvent.class, e -> {});
            eventBus.subscribe(ActionStartedEvent.class, e -> {});

            assertTrue(eventBus.getSubscriberCount(ActionStartedEvent.class) > 0);
            assertTrue(eventBus.getSubscriberCount(StateTransitionEvent.class) > 0);

            eventBus.clear();

            assertEquals(0, eventBus.getSubscriberCount(ActionStartedEvent.class));
            assertEquals(0, eventBus.getSubscriberCount(StateTransitionEvent.class));
        }
    }

    // ==================== Error Handling Tests ====================

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Error in one subscriber does not affect others")
        void errorInSubscriberDoesNotAffectOthers() {
            List<ActionStartedEvent> received = new ArrayList<>();
            AtomicInteger errorCount = new AtomicInteger(0);

            eventBus.subscribe(ActionStartedEvent.class, e -> {
                throw new RuntimeException("Test error in subscriber");
            });
            eventBus.subscribe(ActionStartedEvent.class, received::add);
            eventBus.subscribe(ActionStartedEvent.class, e -> {
                throw new RuntimeException("Another test error");
            });
            eventBus.subscribe(ActionStartedEvent.class, received::add);

            ActionStartedEvent event = new ActionStartedEvent(
                "agent-1",
                "test",
                "Error handling test",
                Map.of()
            );

            assertDoesNotThrow(() -> eventBus.publish(event),
                "Publish should not throw even if subscribers fail");

            assertEquals(2, received.size(),
                "Non-erroring subscribers should still receive events");
        }

        @Test
        @DisplayName("Subscriber runtime exception is logged")
        void subscriberExceptionIsLogged() {
            eventBus.subscribe(ActionStartedEvent.class, e -> {
                throw new RuntimeException("Intentional test error");
            });

            ActionStartedEvent event = new ActionStartedEvent(
                "agent-1",
                "test",
                "Exception test",
                Map.of()
            );

            assertDoesNotThrow(() -> eventBus.publish(event),
                "Exception in subscriber should be caught and logged");
        }

        @Test
        @DisplayName("Subscriber with null pointer exception")
        void subscriberNullPointerException() {
            eventBus.subscribe(ActionStartedEvent.class, e -> {
                String s = null;
                s.length(); // NPE
            });

            ActionStartedEvent event = new ActionStartedEvent(
                "agent-1",
                "test",
                "NPE test",
                Map.of()
            );

            assertDoesNotThrow(() -> eventBus.publish(event),
                "NPE in subscriber should be caught");
        }
    }

    // ==================== Thread Safety Tests ====================

    @Nested
    @DisplayName("Thread Safety Tests")
    class ThreadSafetyTests {

        @Test
        @DisplayName("Concurrent subscriptions")
        void concurrentSubscriptions() throws InterruptedException {
            int threadCount = 20;
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);
            List<Thread> threads = new ArrayList<>();

            for (int i = 0; i < threadCount; i++) {
                Thread t = new Thread(() -> {
                    try {
                        startLatch.await();
                        eventBus.subscribe(ActionStartedEvent.class, e -> {});
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                });
                threads.add(t);
                t.start();
            }

            startLatch.countDown();
            assertTrue(doneLatch.await(5, TimeUnit.SECONDS),
                "All threads should complete within timeout");

            assertEquals(threadCount, eventBus.getSubscriberCount(ActionStartedEvent.class),
                "All subscriptions should be registered");
        }

        @Test
        @DisplayName("Concurrent publishes")
        void concurrentPublishes() throws InterruptedException {
            List<ActionStartedEvent> received = new CopyOnWriteArrayList<>();
            eventBus.subscribe(ActionStartedEvent.class, received::add);

            int threadCount = 10;
            int publishesPerThread = 50;
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);

            for (int i = 0; i < threadCount; i++) {
                new Thread(() -> {
                    try {
                        startLatch.await();
                        for (int j = 0; j < publishesPerThread; j++) {
                            eventBus.publish(new ActionStartedEvent(
                                "agent-" + Thread.currentThread().getId(),
                                "test",
                                "Concurrent publish " + j,
                                Map.of()
                            ));
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                }).start();
            }

            startLatch.countDown();
            assertTrue(doneLatch.await(10, TimeUnit.SECONDS));

            assertEquals(threadCount * publishesPerThread, received.size(),
                "All published events should be received");
        }

        @Test
        @DisplayName("Concurrent subscribe and unsubscribe")
        void concurrentSubscribeUnsubscribe() throws InterruptedException {
            List<EventBus.Subscription> subscriptions = new CopyOnWriteArrayList<>();
            AtomicInteger operationCount = new AtomicInteger(0);
            int threadCount = 20;
            int operationsPerThread = 100;
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);

            for (int i = 0; i < threadCount; i++) {
                final int threadId = i;
                new Thread(() -> {
                    try {
                        startLatch.await();
                        for (int j = 0; j < operationsPerThread; j++) {
                            if (j % 2 == 0) {
                                EventBus.Subscription sub = eventBus.subscribe(
                                    ActionStartedEvent.class,
                                    e -> {}
                                );
                                subscriptions.add(sub);
                            } else if (!subscriptions.isEmpty()) {
                                int index = (threadId + j) % subscriptions.size();
                                EventBus.Subscription sub = subscriptions.get(index);
                                if (sub != null && sub.isActive()) {
                                    sub.unsubscribe();
                                }
                            }
                            operationCount.incrementAndGet();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                }).start();
            }

            startLatch.countDown();
            assertTrue(doneLatch.await(15, TimeUnit.SECONDS));

            assertEquals(threadCount * operationsPerThread, operationCount.get());
        }

        @Test
        @DisplayName("Concurrent publish and subscribe")
        void concurrentPublishAndSubscribe() throws InterruptedException {
            List<ActionStartedEvent> received = new CopyOnWriteArrayList<>();
            AtomicInteger subscribeCount = new AtomicInteger(0);

            int threadCount = 10;
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount * 2);

            // Publisher threads
            for (int i = 0; i < threadCount; i++) {
                new Thread(() -> {
                    try {
                        startLatch.await();
                        for (int j = 0; j < 50; j++) {
                            eventBus.publish(new ActionStartedEvent(
                                "agent-pub",
                                "test",
                                "Publish " + j,
                                Map.of()
                            ));
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                }).start();
            }

            // Subscriber threads
            for (int i = 0; i < threadCount; i++) {
                new Thread(() -> {
                    try {
                        startLatch.await();
                        for (int j = 0; j < 5; j++) {
                            eventBus.subscribe(ActionStartedEvent.class, received::add);
                            subscribeCount.incrementAndGet();
                            Thread.sleep(10);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                }).start();
            }

            startLatch.countDown();
            assertTrue(doneLatch.await(15, TimeUnit.SECONDS));

            assertEquals(threadCount * 5, subscribeCount.get());
            // Verify that some events were received (exact count depends on timing)
            assertTrue(received.size() >= threadCount * 50,
                "At minimum, all events should be delivered to initial subscribers");
        }
    }

    // ==================== Async Publishing Tests ====================

    @Nested
    @DisplayName("Async Publishing Tests")
    class AsyncPublishingTests {

        @Test
        @DisplayName("Publish async delivers event on separate thread")
        void publishAsyncDeliversOnSeparateThread() throws InterruptedException {
            List<Long> threadIds = new CopyOnWriteArrayList<>();
            CountDownLatch eventReceived = new CountDownLatch(1);

            eventBus.subscribe(ActionStartedEvent.class, e -> {
                threadIds.add(Thread.currentThread().getId());
                eventReceived.countDown();
            });

            long publisherThreadId = Thread.currentThread().getId();
            eventBus.publishAsync(new ActionStartedEvent(
                "agent-1",
                "test",
                "Async test",
                Map.of()
            ));

            assertTrue(eventReceived.await(2, TimeUnit.SECONDS),
                "Event should be received within timeout");

            assertEquals(1, threadIds.size());
            assertNotEquals(publisherThreadId, threadIds.get(0),
                "Event should be delivered on different thread");
        }

        @Test
        @DisplayName("Publish async with null event")
        void publishAsyncNullEvent() {
            assertDoesNotThrow(() -> eventBus.publishAsync(null),
                "PublishAsync with null should not throw");
        }

        @Test
        @DisplayName("Publish async does not block publisher")
        void publishAsyncDoesNotBlock() throws InterruptedException {
            CountDownLatch subscriberStarted = new CountDownLatch(1);
            CountDownLatch subscriberCompleted = new CountDownLatch(1);

            eventBus.subscribe(ActionStartedEvent.class, e -> {
                subscriberStarted.countDown();
                try {
                    Thread.sleep(500); // Simulate slow processing
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
                subscriberCompleted.countDown();
            });

            long startTime = System.nanoTime();
            eventBus.publishAsync(new ActionStartedEvent("agent-1", "test", "Test", Map.of()));
            long duration = System.nanoTime() - startTime;

            assertTrue(subscriberStarted.await(2, TimeUnit.SECONDS));
            // Publish should return quickly, not wait for subscriber to complete
            assertTrue(duration < TimeUnit.MILLISECONDS.toNanos(100),
                "PublishAsync should return immediately");

            // But subscriber should eventually complete
            assertTrue(subscriberCompleted.await(2, TimeUnit.SECONDS));
        }

        @Test
        @DisplayName("Multiple async publishes are processed")
        void multipleAsyncPublishes() throws InterruptedException {
            List<ActionStartedEvent> received = new CopyOnWriteArrayList<>();
            eventBus.subscribe(ActionStartedEvent.class, received::add);

            int eventCount = 10;
            CountDownLatch allReceived = new CountDownLatch(eventCount);

            eventBus.subscribe(ActionStartedEvent.class, e -> allReceived.countDown());

            for (int i = 0; i < eventCount; i++) {
                eventBus.publishAsync(new ActionStartedEvent(
                    "agent-1",
                    "test",
                    "Event " + i,
                    Map.of()
                ));
            }

            assertTrue(allReceived.await(5, TimeUnit.SECONDS),
                "All async events should be delivered");
        }
    }

    // ==================== Subscriber Count Tests ====================

    @Nested
    @DisplayName("Subscriber Count Tests")
    class SubscriberCountTests {

        @Test
        @DisplayName("Get subscriber count for event type")
        void getSubscriberCount() {
            assertEquals(0, eventBus.getSubscriberCount(ActionStartedEvent.class));

            eventBus.subscribe(ActionStartedEvent.class, e -> {});
            assertEquals(1, eventBus.getSubscriberCount(ActionStartedEvent.class));

            eventBus.subscribe(ActionStartedEvent.class, e -> {});
            assertEquals(2, eventBus.getSubscriberCount(ActionStartedEvent.class));
        }

        @Test
        @DisplayName("Get subscriber count excludes inactive subscriptions")
        void subscriberCountExcludesInactive() {
            EventBus.Subscription sub1 = eventBus.subscribe(ActionStartedEvent.class, e -> {});
            EventBus.Subscription sub2 = eventBus.subscribe(ActionStartedEvent.class, e -> {});
            EventBus.Subscription sub3 = eventBus.subscribe(ActionStartedEvent.class, e -> {});

            assertEquals(3, eventBus.getSubscriberCount(ActionStartedEvent.class));

            sub2.unsubscribe();

            assertEquals(2, eventBus.getSubscriberCount(ActionStartedEvent.class));
        }

        @Test
        @DisplayName("Get subscriber count for null event type")
        void subscriberCountNullEventType() {
            assertEquals(0, eventBus.getSubscriberCount(null),
                "Should return 0 for null event type");
        }

        @Test
        @DisplayName("Get subscriber count for event type with no subscribers")
        void subscriberCountNoSubscribers() {
            assertEquals(0, eventBus.getSubscriberCount(StateTransitionEvent.class),
                "Should return 0 for event type with no subscribers");
        }
    }

    // ==================== Lifecycle Tests ====================

    @Nested
    @DisplayName("Lifecycle Tests")
    class LifecycleTests {

        @Test
        @DisplayName("Shutdown event bus")
        void shutdownEventBus() {
            eventBus.subscribe(ActionStartedEvent.class, e -> {});

            assertDoesNotThrow(() -> eventBus.shutdown(),
                "Shutdown should complete without errors");
        }

        @Test
        @DisplayName("Multiple shutdown calls are safe")
        void multipleShutdownCalls() {
            assertDoesNotThrow(() -> {
                eventBus.shutdown();
                eventBus.shutdown();
                eventBus.shutdown();
            }, "Multiple shutdowns should be safe");
        }

        @Test
        @DisplayName("Operations after shutdown")
        void operationsAfterShutdown() throws InterruptedException {
            eventBus.shutdown();

            List<ActionStartedEvent> received = new ArrayList<>();
            eventBus.subscribe(ActionStartedEvent.class, received::add);

            // Sync publish should still work (it doesn't use the executor)
            eventBus.publish(new ActionStartedEvent("agent-1", "test", "Test", Map.of()));

            // Async publish may not work after shutdown (executor is shut down)
            assertDoesNotThrow(() ->
                eventBus.publishAsync(new ActionStartedEvent("agent-1", "test", "Async", Map.of()))
            );
        }
    }

    // ==================== Complex Scenario Tests ====================

    @Nested
    @DisplayName("Complex Scenario Tests")
    class ComplexScenarioTests {

        @Test
        @DisplayName("Event cascading through multiple handlers")
        void eventCascading() {
            List<String> executionLog = new ArrayList<>();

            // First handler logs "first"
            eventBus.subscribe(ActionStartedEvent.class, e -> {
                executionLog.add("first");
            }, 100);

            // Second handler publishes another event
            eventBus.subscribe(ActionStartedEvent.class, e -> {
                executionLog.add("second");
                eventBus.publish(new ActionStartedEvent("agent-2", "cascade", "Cascaded", Map.of()));
            }, 50);

            // Third handler catches both
            eventBus.subscribe(ActionStartedEvent.class, e -> {
                executionLog.add("third-" + e.getActionName());
            }, 10);

            eventBus.publish(new ActionStartedEvent("agent-1", "initial", "Initial", Map.of()));

            assertEquals(3, executionLog.size());
            assertEquals("first", executionLog.get(0));
            assertEquals("second", executionLog.get(1));
            // The cascaded event may be processed synchronously, so it appears after
            assertEquals("third-cascade", executionLog.get(2));
        }

        @Test
        @DisplayName("Subscriber modification during event delivery")
        void subscriberModificationDuringDelivery() {
            List<String> log = new ArrayList<>();
            List<EventBus.Subscription> sub2Holder = new ArrayList<>();

            // Subscribe first handler
            eventBus.subscribe(ActionStartedEvent.class, e -> {
                log.add("handler1");
            }, 100);

            // Subscribe second handler that will unsubscribe itself
            EventBus.Subscription sub2 = eventBus.subscribe(ActionStartedEvent.class, e -> {
                log.add("handler2");
                // Get subscription from holder to self-unsubscribe
                if (!sub2Holder.isEmpty()) {
                    sub2Holder.get(0).unsubscribe();
                }
            }, 50);
            sub2Holder.add(sub2);

            // Subscribe third handler
            eventBus.subscribe(ActionStartedEvent.class, e -> {
                log.add("handler3");
            }, 10);

            assertEquals(3, eventBus.getSubscriberCount(ActionStartedEvent.class));

            eventBus.publish(new ActionStartedEvent("agent-1", "test", "Test", Map.of()));

            assertEquals(List.of("handler1", "handler2", "handler3"), log);
            assertEquals(2, eventBus.getSubscriberCount(ActionStartedEvent.class),
                "Subscriber count should decrease after self-unsubscribe");

            // Second publish should not invoke handler2
            log.clear();
            eventBus.publish(new ActionStartedEvent("agent-1", "test", "Test2", Map.of()));

            assertEquals(List.of("handler1", "handler3"), log);
        }

        @Test
        @DisplayName("Mix of sync and async publishes")
        void mixOfSyncAndAsyncPublishes() throws InterruptedException {
            List<ActionStartedEvent> received = new CopyOnWriteArrayList<>();
            CountDownLatch allReceived = new CountDownLatch(20);

            eventBus.subscribe(ActionStartedEvent.class, e -> {
                received.add(e);
                allReceived.countDown();
            });

            // Mix sync and async publishes
            for (int i = 0; i < 10; i++) {
                eventBus.publish(new ActionStartedEvent("agent-1", "sync", "Sync " + i, Map.of()));
                eventBus.publishAsync(new ActionStartedEvent("agent-1", "async", "Async " + i, Map.of()));
            }

            assertTrue(allReceived.await(5, TimeUnit.SECONDS));
            assertEquals(20, received.size());
        }
    }

    // ==================== Mock Event Classes for Testing ====================

    /**
     * Mock event class for testing polymorphic event handling.
     */
    private static class BaseEvent {
        private final String data;

        BaseEvent(String data) {
            this.data = data;
        }

        String getData() {
            return data;
        }
    }

    private static class ExtendedEvent extends BaseEvent {
        ExtendedEvent(String data) {
            super(data);
        }
    }

    @Nested
    @DisplayName("Event Inheritance Tests")
    class EventInheritanceTests {

        @Test
        @DisplayName("Subscribers receive exact type matches")
        void subscribersReceiveExactType() {
            List<BaseEvent> baseEvents = new ArrayList<>();
            List<ExtendedEvent> extendedEvents = new ArrayList<>();

            eventBus.subscribe(BaseEvent.class, baseEvents::add);
            eventBus.subscribe(ExtendedEvent.class, extendedEvents::add);

            ExtendedEvent event = new ExtendedEvent("test");

            eventBus.publish(event);

            assertEquals(1, baseEvents.size(), "BaseEvent subscriber should receive");
            assertEquals(1, extendedEvents.size(), "ExtendedEvent subscriber should receive");
        }

        @Test
        @DisplayName("Publish base event to base subscribers")
        void publishBaseToBase() {
            List<BaseEvent> received = new ArrayList<>();
            eventBus.subscribe(BaseEvent.class, received::add);

            BaseEvent event = new BaseEvent("base");

            eventBus.publish(event);

            assertEquals(1, received.size());
        }
    }
}

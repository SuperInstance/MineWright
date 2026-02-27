package com.minewright.execution;

import com.minewright.action.actions.BaseAction;
import com.minewright.action.ActionResult;
import com.minewright.event.ActionCompletedEvent;
import com.minewright.event.ActionStartedEvent;
import com.minewright.event.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Interceptor that publishes action lifecycle events to the EventBus.
 *
 * <p>Enables decoupled observation of action execution through the
 * pub-sub pattern. Other components can subscribe to ActionStartedEvent
 * and ActionCompletedEvent without coupling to the action execution.</p>
 *
 * @since 1.1.0
 */
public class EventPublishingInterceptor implements ActionInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventPublishingInterceptor.class);

    private final EventBus eventBus;
    private final String agentId;

    /**
     * Tracks start times for duration calculation.
     */
    private final ActionUtils.ActionTimer actionTimer = new ActionUtils.ActionTimer();

    /**
     * Constructs an EventPublishingInterceptor.
     *
     * @param eventBus EventBus to publish to
     * @param agentId  Agent identifier for events
     */
    public EventPublishingInterceptor(EventBus eventBus, String agentId) {
        this.eventBus = eventBus;
        this.agentId = agentId;
    }

    @Override
    public boolean beforeAction(BaseAction action, ActionContext context) {
        // Record start time
        actionTimer.recordStart(action);

        // Publish ActionStartedEvent
        ActionStartedEvent event = new ActionStartedEvent(
            agentId,
            ActionUtils.extractActionType(action),
            action.getDescription(),
            Map.of() // Could extract parameters if needed
        );

        eventBus.publish(event);
        LOGGER.debug("Published ActionStartedEvent: {}", action.getDescription());

        return true;
    }

    @Override
    public void afterAction(BaseAction action, ActionResult result, ActionContext context) {
        // Calculate duration
        long duration = actionTimer.getElapsedAndRemove(action);

        // Publish ActionCompletedEvent
        ActionCompletedEvent event = new ActionCompletedEvent(
            agentId,
            ActionUtils.extractActionType(action),
            result.isSuccess(),
            result.getMessage(),
            duration
        );

        eventBus.publish(event);
        LOGGER.debug("Published ActionCompletedEvent: {} (success: {}, duration: {}ms)",
            action.getDescription(), result.isSuccess(), duration);
    }

    @Override
    public boolean onError(BaseAction action, Exception exception, ActionContext context) {
        // Calculate duration
        long duration = actionTimer.getElapsedAndRemove(action);

        // Publish failed completion event
        ActionCompletedEvent event = new ActionCompletedEvent(
            agentId,
            ActionUtils.extractActionType(action),
            false,
            "Exception: " + exception.getMessage(),
            duration
        );

        eventBus.publish(event);
        return false;
    }

    @Override
    public int getPriority() {
        return 500; // Medium-high priority
    }

    @Override
    public String getName() {
        return "EventPublishingInterceptor";
    }
}

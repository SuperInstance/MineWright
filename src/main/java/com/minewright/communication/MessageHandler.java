package com.minewright.communication;

/**
 * Functional interface for handling inter-agent messages.
 *
 * <p>Message handlers are registered with the {@link CommunicationBus}
 * and invoked when messages are delivered to an agent.</p>
 *
 * <p><b>Example Usage:</b></p>
 * <pre>
 * MessageHandler handler = message -> {
 *     switch (message.type()) {
 *         case STATUS_UPDATE -> handleStatusUpdate(message);
 *         case TASK_REQUEST -> handleTaskRequest(message);
 *         case ALERT -> handleAlert(message);
 *         // ... handle other types
 *     }
 * };
 *
 * bus.register(agentId, handler);
 * </pre>
 *
 * <p><b>Thread Safety:</b> Handlers may be called concurrently from different
 * threads. Implementations must be thread-safe or use external synchronization.</p>
 *
 * <p><b>Error Handling:</b> Exceptions thrown by handlers are caught and logged
 * by the CommunicationBus, preventing one agent's handler from affecting others.</p>
 *
 * @since 1.3.0
 * @see CommunicationBus
 * @see AgentMessage
 */
@FunctionalInterface
public interface MessageHandler {

    /**
     * Handles an incoming message.
     *
     * <p>This method is called synchronously when a message is delivered.
     * Implementations should return quickly to avoid blocking message processing.</p>
     *
     * <p><b>Important:</b> This method must be thread-safe. It may be called
     * concurrently from multiple threads.</p>
     *
     * @param message The message to handle
     * @throws MessageHandlerException if handling fails (will be caught and logged)
     */
    void handle(AgentMessage message);

    /**
     * Default handler that does nothing.
     */
    static MessageHandler noop() {
        return message -> {
            // Do nothing
        };
    }

    /**
     * Creates a handler that delegates based on message type.
     *
     * @param handlers Map of message type to handler
     * @return Composite handler
     */
    static MessageHandler dispatching(java.util.Map<AgentMessage.MessageType,
                                       MessageHandler> handlers) {
        return message -> {
            MessageHandler handler = handlers.get(message.type());
            if (handler != null) {
                handler.handle(message);
            }
        };
    }

    /**
     * Chains multiple handlers together.
     *
     * @param handlers Handlers to chain
     * @return Composite handler that calls all handlers in order
     */
    static MessageHandler chaining(MessageHandler... handlers) {
        return message -> {
            for (MessageHandler handler : handlers) {
                handler.handle(message);
            }
        };
    }

    /**
     * Creates a handler with error handling.
     *
     * @param delegate The delegate handler
     * @param errorHandler Error handler
     * @return Handler with error handling
     */
    static MessageHandler withErrorHandling(MessageHandler delegate,
                                           java.util.function.Consumer<Exception> errorHandler) {
        return message -> {
            try {
                delegate.handle(message);
            } catch (Exception e) {
                errorHandler.accept(e);
            }
        };
    }

    /**
     * Creates a handler that filters messages.
     *
     * @param filter Filter predicate
     * @param delegate Handler to call if filter passes
     * @return Filtered handler
     */
    static MessageHandler filtering(java.util.function.Predicate<AgentMessage> filter,
                                    MessageHandler delegate) {
        return message -> {
            if (filter.test(message)) {
                delegate.handle(message);
            }
        };
    }

    /**
     * Creates a handler that only handles specific message types.
     *
     * @param types Message types to handle
     * @param delegate Handler to call for matching types
     * @return Type-filtered handler
     */
    static MessageHandler forTypes(java.util.Set<AgentMessage.MessageType> types,
                                   MessageHandler delegate) {
        return filtering(message -> types.contains(message.type()), delegate);
    }
}

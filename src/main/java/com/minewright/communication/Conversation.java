package com.minewright.communication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Tracks multi-turn conversations between agents.
 *
 * <p>A conversation represents a sequence of related messages between agents,
 * typically for coordinating complex tasks that require multiple exchanges.
 * Conversations track state, participants, and message history.</p>
 *
 * <p><b>Conversation States:</b></p>
 * <ul>
 *   <li>INITIATED - Conversation started, waiting for response</li>
 *   <li>ACTIVE - Conversation in progress</li>
 *   <li>PAUSED - Conversation temporarily suspended</li>
 *   <li>COMPLETED - Conversation successfully concluded</li>
 *   <li>FAILED - Conversation failed or timed out</li>
 *   <li>CANCELLED - Conversation cancelled by participant</li>
 * </ul>
 *
 * <p><b>Example Usage:</b></p>
 * <pre>
 * Conversation conv = new Conversation(
 *     UUID.randomUUID(),
 *     Set.of(agent1, agent2),
 *     "Collaborative build coordination"
 * );
 *
 * // Add messages
 * conv.addMessage(taskRequest);
 * conv.addMessage(taskResponse);
 *
 * // Check state
 * if (conv.isActive()) {
 *     // Still ongoing
 * }
 *
 * // Get last message from specific agent
 * Optional&lt;AgentMessage&gt; last = conv.getLastFrom(agent1);
 * </pre>
 *
 * <p><b>Thread Safety:</b> This class is thread-safe using concurrent collections.</p>
 *
 * @since 1.3.0
 */
public class Conversation {

    private static final Logger LOGGER = LoggerFactory.getLogger(Conversation.class);

    /**
     * Default timeout for inactive conversations (5 minutes).
     */
    private static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(5);

    /**
     * Unique conversation identifier.
     */
    private final UUID conversationId;

    /**
     * Description of the conversation purpose.
     */
    private final String description;

    /**
     * Participants in this conversation.
     */
    private final Set<UUID> participants;

    /**
     * Messages in this conversation, in order.
     */
    private final List<AgentMessage> messages;

    /**
     * Current conversation state.
     */
    private volatile ConversationState state;

    /**
     * When the conversation was created.
     */
    private final Instant createdAt;

    /**
     * When the conversation was last updated.
     */
    private volatile Instant lastUpdatedAt;

    /**
     * When the conversation timed out (null if not timed out).
     */
    private volatile Instant timedOutAt;

    /**
     * Timeout duration for inactivity.
     */
    private final Duration timeout;

    /**
     * Additional metadata for the conversation.
     */
    private final Map<String, Object> metadata;

    /**
     * Conversation states.
     */
    public enum ConversationState {
        /** Conversation initiated, waiting for first response */
        INITIATED,

        /** Conversation actively in progress */
        ACTIVE,

        /** Conversation paused (can be resumed) */
        PAUSED,

        /** Conversation completed successfully */
        COMPLETED,

        /** Conversation failed */
        FAILED,

        /** Conversation cancelled */
        CANCELLED
    }

    /**
     * Creates a new conversation.
     *
     * @param conversationId Unique conversation ID
     * @param participants Conversation participants
     * @param description Conversation description
     */
    public Conversation(UUID conversationId, Set<UUID> participants, String description) {
        this(conversationId, participants, description, DEFAULT_TIMEOUT);
    }

    /**
     * Creates a new conversation with custom timeout.
     *
     * @param conversationId Unique conversation ID
     * @param participants Conversation participants
     * @param description Conversation description
     * @param timeout Inactivity timeout
     */
    public Conversation(UUID conversationId, Set<UUID> participants,
                       String description, Duration timeout) {
        this.conversationId = Objects.requireNonNull(conversationId, "conversationId cannot be null");
        this.participants = Collections.newSetFromMap(new ConcurrentHashMap<>());
        this.participants.addAll(Objects.requireNonNull(participants, "participants cannot be null"));
        this.description = Objects.requireNonNull(description, "description cannot be null");
        this.messages = new ArrayList<>();
        this.state = ConversationState.INITIATED;
        this.createdAt = Instant.now();
        this.lastUpdatedAt = Instant.now();
        this.timeout = Objects.requireNonNull(timeout, "timeout cannot be null");
        this.metadata = new ConcurrentHashMap<>();

        LOGGER.debug("Created conversation: {} with participants: {}", conversationId, participants);
    }

    /**
     * Gets the conversation ID.
     *
     * @return Conversation ID
     */
    public UUID getConversationId() {
        return conversationId;
    }

    /**
     * Gets the conversation description.
     *
     * @return Description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Gets the conversation participants.
     *
     * @return Set of participant IDs
     */
    public Set<UUID> getParticipants() {
        return Collections.unmodifiableSet(participants);
    }

    /**
     * Gets all messages in the conversation.
     *
     * @return List of messages (in order)
     */
    public List<AgentMessage> getMessages() {
        return Collections.unmodifiableList(messages);
    }

    /**
     * Gets the current conversation state.
     *
     * @return Current state
     */
    public ConversationState getState() {
        return state;
    }

    /**
     * Checks if the conversation is active.
     *
     * @return true if state is INITIATED or ACTIVE
     */
    public boolean isActive() {
        return state == ConversationState.INITIATED || state == ConversationState.ACTIVE;
    }

    /**
     * Checks if the conversation is completed.
     *
     * @return true if state is COMPLETED
     */
    public boolean isCompleted() {
        return state == ConversationState.COMPLETED;
    }

    /**
     * Checks if the conversation has failed.
     *
     * @return true if state is FAILED
     */
    public boolean isFailed() {
        return state == ConversationState.FAILED;
    }

    /**
     * Gets when the conversation was created.
     *
     * @return Creation timestamp
     */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * Gets when the conversation was last updated.
     *
     * @return Last update timestamp
     */
    public Instant getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    /**
     * Gets the conversation timeout duration.
     *
     * @return Timeout duration
     */
    public Duration getTimeout() {
        return timeout;
    }

    /**
     * Checks if the conversation has timed out.
     *
     * @return true if timed out
     */
    public boolean isTimedOut() {
        return timedOutAt != null || Duration.between(lastUpdatedAt, Instant.now()).compareTo(timeout) > 0;
    }

    /**
     * Gets metadata value.
     *
     * @param key Metadata key
     * @param <T> Expected type
     * @return Value or null
     */
    @SuppressWarnings("unchecked")
    public <T> T getMetadata(String key) {
        return (T) metadata.get(key);
    }

    /**
     * Sets metadata value.
     *
     * @param key Metadata key
     * @param value Metadata value
     */
    public void setMetadata(String key, Object value) {
        metadata.put(key, value);
    }

    /**
     * Adds a message to the conversation.
     *
     * @param message Message to add
     * @throws IllegalArgumentException if sender is not a participant
     */
    public synchronized void addMessage(AgentMessage message) {
        Objects.requireNonNull(message, "message cannot be null");

        if (!participants.contains(message.senderId())) {
            throw new IllegalArgumentException(
                "Sender " + message.senderId() + " is not a participant");
        }

        messages.add(message);
        lastUpdatedAt = Instant.now();

        // Update state based on message count
        if (state == ConversationState.INITIATED && messages.size() > 1) {
            state = ConversationState.ACTIVE;
        }

        LOGGER.debug("Added message to conversation {}: {}", conversationId, message.type());
    }

    /**
     * Gets the last message from a specific participant.
     *
     * @param agentId Agent ID
     * @return Last message or empty if no messages from this agent
     */
    public Optional<AgentMessage> getLastFrom(UUID agentId) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            AgentMessage message = messages.get(i);
            if (message.senderId().equals(agentId)) {
                return Optional.of(message);
            }
        }
        return Optional.empty();
    }

    /**
     * Gets all messages from a specific participant.
     *
     * @param agentId Agent ID
     * @return List of messages from this agent
     */
    public List<AgentMessage> getMessagesFrom(UUID agentId) {
        return messages.stream()
            .filter(m -> m.senderId().equals(agentId))
            .collect(Collectors.toList());
    }

    /**
     * Gets messages of a specific type.
     *
     * @param type Message type
     * @return List of messages of this type
     */
    public List<AgentMessage> getMessagesOfType(AgentMessage.MessageType type) {
        return messages.stream()
            .filter(m -> m.type() == type)
            .collect(Collectors.toList());
    }

    /**
     * Transitions the conversation to a new state.
     *
     * @param newState New state
     * @throws IllegalStateException if transition is invalid
     */
    public synchronized void transitionTo(ConversationState newState) {
        Objects.requireNonNull(newState, "newState cannot be null");

        if (!isValidTransition(state, newState)) {
            throw new IllegalStateException(
                "Invalid state transition: " + state + " -> " + newState);
        }

        ConversationState oldState = this.state;
        this.state = newState;
        this.lastUpdatedAt = Instant.now();

        LOGGER.debug("Conversation {} state: {} -> {}", conversationId, oldState, newState);
    }

    /**
     * Completes the conversation successfully.
     */
    public void complete() {
        transitionTo(ConversationState.COMPLETED);
    }

    /**
     * Fails the conversation.
     */
    public void fail() {
        transitionTo(ConversationState.FAILED);
    }

    /**
     * Cancels the conversation.
     */
    public void cancel() {
        transitionTo(ConversationState.CANCELLED);
    }

    /**
     * Pauses the conversation.
     */
    public void pause() {
        transitionTo(ConversationState.PAUSED);
    }

    /**
     * Resumes a paused conversation.
     */
    public void resume() {
        if (state != ConversationState.PAUSED) {
            throw new IllegalStateException("Can only resume from PAUSED state");
        }
        transitionTo(ConversationState.ACTIVE);
    }

    /**
     * Marks the conversation as timed out.
     */
    public void markTimedOut() {
        this.timedOutAt = Instant.now();
        this.state = ConversationState.FAILED;
        LOGGER.warn("Conversation {} timed out after {} of inactivity",
            conversationId, Duration.between(lastUpdatedAt, timedOutAt));
    }

    /**
     * Gets the duration since creation.
     *
     * @return Duration since creation
     */
    public Duration getAge() {
        return Duration.between(createdAt, Instant.now());
    }

    /**
     * Gets the duration since last update.
     *
     * @return Duration since last update
     */
    public Duration getTimeSinceLastUpdate() {
        return Duration.between(lastUpdatedAt, Instant.now());
    }

    /**
     * Gets the number of messages in the conversation.
     *
     * @return Message count
     */
    public int getMessageCount() {
        return messages.size();
    }

    /**
     * Checks if a given agent is a participant.
     *
     * @param agentId Agent ID
     * @return true if participant
     */
    public boolean hasParticipant(UUID agentId) {
        return participants.contains(agentId);
    }

    /**
     * Checks if the conversation involves both agents.
     *
     * @param agent1 First agent
     * @param agent2 Second agent
     * @return true if both are participants
     */
    public boolean involves(UUID agent1, UUID agent2) {
        return participants.contains(agent1) && participants.contains(agent2);
    }

    /**
     * Gets a summary of the conversation.
     *
     * @return Summary string
     */
    public String getSummary() {
        return String.format("Conversation[%s: %s, state=%s, participants=%d, messages=%d, age=%s]",
            conversationId.toString().substring(0, 8),
            description,
            state,
            participants.size(),
            messages.size(),
            getAge());
    }

    @Override
    public String toString() {
        return getSummary();
    }

    /**
     * Checks if a state transition is valid.
     */
    private boolean isValidTransition(ConversationState from, ConversationState to) {
        return switch (from) {
            case INITIATED -> to == ConversationState.ACTIVE
                || to == ConversationState.CANCELLED
                || to == ConversationState.FAILED;
            case ACTIVE -> to == ConversationState.COMPLETED
                || to == ConversationState.FAILED
                || to == ConversationState.CANCELLED
                || to == ConversationState.PAUSED;
            case PAUSED -> to == ConversationState.ACTIVE
                || to == ConversationState.CANCELLED
                || to == ConversationState.FAILED;
            case COMPLETED, FAILED, CANCELLED -> false; // Terminal states
        };
    }

    /**
     * Builder for creating conversations.
     */
    public static class Builder {
        private UUID conversationId;
        private final Set<UUID> participants = new HashSet<>();
        private String description;
        private Duration timeout = DEFAULT_TIMEOUT;
        private final Map<String, Object> metadata = new HashMap<>();

        public Builder conversationId(UUID conversationId) {
            this.conversationId = conversationId;
            return this;
        }

        public Builder addParticipant(UUID participant) {
            this.participants.add(participant);
            return this;
        }

        public Builder participants(Set<UUID> participants) {
            this.participants.addAll(participants);
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder metadata(String key, Object value) {
            this.metadata.put(key, value);
            return this;
        }

        public Conversation build() {
            if (conversationId == null) {
                conversationId = UUID.randomUUID();
            }
            if (description == null) {
                description = "Conversation";
            }
            if (participants.isEmpty()) {
                throw new IllegalStateException("At least one participant is required");
            }

            Conversation conv = new Conversation(conversationId, participants, description, timeout);
            conv.metadata.putAll(metadata);
            return conv;
        }
    }
}

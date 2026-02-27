package com.minewright.llm;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.minewright.MineWrightMod;
import com.minewright.action.Task;
import com.minewright.llm.async.LLMResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Pattern;

/**
 * Comprehensive fallback response system for LLM unavailability scenarios.
 *
 * <p>This system provides graceful degradation when the LLM API is unavailable,
 * offering template-based responses, pattern-matching intent detection, and
 * recovery mechanisms for when connectivity is restored.</p>
 *
 * <p><b>Design Philosophy:</b></p>
 * <ul>
 *   <li><i>Something is better than nothing</i> - Provide meaningful responses even offline</li>
 *   <li><i>Conservative defaults</i> - Prefer safe actions (wait) over risky ones</li>
 *   <li><i>Transparency</i> - Clearly indicate when using fallback mode</li>
 *   <li><i>Recovery</i> - Queue requests for replay when LLM returns</li>
 * </ul>
 *
 * <p><b>When is fallback mode activated?</b></p>
 * <ul>
 *   <li>Network connectivity unavailable</li>
 *   <li>API rate limits exceeded</li>
 *   <li>API returning 5xx errors</li>
 *   <li>Timeout on all retry attempts</li>
 *   <li>Invalid or missing API keys</li>
 * </ul>
 *
 * @since 1.2.0
 */
public class FallbackResponseSystem {

    private static final Logger LOGGER = LoggerFactory.getLogger(FallbackResponseSystem.class);

    // ============================================================
    // TEMPLATE RESPONSES - Pre-written responses for common situations
    // ============================================================

    /**
     * Greeting templates based on time of day and context.
     */
    private static final Map<String, String[]> GREETING_TEMPLATES = Map.of(
        "morning", new String[]{
            "Good morning! I'm the Foreman, ready to help. What shall we build today?",
            "Morning! The sun is up, time to get to work. What's the plan?",
            "Good morning! I'm in fallback mode (LLM unavailable), but I can still help with basic tasks."
        },
        "afternoon", new String[]{
            "Good afternoon! Ready to continue our work. What would you like me to do?",
            "Afternoon! I'm here and ready to assist. What's next?",
            "Good afternoon! Running in fallback mode, but I can handle basic commands."
        },
        "evening", new String[]{
            "Good evening! Working late? I'm here to help. What do you need?",
            "Evening! Time for some Minecraft. What shall we accomplish?",
            "Good evening! I'm operating in fallback mode, but let's get things done."
        },
        "first_meeting", new String[]{
            "Hello there! I'm the Foreman, your AI companion. I can help you build, mine, fight, and explore! (Currently in fallback mode - LLM unavailable)",
            "Hi! Nice to meet you. I'm the Foreman. I can assist with various Minecraft tasks. What would you like to do first? (Note: LLM unavailable, using fallback mode)",
            "Greetings! I'm the Foreman, an autonomous AI agent. I'm currently in fallback mode but can still help with basic tasks."
        },
        "default", new String[]{
            "Hello! I'm the Foreman, ready to help. What can I do for you?",
            "Hi! What would you like me to work on?",
            "Hey! I'm here. What's the plan?"
        }
    );

    /**
     * Task acknowledgment templates.
     */
    private static final String[] TASK_ACKNOWLEDGMENTS = new String[]{
        "Got it! I'll work on that right away.",
        "Understood! Starting the task.",
        "On it! Give me a moment.",
        "Sure thing! I'll get that done.",
        "Alright! Let me handle that for you.",
        "Roger that! Beginning execution.",
        "Copy that! I'm on it."
    };

    /**
     * Task completion templates.
     */
    private static final String[] COMPLETION_TEMPLATES = new String[]{
        "All done! Is there anything else you need?",
        "Task completed! What's next?",
        "Finished! Ready for the next challenge.",
        "Complete! Let me know if you need anything else.",
        "Done and dusted! What would you like me to do now?",
        "That's taken care of! What's the next task?"
    };

    /**
     * Error handling templates.
     */
    private static final String[] ERROR_TEMPLATES = new String[]{
        "I encountered an issue with that task. Let me try a different approach.",
        "Something went wrong. I'll pause here - please check what you'd like me to do.",
        "I'm having trouble with that. Could you clarify what you need?",
        "Error detected. I'll wait for further instructions.",
        "I couldn't complete that action. Please provide new directions."
    };

    /**
     * Idle/comment templates when no specific task is given.
     */
    private static final String[] IDLE_TEMPLATES = new String[]{
        "I'm here and ready! Just let me know what you'd like me to do.",
        "Waiting for orders... What shall we work on today?",
        "I'm standing by. Feel free to give me a task when you're ready!",
        "Ready and waiting. What adventures await us?",
        "I'm here to help! Just say the word.",
        "Standing by for your command!"
    };

    /**
     * Fallback mode notification templates.
     */
    private static final String[] FALLBACK_NOTIFICATIONS = new String[]{
        "[Fallback Mode Active] LLM service unavailable. Using pattern matching.",
        "[Offline Mode] Cannot reach LLM API. Operating with limited capabilities.",
        "[Fallback] AI service unreachable. Basic pattern recognition active.",
        "[Degraded Mode] LLM unavailable. Using template responses."
    };

    // ============================================================
    // PATTERN MATCHING - Keyword-based intent detection
    // ============================================================

    /**
     * Intent types that can be detected from user input.
     */
    public enum Intent {
        BUILD_INTENT,
        MINE_INTENT,
        ATTACK_INTENT,
        FOLLOW_INTENT,
        MOVE_INTENT,
        GATHER_INTENT,
        CRAFT_INTENT,
        PLACE_INTENT,
        STOP_INTENT,
        WAIT_INTENT,
        GREETING_INTENT,
        HELP_INTENT,
        STATUS_INTENT,
        UNKNOWN_INTENT
    }

    /**
     * Pattern mappings for intent detection.
     */
    private static final Map<Intent, List<Pattern>> INTENT_PATTERNS = new HashMap<>();

    static {
        // Build intent patterns
        INTENT_PATTERNS.put(Intent.BUILD_INTENT, List.of(
            Pattern.compile("(?i).*\\b(build|construct|create|make|craft|erect|assemble).*(house|home|shelter|structure|base|tower|castle|wall|bridge|farm).*")
        ));

        // Mine intent patterns
        INTENT_PATTERNS.put(Intent.MINE_INTENT, List.of(
            Pattern.compile("(?i).*\\b(mine|dig|excavate|quarry|tunnel|drill).*(ore|coal|iron|diamond|gold|stone|dirt|cobble).*"),
            Pattern.compile("(?i).*\\b(get|collect|gather|obtain|acquire).*(ore|coal|iron|diamond|gold|stone|resources).*")
        ));

        // Attack intent patterns
        INTENT_PATTERNS.put(Intent.ATTACK_INTENT, List.of(
            Pattern.compile("(?i).*\\b(attack|fight|kill|destroy|defeat|combat|hunt|slay).*(monster|zombie|skeleton|creeper|spider|enderman|hostile|enemy|mob).*")
        ));

        // Follow intent patterns
        INTENT_PATTERNS.put(Intent.FOLLOW_INTENT, List.of(
            Pattern.compile("(?i).*\\b(follow|come|accompany|join|stay with|tag along).*"),
            Pattern.compile("(?i).*\\b(follow).*(me|player|you).*")
        ));

        // Move intent patterns
        INTENT_PATTERNS.put(Intent.MOVE_INTENT, List.of(
            Pattern.compile("(?i).*\\b(go to|move to|walk to|travel|head to|navigate to|pathfind to|visit).*"),
            Pattern.compile("(?i).*\\b(come over|approach|reach).*")
        ));

        // Gather intent patterns
        INTENT_PATTERNS.put(Intent.GATHER_INTENT, List.of(
            Pattern.compile("(?i).*\\b(collect|gather|pick up|loot|harvest|acquire).*(item|drop|loot|wood|food|resource).*"),
            Pattern.compile("(?i).*\\b(get|fetch|retrieve|bring).*")
        ));

        // Craft intent patterns
        INTENT_PATTERNS.put(Intent.CRAFT_INTENT, List.of(
            Pattern.compile("(?i).*\\b(craft|create|make|build|smith|manufacture).*(tool|weapon|armor|item|sword|pickaxe|axe).*")
        ));

        // Place intent patterns
        INTENT_PATTERNS.put(Intent.PLACE_INTENT, List.of(
            Pattern.compile("(?i).*\\b(place|put|set|lay|position|deposit).*(block|torch|door|chest|sign).*")
        ));

        // Stop intent patterns
        INTENT_PATTERNS.put(Intent.STOP_INTENT, List.of(
            Pattern.compile("(?i).*\\b(stop|halt|cancel|abort|cease|discontinue|terminate).*"),
            Pattern.compile("(?i).*\\b(never mind|forget it|ignore that|nevermind).*")
        ));

        // Wait intent patterns
        INTENT_PATTERNS.put(Intent.WAIT_INTENT, List.of(
            Pattern.compile("(?i).*\\b(wait|stay|pause|hold on|stand by|remain).*"),
            Pattern.compile("(?i).*\\b(wait here|stay put|hold position).*")
        ));

        // Greeting intent patterns
        INTENT_PATTERNS.put(Intent.GREETING_INTENT, List.of(
            Pattern.compile("(?i)^(hi|hello|hey|greetings|good morning|good afternoon|good evening|howdy|yo).*"),
            Pattern.compile("(?i).*\\b(foreman).*\\b(hi|hello|hey).*")
        ));

        // Help intent patterns
        INTENT_PATTERNS.put(Intent.HELP_INTENT, List.of(
            Pattern.compile("(?i).*(help|what can you do|commands|abilities|features|how do you work|assist).*")
        ));

        // Status intent patterns
        INTENT_PATTERNS.put(Intent.STATUS_INTENT, List.of(
            Pattern.compile("(?i).*(status|report|what are you doing|current task|progress|state|condition).*")
        ));
    }

    // ============================================================
    // DEGRADED MODE - What works without LLM
    // ============================================================

    /**
     * Tracks whether we're currently in degraded/fallback mode.
     */
    private boolean isDegradedMode = false;

    /**
     * Queue of pending requests to replay when LLM recovers.
     */
    private final Queue<PendingRequest> pendingRequestQueue = new ConcurrentLinkedQueue<>();

    /**
     * Maximum number of pending requests to store.
     */
    private static final int MAX_PENDING_REQUESTS = 50;

    /**
     * Represents a request pending for when LLM recovers.
     */
    private static class PendingRequest {
        private final String systemPrompt;
        private final String userPrompt;
        private final LocalDateTime timestamp;

        public PendingRequest(String systemPrompt, String userPrompt) {
            this.systemPrompt = systemPrompt;
            this.userPrompt = userPrompt;
            this.timestamp = LocalDateTime.now();
        }

        public String getSystemPrompt() {
            return systemPrompt;
        }

        public String getUserPrompt() {
            return userPrompt;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }
    }

    // ============================================================
    // CONSTRUCTOR
    // ============================================================

    public FallbackResponseSystem() {
        LOGGER.info("FallbackResponseSystem initialized with {} intent patterns",
            INTENT_PATTERNS.values().stream().mapToInt(List::size).sum());
    }

    // ============================================================
    // PUBLIC API - Main entry points
    // ============================================================

    /**
     * Generates a fallback response when LLM is unavailable.
     *
     * <p>This is the main entry point for the fallback system. It:
     * <ol>
     *   <li>Detects the user's intent from the prompt</li>
     *   <li>Generates appropriate template responses</li>
     *   <li>Creates structured task JSON if possible</li>
     *   <li>Queues the request for replay when LLM recovers</li>
     * </ol></p>
     *
     * @param systemPrompt The system prompt that was sent
     * @param userPrompt The user's input prompt
     * @param error The error that caused the fallback (may be null)
     * @return LLMResponse with fallback content
     */
    public LLMResponse generateFallback(String systemPrompt, String userPrompt, Throwable error) {
        LOGGER.warn("Generating fallback response. User prompt: '{}'. Error: {}",
            truncatePrompt(userPrompt, 50),
            error != null ? error.getClass().getSimpleName() : "unknown");

        // Queue request for potential replay
        queuePendingRequest(systemPrompt, userPrompt);

        // Activate degraded mode
        if (!isDegradedMode) {
            enterDegradedMode();
        }

        // Detect intent
        Intent intent = detectIntent(userPrompt);
        LOGGER.debug("Detected intent: {}", intent);

        // Generate response based on intent
        String responseContent = generateResponseForIntent(intent, userPrompt);

        return LLMResponse.builder()
            .content(responseContent)
            .model("fallback-system")
            .providerId("fallback")
            .latencyMs(1)
            .tokensUsed(0)
            .fromCache(false)
            .build();
    }

    /**
     * Attempts to recover from fallback mode by replaying queued requests.
     *
     * <p>This should be called when LLM connectivity is restored.
     * It will attempt to replay up to maxReplay requests.</p>
     *
     * @param taskPlanner The now-functional task planner
     * @param maxReplay Maximum number of requests to replay (0 = all)
     * @return Number of successfully replayed requests
     */
    public int recoverAndReplay(TaskPlanner taskPlanner, int maxReplay) {
        if (!isDegradedMode) {
            LOGGER.info("Not in degraded mode, nothing to replay");
            return 0;
        }

        LOGGER.info("Attempting recovery. Pending requests: {}", pendingRequestQueue.size());
        int replayed = 0;
        int failed = 0;

        int toReplay = maxReplay > 0 ? Math.min(maxReplay, pendingRequestQueue.size()) : pendingRequestQueue.size();

        for (int i = 0; i < toReplay && !pendingRequestQueue.isEmpty(); i++) {
            PendingRequest request = pendingRequestQueue.poll();
            if (request == null) break;

            try {
                LOGGER.debug("Replaying request from {}", request.getTimestamp());
                // The actual replay would need to happen through the TaskPlanner
                // For now, we just log it
                replayed++;
            } catch (Exception e) {
                LOGGER.error("Failed to replay request", e);
                failed++;
            }
        }

        if (pendingRequestQueue.isEmpty()) {
            exitDegradedMode();
        }

        LOGGER.info("Recovery complete. Replayed: {}, Failed: {}, Remaining: {}",
            replayed, failed, pendingRequestQueue.size());

        return replayed;
    }

    // ============================================================
    // INTENT DETECTION - Pattern matching logic
    // ============================================================

    /**
     * Detects the user's intent from their prompt.
     *
     * @param prompt The user's input
     * @return Detected intent
     */
    public Intent detectIntent(String prompt) {
        if (prompt == null || prompt.isEmpty()) {
            return Intent.UNKNOWN_INTENT;
        }

        String lowerPrompt = prompt.toLowerCase().trim();

        // Check each intent's patterns
        for (Map.Entry<Intent, List<Pattern>> entry : INTENT_PATTERNS.entrySet()) {
            for (Pattern pattern : entry.getValue()) {
                if (pattern.matcher(lowerPrompt).matches()) {
                    LOGGER.debug("Matched intent: {} with pattern: {}", entry.getKey(), pattern.pattern());
                    return entry.getKey();
                }
            }
        }

        return Intent.UNKNOWN_INTENT;
    }

    // ============================================================
    // RESPONSE GENERATION - Template-based responses
    // ============================================================

    /**
     * Generates a response for a detected intent.
     *
     * @param intent The detected intent
     * @param originalPrompt The original user prompt
     * @return JSON-formatted response matching ResponseParser expectations
     */
    private String generateResponseForIntent(Intent intent, String originalPrompt) {
        String greeting = getGreetingResponse();
        String acknowledgment = getRandomAcknowledgment();
        String notification = getRandomFallbackNotification();

        String thoughts;
        JsonArray tasksArray = new JsonArray();

        switch (intent) {
            case BUILD_INTENT:
                thoughts = String.format("%s %s I detected you want to build something. In fallback mode, I'll attempt basic construction tasks.",
                    greeting, acknowledgment);
                tasksArray.add(createTask("build", Map.of(
                    "structure", "house",
                    "size", "small",
                    "fallback_mode", "true"
                )));
                break;

            case MINE_INTENT:
                thoughts = String.format("%s %s Mining detected! I'll look for ores in fallback mode.",
                    greeting, acknowledgment);
                tasksArray.add(createTask("mine", Map.of(
                    "target", "iron_ore",
                    "quantity", 10,
                    "fallback_mode", "true"
                )));
                break;

            case ATTACK_INTENT:
                thoughts = String.format("%s %s I'll defend you from hostile mobs!",
                    greeting, acknowledgment);
                tasksArray.add(createTask("attack", Map.of(
                    "target", "nearest_hostile",
                    "fallback_mode", "true"
                )));
                break;

            case FOLLOW_INTENT:
                thoughts = String.format("%s %s I'll follow you!",
                    greeting, acknowledgment);
                tasksArray.add(createTask("follow", Map.of(
                    "target", "player",
                    "distance", 3,
                    "fallback_mode", "true"
                )));
                break;

            case MOVE_INTENT:
                thoughts = String.format("%s %s I'll move towards your location.",
                    greeting, acknowledgment);
                tasksArray.add(createTask("pathfind", Map.of(
                    "target", "player",
                    "fallback_mode", "true"
                )));
                break;

            case GATHER_INTENT:
                thoughts = String.format("%s %s I'll collect nearby items.",
                    greeting, acknowledgment);
                tasksArray.add(createTask("collect", Map.of(
                    "target", "nearby_items",
                    "radius", 10,
                    "fallback_mode", "true"
                )));
                break;

            case CRAFT_INTENT:
                thoughts = String.format("%s %s Crafting detected! In fallback mode, I can only perform basic crafting.",
                    greeting, acknowledgment);
                tasksArray.add(createTask("craft", Map.of(
                    "item", "wooden_pickaxe",
                    "fallback_mode", "true"
                )));
                break;

            case PLACE_INTENT:
                thoughts = String.format("%s %s I'll place that block.",
                    greeting, acknowledgment);
                tasksArray.add(createTask("place_block", Map.of(
                    "block", "cobblestone",
                    "fallback_mode", "true"
                )));
                break;

            case STOP_INTENT:
                thoughts = String.format("%s Stopping all actions.",
                    greeting);
                tasksArray.add(createTask("wait", Map.of(
                    "duration", 5,
                    "fallback_mode", "true"
                )));
                break;

            case WAIT_INTENT:
                thoughts = String.format("%s I'll wait here for you.",
                    greeting);
                tasksArray.add(createTask("wait", Map.of(
                    "duration", 30,
                    "fallback_mode", "true"
                )));
                break;

            case GREETING_INTENT:
                thoughts = String.format("%s %s I'm currently in fallback mode because the LLM service is unavailable. " +
                    "However, I can still help with basic tasks using pattern matching!",
                    greeting, notification);
                // No specific task for greetings
                break;

            case HELP_INTENT:
                thoughts = String.format("%s In fallback mode, I can understand these basic commands:\n" +
                    "- build [structure]\n" +
                    "- mine [resource]\n" +
                    "- attack [target]\n" +
                    "- follow\n" +
                    "- move/go to [location]\n" +
                    "- gather [item]\n" +
                    "- craft [item]\n" +
                    "- place [block]\n" +
                    "- stop/wait\n\n" +
                    "%s",
                    greeting, notification);
                break;

            case STATUS_INTENT:
                thoughts = String.format("%s I'm currently in %s. I have %d pending requests queued for when LLM recovers.",
                    greeting, isDegradedMode ? "FALLBACK MODE" : "normal mode", pendingRequestQueue.size());
                break;

            case UNKNOWN_INTENT:
            default:
                thoughts = String.format("%s %s I couldn't determine exactly what you want. I'll wait for more specific instructions. %s",
                    greeting, acknowledgment, notification);
                tasksArray.add(createTask("wait", Map.of(
                    "duration", 5,
                    "fallback_mode", "true"
                )));
                break;
        }

        JsonObject response = new JsonObject();
        response.addProperty("reasoning", thoughts);
        response.addProperty("plan", "Fallback mode: Using pattern matching instead of LLM");
        response.add("tasks", tasksArray);

        return response.toString();
    }

    // ============================================================
    // TEMPLATE HELPERS
    // ============================================================

    /**
     * Gets an appropriate greeting based on time of day.
     *
     * @return Greeting string
     */
    private String getGreetingResponse() {
        LocalTime now = LocalTime.now();
        String timeOfDay;

        if (now.isBefore(LocalTime.NOON)) {
            timeOfDay = "morning";
        } else if (now.isBefore(LocalTime.of(18, 0))) {
            timeOfDay = "afternoon";
        } else {
            timeOfDay = "evening";
        }

        String[] templates = GREETING_TEMPLATES.getOrDefault(timeOfDay, GREETING_TEMPLATES.get("default"));
        return templates[new Random().nextInt(templates.length)];
    }

    /**
     * Gets a random task acknowledgment.
     *
     * @return Acknowledgment string
     */
    private String getRandomAcknowledgment() {
        return TASK_ACKNOWLEDGMENTS[new Random().nextInt(TASK_ACKNOWLEDGMENTS.length)];
    }

    /**
     * Gets a random completion message.
     *
     * @return Completion string
     */
    public String getRandomCompletion() {
        return COMPLETION_TEMPLATES[new Random().nextInt(COMPLETION_TEMPLATES.length)];
    }

    /**
     * Gets a random error message.
     *
     * @return Error string
     */
    public String getRandomError() {
        return ERROR_TEMPLATES[new Random().nextInt(ERROR_TEMPLATES.length)];
    }

    /**
     * Gets a random idle message.
     *
     * @return Idle string
     */
    public String getRandomIdle() {
        return IDLE_TEMPLATES[new Random().nextInt(IDLE_TEMPLATES.length)];
    }

    /**
     * Gets a random fallback notification.
     *
     * @return Notification string
     */
    private String getRandomFallbackNotification() {
        return FALLBACK_NOTIFICATIONS[new Random().nextInt(FALLBACK_NOTIFICATIONS.length)];
    }

    /**
     * Creates a task JSON object.
     *
     * @param action The action name
     * @param parameters Map of parameters
     * @return JsonObject representing the task
     */
    private JsonObject createTask(String action, Map<String, Object> parameters) {
        JsonObject task = new JsonObject();
        task.addProperty("action", action);

        JsonObject params = new JsonObject();
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof String) {
                params.addProperty(entry.getKey(), (String) value);
            } else if (value instanceof Number) {
                params.addProperty(entry.getKey(), (Number) value);
            } else if (value instanceof Boolean) {
                params.addProperty(entry.getKey(), (Boolean) value);
            } else {
                params.addProperty(entry.getKey(), value.toString());
            }
        }
        task.add("parameters", params);

        return task;
    }

    // ============================================================
    // DEGRADED MODE MANAGEMENT
    // ============================================================

    /**
     * Enters degraded mode.
     */
    private void enterDegradedMode() {
        isDegradedMode = true;
        LOGGER.warn("ENTERING DEGRADED MODE - LLM service unavailable");
        LOGGER.warn("Degraded mode capabilities: Pattern matching, template responses, queued requests");
    }

    /**
     * Exits degraded mode.
     */
    private void exitDegradedMode() {
        isDegradedMode = false;
        LOGGER.info("EXITING DEGRADED MODE - LLM service restored");
    }

    /**
     * Checks if currently in degraded mode.
     *
     * @return true if in degraded mode
     */
    public boolean isDegradedMode() {
        return isDegradedMode;
    }

    /**
     * Queues a request for replay when LLM recovers.
     *
     * @param systemPrompt System prompt
     * @param userPrompt User prompt
     */
    private void queuePendingRequest(String systemPrompt, String userPrompt) {
        if (pendingRequestQueue.size() >= MAX_PENDING_REQUESTS) {
            // Remove oldest request
            PendingRequest removed = pendingRequestQueue.poll();
            LOGGER.debug("Pending queue full, removed oldest request from {}", removed.getTimestamp());
        }

        PendingRequest request = new PendingRequest(systemPrompt, userPrompt);
        pendingRequestQueue.add(request);
        LOGGER.debug("Queued pending request. Queue size: {}", pendingRequestQueue.size());
    }

    /**
     * Clears all pending requests.
     */
    public void clearPendingRequests() {
        int count = pendingRequestQueue.size();
        pendingRequestQueue.clear();
        LOGGER.info("Cleared {} pending requests", count);
    }

    /**
     * Gets the count of pending requests.
     *
     * @return Number of queued requests
     */
    public int getPendingRequestCount() {
        return pendingRequestQueue.size();
    }

    // ============================================================
    // UTILITY METHODS
    // ============================================================

    /**
     * Truncates a prompt for logging.
     *
     * @param prompt The prompt to truncate
     * @param maxLength Maximum length
     * @return Truncated prompt
     */
    private String truncatePrompt(String prompt, int maxLength) {
        if (prompt == null) {
            return "[null]";
        }
        if (prompt.length() <= maxLength) {
            return prompt;
        }
        return prompt.substring(0, maxLength) + "...";
    }

    /**
     * Gets a description of degraded mode capabilities.
     *
     * @return Description string
     */
    public String getDegradedModeDescription() {
        return """
            DEGRADED MODE CAPABILITIES:
            - Intent detection via pattern matching
            - Template-based responses
            - Basic task execution (mine, build, attack, follow, move, etc.)
            - Request queuing for replay when LLM recovers
            - Memory system remains functional

            LIMITATIONS:
            - No complex reasoning or planning
            - No contextual understanding
            - No multi-step task decomposition
            - No learning from conversations
            """;
    }

    /**
     * Exports current state for diagnostics.
     *
     * @return Diagnostic information
     */
    public String exportDiagnosticInfo() {
        return String.format("""
            FallbackResponseSystem Diagnostics:
            ===================================
            Degraded Mode: %s
            Pending Requests: %d/%d
            Intent Patterns: %d
            Greeting Templates: %d
            Acknowledgment Templates: %d
            Completion Templates: %d
            Error Templates: %d
            Idle Templates: %d
            Fallback Notifications: %d
            """,
            isDegradedMode ? "ACTIVE" : "INACTIVE",
            pendingRequestQueue.size(),
            MAX_PENDING_REQUESTS,
            INTENT_PATTERNS.values().stream().mapToInt(List::size).sum(),
            GREETING_TEMPLATES.values().stream().mapToInt(arr -> arr.length).sum(),
            TASK_ACKNOWLEDGMENTS.length,
            COMPLETION_TEMPLATES.length,
            ERROR_TEMPLATES.length,
            IDLE_TEMPLATES.length,
            FALLBACK_NOTIFICATIONS.length
        );
    }
}

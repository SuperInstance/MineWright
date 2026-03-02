package com.minewright.config;

import com.minewright.testutil.TestLogger;
import org.slf4j.Logger;
import com.minewright.exception.ConfigException;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.Arrays;
import java.util.List;

/**
 * Configuration for MineWright mod.
 *
 * <h2>Config File Location</h2>
 * <p><code>config/minewright-common.toml</code></p>
 *
 * <p><b>Platform-specific paths:</b></p>
 * <ul>
 *   <li><b>Windows:</b> <code>.minecraft/config/minewright-common.toml</code></li>
 *   <li><b>Linux/Mac:</b> <code>~/.minecraft/config/minewright-common.toml</code></li>
 * </ul>
 *
 * <h2>Config Reload</h2>
 * <p>Edit the config file and use <code>/reload</code> to reload without restart.</p>
 *
 * <h2>Configuration Sections</h2>
 * <ul>
 *   <li><b>{@code [ai]}</b> - AI provider selection (default: openai/z.ai)</li>
 *   <li><b>{@code [openai]}</b> - z.ai API credentials and model settings</li>
 *   <li><b>{@code [behavior]}</b> - Crew behavior settings</li>
 *   <li><b>{@code [voice]}</b> - Voice input/output configuration</li>
 *   <li><b>{@code [hivemind]}</b> - Cloudflare Edge integration</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Get config value
 * String provider = MineWrightConfig.getValidatedProvider();
 * int maxTokens = MineWrightConfig.MAX_TOKENS.get();
 *
 * // Validate configuration
 * boolean isValid = MineWrightConfig.validateAndLog();
 *
 * // Register for reload notifications
 * ConfigManager.getInstance().registerListener(myListener);
 * }</pre>
 *
 * <h2>Configuration Documentation</h2>
 * <p>See {@link ConfigDocumentation} for detailed information about each config option,
 * including default values, valid ranges, and descriptions.</p>
 *
 * @see ConfigDocumentation
 * @see ConfigManager
 * @see ConfigChangeListener
 * @since 1.0.0
 */
public class MineWrightConfig {
    private static final Logger LOGGER = TestLogger.getLogger(MineWrightConfig.class);
    // Valid AI providers (openai uses z.ai API)
    private static final List<String> VALID_PROVIDERS = Arrays.asList("openai", "groq", "gemini");
    private static final List<String> VALID_VOICE_MODES = Arrays.asList("disabled", "logging", "real");

    // ========================================================================
    // Configuration Specification
    // ========================================================================

    /** The Forge configuration spec */
    public static final ForgeConfigSpec SPEC;

    // ------------------------------------------------------------------------
    // AI Configuration
    // ------------------------------------------------------------------------

    /**
     * AI provider to use for LLM requests.
     * <p><b>Valid values:</b> {@code groq}, {@code openai}, {@code gemini}</p>
     * <p><b>Default:</b> {@code groq}</p>
     * <p><b>Config key:</b> {@code ai.provider}</p>
     *
     * @see ConfigDocumentation#AI
     */
    public static final ForgeConfigSpec.ConfigValue<String> AI_PROVIDER;

    // ------------------------------------------------------------------------
    // OpenAI/Gemini API Configuration
    // ------------------------------------------------------------------------

    /**
     * API key for the chosen provider.
     * <p><b>Required:</b> Yes</p>
     * <p><b>Default:</b> {@code ""} (empty, must be configured)</p>
     * <p><b>Config key:</b> {@code openai.apiKey}</p>
     *
     * @see ConfigDocumentation#OPENAI
     */
    public static final ForgeConfigSpec.ConfigValue<String> OPENAI_API_KEY;

    /**
     * LLM model to use.
     * <p><b>Default:</b> {@code glm-5}</p>
     * <p><b>Config key:</b> {@code openai.model}</p>
     *
     * @see ConfigDocumentation#OPENAI
     */
    public static final ForgeConfigSpec.ConfigValue<String> OPENAI_MODEL;

    /**
     * Maximum tokens per API request.
     * <p><b>Range:</b> 100 to 65536</p>
     * <p><b>Default:</b> 8000</p>
     * <p><b>Config key:</b> {@code openai.maxTokens}</p>
     *
     * @see ConfigDocumentation#OPENAI
     */
    public static final ForgeConfigSpec.IntValue MAX_TOKENS;

    /**
     * Temperature for AI responses (creativity control).
     * <p><b>Range:</b> 0.0 to 2.0</p>
     * <p><b>Default:</b> 0.7</p>
     * <p><b>Config key:</b> {@code openai.temperature}</p>
     *
     * @see ConfigDocumentation#OPENAI
     */
    public static final ForgeConfigSpec.DoubleValue TEMPERATURE;

    // ------------------------------------------------------------------------
    // Behavior Configuration
    // ------------------------------------------------------------------------

    /**
     * Ticks between action checks.
     * <p><b>Range:</b> 1 to 100 (20 ticks = 1 second)</p>
     * <p><b>Default:</b> 20</p>
     * <p><b>Config key:</b> {@code behavior.actionTickDelay}</p>
     *
     * @see ConfigDocumentation#BEHAVIOR
     */
    public static final ForgeConfigSpec.IntValue ACTION_TICK_DELAY;

    /**
     * Allow crew members to respond in chat.
     * <p><b>Default:</b> {@code true}</p>
     * <p><b>Config key:</b> {@code behavior.enableChatResponses}</p>
     *
     * @see ConfigDocumentation#BEHAVIOR
     */
    public static final ForgeConfigSpec.BooleanValue ENABLE_CHAT_RESPONSES;

    /**
     * Maximum number of crew members that can be active simultaneously.
     * <p><b>Range:</b> 1 to 50</p>
     * <p><b>Default:</b> 10</p>
     * <p><b>Config key:</b> {@code behavior.maxActiveCrewMembers}</p>
     *
     * @see ConfigDocumentation#BEHAVIOR
     */
    public static final ForgeConfigSpec.IntValue MAX_ACTIVE_CREW_MEMBERS;

    // ------------------------------------------------------------------------
    // Voice Configuration
    // ------------------------------------------------------------------------

    /**
     * Enable voice input/output features.
     * <p><b>Default:</b> {@code false}</p>
     * <p><b>Config key:</b> {@code voice.enabled}</p>
     *
     * @see ConfigDocumentation#VOICE
     */
    public static final ForgeConfigSpec.BooleanValue VOICE_ENABLED;

    /**
     * Voice system mode.
     * <p><b>Valid values:</b> {@code disabled}, {@code logging}, {@code real}</p>
     * <p><b>Default:</b> {@code logging}</p>
     * <p><b>Config key:</b> {@code voice.mode}</p>
     *
     * @see ConfigDocumentation#VOICE
     */
    public static final ForgeConfigSpec.ConfigValue<String> VOICE_MODE;

    /**
     * Speech-to-text language code.
     * <p><b>Default:</b> {@code en-US}</p>
     * <p><b>Config key:</b> {@code voice.sttLanguage}</p>
     *
     * @see ConfigDocumentation#VOICE
     */
    public static final ForgeConfigSpec.ConfigValue<String> VOICE_STT_LANGUAGE;

    /**
     * Text-to-speech voice name.
     * <p><b>Default:</b> {@code default}</p>
     * <p><b>Config key:</b> {@code voice.ttsVoice}</p>
     *
     * @see ConfigDocumentation#VOICE
     */
    public static final ForgeConfigSpec.ConfigValue<String> VOICE_TTS_VOICE;

    /**
     * TTS volume level.
     * <p><b>Range:</b> 0.0 to 1.0</p>
     * <p><b>Default:</b> 0.8</p>
     * <p><b>Config key:</b> {@code voice.ttsVolume}</p>
     *
     * @see ConfigDocumentation#VOICE
     */
    public static final ForgeConfigSpec.DoubleValue VOICE_TTS_VOLUME;

    /**
     * TTS speech rate.
     * <p><b>Range:</b> 0.5 to 2.0 (1.0 = normal)</p>
     * <p><b>Default:</b> 1.0</p>
     * <p><b>Config key:</b> {@code voice.ttsRate}</p>
     *
     * @see ConfigDocumentation#VOICE
     */
    public static final ForgeConfigSpec.DoubleValue VOICE_TTS_RATE;

    /**
     * TTS pitch adjustment.
     * <p><b>Range:</b> 0.5 to 2.0 (1.0 = normal)</p>
     * <p><b>Default:</b> 1.0</p>
     * <p><b>Config key:</b> {@code voice.ttsPitch}</p>
     *
     * @see ConfigDocumentation#VOICE
     */
    public static final ForgeConfigSpec.DoubleValue VOICE_TTS_PITCH;

    /**
     * STT sensitivity for speech detection.
     * <p><b>Range:</b> 0.0 to 1.0</p>
     * <p><b>Default:</b> 0.5</p>
     * <p><b>Config key:</b> {@code voice.sttSensitivity}</p>
     *
     * @see ConfigDocumentation#VOICE
     */
    public static final ForgeConfigSpec.DoubleValue VOICE_STT_SENSITIVITY;

    /**
     * Require push-to-talk key for voice input.
     * <p><b>Default:</b> {@code true}</p>
     * <p><b>Config key:</b> {@code voice.pushToTalk}</p>
     *
     * @see ConfigDocumentation#VOICE
     */
    public static final ForgeConfigSpec.BooleanValue VOICE_PUSH_TO_TALK;

    /**
     * Auto-stop listening after N seconds of silence.
     * <p><b>Range:</b> 0 to 60 (0 = no timeout)</p>
     * <p><b>Default:</b> 10</p>
     * <p><b>Config key:</b> {@code voice.listeningTimeout}</p>
     *
     * @see ConfigDocumentation#VOICE
     */
    public static final ForgeConfigSpec.IntValue VOICE_LISTENING_TIMEOUT;

    /**
     * Enable verbose logging for voice system operations.
     * <p><b>Default:</b> {@code true}</p>
     * <p><b>Config key:</b> {@code voice.debugLogging}</p>
     *
     * @see ConfigDocumentation#VOICE
     */
    public static final ForgeConfigSpec.BooleanValue VOICE_DEBUG_LOGGING;

    // ------------------------------------------------------------------------
    // Hive Mind (Cloudflare Edge) Configuration
    // ------------------------------------------------------------------------

    /**
     * Enable Hive Mind - distributed AI for tactical reflexes.
     * <p><b>Default:</b> {@code false}</p>
     * <p><b>Config key:</b> {@code hivemind.enabled}</p>
     *
     * @see ConfigDocumentation#HIVEMIND
     */
    public static final ForgeConfigSpec.BooleanValue HIVEMIND_ENABLED;

    /**
     * Cloudflare Worker URL for Hive Mind edge computing.
     * <p><b>Default:</b> {@code https://minecraft-agent-reflex.workers.dev}</p>
     * <p><b>Config key:</b> {@code hivemind.workerUrl}</p>
     *
     * @see ConfigDocumentation#HIVEMIND
     */
    public static final ForgeConfigSpec.ConfigValue<String> HIVEMIND_WORKER_URL;

    /**
     * Connection timeout in milliseconds.
     * <p><b>Range:</b> 500 to 10000</p>
     * <p><b>Default:</b> 2000</p>
     * <p><b>Config key:</b> {@code hivemind.connectTimeoutMs}</p>
     *
     * @see ConfigDocumentation#HIVEMIND
     */
    public static final ForgeConfigSpec.IntValue HIVEMIND_CONNECT_TIMEOUT;

    /**
     * Tactical decision timeout in milliseconds.
     * <p><b>Range:</b> 10 to 500</p>
     * <p><b>Default:</b> 50</p>
     * <p><b>Config key:</b> {@code hivemind.tacticalTimeoutMs}</p>
     *
     * @see ConfigDocumentation#HIVEMIND
     */
    public static final ForgeConfigSpec.IntValue HIVEMIND_TACTICAL_TIMEOUT;

    /**
     * State sync timeout in milliseconds.
     * <p><b>Range:</b> 100 to 5000</p>
     * <p><b>Default:</b> 1000</p>
     * <p><b>Config key:</b> {@code hivemind.syncTimeoutMs}</p>
     *
     * @see ConfigDocumentation#HIVEMIND
     */
    public static final ForgeConfigSpec.IntValue HIVEMIND_SYNC_TIMEOUT;

    /**
     * How often to check for tactical situations (in ticks).
     * <p><b>Range:</b> 5 to 100 (20 ticks = 1 second)</p>
     * <p><b>Default:</b> 20</p>
     * <p><b>Config key:</b> {@code hivemind.tacticalCheckInterval}</p>
     *
     * @see ConfigDocumentation#HIVEMIND
     */
    public static final ForgeConfigSpec.IntValue HIVEMIND_TACTICAL_CHECK_INTERVAL;

    /**
     * How often to sync state with edge (in ticks).
     * <p><b>Range:</b> 20 to 200</p>
     * <p><b>Default:</b> 100</p>
     * <p><b>Config key:</b> {@code hivemind.syncInterval}</p>
     *
     * @see ConfigDocumentation#HIVEMIND
     */
    public static final ForgeConfigSpec.IntValue HIVEMIND_SYNC_INTERVAL;

    /**
     * When edge is unavailable, fall back to local decision-making.
     * <p><b>Default:</b> {@code true}</p>
     * <p><b>Config key:</b> {@code hivemind.fallbackToLocal}</p>
     *
     * @see ConfigDocumentation#HIVEMIND
     */
    public static final ForgeConfigSpec.BooleanValue HIVEMIND_FALLBACK_TO_LOCAL;

    // ------------------------------------------------------------------------
    // Skill Library Configuration
    // ------------------------------------------------------------------------

    /**
     * Enable skill library for learning and storing successful action patterns.
     * <p><b>Default:</b> {@code true}</p>
     * <p><b>Config key:</b> {@code skill_library.enabled}</p>
     *
     * @since 2.0.0
     */
    public static final ForgeConfigSpec.BooleanValue SKILL_LIBRARY_ENABLED;

    /**
     * Maximum number of skills to store in the library.
     * <p><b>Range:</b> 10 to 1000</p>
     * <p><b>Default:</b> 100</p>
     * <p><b>Config key:</b> {@code skill_library.max_skills}</p>
     *
     * @since 2.0.0
     */
    public static final ForgeConfigSpec.IntValue MAX_SKILLS_STORED;

    /**
     * Success threshold for considering a skill as learned.
     * <p><b>Range:</b> 0.0 to 1.0</p>
     * <p><b>Default:</b> 0.7</p>
     * <p><b>Config key:</b> {@code skill_library.success_threshold}</p>
     *
     * @since 2.0.0
     */
    public static final ForgeConfigSpec.DoubleValue SKILL_SUCCESS_THRESHOLD;

    // ------------------------------------------------------------------------
    // Cascade Router Configuration
    // ------------------------------------------------------------------------

    /**
     * Enable cascade router for intelligent LLM selection.
     * <p><b>Default:</b> {@code true}</p>
     * <p><b>Config key:</b> {@code cascade_router.enabled}</p>
     *
     * @since 2.0.0
     */
    public static final ForgeConfigSpec.BooleanValue CASCADE_ROUTER_ENABLED;

    /**
     * Semantic similarity threshold for cascade routing decisions.
     * <p><b>Range:</b> 0.0 to 1.0</p>
     * <p><b>Default:</b> 0.85</p>
     * <p><b>Config key:</b> {@code cascade_router.similarity_threshold}</p>
     *
     * @since 2.0.0
     */
    public static final ForgeConfigSpec.DoubleValue SEMANTIC_SIMILARITY_THRESHOLD;

    /**
     * Use local LLM for cascade router fallback.
     * <p><b>Default:</b> {@code false}</p>
     * <p><b>Config key:</b> {@code cascade_router.use_local_llm}</p>
     *
     * @since 2.0.0
     */
    public static final ForgeConfigSpec.BooleanValue USE_LOCAL_LLM;

    // ------------------------------------------------------------------------
    // Utility AI Configuration
    // ------------------------------------------------------------------------

    /**
     * Enable utility AI for decision-making.
     * <p><b>Default:</b> {@code true}</p>
     * <p><b>Config key:</b> {@code utility_ai.enabled}</p>
     *
     * @since 2.0.0
     */
    public static final ForgeConfigSpec.BooleanValue UTILITY_AI_ENABLED;

    /**
     * Weight for urgency in utility calculations.
     * <p><b>Range:</b> 0.0 to 2.0</p>
     * <p><b>Default:</b> 1.0</p>
     * <p><b>Config key:</b> {@code utility_ai.urgency_weight}</p>
     *
     * @since 2.0.0
     */
    public static final ForgeConfigSpec.DoubleValue URGENCY_WEIGHT;

    /**
     * Weight for proximity in utility calculations.
     * <p><b>Range:</b> 0.0 to 2.0</p>
     * <p><b>Default:</b> 0.8</p>
     * <p><b>Config key:</b> {@code utility_ai.proximity_weight}</p>
     *
     * @since 2.0.0
     */
    public static final ForgeConfigSpec.DoubleValue PROXIMITY_WEIGHT;

    /**
     * Weight for safety in utility calculations.
     * <p><b>Range:</b> 0.0 to 2.0</p>
     * <p><b>Default:</b> 1.2</p>
     * <p><b>Config key:</b> {@code utility_ai.safety_weight}</p>
     *
     * @since 2.0.0
     */
    public static final ForgeConfigSpec.DoubleValue SAFETY_WEIGHT;

    // ------------------------------------------------------------------------
    // Multi-Agent Configuration
    // ------------------------------------------------------------------------

    /**
     * Enable multi-agent coordination features.
     * <p><b>Default:</b> {@code true}</p>
     * <p><b>Config key:</b> {@code multi_agent.enabled}</p>
     *
     * @since 2.0.0
     */
    public static final ForgeConfigSpec.BooleanValue MULTI_AGENT_ENABLED;

    /**
     * Maximum time to wait for agent bids in milliseconds.
     * <p><b>Range:</b> 100 to 5000</p>
     * <p><b>Default:</b> 1000</p>
     * <p><b>Config key:</b> {@code multi_agent.max_bid_wait_ms}</p>
     *
     * @since 2.0.0
     */
    public static final ForgeConfigSpec.IntValue MAX_BID_WAIT_MS;

    /**
     * Time-to-live for blackboard entries in seconds.
     * <p><b>Range:</b> 60 to 3600</p>
     * <p><b>Default:</b> 300</p>
     * <p><b>Config key:</b> {@code multi_agent.blackboard_ttl_seconds}</p>
     *
     * @since 2.0.0
     */
    public static final ForgeConfigSpec.IntValue BLACKBOARD_TTL_SECONDS;

    // ------------------------------------------------------------------------
    // Pathfinding Configuration
    // ------------------------------------------------------------------------

    /**
     * Enable enhanced pathfinding algorithms.
     * <p><b>Default:</b> {@code true}</p>
     * <p><b>Config key:</b> {@code pathfinding.enhanced}</p>
     *
     * @since 2.0.0
     */
    public static final ForgeConfigSpec.BooleanValue ENHANCED_PATHFINDING;

    /**
     * Maximum nodes to search in pathfinding algorithms.
     * <p><b>Range:</b> 1000 to 50000</p>
     * <p><b>Default:</b> 10000</p>
     * <p><b>Config key:</b> {@code pathfinding.max_search_nodes}</p>
     *
     * @since 2.0.0
     */
    public static final ForgeConfigSpec.IntValue MAX_PATH_SEARCH_NODES;

    // ------------------------------------------------------------------------
    // Semantic Cache Configuration
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // Performance Configuration
    // ------------------------------------------------------------------------

    /**
     * AI tick budget in milliseconds.
     * <p>AI operations must complete within this time to prevent server lag.</p>
     * <p><b>Range:</b> 1 to 20 (default: 5)</p>
     * <p><b>Default:</b> {@code 5}</p>
     * <p><b>Config key:</b> {@code performance.aiTickBudgetMs}</p>
     *
     * @since 2.1.0
     */
    public static final ForgeConfigSpec.IntValue AI_TICK_BUDGET_MS;

    /**
     * Warning threshold as percentage of budget.
     * <p>Warnings are logged when AI operations exceed this percentage of budget.</p>
     * <p><b>Range:</b> 50 to 95 (default: 80)</p>
     * <p><b>Default:</b> {@code 80}</p>
     * <p><b>Config key:</b> {@code performance.budgetWarningThreshold}</p>
     *
     * @since 2.1.0
     */
    public static final ForgeConfigSpec.IntValue BUDGET_WARNING_THRESHOLD;

    /**
     * Enable strict budget enforcement.
     * <p>When enabled, operations defer work when budget is exceeded.</p>
     * <p><b>Default:</b> {@code true}</p>
     * <p><b>Config key:</b> {@code performance.strictBudgetEnforcement}</p>
     *
     * @since 2.1.0
     */
    public static final ForgeConfigSpec.BooleanValue STRICT_BUDGET_ENFORCEMENT;

    /**
     * Pathfinding timeout threshold in milliseconds.
     * <p>Pathfinding operations that exceed this timeout are aborted and fallback paths are used.</p>
     * <p><b>Range:</b> 100 to 10000 (default: 2000)</p>
     * <p><b>Default:</b> {@code 2000}</p>
     * <p><b>Config key:</b> {@code performance.pathfindingTimeoutMs}</p>
     *
     * @since 2.2.0
     */
    public static final ForgeConfigSpec.IntValue PATHFINDING_TIMEOUT_MS;

    /**
     * Enable semantic caching for LLM responses.
     * <p>When enabled, caches similar prompts using text similarity matching.</p>
     * <p><b>Default:</b> {@code true}</p>
     * <p><b>Config key:</b> {@code semantic_cache.enabled}</p>
     *
     * @since 1.6.0
     */
    public static final ForgeConfigSpec.BooleanValue SEMANTIC_CACHE_ENABLED;

    /**
     * Minimum similarity threshold for semantic cache hits.
     * <p>Prompts with similarity above this threshold are considered matches.</p>
     * <p><b>Range:</b> 0.5 to 1.0</p>
     * <p><b>Default:</b> 0.85</p>
     * <p><b>Config key:</b> {@code semantic_cache.similarity_threshold}</p>
     *
     * @since 1.6.0
     */
    public static final ForgeConfigSpec.DoubleValue SEMANTIC_CACHE_SIMILARITY_THRESHOLD;

    /**
     * Maximum number of entries in the semantic cache.
     * <p><b>Range:</b> 100 to 2000</p>
     * <p><b>Default:</b> 500</p>
     * <p><b>Config key:</b> {@code semantic_cache.max_size}</p>
     *
     * @since 1.6.0
     */
    public static final ForgeConfigSpec.IntValue SEMANTIC_CACHE_MAX_SIZE;

    /**
     * Time-to-live for semantic cache entries in minutes.
     * <p>Entries older than this are evicted from the cache.</p>
     * <p><b>Range:</b> 1 to 60</p>
     * <p><b>Default:</b> 5</p>
     * <p><b>Config key:</b> {@code semantic_cache.ttl_minutes}</p>
     *
     * @since 1.6.0
     */
    public static final ForgeConfigSpec.IntValue SEMANTIC_CACHE_TTL_MINUTES;

    /**
     * Embedding method to use for semantic similarity.
     * <p><b>Valid values:</b> {@code tfidf}, {@code ngram}</p>
     * <p><b>Default:</b> {@code tfidf}</p>
     * <p><b>Config key:</b> {@code semantic_cache.embedding_method}</p>
     *
     * @since 1.6.0
     */
    public static final ForgeConfigSpec.ConfigValue<String> SEMANTIC_CACHE_EMBEDDING_METHOD;

    // ------------------------------------------------------------------------
    // Humanization Configuration
    // ------------------------------------------------------------------------

    /**
     * Enable humanization features for more natural agent behavior.
     * <p><b>Default:</b> {@code true}</p>
     * <p><b>Config key:</b> {@code humanization.enabled}</p>
     *
     * @since 2.2.0
     */
    public static final ForgeConfigSpec.BooleanValue HUMANIZATION_ENABLED;

    /**
     * Timing variance as fraction of base value.
     * <p><b>Range:</b> 0.0 to 1.0</p>
     * <p><b>Default:</b> 0.3 (30% variance)</p>
     * <p><b>Config key:</b> {@code humanization.timing_variance}</p>
     *
     * @since 2.2.0
     */
    public static final ForgeConfigSpec.DoubleValue TIMING_VARIANCE;

    /**
     * Minimum action delay in ticks.
     * <p><b>Range:</b> 1 to 100 (20 ticks = 1 second)</p>
     * <p><b>Default:</b> 2</p>
     * <p><b>Config key:</b> {@code humanization.min_action_delay_ticks}</p>
     *
     * @since 2.2.0
     */
    public static final ForgeConfigSpec.IntValue MIN_ACTION_DELAY_TICKS;

    /**
     * Maximum action delay in ticks.
     * <p><b>Range:</b> 1 to 200 (20 ticks = 1 second)</p>
     * <p><b>Default:</b> 20</p>
     * <p><b>Config key:</b> {@code humanization.max_action_delay_ticks}</p>
     *
     * @since 2.2.0
     */
    public static final ForgeConfigSpec.IntValue MAX_ACTION_DELAY_TICKS;

    /**
     * Movement speed variance as fraction of base speed.
     * <p><b>Range:</b> 0.0 to 0.5</p>
     * <p><b>Default:</b> 0.1 (10% variance)</p>
     * <p><b>Config key:</b> {@code humanization.speed_variance}</p>
     *
     * @since 2.2.0
     */
    public static final ForgeConfigSpec.DoubleValue SPEED_VARIANCE;

    /**
     * Chance per tick of micro-movement.
     * <p><b>Range:</b> 0.0 to 0.2</p>
     * <p><b>Default:</b> 0.05 (5% chance)</p>
     * <p><b>Config key:</b> {@code humanization.micro_movement_chance}</p>
     *
     * @since 2.2.0
     */
    public static final ForgeConfigSpec.DoubleValue MICRO_MOVEMENT_CHANCE;

    /**
     * Enable smooth look transitions.
     * <p><b>Default:</b> {@code true}</p>
     * <p><b>Config key:</b> {@code humanization.smooth_look}</p>
     *
     * @since 2.2.0
     */
    public static final ForgeConfigSpec.BooleanValue SMOOTH_LOOK;

    /**
     * Base mistake rate for agent actions.
     * <p><b>Range:</b> 0.0 to 0.2</p>
     * <p><b>Default:</b> 0.03 (3% mistake rate)</p>
     * <p><b>Config key:</b> {@code humanization.mistake_rate}</p>
     *
     * @since 2.2.0
     */
    public static final ForgeConfigSpec.DoubleValue MISTAKE_RATE;

    /**
     * Minimum agent reaction time in milliseconds.
     * <p>Agents wait at least this long before responding to stimuli.</p>
     * <p><b>Range:</b> 50 to 1000 (default: 150)</p>
     * <p><b>Default:</b> {@code 150}</p>
     * <p><b>Config key:</b> {@code humanization.reaction_time_min_ms}</p>
     *
     * @since 2.2.0
     */
    public static final ForgeConfigSpec.IntValue REACTION_TIME_MIN_MS;

    /**
     * Maximum agent reaction time in milliseconds.
     * <p>Agents wait at most this long before responding to stimuli.</p>
     * <p><b>Range:</b> 100 to 5000 (default: 500)</p>
     * <p><b>Default:</b> {@code 500}</p>
     * <p><b>Config key:</b> {@code humanization.reaction_time_max_ms}</p>
     *
     * @since 2.2.0
     */
    public static final ForgeConfigSpec.IntValue REACTION_TIME_MAX_MS;

    /**
     * Idle action chance per tick.
     * <p><b>Range:</b> 0.0 to 0.1</p>
     * <p><b>Default:</b> 0.02 (2% chance)</p>
     * <p><b>Config key:</b> {@code humanization.idle_action_chance}</p>
     *
     * @since 2.2.0
     */
    public static final ForgeConfigSpec.DoubleValue IDLE_ACTION_CHANCE;

    /**
     * Enable personality-driven idle behaviors.
     * <p><b>Default:</b> {@code true}</p>
     * <p><b>Config key:</b> {@code humanization.personality_affects_idle}</p>
     *
     * @since 2.2.0
     */
    public static final ForgeConfigSpec.BooleanValue PERSONALITY_AFFECTS_IDLE;

    /**
     * Enable session modeling (warm-up, fatigue, breaks).
     * <p><b>Default:</b> {@code true}</p>
     * <p><b>Config key:</b> {@code humanization.session_modeling_enabled}</p>
     *
     * @since 2.2.0
     */
    public static final ForgeConfigSpec.BooleanValue SESSION_MODELING_ENABLED;

    /**
     * Warm-up duration in minutes.
     * <p><b>Range:</b> 1 to 60</p>
     * <p><b>Default:</b> 10</p>
     * <p><b>Config key:</b> {@code humanization.warmup_duration_minutes}</p>
     *
     * @since 2.2.0
     */
    public static final ForgeConfigSpec.IntValue WARMUP_DURATION_MINUTES;

    /**
     * Fatigue onset time in minutes.
     * <p><b>Range:</b> 15 to 180</p>
     * <p><b>Default:</b> 60</p>
     * <p><b>Config key:</b> {@code humanization.fatigue_start_minutes}</p>
     *
     * @since 2.2.0
     */
    public static final ForgeConfigSpec.IntValue FATIGUE_START_MINUTES;

    /**
     * Minimum break interval in minutes.
     * <p><b>Range:</b> 5 to 120</p>
     * <p><b>Default:</b> 30</p>
     * <p><b>Config key:</b> {@code humanization.break_interval_minutes}</p>
     *
     * @since 2.2.0
     */
    public static final ForgeConfigSpec.IntValue BREAK_INTERVAL_MINUTES;

    /**
     * Break duration in minutes.
     * <p><b>Range:</b> 1 to 10</p>
     * <p><b>Default:</b> 2</p>
     * <p><b>Config key:</b> {@code humanization.break_duration_minutes}</p>
     *
     * @since 2.2.0
     */
    public static final ForgeConfigSpec.IntValue BREAK_DURATION_MINUTES;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("AI API Configuration").push("ai");

        AI_PROVIDER = builder
            .comment("AI provider to use: 'openai' (z.ai GLM-5, RECOMMENDED), 'groq', or 'gemini'")
            .define("provider", "openai");

        builder.pop();

        builder.comment("z.ai/OpenAI API Configuration").push("openai");

        OPENAI_API_KEY = builder
            .comment("Your z.ai API key (required - get from console.z.ai)")
            .define("apiKey", "");

        OPENAI_MODEL = builder
            .comment("LLM model: 'glm-5' (recommended), 'glm-4-flash' (faster), or 'gpt-4' (OpenAI)")
            .define("model", "glm-5");

        MAX_TOKENS = builder
            .comment("Maximum tokens per API request")
            .defineInRange("maxTokens", 8000, 100, 65536);

        TEMPERATURE = builder
            .comment("Temperature for AI responses (0.0-2.0, lower is more deterministic)")
            .defineInRange("temperature", 0.7, 0.0, 2.0);

        builder.pop();

        builder.comment("MineWright Behavior Configuration").push("behavior");

        ACTION_TICK_DELAY = builder
            .comment("Ticks between action checks (20 ticks = 1 second)")
            .defineInRange("actionTickDelay", 20, 1, 100);

        ENABLE_CHAT_RESPONSES = builder
            .comment("Allow crew members to respond in chat")
            .define("enableChatResponses", true);

        MAX_ACTIVE_CREW_MEMBERS = builder
            .comment("Maximum number of crew members that can be active simultaneously")
            .defineInRange("maxActiveCrewMembers", 10, 1, 50);

        builder.pop();

        // Voice Configuration
        builder.comment("Voice Integration Configuration").push("voice");

        VOICE_ENABLED = builder
            .comment("Enable voice input/output features")
            .define("enabled", false);

        VOICE_MODE = builder
            .comment("Voice system mode: 'disabled', 'logging' (logs what would be heard/said), or 'real' (actual TTS/STT)")
            .define("mode", "logging");

        VOICE_STT_LANGUAGE = builder
            .comment("Speech-to-text language (e.g., 'en-US', 'en-GB', 'es-ES')")
            .define("sttLanguage", "en-US");

        VOICE_TTS_VOICE = builder
            .comment("Text-to-speech voice name")
            .define("ttsVoice", "default");

        VOICE_TTS_VOLUME = builder
            .comment("TTS volume level (0.0 to 1.0)")
            .defineInRange("ttsVolume", 0.8, 0.0, 1.0);

        VOICE_TTS_RATE = builder
            .comment("TTS speech rate (0.5 to 2.0, 1.0 = normal)")
            .defineInRange("ttsRate", 1.0, 0.5, 2.0);

        VOICE_TTS_PITCH = builder
            .comment("TTS pitch adjustment (0.5 to 2.0, 1.0 = normal)")
            .defineInRange("ttsPitch", 1.0, 0.5, 2.0);

        VOICE_STT_SENSITIVITY = builder
            .comment("STT sensitivity for speech detection (0.0 to 1.0, higher = more sensitive)")
            .defineInRange("sttSensitivity", 0.5, 0.0, 1.0);

        VOICE_PUSH_TO_TALK = builder
            .comment("Require push-to-talk key for voice input (vs continuous listening)")
            .define("pushToTalk", true);

        VOICE_LISTENING_TIMEOUT = builder
            .comment("Auto-stop listening after N seconds of silence (0 = no timeout)")
            .defineInRange("listeningTimeout", 10, 0, 60);

        VOICE_DEBUG_LOGGING = builder
            .comment("Enable verbose logging for voice system operations")
            .define("debugLogging", true);

        builder.pop();

        // Hive Mind Configuration (Cloudflare Edge Integration)
        builder.comment("Hive Mind Configuration (Cloudflare Edge for tactical decisions)").push("hivemind");

        HIVEMIND_ENABLED = builder
            .comment("Enable Hive Mind - distributed AI with Cloudflare edge for tactical reflexes",
                     "When enabled, agents get sub-20ms combat/hazard responses from edge workers",
                     "When disabled, all decisions are made locally")
            .define("enabled", false);

        HIVEMIND_WORKER_URL = builder
            .comment("Cloudflare Worker URL (e.g., 'https://minecraft-agent-reflex.your-subdomain.workers.dev')")
            .define("workerUrl", "https://minecraft-agent-reflex.workers.dev");

        HIVEMIND_CONNECT_TIMEOUT = builder
            .comment("Connection timeout in milliseconds (for initial connection)")
            .defineInRange("connectTimeoutMs", 2000, 500, 10000);

        HIVEMIND_TACTICAL_TIMEOUT = builder
            .comment("Tactical decision timeout in milliseconds (target: sub-20ms, max: 100ms)")
            .defineInRange("tacticalTimeoutMs", 50, 10, 500);

        HIVEMIND_SYNC_TIMEOUT = builder
            .comment("State sync timeout in milliseconds (less time-critical)")
            .defineInRange("syncTimeoutMs", 1000, 100, 5000);

        HIVEMIND_TACTICAL_CHECK_INTERVAL = builder
            .comment("How often to check for tactical situations (in ticks, 20 ticks = 1 second)",
                     "Lower = faster reflexes but more API calls",
                     "Recommended: 20 (1 second) for balanced performance")
            .defineInRange("tacticalCheckInterval", 20, 5, 100);

        HIVEMIND_SYNC_INTERVAL = builder
            .comment("How often to sync state with edge (in ticks)",
                     "Higher = less network traffic but potentially stale state",
                     "Recommended: 100 (5 seconds)")
            .defineInRange("syncInterval", 100, 20, 200);

        HIVEMIND_FALLBACK_TO_LOCAL = builder
            .comment("When edge is unavailable, fall back to local decision-making",
                     "If false, agent will wait for edge response (not recommended)")
            .define("fallbackToLocal", true);

        builder.pop();

        // Skill Library Configuration
        builder.comment("Skill Library Configuration (Learning and pattern storage)").push("skill_library");

        SKILL_LIBRARY_ENABLED = builder
            .comment("Enable skill library for learning and storing successful action patterns",
                     "When enabled, agents learn from successful executions and reuse patterns",
                     "When disabled, each action is planned from scratch")
            .define("enabled", true);

        MAX_SKILLS_STORED = builder
            .comment("Maximum number of skills to store in the library",
                     "Higher = more learned patterns but more memory usage",
                     "Recommended: 100 for balanced performance")
            .defineInRange("max_skills", 100, 10, 1000);

        SKILL_SUCCESS_THRESHOLD = builder
            .comment("Success threshold for considering a skill as learned (0.0 to 1.0)",
                     "Skills with success rate above this threshold are stored",
                     "Higher = fewer but more reliable skills")
            .defineInRange("success_threshold", 0.7, 0.0, 1.0);

        builder.pop();

        // Cascade Router Configuration
        builder.comment("Cascade Router Configuration (Intelligent LLM selection)").push("cascade_router");

        CASCADE_ROUTER_ENABLED = builder
            .comment("Enable cascade router for intelligent LLM selection",
                     "When enabled, routes tasks to appropriate LLM based on complexity",
                     "When disabled, always uses primary LLM")
            .define("enabled", true);

        SEMANTIC_SIMILARITY_THRESHOLD = builder
            .comment("Semantic similarity threshold for cascade routing decisions (0.0 to 1.0)",
                     "Tasks with similarity above this threshold use cached/local LLM",
                     "Higher = more local processing, less API usage")
            .defineInRange("similarity_threshold", 0.85, 0.0, 1.0);

        USE_LOCAL_LLM = builder
            .comment("Use local LLM for cascade router fallback",
                     "When true, falls back to local LLM for similar tasks",
                     "When false, always uses primary API LLM")
            .define("use_local_llm", false);

        builder.pop();

        // Utility AI Configuration
        builder.comment("Utility AI Configuration (Decision-making weights)").push("utility_ai");

        UTILITY_AI_ENABLED = builder
            .comment("Enable utility AI for decision-making",
                     "When enabled, uses weighted scoring for action selection",
                     "When disabled, uses simple priority-based selection")
            .define("enabled", true);

        URGENCY_WEIGHT = builder
            .comment("Weight for urgency in utility calculations (0.0 to 2.0)",
                     "Higher = prioritizes time-sensitive actions more",
                     "Recommended: 1.0 for balanced behavior")
            .defineInRange("urgency_weight", 1.0, 0.0, 2.0);

        PROXIMITY_WEIGHT = builder
            .comment("Weight for proximity in utility calculations (0.0 to 2.0)",
                     "Higher = prioritizes nearby tasks more",
                     "Recommended: 0.8 for balanced behavior")
            .defineInRange("proximity_weight", 0.8, 0.0, 2.0);

        SAFETY_WEIGHT = builder
            .comment("Weight for safety in utility calculations (0.0 to 2.0)",
                     "Higher = prioritizes safe actions over risky ones",
                     "Recommended: 1.2 for safety-focused behavior")
            .defineInRange("safety_weight", 1.2, 0.0, 2.0);

        builder.pop();

        // Multi-Agent Configuration
        builder.comment("Multi-Agent Configuration (Coordination features)").push("multi_agent");

        MULTI_AGENT_ENABLED = builder
            .comment("Enable multi-agent coordination features",
                     "When enabled, agents can collaborate and coordinate tasks",
                     "When disabled, each agent operates independently")
            .define("enabled", true);

        MAX_BID_WAIT_MS = builder
            .comment("Maximum time to wait for agent bids in milliseconds",
                     "Lower = faster coordination but may miss capable agents",
                     "Recommended: 1000 for balanced performance")
            .defineInRange("max_bid_wait_ms", 1000, 100, 5000);

        BLACKBOARD_TTL_SECONDS = builder
            .comment("Time-to-live for blackboard entries in seconds",
                     "Lower = less stale data but more frequent updates",
                     "Recommended: 300 (5 minutes)")
            .defineInRange("blackboard_ttl_seconds", 300, 60, 3600);

        builder.pop();

        // Pathfinding Configuration
        builder.comment("Pathfinding Configuration (Navigation algorithms)").push("pathfinding");

        ENHANCED_PATHFINDING = builder
            .comment("Enable enhanced pathfinding algorithms",
                     "When enabled, uses advanced pathfinding with obstacle avoidance",
                     "When disabled, uses basic pathfinding")
            .define("enhanced", true);

        MAX_PATH_SEARCH_NODES = builder
            .comment("Maximum nodes to search in pathfinding algorithms",
                     "Higher = can find longer paths but uses more CPU",
                     "Recommended: 10000 for balanced performance")
            .defineInRange("max_search_nodes", 10000, 1000, 50000);

        builder.pop();

        // Performance Configuration
        builder.comment("Performance Configuration (Tick budget and enforcement)").push("performance");

        AI_TICK_BUDGET_MS = builder
            .comment("AI tick budget in milliseconds (must complete within this time)",
                     "Minecraft ticks are 50ms total, AI should use significantly less",
                     "Lower = more frequent operation yielding but smoother server performance",
                     "Recommended: 5ms for balanced performance (10% of tick budget)")
            .defineInRange("aiTickBudgetMs", 5, 1, 20);

        BUDGET_WARNING_THRESHOLD = builder
            .comment("Warning threshold as percentage of budget (50 to 95)",
                     "Warnings logged when AI operations exceed this percentage of budget",
                     "Lower = earlier warnings, more conservative operation",
                     "Recommended: 80 (warn when using 80%+ of budget)")
            .defineInRange("budgetWarningThreshold", 80, 50, 95);

        STRICT_BUDGET_ENFORCEMENT = builder
            .comment("Enable strict budget enforcement",
                     "When true, operations defer work when budget is exceeded",
                     "When false, budget is tracked but operations continue (not recommended)",
                     "Recommended: true for production servers")
            .define("strictBudgetEnforcement", true);

        PATHFINDING_TIMEOUT_MS = builder
            .comment("Pathfinding timeout threshold in milliseconds",
                     "Pathfinding operations exceeding this timeout are aborted",
                     "Higher = can find longer paths but may cause lag",
                     "Lower = faster abort but may fail to find paths",
                     "Recommended: 2000 for balanced performance")
            .defineInRange("pathfindingTimeoutMs", 2000, 100, 10000);

        builder.pop();

        // Semantic Cache Configuration
        builder.comment("Semantic Cache Configuration (Intelligent LLM response caching)").push("semantic_cache");

        SEMANTIC_CACHE_ENABLED = builder
            .comment("Enable semantic caching for LLM responses",
                     "When enabled, caches similar prompts using text similarity matching",
                     "Significantly reduces API costs for repetitive command patterns",
                     "When disabled, only exact-match caching is used")
            .define("enabled", true);

        SEMANTIC_CACHE_SIMILARITY_THRESHOLD = builder
            .comment("Minimum similarity threshold for semantic cache hits (0.5 to 1.0)",
                     "Prompts with similarity above this threshold are considered matches",
                     "Higher = more strict matching (fewer cache hits)",
                     "Lower = more permissive matching (may return incorrect responses)",
                     "Recommended: 0.85 for balanced accuracy and hit rate")
            .defineInRange("similarity_threshold", 0.85, 0.5, 1.0);

        SEMANTIC_CACHE_MAX_SIZE = builder
            .comment("Maximum number of entries in the semantic cache",
                     "Higher = more cache hits but more memory usage",
                     "Each entry ~2.5KB, so 500 entries ~1.25MB",
                     "Recommended: 500 for balanced performance")
            .defineInRange("max_size", 500, 100, 2000);

        SEMANTIC_CACHE_TTL_MINUTES = builder
            .comment("Time-to-live for semantic cache entries in minutes",
                     "Entries older than this are evicted from the cache",
                     "Lower = fresher responses but more cache misses",
                     "Recommended: 5 minutes for typical command patterns")
            .defineInRange("ttl_minutes", 5, 1, 60);

        SEMANTIC_CACHE_EMBEDDING_METHOD = builder
            .comment("Embedding method to use for semantic similarity",
                     "'tfidf' - Term frequency-inverse document frequency (recommended)",
                     "'ngram' - N-gram based similarity (faster, less accurate)",
                     "For production use, 'tfidf' is recommended")
            .define("embedding_method", "tfidf");

        builder.pop();

        // Humanization Configuration
        builder.comment("Humanization Configuration (Natural agent behavior)").push("humanization");

        HUMANIZATION_ENABLED = builder
            .comment("Enable humanization features for more natural agent behavior",
                     "When enabled, agents exhibit human-like timing, mistakes, and behaviors",
                     "When disabled, agents act with perfect consistency (robotic)",
                     "Recommended: true for immersive gameplay")
            .define("enabled", true);

        TIMING_VARIANCE = builder
            .comment("Timing variance as fraction of base value (0.0 to 1.0)",
                     "Higher = more variation in action delays",
                     "30% variance means delays vary by ±30% from base",
                     "Recommended: 0.3 for natural behavior")
            .defineInRange("timing_variance", 0.3, 0.0, 1.0);

        MIN_ACTION_DELAY_TICKS = builder
            .comment("Minimum action delay in ticks (20 ticks = 1 second)",
                     "Prevents unrealistically fast actions",
                     "Recommended: 2 ticks (100ms minimum)")
            .defineInRange("min_action_delay_ticks", 2, 1, 100);

        MAX_ACTION_DELAY_TICKS = builder
            .comment("Maximum action delay in ticks (20 ticks = 1 second)",
                     "Prevents excessively slow actions",
                     "Recommended: 20 ticks (1 second maximum)")
            .defineInRange("max_action_delay_ticks", 20, 1, 200);

        SPEED_VARIANCE = builder
            .comment("Movement speed variance as fraction of base speed (0.0 to 0.5)",
                     "Higher = more variation in movement speed",
                     "10% variance means speed varies by ±10% from base",
                     "Recommended: 0.1 for subtle variation")
            .defineInRange("speed_variance", 0.1, 0.0, 0.5);

        MICRO_MOVEMENT_CHANCE = builder
            .comment("Chance per tick of micro-movement (0.0 to 0.2)",
                     "Small fidgeting movements when idle or moving",
                     "5% chance = 1 in 20 ticks",
                     "Recommended: 0.05 for realistic micro-movements")
            .defineInRange("micro_movement_chance", 0.05, 0.0, 0.2);

        SMOOTH_LOOK = builder
            .comment("Enable smooth look transitions",
                     "When true, agents gradually turn instead of snapping",
                     "When false, instant direction changes",
                     "Recommended: true for natural movement")
            .define("smooth_look", true);

        MISTAKE_RATE = builder
            .comment("Base mistake rate for agent actions (0.0 to 0.2)",
                     "3% = 1 mistake per 33 actions (average human level)",
                     "Higher = more mistakes (beginner/fatigued)",
                     "Lower = fewer mistakes (expert)",
                     "Recommended: 0.03 for realistic behavior")
            .defineInRange("mistake_rate", 0.03, 0.0, 0.2);

        REACTION_TIME_MIN_MS = builder
            .comment("Minimum agent reaction time in milliseconds",
                     "Agents wait at least this long before responding to stimuli",
                     "Lower = faster responses (less realistic)",
                     "Higher = more realistic but potentially frustrating",
                     "Recommended: 150 for realistic human reaction time")
            .defineInRange("reaction_time_min_ms", 150, 50, 1000);

        REACTION_TIME_MAX_MS = builder
            .comment("Maximum agent reaction time in milliseconds",
                     "Agents wait at most this long before responding to stimuli",
                     "Lower = more consistent reaction times",
                     "Higher = more variance (can simulate fatigue/distraction)",
                     "Recommended: 500 for typical human variance")
            .defineInRange("reaction_time_max_ms", 500, 100, 5000);

        IDLE_ACTION_CHANCE = builder
            .comment("Idle action chance per tick (0.0 to 0.1)",
                     "2% = agent performs idle action ~2.4 times per second",
                     "Actions include: look around, fidget, stretch",
                     "Recommended: 0.02 for lively but not chaotic behavior")
            .defineInRange("idle_action_chance", 0.02, 0.0, 0.1);

        PERSONALITY_AFFECTS_IDLE = builder
            .comment("Enable personality-driven idle behaviors",
                     "When true, idle actions vary by personality traits",
                     "When false, all agents use same idle pattern",
                     "Recommended: true for diverse agent behaviors")
            .define("personality_affects_idle", true);

        SESSION_MODELING_ENABLED = builder
            .comment("Enable session modeling (warm-up, fatigue, breaks)",
                     "When true, agent performance changes over session",
                     "When false, consistent performance throughout",
                     "Recommended: true for long-term realism")
            .define("session_modeling_enabled", true);

        WARMUP_DURATION_MINUTES = builder
            .comment("Warm-up duration in minutes",
                     "Agents slower/more mistake-prone during warm-up",
                     "Recommended: 10 minutes for realistic warm-up")
            .defineInRange("warmup_duration_minutes", 10, 1, 60);

        FATIGUE_START_MINUTES = builder
            .comment("Fatigue onset time in minutes",
                     "Agents begin degrading after this time",
                     "Recommended: 60 minutes for typical play session")
            .defineInRange("fatigue_start_minutes", 60, 15, 180);

        BREAK_INTERVAL_MINUTES = builder
            .comment("Minimum break interval in minutes",
                     "Agents may take breaks after this interval",
                     "10% chance per check after minimum, forced at 2 hours",
                     "Recommended: 30 minutes for realistic break pattern")
            .defineInRange("break_interval_minutes", 30, 5, 120);

        BREAK_DURATION_MINUTES = builder
            .comment("Break duration in minutes",
                     "Agents pause actions during breaks",
                     "Recommended: 2 minutes for short breaks")
            .defineInRange("break_duration_minutes", 2, 1, 10);

        builder.pop();

        SPEC = builder.build();
    }

    /**
     * Validates the configuration and logs warnings for any issues.
     *
     * <p>This method checks for:
     * <ul>
     *   <li>Missing or empty API keys</li>
     *   <li>Invalid AI provider</li>
     *   <li>Invalid voice mode</li>
     *   <li>Out-of-range values (shouldn't happen with Forge's validation)</li>
     * </ul>
     *
     * @return true if all critical configuration is valid, false otherwise
     */
    public static boolean validateAndLog() {
        boolean isValid = true;
        LOGGER.info("Validating MineWright configuration...");

        // Validate AI provider
        String provider = AI_PROVIDER.get();
        if (provider == null || provider.trim().isEmpty()) {
            LOGGER.warn("AI provider is not set! Defaulting to 'openai' (z.ai).");
            isValid = false;
        } else if (!VALID_PROVIDERS.contains(provider.toLowerCase())) {
            LOGGER.warn("Invalid AI provider '{}'. Valid options: {}. Defaulting to 'openai' (z.ai).",
                provider, VALID_PROVIDERS);
            isValid = false;
        } else {
            LOGGER.info("AI provider: {}", provider);
        }

        // Validate API key
        String apiKey = OPENAI_API_KEY.get();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            LOGGER.error("API key is not configured! AI features will not work.");
            LOGGER.error("Please set 'apiKey' in [openai] section of config/minewright-common.toml");
            isValid = false;
        } else {
            // Log first few chars to confirm it's set without leaking the full key
            String preview = apiKey.length() > 8
                ? apiKey.substring(0, 4) + "..." + apiKey.substring(apiKey.length() - 4)
                : "****";
            LOGGER.info("API key configured: {}", preview);
        }

        // Validate voice mode
        String voiceMode = VOICE_MODE.get();
        if (voiceMode != null && !VALID_VOICE_MODES.contains(voiceMode.toLowerCase())) {
            LOGGER.warn("Invalid voice mode '{}'. Valid options: {}. Defaulting to 'disabled'.",
                voiceMode, VALID_VOICE_MODES);
            isValid = false;
        }

        // Log voice status
        if (VOICE_ENABLED.get()) {
            LOGGER.info("Voice system: enabled (mode: {})", VOICE_MODE.get());
        } else {
            LOGGER.info("Voice system: disabled");
        }

        // Log other important settings
        LOGGER.info("Max active crew members: {}", MAX_ACTIVE_CREW_MEMBERS.get());
        LOGGER.info("Action tick delay: {} ticks ({} seconds)",
            ACTION_TICK_DELAY.get(), ACTION_TICK_DELAY.get() / 20.0);
        LOGGER.info("Chat responses: {}", ENABLE_CHAT_RESPONSES.get() ? "enabled" : "disabled");

        // Log Hive Mind status
        if (HIVEMIND_ENABLED.get()) {
            LOGGER.info("Hive Mind: enabled (URL: {})", HIVEMIND_WORKER_URL.get());
        } else {
            LOGGER.info("Hive Mind: disabled");
        }

        // Validate and log new feature configurations
        validateNewFeatures();

        if (isValid) {
            LOGGER.info("MineWright configuration validated successfully.");
        } else {
            LOGGER.warn("MineWright configuration has issues. Please check config/minewright-common.toml");
        }

        return isValid;
    }

    /**
     * Validates new feature configurations.
     *
     * <p>This method validates:
     * <ul>
     *   <li>Skill Library thresholds and limits</li>
     *   <li>Cascade Router similarity thresholds</li>
     *   <li>Utility AI weight sums</li>
     *   <li>Multi-Agent timeout values</li>
     *   <li>Pathfinding node limits</li>
     * </ul>
     *
     * @since 2.0.0
     */
    private static void validateNewFeatures() {
        LOGGER.debug("Validating new feature configurations...");

        // Validate Skill Library
        if (SKILL_LIBRARY_ENABLED.get()) {
            double threshold = SKILL_SUCCESS_THRESHOLD.get();
            if (threshold < 0.0 || threshold > 1.0) {
                LOGGER.warn("Skill Library success threshold out of range: {}. Should be 0.0-1.0", threshold);
            }
            int maxSkills = MAX_SKILLS_STORED.get();
            if (maxSkills < 10 || maxSkills > 1000) {
                LOGGER.warn("Max skills stored out of range: {}. Should be 10-1000", maxSkills);
            }
            LOGGER.info("Skill Library: enabled (threshold: {}, max skills: {})", threshold, maxSkills);
        } else {
            LOGGER.info("Skill Library: disabled");
        }

        // Validate Cascade Router
        if (CASCADE_ROUTER_ENABLED.get()) {
            double similarity = SEMANTIC_SIMILARITY_THRESHOLD.get();
            if (similarity < 0.0 || similarity > 1.0) {
                LOGGER.warn("Semantic similarity threshold out of range: {}. Should be 0.0-1.0", similarity);
            }
            LOGGER.info("Cascade Router: enabled (similarity: {}, use_local_llm: {})",
                similarity, USE_LOCAL_LLM.get());
        } else {
            LOGGER.info("Cascade Router: disabled");
        }

        // Validate Utility AI
        if (UTILITY_AI_ENABLED.get()) {
            double urgency = URGENCY_WEIGHT.get();
            double proximity = PROXIMITY_WEIGHT.get();
            double safety = SAFETY_WEIGHT.get();
            double totalWeight = urgency + proximity + safety;

            if (urgency < 0.0 || urgency > 2.0) {
                LOGGER.warn("Urgency weight out of range: {}. Should be 0.0-2.0", urgency);
            }
            if (proximity < 0.0 || proximity > 2.0) {
                LOGGER.warn("Proximity weight out of range: {}. Should be 0.0-2.0", proximity);
            }
            if (safety < 0.0 || safety > 2.0) {
                LOGGER.warn("Safety weight out of range: {}. Should be 0.0-2.0", safety);
            }

            LOGGER.info("Utility AI: enabled (urgency: {}, proximity: {}, safety: {}, total: {})",
                urgency, proximity, safety, totalWeight);

            if (totalWeight > 5.0) {
                LOGGER.warn("Utility AI weights sum to {}, which is high. Consider balancing weights.", totalWeight);
            }
        } else {
            LOGGER.info("Utility AI: disabled");
        }

        // Validate Multi-Agent
        if (MULTI_AGENT_ENABLED.get()) {
            int maxBidWait = MAX_BID_WAIT_MS.get();
            int ttl = BLACKBOARD_TTL_SECONDS.get();

            if (maxBidWait < 100 || maxBidWait > 5000) {
                LOGGER.warn("Max bid wait out of range: {}. Should be 100-5000ms", maxBidWait);
            }
            if (ttl < 60 || ttl > 3600) {
                LOGGER.warn("Blackboard TTL out of range: {}. Should be 60-3600 seconds", ttl);
            }

            LOGGER.info("Multi-Agent: enabled (max_bid_wait: {}ms, blackboard_ttl: {}s)",
                maxBidWait, ttl);
        } else {
            LOGGER.info("Multi-Agent: disabled");
        }

        // Validate Pathfinding
        if (ENHANCED_PATHFINDING.get()) {
            int maxNodes = MAX_PATH_SEARCH_NODES.get();
            int timeout = PATHFINDING_TIMEOUT_MS.get();
            if (maxNodes < 1000 || maxNodes > 50000) {
                LOGGER.warn("Max path search nodes out of range: {}. Should be 1000-50000", maxNodes);
            }
            if (timeout < 100 || timeout > 10000) {
                LOGGER.warn("Pathfinding timeout out of range: {}. Should be 100-10000ms", timeout);
            }
            LOGGER.info("Pathfinding: enhanced (max_search_nodes: {}, timeout: {}ms)", maxNodes, timeout);
        } else {
            LOGGER.info("Pathfinding: basic");
        }

        // Validate Performance Settings
        int tickBudget = AI_TICK_BUDGET_MS.get();
        int budgetWarning = BUDGET_WARNING_THRESHOLD.get();
        boolean strictEnforcement = STRICT_BUDGET_ENFORCEMENT.get();
        LOGGER.info("Performance: tick_budget={}ms, warning_threshold={}%, strict_enforcement={}",
            tickBudget, budgetWarning, strictEnforcement);

        LOGGER.debug("New feature configuration validation completed");
    }

    /**
     * Validates the configuration and throws an exception for critical errors.
     *
     * <p>Unlike {@link #validateAndLog()}, this method throws a {@link ConfigException}
     * for critical configuration errors.</p>
     *
     * @throws ConfigException if critical configuration errors are found
     * @since 1.5.0
     */
    public static void validateOrThrow() throws ConfigException {
        LOGGER.debug("Performing strict configuration validation...");

        // Validate API key (critical)
        String apiKey = OPENAI_API_KEY.get();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw ConfigException.missingKey("apiKey", "openai", true);
        }

        // Validate AI provider
        String provider = AI_PROVIDER.get();
        if (provider == null || provider.trim().isEmpty()) {
            LOGGER.warn("AI provider is empty, will use 'openai' (z.ai) as fallback");
        } else if (!VALID_PROVIDERS.contains(provider.toLowerCase())) {
            throw ConfigException.invalidValue("provider", provider,
                String.join(", ", VALID_PROVIDERS), "ai");
        }

        // Validate Hive Mind URL if enabled
        if (HIVEMIND_ENABLED.get()) {
            String workerUrl = HIVEMIND_WORKER_URL.get();
            if (workerUrl != null && !workerUrl.trim().isEmpty()) {
                try {
                    new java.net.URL(workerUrl);
                } catch (java.net.MalformedURLException e) {
                    throw ConfigException.validationFailed("hivemind.workerUrl",
                        "Invalid URL: " + e.getMessage());
                }
            }
        }

        // Validate thresholds are 0-1
        if (SKILL_LIBRARY_ENABLED.get()) {
            double threshold = SKILL_SUCCESS_THRESHOLD.get();
            if (threshold < 0.0 || threshold > 1.0) {
                throw ConfigException.validationFailed("skill_library.success_threshold",
                    "Threshold must be between 0.0 and 1.0, got: " + threshold);
            }
        }

        if (CASCADE_ROUTER_ENABLED.get()) {
            double similarity = SEMANTIC_SIMILARITY_THRESHOLD.get();
            if (similarity < 0.0 || similarity > 1.0) {
                throw ConfigException.validationFailed("cascade_router.similarity_threshold",
                    "Similarity must be between 0.0 and 1.0, got: " + similarity);
            }
        }

        // Validate utility AI weights are reasonable
        if (UTILITY_AI_ENABLED.get()) {
            double urgency = URGENCY_WEIGHT.get();
            double proximity = PROXIMITY_WEIGHT.get();
            double safety = SAFETY_WEIGHT.get();

            if (urgency < 0.0 || urgency > 2.0) {
                throw ConfigException.validationFailed("utility_ai.urgency_weight",
                    "Weight must be between 0.0 and 2.0, got: " + urgency);
            }
            if (proximity < 0.0 || proximity > 2.0) {
                throw ConfigException.validationFailed("utility_ai.proximity_weight",
                    "Weight must be between 0.0 and 2.0, got: " + proximity);
            }
            if (safety < 0.0 || safety > 2.0) {
                throw ConfigException.validationFailed("utility_ai.safety_weight",
                    "Weight must be between 0.0 and 2.0, got: " + safety);
            }
        }

        LOGGER.debug("Configuration validation passed");
    }

    /**
     * Gets the current AI provider with validation and default fallback.
     *
     * @return The provider name (never null)
     */
    public static String getValidatedProvider() {
        String provider = AI_PROVIDER.get();
        if (provider == null || provider.trim().isEmpty()) {
            LOGGER.warn("AI provider not configured, using 'openai' (z.ai) as default");
            return "openai";
        }
        String lowerProvider = provider.toLowerCase();
        if (!VALID_PROVIDERS.contains(lowerProvider)) {
            LOGGER.warn("Unknown AI provider '{}', using 'openai' (z.ai) as default", provider);
            return "openai";
        }
        return lowerProvider;
    }

    /**
     * Checks if the API key is configured and non-empty.
     *
     * @return true if API key is present
     */
    public static boolean hasValidApiKey() {
        String key = getResolvedApiKey();
        return key != null && !key.trim().isEmpty();
    }

    /**
     * Gets the API key with environment variable resolution.
     *
     * <p>If the config value is in format {@code ${ENV_VAR_NAME}}, it will be
     * resolved from the environment. Otherwise returns the value as-is.</p>
     *
     * <p>Example config values:</p>
     * <ul>
     *   <li>{@code apiKey = "${OPENAI_API_KEY}"} - Resolved from env var</li>
     *   <li>{@code apiKey = "sk-abc123"} - Used directly (NOT recommended for commits)</li>
     * </ul>
     *
     * @return The resolved API key, or empty string if not set
     * @since 2.2.0
     */
    public static String getResolvedApiKey() {
        String key = OPENAI_API_KEY.get();
        return resolveEnvVar(key);
    }

    /**
     * Resolves a configuration value that may contain an environment variable reference.
     *
     * <p>Supports the format: {@code ${ENV_VAR_NAME}}</p>
     *
     * @param value The config value, possibly containing ${ENV_VAR} syntax
     * @return The resolved value, or the original if not an env var reference
     * @since 2.2.0
     */
    public static String resolveEnvVar(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }

        // Check for ${ENV_VAR} syntax
        if (value.startsWith("${") && value.endsWith("}")) {
            String envVarName = value.substring(2, value.length() - 1);
            String envValue = System.getenv(envVarName);
            if (envValue != null && !envValue.isEmpty()) {
                return envValue;
            }
            LOGGER.warn("Environment variable '{}' is not set or empty", envVarName);
            return "";
        }

        return value;
    }

    /**
     * Checks if voice is properly configured and enabled.
     *
     * @return true if voice is enabled
     */
    public static boolean isVoiceEnabled() {
        return VOICE_ENABLED.get();
    }

    /**
     * Gets the validated voice mode.
     *
     * @return The voice mode (never null)
     */
    public static String getValidatedVoiceMode() {
        String mode = VOICE_MODE.get();
        if (mode == null || mode.trim().isEmpty()) {
            return "disabled";
        }
        String lowerMode = mode.toLowerCase();
        return VALID_VOICE_MODES.contains(lowerMode) ? lowerMode : "disabled";
    }

    /**
     * Gets a configuration summary for logging/debugging.
     *
     * @return Summary string of current configuration
     * @since 1.5.0
     */
    public static String getConfigSummary() {
        return String.format(
            "MineWrightConfig[provider=%s, model=%s, maxTokens=%d, temperature=%.2f, " +
            "actionTickDelay=%d, maxCrew=%d, voice=%s, hivemind=%s, " +
            "skillLibrary=%s, cascadeRouter=%s, utilityAI=%s, multiAgent=%s, pathfinding=%s, " +
            "perf[tickBudget=%dms, pathfindingTimeout=%dms, strictEnforcement=%s], " +
            "humanization[reactionTime=%d-%dms, mistakeRate=%.3f]]",
            getValidatedProvider(),
            OPENAI_MODEL.get(),
            MAX_TOKENS.get(),
            TEMPERATURE.get(),
            ACTION_TICK_DELAY.get(),
            MAX_ACTIVE_CREW_MEMBERS.get(),
            VOICE_ENABLED.get() ? "enabled(" + VOICE_MODE.get() + ")" : "disabled",
            HIVEMIND_ENABLED.get() ? "enabled" : "disabled",
            SKILL_LIBRARY_ENABLED.get() ? "enabled" : "disabled",
            CASCADE_ROUTER_ENABLED.get() ? "enabled" : "disabled",
            UTILITY_AI_ENABLED.get() ? "enabled" : "disabled",
            MULTI_AGENT_ENABLED.get() ? "enabled" : "disabled",
            ENHANCED_PATHFINDING.get() ? "enhanced" : "basic",
            AI_TICK_BUDGET_MS.get(),
            PATHFINDING_TIMEOUT_MS.get(),
            STRICT_BUDGET_ENFORCEMENT.get() ? "true" : "false",
            REACTION_TIME_MIN_MS.get(),
            REACTION_TIME_MAX_MS.get(),
            MISTAKE_RATE.get()
        );
    }
}


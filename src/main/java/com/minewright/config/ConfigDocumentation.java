package com.minewright.config;

/**
 * Comprehensive documentation for all configuration options.
 *
 * <p>This class serves as a central reference for configuration values,
 * their defaults, valid ranges, and descriptions. It is used to generate
 * documentation and validate configuration.</p>
 *
 * <p><b>Config File Location:</b> {@code config/minewright-common.toml}</p>
 *
 * <p><b>Config Reload:</b> Edit the config file and use {@code /reload} to apply changes without restarting.</p>
 *
 * @since 1.5.0
 */
public final class ConfigDocumentation {

    private ConfigDocumentation() {
        // Utility class
    }

    // ========================================================================
    // AI Configuration Section
    // ========================================================================

    /**
     * AI Provider Configuration
     */
    public static final class AI {
        /** Config section: ai */
        public static final String SECTION = "ai";

        /** Configuration key: ai.provider */
        public static final String PROVIDER_KEY = SECTION + ".provider";

        /** Default value for provider */
        public static final String PROVIDER_DEFAULT = "groq";

        /** Valid provider options */
        public static final String PROVIDER_VALID_VALUES = "groq, openai, gemini";

        /** Description: AI provider to use */
        public static final String PROVIDER_DESCRIPTION = """
            AI provider to use for LLM requests:
            - groq: FASTEST, FREE tier available, uses Llama models
            - openai: GPT-4 and GPT-3.5, requires API key
            - gemini: Google's Gemini models, requires API key
            """;
    }

    /**
     * OpenAI/Gemini API Configuration
     */
    public static final class OPENAI {
        /** Config section: openai */
        public static final String SECTION = "openai";

        /** Configuration key: openai.apiKey */
        public static final String API_KEY_KEY = SECTION + ".apiKey";

        /** Configuration key: openai.model */
        public static final String MODEL_KEY = SECTION + ".model";

        /** Configuration key: openai.maxTokens */
        public static final String MAX_TOKENS_KEY = SECTION + ".maxTokens";

        /** Configuration key: openai.temperature */
        public static final String TEMPERATURE_KEY = SECTION + ".temperature";

        /** Default API key (empty, must be set by user) */
        public static final String API_KEY_DEFAULT = "";

        /** Default model name */
        public static final String MODEL_DEFAULT = "glm-5";

        /** Default max tokens */
        public static final int MAX_TOKENS_DEFAULT = 8000;

        /** Minimum max tokens */
        public static final int MAX_TOKENS_MIN = 100;

        /** Maximum max tokens */
        public static final int MAX_TOKENS_MAX = 65536;

        /** Default temperature */
        public static final double TEMPERATURE_DEFAULT = 0.7;

        /** Minimum temperature */
        public static final double TEMPERATURE_MIN = 0.0;

        /** Maximum temperature */
        public static final double TEMPERATURE_MAX = 2.0;

        /** Description: API key */
        public static final String API_KEY_DESCRIPTION = """
            Your API key for the chosen provider.
            - OpenAI: Get from https://platform.openai.com/api-keys
            - Groq: Get from https://console.groq.com/keys
            - Gemini: Get from https://makersuite.google.com/app/apikey
            """;

        /** Description: Model name */
        public static final String MODEL_DESCRIPTION = """
            LLM model to use:
            - For Groq: llama3-70b-8192, mixtral-8x7b-32768
            - For OpenAI: gpt-4, gpt-3.5-turbo
            - For Gemini: gemini-pro, gemini-ultra
            """;

        /** Description: Max tokens */
        public static final String MAX_TOKENS_DESCRIPTION = """
            Maximum tokens per API request.
            Higher values allow longer responses but cost more (for paid providers).
            """;

        /** Description: Temperature */
        public static final String TEMPERATURE_DESCRIPTION = """
            Temperature for AI responses (0.0 to 2.0):
            - 0.0: More deterministic, focused responses
            - 0.7: Balanced creativity and focus (recommended)
            - 1.0+: More creative, varied responses
            - 2.0: Maximum creativity, may be unfocused
            """;
    }

    // ========================================================================
    // Behavior Configuration Section
    // ========================================================================

    /**
     * Behavior Configuration
     */
    public static final class BEHAVIOR {
        /** Config section: behavior */
        public static final String SECTION = "behavior";

        /** Configuration key: behavior.actionTickDelay */
        public static final String ACTION_TICK_DELAY_KEY = SECTION + ".actionTickDelay";

        /** Configuration key: behavior.enableChatResponses */
        public static final String ENABLE_CHAT_RESPONSES_KEY = SECTION + ".enableChatResponses";

        /** Configuration key: behavior.maxActiveCrewMembers */
        public static final String MAX_ACTIVE_CREW_MEMBERS_KEY = SECTION + ".maxActiveCrewMembers";

        /** Default action tick delay */
        public static final int ACTION_TICK_DELAY_DEFAULT = 20;

        /** Minimum action tick delay */
        public static final int ACTION_TICK_DELAY_MIN = 1;

        /** Maximum action tick delay */
        public static final int ACTION_TICK_DELAY_MAX = 100;

        /** Default chat responses enabled */
        public static final boolean ENABLE_CHAT_RESPONSES_DEFAULT = true;

        /** Default max active crew members */
        public static final int MAX_ACTIVE_CREW_MEMBERS_DEFAULT = 10;

        /** Minimum max active crew members */
        public static final int MAX_ACTIVE_CREW_MEMBERS_MIN = 1;

        /** Maximum max active crew members */
        public static final int MAX_ACTIVE_CREW_MEMBERS_MAX = 50;

        /** Description: Action tick delay */
        public static final String ACTION_TICK_DELAY_DESCRIPTION = """
            Ticks between action checks (20 ticks = 1 second).
            Lower values = faster response but more CPU usage.
            Recommended: 20 (1 second) for balanced performance.
            """;

        /** Description: Enable chat responses */
        public static final String ENABLE_CHAT_RESPONSES_DESCRIPTION = """
            Allow crew members to respond in chat.
            When enabled, crew will announce their actions in the chat.
            """;

        /** Description: Max active crew members */
        public static final String MAX_ACTIVE_CREW_MEMBERS_DESCRIPTION = """
            Maximum number of crew members that can be active simultaneously.
            Higher values may impact performance depending on your CPU.
            """;
    }

    // ========================================================================
    // Voice Configuration Section
    // ========================================================================

    /**
     * Voice Configuration
     */
    public static final class VOICE {
        /** Config section: voice */
        public static final String SECTION = "voice";

        /** Configuration keys */
        public static final String ENABLED_KEY = SECTION + ".enabled";
        public static final String MODE_KEY = SECTION + ".mode";
        public static final String STT_LANGUAGE_KEY = SECTION + ".sttLanguage";
        public static final String TTS_VOICE_KEY = SECTION + ".ttsVoice";
        public static final String TTS_VOLUME_KEY = SECTION + ".ttsVolume";
        public static final String TTS_RATE_KEY = SECTION + ".ttsRate";
        public static final String TTS_PITCH_KEY = SECTION + ".ttsPitch";
        public static final String STT_SENSITIVITY_KEY = SECTION + ".sttSensitivity";
        public static final String PUSH_TO_TALK_KEY = SECTION + ".pushToTalk";
        public static final String LISTENING_TIMEOUT_KEY = SECTION + ".listeningTimeout";
        public static final String DEBUG_LOGGING_KEY = SECTION + ".debugLogging";

        /** Default values */
        public static final boolean ENABLED_DEFAULT = false;
        public static final String MODE_DEFAULT = "logging";
        public static final String STT_LANGUAGE_DEFAULT = "en-US";
        public static final String TTS_VOICE_DEFAULT = "default";
        public static final double TTS_VOLUME_DEFAULT = 0.8;
        public static final double TTS_RATE_DEFAULT = 1.0;
        public static final double TTS_PITCH_DEFAULT = 1.0;
        public static final double STT_SENSITIVITY_DEFAULT = 0.5;
        public static final boolean PUSH_TO_TALK_DEFAULT = true;
        public static final int LISTENING_TIMEOUT_DEFAULT = 10;
        public static final boolean DEBUG_LOGGING_DEFAULT = true;

        /** Value ranges */
        public static final String MODE_VALID_VALUES = "disabled, logging, real";
        public static final double TTS_VOLUME_MIN = 0.0;
        public static final double TTS_VOLUME_MAX = 1.0;
        public static final double TTS_RATE_MIN = 0.5;
        public static final double TTS_RATE_MAX = 2.0;
        public static final double TTS_PITCH_MIN = 0.5;
        public static final double TTS_PITCH_MAX = 2.0;
        public static final double STT_SENSITIVITY_MIN = 0.0;
        public static final double STT_SENSITIVITY_MAX = 1.0;
        public static final int LISTENING_TIMEOUT_MIN = 0;
        public static final int LISTENING_TIMEOUT_MAX = 60;

        /** Descriptions */
        public static final String ENABLED_DESCRIPTION = """
            Enable voice input/output features.
            Requires a microphone for input and speakers for output.
            """;

        public static final String MODE_DESCRIPTION = """
            Voice system mode:
            - disabled: Voice features completely off
            - logging: Logs what would be heard/said (for testing)
            - real: Actual TTS/STT functionality
            """;

        public static final String STT_LANGUAGE_DESCRIPTION = """
            Speech-to-text language code.
            Examples: en-US, en-GB, es-ES, fr-FR, de-DE, ja-JP
            """;

        public static final String TTS_VOICE_DESCRIPTION = """
            Text-to-speech voice name.
            Depends on the TTS engine being used.
            """;

        public static final String TTS_VOLUME_DESCRIPTION = """
            TTS volume level (0.0 to 1.0).
            """;

        public static final String TTS_RATE_DESCRIPTION = """
            TTS speech rate (0.5 to 2.0).
            1.0 = normal speed, 2.0 = 2x speed
            """;

        public static final String TTS_PITCH_DESCRIPTION = """
            TTS pitch adjustment (0.5 to 2.0).
            1.0 = normal pitch
            """;

        public static final String STT_SENSITIVITY_DESCRIPTION = """
            STT sensitivity for speech detection (0.0 to 1.0).
            Higher = more sensitive to quiet sounds
            """;

        public static final String PUSH_TO_TALK_DESCRIPTION = """
            Require push-to-talk key for voice input.
            If false, uses continuous listening mode.
            """;

        public static final String LISTENING_TIMEOUT_DESCRIPTION = """
            Auto-stop listening after N seconds of silence.
            0 = no timeout (manually stop listening)
            """;

        public static final String DEBUG_LOGGING_DESCRIPTION = """
            Enable verbose logging for voice system operations.
            Useful for troubleshooting voice issues.
            """;
    }

    // ========================================================================
    // Hive Mind Configuration Section
    // ========================================================================

    /**
     * Hive Mind Configuration
     */
    public static final class HIVEMIND {
        /** Config section: hivemind */
        public static final String SECTION = "hivemind";

        /** Configuration keys */
        public static final String ENABLED_KEY = SECTION + ".enabled";
        public static final String WORKER_URL_KEY = SECTION + ".workerUrl";
        public static final String CONNECT_TIMEOUT_KEY = SECTION + ".connectTimeoutMs";
        public static final String TACTICAL_TIMEOUT_KEY = SECTION + ".tacticalTimeoutMs";
        public static final String SYNC_TIMEOUT_KEY = SECTION + ".syncTimeoutMs";
        public static final String TACTICAL_CHECK_INTERVAL_KEY = SECTION + ".tacticalCheckInterval";
        public static final String SYNC_INTERVAL_KEY = SECTION + ".syncInterval";
        public static final String FALLBACK_TO_LOCAL_KEY = SECTION + ".fallbackToLocal";

        /** Default values */
        public static final boolean ENABLED_DEFAULT = false;
        public static final String WORKER_URL_DEFAULT = "https://minecraft-agent-reflex.workers.dev";
        public static final int CONNECT_TIMEOUT_DEFAULT = 2000;
        public static final int TACTICAL_TIMEOUT_DEFAULT = 50;
        public static final int SYNC_TIMEOUT_DEFAULT = 1000;
        public static final int TACTICAL_CHECK_INTERVAL_DEFAULT = 20;
        public static final int SYNC_INTERVAL_DEFAULT = 100;
        public static final boolean FALLBACK_TO_LOCAL_DEFAULT = true;

        /** Value ranges */
        public static final int CONNECT_TIMEOUT_MIN = 500;
        public static final int CONNECT_TIMEOUT_MAX = 10000;
        public static final int TACTICAL_TIMEOUT_MIN = 10;
        public static final int TACTICAL_TIMEOUT_MAX = 500;
        public static final int SYNC_TIMEOUT_MIN = 100;
        public static final int SYNC_TIMEOUT_MAX = 5000;
        public static final int TACTICAL_CHECK_INTERVAL_MIN = 5;
        public static final int TACTICAL_CHECK_INTERVAL_MAX = 100;
        public static final int SYNC_INTERVAL_MIN = 20;
        public static final int SYNC_INTERVAL_MAX = 200;

        /** Descriptions */
        public static final String ENABLED_DESCRIPTION = """
            Enable Hive Mind - distributed AI with Cloudflare edge for tactical reflexes.
            When enabled, agents get sub-20ms combat/hazard responses from edge workers.
            When disabled, all decisions are made locally.
            """;

        public static final String WORKER_URL_DESCRIPTION = """
            Cloudflare Worker URL for Hive Mind edge computing.
            Deploy your own worker for best performance.
            """;

        public static final String CONNECT_TIMEOUT_DESCRIPTION = """
            Connection timeout in milliseconds.
            For initial connection to the edge worker.
            """;

        public static final String TACTICAL_TIMEOUT_DESCRIPTION = """
            Tactical decision timeout in milliseconds.
            Target: sub-20ms, Max: 100ms for combat reactions.
            """;

        public static final String SYNC_TIMEOUT_DESCRIPTION = """
            State sync timeout in milliseconds.
            Less time-critical than tactical decisions.
            """;

        public static final String TACTICAL_CHECK_INTERVAL_DESCRIPTION = """
            How often to check for tactical situations (in ticks).
            20 ticks = 1 second.
            Lower = faster reflexes but more API calls.
            """;

        public static final String SYNC_INTERVAL_DESCRIPTION = """
            How often to sync state with edge (in ticks).
            Higher = less network traffic but potentially stale state.
            """;

        public static final String FALLBACK_TO_LOCAL_DESCRIPTION = """
            When edge is unavailable, fall back to local decision-making.
            If false, agent will wait for edge response (not recommended).
            """;
    }

    // ========================================================================
    // Skill Library Configuration Section
    // ========================================================================

    /**
     * Skill Library Configuration
     */
    public static final class SKILL_LIBRARY {
        /** Config section: skill_library */
        public static final String SECTION = "skill_library";

        /** Configuration keys */
        public static final String ENABLED_KEY = SECTION + ".enabled";
        public static final String MAX_SKILLS_KEY = SECTION + ".max_skills";
        public static final String SUCCESS_THRESHOLD_KEY = SECTION + ".success_threshold";

        /** Default values */
        public static final boolean ENABLED_DEFAULT = true;
        public static final int MAX_SKILLS_DEFAULT = 100;
        public static final double SUCCESS_THRESHOLD_DEFAULT = 0.7;

        /** Value ranges */
        public static final int MAX_SKILLS_MIN = 10;
        public static final int MAX_SKILLS_MAX = 1000;
        public static final double SUCCESS_THRESHOLD_MIN = 0.0;
        public static final double SUCCESS_THRESHOLD_MAX = 1.0;

        /** Descriptions */
        public static final String ENABLED_DESCRIPTION = """
            Enable skill library for learning and storing successful action patterns.
            When enabled, agents learn from successful executions and reuse patterns.
            When disabled, each action is planned from scratch.
            """;

        public static final String MAX_SKILLS_DESCRIPTION = """
            Maximum number of skills to store in the library.
            Higher = more learned patterns but more memory usage.
            Recommended: 100 for balanced performance.
            """;

        public static final String SUCCESS_THRESHOLD_DESCRIPTION = """
            Success threshold for considering a skill as learned (0.0 to 1.0).
            Skills with success rate above this threshold are stored.
            Higher = fewer but more reliable skills.
            """;
    }

    // ========================================================================
    // Cascade Router Configuration Section
    // ========================================================================

    /**
     * Cascade Router Configuration
     */
    public static final class CASCADE_ROUTER {
        /** Config section: cascade_router */
        public static final String SECTION = "cascade_router";

        /** Configuration keys */
        public static final String ENABLED_KEY = SECTION + ".enabled";
        public static final String SIMILARITY_THRESHOLD_KEY = SECTION + ".similarity_threshold";
        public static final String USE_LOCAL_LLM_KEY = SECTION + ".use_local_llm";

        /** Default values */
        public static final boolean ENABLED_DEFAULT = true;
        public static final double SIMILARITY_THRESHOLD_DEFAULT = 0.85;
        public static final boolean USE_LOCAL_LLM_DEFAULT = false;

        /** Value ranges */
        public static final double SIMILARITY_THRESHOLD_MIN = 0.0;
        public static final double SIMILARITY_THRESHOLD_MAX = 1.0;

        /** Descriptions */
        public static final String ENABLED_DESCRIPTION = """
            Enable cascade router for intelligent LLM selection.
            When enabled, routes tasks to appropriate LLM based on complexity.
            When disabled, always uses primary LLM.
            """;

        public static final String SIMILARITY_THRESHOLD_DESCRIPTION = """
            Semantic similarity threshold for cascade routing decisions (0.0 to 1.0).
            Tasks with similarity above this threshold use cached/local LLM.
            Higher = more local processing, less API usage.
            """;

        public static final String USE_LOCAL_LLM_DESCRIPTION = """
            Use local LLM for cascade router fallback.
            When true, falls back to local LLM for similar tasks.
            When false, always uses primary API LLM.
            """;
    }

    // ========================================================================
    // Utility AI Configuration Section
    // ========================================================================

    /**
     * Utility AI Configuration
     */
    public static final class UTILITY_AI {
        /** Config section: utility_ai */
        public static final String SECTION = "utility_ai";

        /** Configuration keys */
        public static final String ENABLED_KEY = SECTION + ".enabled";
        public static final String URGENCY_WEIGHT_KEY = SECTION + ".urgency_weight";
        public static final String PROXIMITY_WEIGHT_KEY = SECTION + ".proximity_weight";
        public static final String SAFETY_WEIGHT_KEY = SECTION + ".safety_weight";

        /** Default values */
        public static final boolean ENABLED_DEFAULT = true;
        public static final double URGENCY_WEIGHT_DEFAULT = 1.0;
        public static final double PROXIMITY_WEIGHT_DEFAULT = 0.8;
        public static final double SAFETY_WEIGHT_DEFAULT = 1.2;

        /** Value ranges */
        public static final double WEIGHT_MIN = 0.0;
        public static final double WEIGHT_MAX = 2.0;

        /** Descriptions */
        public static final String ENABLED_DESCRIPTION = """
            Enable utility AI for decision-making.
            When enabled, uses weighted scoring for action selection.
            When disabled, uses simple priority-based selection.
            """;

        public static final String URGENCY_WEIGHT_DESCRIPTION = """
            Weight for urgency in utility calculations (0.0 to 2.0).
            Higher = prioritizes time-sensitive actions more.
            Recommended: 1.0 for balanced behavior.
            """;

        public static final String PROXIMITY_WEIGHT_DESCRIPTION = """
            Weight for proximity in utility calculations (0.0 to 2.0).
            Higher = prioritizes nearby tasks more.
            Recommended: 0.8 for balanced behavior.
            """;

        public static final String SAFETY_WEIGHT_DESCRIPTION = """
            Weight for safety in utility calculations (0.0 to 2.0).
            Higher = prioritizes safe actions over risky ones.
            Recommended: 1.2 for safety-focused behavior.
            """;
    }

    // ========================================================================
    // Multi-Agent Configuration Section
    // ========================================================================

    /**
     * Multi-Agent Configuration
     */
    public static final class MULTI_AGENT {
        /** Config section: multi_agent */
        public static final String SECTION = "multi_agent";

        /** Configuration keys */
        public static final String ENABLED_KEY = SECTION + ".enabled";
        public static final String MAX_BID_WAIT_MS_KEY = SECTION + ".max_bid_wait_ms";
        public static final String BLACKBOARD_TTL_SECONDS_KEY = SECTION + ".blackboard_ttl_seconds";

        /** Default values */
        public static final boolean ENABLED_DEFAULT = true;
        public static final int MAX_BID_WAIT_MS_DEFAULT = 1000;
        public static final int BLACKBOARD_TTL_SECONDS_DEFAULT = 300;

        /** Value ranges */
        public static final int MAX_BID_WAIT_MS_MIN = 100;
        public static final int MAX_BID_WAIT_MS_MAX = 5000;
        public static final int BLACKBOARD_TTL_SECONDS_MIN = 60;
        public static final int BLACKBOARD_TTL_SECONDS_MAX = 3600;

        /** Descriptions */
        public static final String ENABLED_DESCRIPTION = """
            Enable multi-agent coordination features.
            When enabled, agents can collaborate and coordinate tasks.
            When disabled, each agent operates independently.
            """;

        public static final String MAX_BID_WAIT_MS_DESCRIPTION = """
            Maximum time to wait for agent bids in milliseconds.
            Lower = faster coordination but may miss capable agents.
            Recommended: 1000 for balanced performance.
            """;

        public static final String BLACKBOARD_TTL_SECONDS_DESCRIPTION = """
            Time-to-live for blackboard entries in seconds.
            Lower = less stale data but more frequent updates.
            Recommended: 300 (5 minutes).
            """;
    }

    // ========================================================================
    // Pathfinding Configuration Section
    // ========================================================================

    /**
     * Pathfinding Configuration
     */
    public static final class PATHFINDING {
        /** Config section: pathfinding */
        public static final String SECTION = "pathfinding";

        /** Configuration keys */
        public static final String ENHANCED_KEY = SECTION + ".enhanced";
        public static final String MAX_SEARCH_NODES_KEY = SECTION + ".max_search_nodes";

        /** Default values */
        public static final boolean ENHANCED_DEFAULT = true;
        public static final int MAX_SEARCH_NODES_DEFAULT = 10000;

        /** Value ranges */
        public static final int MAX_SEARCH_NODES_MIN = 1000;
        public static final int MAX_SEARCH_NODES_MAX = 50000;

        /** Descriptions */
        public static final String ENHANCED_DESCRIPTION = """
            Enable enhanced pathfinding algorithms.
            When enabled, uses advanced pathfinding with obstacle avoidance.
            When disabled, uses basic pathfinding.
            """;

        public static final String MAX_SEARCH_NODES_DESCRIPTION = """
            Maximum nodes to search in pathfinding algorithms.
            Higher = can find longer paths but uses more CPU.
            Recommended: 10000 for balanced performance.
            """;
    }

    // ========================================================================
    // Configuration File Template
    // ========================================================================

    /**
     * Generates a complete config file template with all defaults and documentation.
     *
     * @return TOML-formatted config template
     */
    public static String generateConfigTemplate() {
        return """
            # ========================================================================
            # MineWright Configuration File
            # ========================================================================
            # This file controls all aspects of the MineWright mod.
            #
            # Config Reload: Edit this file and use /reload in-game to apply changes.
            # File Location: config/minewright-common.toml
            #
            # Documentation: https://github.com/your-repo/minewright/wiki/Configuration
            # ========================================================================

            # ------------------------------------------------------------------------
            # AI API Configuration
            # ------------------------------------------------------------------------
            [ai]
            # AI provider to use: groq (FASTEST, FREE), openai, or gemini
            # Default: groq
            provider = "%s"

            # ------------------------------------------------------------------------
            # OpenAI/Gemini API Configuration
            # ------------------------------------------------------------------------
            [openai]
            # Your API key (required for all providers)
            # Get from: https://platform.openai.com/api-keys (OpenAI)
            #          https://console.groq.com/keys (Groq)
            #          https://makersuite.google.com/app/apikey (Gemini)
            apiKey = "%s"

            # LLM model to use
            # Groq: llama3-70b-8192, mixtral-8x7b-32768
            # OpenAI: gpt-4, gpt-3.5-turbo
            # Gemini: gemini-pro
            # Default: glm-5
            model = "%s"

            # Maximum tokens per API request (100 to 65536)
            # Higher = longer responses but more cost (for paid providers)
            # Default: 8000
            maxTokens = %d

            # Temperature for AI responses (0.0 to 2.0)
            # 0.0 = deterministic, 0.7 = balanced, 2.0 = creative
            # Default: 0.7
            temperature = %.1f

            # ------------------------------------------------------------------------
            # Behavior Configuration
            # ------------------------------------------------------------------------
            [behavior]
            # Ticks between action checks (20 ticks = 1 second)
            # Range: 1 to 100, Default: 20
            actionTickDelay = %d

            # Allow crew members to respond in chat
            # Default: true
            enableChatResponses = %b

            # Maximum number of crew members that can be active simultaneously
            # Range: 1 to 50, Default: 10
            maxActiveCrewMembers = %d

            # ------------------------------------------------------------------------
            # Voice Integration Configuration
            # ------------------------------------------------------------------------
            [voice]
            # Enable voice input/output features
            # Default: false
            enabled = %b

            # Voice system mode: disabled, logging, or real
            # logging = logs what would be heard/said (for testing)
            # real = actual TTS/STT functionality
            # Default: logging
            mode = "%s"

            # Speech-to-text language (e.g., en-US, en-GB, es-ES)
            # Default: en-US
            sttLanguage = "%s"

            # Text-to-speech voice name
            # Default: default
            ttsVoice = "%s"

            # TTS volume level (0.0 to 1.0)
            # Default: 0.8
            ttsVolume = %.1f

            # TTS speech rate (0.5 to 2.0, 1.0 = normal)
            # Default: 1.0
            ttsRate = %.1f

            # TTS pitch adjustment (0.5 to 2.0, 1.0 = normal)
            # Default: 1.0
            ttsPitch = %.1f

            # STT sensitivity for speech detection (0.0 to 1.0)
            # Higher = more sensitive
            # Default: 0.5
            sttSensitivity = %.1f

            # Require push-to-talk key for voice input
            # Default: true
            pushToTalk = %b

            # Auto-stop listening after N seconds of silence (0 = no timeout)
            # Range: 0 to 60, Default: 10
            listeningTimeout = %d

            # Enable verbose logging for voice system operations
            # Default: true
            debugLogging = %b

            # ------------------------------------------------------------------------
            # Hive Mind Configuration (Cloudflare Edge)
            # ------------------------------------------------------------------------
            [hivemind]
            # Enable Hive Mind - distributed AI for tactical reflexes
            # When enabled, agents get sub-20ms combat responses from edge workers
            # Default: false
            enabled = %b

            # Cloudflare Worker URL
            # Default: https://minecraft-agent-reflex.workers.dev
            workerUrl = "%s"

            # Connection timeout in milliseconds
            # Range: 500 to 10000, Default: 2000
            connectTimeoutMs = %d

            # Tactical decision timeout in milliseconds (target: sub-20ms)
            # Range: 10 to 500, Default: 50
            tacticalTimeoutMs = %d

            # State sync timeout in milliseconds
            # Range: 100 to 5000, Default: 1000
            syncTimeoutMs = %d

            # How often to check for tactical situations (in ticks, 20 = 1 second)
            # Range: 5 to 100, Default: 20
            tacticalCheckInterval = %d

            # How often to sync state with edge (in ticks)
            # Range: 20 to 200, Default: 100
            syncInterval = %d

            # When edge is unavailable, fall back to local decision-making
            # Default: true
            fallbackToLocal = %b

            # ------------------------------------------------------------------------
            # Skill Library Configuration
            # ------------------------------------------------------------------------
            [skill_library]
            # Enable skill library for learning and storing successful action patterns
            # When enabled, agents learn from successful executions and reuse patterns
            # Default: true
            enabled = %b

            # Maximum number of skills to store in the library
            # Range: 10 to 1000, Default: 100
            max_skills = %d

            # Success threshold for considering a skill as learned (0.0 to 1.0)
            # Skills with success rate above this threshold are stored
            # Default: 0.7
            success_threshold = %.1f

            # ------------------------------------------------------------------------
            # Cascade Router Configuration
            # ------------------------------------------------------------------------
            [cascade_router]
            # Enable cascade router for intelligent LLM selection
            # When enabled, routes tasks to appropriate LLM based on complexity
            # Default: true
            enabled = %b

            # Semantic similarity threshold for cascade routing decisions (0.0 to 1.0)
            # Tasks with similarity above this threshold use cached/local LLM
            # Default: 0.85
            similarity_threshold = %.2f

            # Use local LLM for cascade router fallback
            # When true, falls back to local LLM for similar tasks
            # Default: false
            use_local_llm = %b

            # ------------------------------------------------------------------------
            # Utility AI Configuration
            # ------------------------------------------------------------------------
            [utility_ai]
            # Enable utility AI for decision-making
            # When enabled, uses weighted scoring for action selection
            # Default: true
            enabled = %b

            # Weight for urgency in utility calculations (0.0 to 2.0)
            # Higher = prioritizes time-sensitive actions more
            # Default: 1.0
            urgency_weight = %.1f

            # Weight for proximity in utility calculations (0.0 to 2.0)
            # Higher = prioritizes nearby tasks more
            # Default: 0.8
            proximity_weight = %.1f

            # Weight for safety in utility calculations (0.0 to 2.0)
            # Higher = prioritizes safe actions over risky ones
            # Default: 1.2
            safety_weight = %.1f

            # ------------------------------------------------------------------------
            # Multi-Agent Configuration
            # ------------------------------------------------------------------------
            [multi_agent]
            # Enable multi-agent coordination features
            # When enabled, agents can collaborate and coordinate tasks
            # Default: true
            enabled = %b

            # Maximum time to wait for agent bids in milliseconds
            # Range: 100 to 5000, Default: 1000
            max_bid_wait_ms = %d

            # Time-to-live for blackboard entries in seconds
            # Range: 60 to 3600, Default: 300
            blackboard_ttl_seconds = %d

            # ------------------------------------------------------------------------
            # Pathfinding Configuration
            # ------------------------------------------------------------------------
            [pathfinding]
            # Enable enhanced pathfinding algorithms
            # When enabled, uses advanced pathfinding with obstacle avoidance
            # Default: true
            enhanced = %b

            # Maximum nodes to search in pathfinding algorithms
            # Range: 1000 to 50000, Default: 10000
            max_search_nodes = %d
            """.formatted(
            AI.PROVIDER_DEFAULT,
            OPENAI.API_KEY_DEFAULT,
            OPENAI.MODEL_DEFAULT,
            OPENAI.MAX_TOKENS_DEFAULT,
            OPENAI.TEMPERATURE_DEFAULT,
            BEHAVIOR.ACTION_TICK_DELAY_DEFAULT,
            BEHAVIOR.ENABLE_CHAT_RESPONSES_DEFAULT,
            BEHAVIOR.MAX_ACTIVE_CREW_MEMBERS_DEFAULT,
            VOICE.ENABLED_DEFAULT,
            VOICE.MODE_DEFAULT,
            VOICE.STT_LANGUAGE_DEFAULT,
            VOICE.TTS_VOICE_DEFAULT,
            VOICE.TTS_VOLUME_DEFAULT,
            VOICE.TTS_RATE_DEFAULT,
            VOICE.TTS_PITCH_DEFAULT,
            VOICE.STT_SENSITIVITY_DEFAULT,
            VOICE.PUSH_TO_TALK_DEFAULT,
            VOICE.LISTENING_TIMEOUT_DEFAULT,
            VOICE.DEBUG_LOGGING_DEFAULT,
            HIVEMIND.ENABLED_DEFAULT,
            HIVEMIND.WORKER_URL_DEFAULT,
            HIVEMIND.CONNECT_TIMEOUT_DEFAULT,
            HIVEMIND.TACTICAL_TIMEOUT_DEFAULT,
            HIVEMIND.SYNC_TIMEOUT_DEFAULT,
            HIVEMIND.TACTICAL_CHECK_INTERVAL_DEFAULT,
            HIVEMIND.SYNC_INTERVAL_DEFAULT,
            HIVEMIND.FALLBACK_TO_LOCAL_DEFAULT,
            SKILL_LIBRARY.ENABLED_DEFAULT,
            SKILL_LIBRARY.MAX_SKILLS_DEFAULT,
            SKILL_LIBRARY.SUCCESS_THRESHOLD_DEFAULT,
            CASCADE_ROUTER.ENABLED_DEFAULT,
            CASCADE_ROUTER.SIMILARITY_THRESHOLD_DEFAULT,
            CASCADE_ROUTER.USE_LOCAL_LLM_DEFAULT,
            UTILITY_AI.ENABLED_DEFAULT,
            UTILITY_AI.URGENCY_WEIGHT_DEFAULT,
            UTILITY_AI.PROXIMITY_WEIGHT_DEFAULT,
            UTILITY_AI.SAFETY_WEIGHT_DEFAULT,
            MULTI_AGENT.ENABLED_DEFAULT,
            MULTI_AGENT.MAX_BID_WAIT_MS_DEFAULT,
            MULTI_AGENT.BLACKBOARD_TTL_SECONDS_DEFAULT,
            PATHFINDING.ENHANCED_DEFAULT,
            PATHFINDING.MAX_SEARCH_NODES_DEFAULT
        );
    }
}

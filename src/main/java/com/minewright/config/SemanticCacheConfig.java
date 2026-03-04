package com.minewright.config;

import com.minewright.testutil.TestLogger;
import org.slf4j.Logger;
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Configuration for semantic caching of LLM responses.
 *
 * <h2>Configuration Section</h2>
 * <p><b>{@code [semantic_cache]}</b> - Intelligent caching settings</p>
 *
 * <h2>Overview</h2>
 * <p>When enabled, caches similar prompts using text similarity matching.
 * Significantly reduces API costs for repetitive command patterns.</p>
 *
 * @since 3.0.0
 */
public class SemanticCacheConfig {
    private static final Logger LOGGER = TestLogger.getLogger(SemanticCacheConfig.class);

    /** The Forge configuration spec */
    public static final ForgeConfigSpec SPEC;

    // ------------------------------------------------------------------------
    // Semantic Cache Configuration
    // ------------------------------------------------------------------------

    /**
     * Enable semantic caching for LLM responses.
     * <p>When enabled, caches similar prompts using text similarity matching.</p>
     * <p><b>Default:</b> {@code true}</p>
     * <p><b>Config key:</b> {@code semantic_cache.enabled}</p>
     */
    public static final ForgeConfigSpec.BooleanValue SEMANTIC_CACHE_ENABLED;

    /**
     * Minimum similarity threshold for semantic cache hits.
     * <p>Prompts with similarity above this threshold are considered matches.</p>
     * <p><b>Range:</b> 0.5 to 1.0</p>
     * <p><b>Default:</b> 0.85</p>
     * <p><b>Config key:</b> {@code semantic_cache.similarity_threshold}</p>
     */
    public static final ForgeConfigSpec.DoubleValue SEMANTIC_CACHE_SIMILARITY_THRESHOLD;

    /**
     * Maximum number of entries in the semantic cache.
     * <p><b>Range:</b> 100 to 2000</p>
     * <p><b>Default:</b> 500</p>
     * <p><b>Config key:</b> {@code semantic_cache.max_size}</p>
     */
    public static final ForgeConfigSpec.IntValue SEMANTIC_CACHE_MAX_SIZE;

    /**
     * Time-to-live for semantic cache entries in minutes.
     * <p>Entries older than this are evicted from the cache.</p>
     * <p><b>Range:</b> 1 to 60</p>
     * <p><b>Default:</b> 5</p>
     * <p><b>Config key:</b> {@code semantic_cache.ttl_minutes}</p>
     */
    public static final ForgeConfigSpec.IntValue SEMANTIC_CACHE_TTL_MINUTES;

    /**
     * Embedding method to use for semantic similarity.
     * <p><b>Valid values:</b> {@code tfidf}, {@code ngram}</p>
     * <p><b>Default:</b> {@code tfidf}</p>
     * <p><b>Config key:</b> {@code semantic_cache.embedding_method}</p>
     */
    public static final ForgeConfigSpec.ConfigValue<String> SEMANTIC_CACHE_EMBEDDING_METHOD;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

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

        SPEC = builder.build();
    }

    /**
     * Validates the semantic cache configuration.
     *
     * @return true if configuration is valid
     */
    public static boolean validate() {
        LOGGER.info("Validating semantic cache configuration...");

        if (SEMANTIC_CACHE_ENABLED.get()) {
            LOGGER.info("Semantic Cache: enabled (threshold: {}, max_size: {}, ttl: {}min, method: {})",
                SEMANTIC_CACHE_SIMILARITY_THRESHOLD.get(),
                SEMANTIC_CACHE_MAX_SIZE.get(),
                SEMANTIC_CACHE_TTL_MINUTES.get(),
                SEMANTIC_CACHE_EMBEDDING_METHOD.get());
        } else {
            LOGGER.info("Semantic Cache: disabled");
        }

        return true;
    }

    /**
     * Gets a configuration summary for logging/debugging.
     *
     * @return Summary string of current semantic cache configuration
     */
    public static String getConfigSummary() {
        if (!SEMANTIC_CACHE_ENABLED.get()) {
            return "SemanticCacheConfig[disabled]";
        }
        return String.format(
            "SemanticCacheConfig[enabled, threshold=%.2f, maxSize=%d, ttl=%dmin, method=%s]",
            SEMANTIC_CACHE_SIMILARITY_THRESHOLD.get(),
            SEMANTIC_CACHE_MAX_SIZE.get(),
            SEMANTIC_CACHE_TTL_MINUTES.get(),
            SEMANTIC_CACHE_EMBEDDING_METHOD.get()
        );
    }
}

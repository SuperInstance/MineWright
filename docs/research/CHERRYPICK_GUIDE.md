# Selective Adoption Guide: Upstream Patterns

## Overview

This guide provides specific instructions for adopting high-value patterns from upstream without merging. Each pattern includes implementation steps adapted for our Hive Mind architecture.

---

## Pattern 1: LLM Response Caching

### Source
Upstream commit: `17dbaf3`
File: `src/main/java/com/steve/ai/llm/async/LLMCache.java`

### Why Adopt This?
- Reduces API costs (20-40% reduction in duplicate queries)
- Faster responses for repeated tasks
- Already have Caffeine dependency

### Implementation

**Step 1: Create Cache Key**

```java
// File: src/main/java/com/minewright/llm/CacheKey.java
package com.minewright.llm;

import java.util.Objects;

public record CacheKey(
    String provider,
    String model,
    String systemPrompt,
    String userPrompt
) {
    public String toHashString() {
        return String.format("%s:%s:%s:%s",
            provider, model,
            systemPrompt.hashCode(),
            userPrompt.hashCode()
        );
    }
}
```

**Step 2: Implement LLMCache**

```java
// File: src/main/java/com/minewright/llm/LLMCache.java
package com.minewright.llm;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.minewright.testutil.TestLogger;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class LLMCache {
    private static final Logger LOGGER = TestLogger.getLogger(LLMCache.class);

    private final Cache<CacheKey, String> cache;

    public LLMCache() {
        this.cache = Caffeine.newBuilder()
            .maximumSize(10_000)  // Cache up to 10K responses
            .expireAfterWrite(Duration.ofHours(1))  // Expire after 1 hour
            .recordStats()  // Enable statistics
            .build();
    }

    public String get(CacheKey key) {
        String cached = cache.getIfPresent(key);
        if (cached != null) {
            LOGGER.debug("Cache HIT for {}", key.provider());
        } else {
            LOGGER.debug("Cache MISS for {}", key.provider());
        }
        return cached;
    }

    public void put(CacheKey key, String response) {
        cache.put(key, response);
        LOGGER.debug("Cached response for {}", key.provider());
    }

    public void invalidate() {
        cache.invalidateAll();
        LOGGER.info("Cache invalidated");
    }

    public CacheStats getStats() {
        var stats = cache.stats();
        return new CacheStats(
            stats.hitCount(),
            stats.missCount(),
            stats.hitRate()
        );
    }

    public record CacheStats(long hits, long misses, double hitRate) {}
}
```

**Step 3: Integrate into CascadeRouter**

```java
// File: src/main/java/com/minewright/llm/cascade/CascadeRouter.java
// Add to existing class:

private final LLMCache cache = new LLMCache();

public LLMResponse queryWithCache(LLMRequest request) throws LLMClientException {
    CacheKey cacheKey = new CacheKey(
        request.provider(),
        request.model(),
        request.systemPrompt(),
        request.userPrompt()
    );

    // Check cache first
    String cachedResponse = cache.get(cacheKey);
    if (cachedResponse != null) {
        return new LLMResponse(cachedResponse, true);
    }

    // Not in cache, query LLM
    LLMResponse response = query(request);

    // Cache the response
    cache.put(cacheKey, response.content());

    return response;
}

// Add method to get cache stats
public LLMCache.CacheStats getCacheStats() {
    return cache.getStats();
}
```

**Estimated Time:** 1-2 hours

---

## Pattern 2: Resilience Wrapper

### Source
Upstream commit: `17dbaf3`
File: `src/main/java/com/steve/ai/llm/resilience/ResilientLLMClient.java`

### Why Adopt This?
- Circuit breaker prevents cascading failures
- Rate limiting protects API quotas
- Better error handling for production

### Implementation

**Step 1: Add Resilience Configuration**

```java
// File: src/main/java/com/minewright/llm/resilience/ResilienceConfig.java
package com.minewright.llm.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.retry.RetryConfig;

import java.time.Duration;

public class ResilienceConfig {

    public static CircuitBreakerConfig circuitBreaker() {
        return CircuitBreakerConfig.custom()
            .slidingWindowSize(50)  // Last 50 calls
            .failureRateThreshold(50)  // Open if 50% fail
            .waitDurationInOpenState(Duration.ofSeconds(30))  // Wait 30s
            .permittedNumberOfCallsInHalfOpenState(5)  // Try 5 calls
            .build();
    }

    public static RetryConfig retry() {
        return RetryConfig.custom()
            .maxAttempts(3)
            .waitDuration(Duration.ofMillis(500))
            .retryExceptions(Exception.class)
            .build();
    }

    public static RateLimiterConfig rateLimiter() {
        return RateLimiterConfig.custom()
            .limitForPeriod(10)  // 10 requests
            .limitRefreshPeriod(Duration.ofSeconds(1))  // per second
            .timeoutDuration(Duration.ofSeconds(5))  // max wait
            .build();
    }
}
```

**Step 2: Create Resilient Wrapper**

```java
// File: src/main/java/com/minewright/llm/resilience/ResilientLLMClient.java
package com.minewright.llm.resilience;

import com.minewright.llm.LLMClient;
import com.minewright.llm.LLMRequest;
import com.minewright.llm.LLMResponse;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.core.IntervalFunction;
import io.vavr.control.Try;

public class ResilientLLMClient implements LLMClient {

    private final LLMClient delegate;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;
    private final RateLimiter rateLimiter;

    public ResilientLLMClient(LLMClient delegate) {
        this.delegate = delegate;
        this.circuitBreaker = CircuitBreaker.of(
            delegate.getClass().getSimpleName(),
            ResilienceConfig.circuitBreaker()
        );
        this.retry = Retry.of(
            delegate.getClass().getSimpleName(),
            ResilienceConfig.retry()
        );
        this.rateLimiter = RateLimiter.of(
            delegate.getClass().getSimpleName(),
            ResilienceConfig.rateLimiter()
        );
    }

    @Override
    public LLMResponse query(LLMRequest request) throws Exception {
        return Try.of(() -> delegate.query(request))
            .mapRetry(throwable -> {
                throw throwable;  // Let retry handle it
            })
            .recover(throwable -> {
                throw new Exception("LLM call failed after retries", throwable);
            })
            .get();
    }

    public CircuitBreaker.State getCircuitBreakerState() {
        return circuitBreaker.getState();
    }
}
```

**Step 3: Wrap Existing Clients**

```java
// File: src/main/java/com/minewright/llm/cascade/CascadeRouter.java
// Update client creation:

private LLMClient createResilientClient(LLMClient baseClient) {
    return new ResilientLLMClient(baseClient);
}

// In constructor or initialization:
this.openAIClient = createResilientClient(new OpenAIClient());
this.groqClient = createResilientClient(new GroqClient());
// etc.
```

**Estimated Time:** 2-3 hours

---

## Pattern 3: SPI Plugin Discovery

### Source
Upstream commit: `bada991`
Files:
- `src/main/java/com/steve/ai/plugin/ActionPlugin.java`
- `src/main/java/com/steve/ai/plugin/PluginManager.java`
- `META-INF/services/com.steve.ai.plugin.ActionPlugin`

### Why Adopt This?
- Third-party developers can add actions without modifying core
- Automatic plugin discovery
- Standard Java pattern

### Implementation

**Step 1: Define Plugin Interface**

```java
// File: src/main/java/com/minewright/plugin/ActionPlugin.java
package com.minewright.plugin;

import com.minewright.action.Action;
import com.minewright.action.ActionRegistry;
import com.minewright.entity.SteveEntity;

/**
 * Plugin interface for actions.
 * Implementations are discovered via Java SPI.
 */
public interface ActionPlugin {

    /**
     * Called when plugin is loaded.
     * Register actions with the registry.
     */
    void registerActions(ActionRegistry registry);

    /**
     * Plugin name for logging.
     */
    String getName();

    /**
     * Plugin version.
     */
    String getVersion();
}
```

**Step 2: Update PluginManager**

```java
// File: src/main/java/com/minewright/plugin/PluginManager.java
package com.minewright.plugin;

import com.minewright.testutil.TestLogger;
import org.slf4j.Logger;

import java.util.ServiceLoader;
import java.util.stream.StreamSupport;

public class PluginManager {
    private static final Logger LOGGER = TestLogger.getLogger(PluginManager.class);

    private final ActionRegistry registry;

    public PluginManager(ActionRegistry registry) {
        this.registry = registry;
    }

    public void loadPlugins() {
        LOGGER.info("Loading plugins via SPI...");

        ServiceLoader<ActionPlugin> loader = ServiceLoader.load(ActionPlugin.class);

        StreamSupport.stream(loader.spliterator(), false)
            .forEach(plugin -> {
                try {
                    LOGGER.info("Loading plugin: {} v{}",
                        plugin.getName(), plugin.getVersion());
                    plugin.registerActions(registry);
                    LOGGER.info("Plugin {} loaded successfully",
                        plugin.getName());
                } catch (Exception e) {
                    LOGGER.error("Failed to load plugin: {}",
                        plugin.getName(), e);
                }
            });

        LOGGER.info("Plugin loading complete");
    }
}
```

**Step 3: Create Core Actions Plugin**

```java
// File: src/main/java/com/minewright/plugin/CoreActionsPlugin.java
package com.minewright.plugin;

import com.minewright.action.Action;
import com.minewright.action.ActionRegistry;
import com.minewright.action.actions.*;
import com.minewright.entity.SteveEntity;
import com.minewright.action.Task;

public class CoreActionsPlugin implements ActionPlugin {

    @Override
    public void registerActions(ActionRegistry registry) {
        // Register all core actions
        registry.register("mine", (steve, task, ctx) ->
            new MineBlockAction(steve, task));
        registry.register("build", (steve, task, ctx) ->
            new BuildStructureAction(steve, task));
        registry.register("move", (steve, task, ctx) ->
            new MoveToAction(steve, task));
        registry.register("attack", (steve, task, ctx) ->
            new AttackAction(steve, task));
        registry.register("follow", (steve, task, ctx) ->
            new FollowAction(steve, task));
        // ... etc
    }

    @Override
    public String getName() {
        return "CoreActions";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }
}
```

**Step 4: Create SPI Configuration File**

```bash
# Create directory: src/main/resources/META-INF/services

# Create file: src/main/resources/META-INF/services/com.minewright.plugin.ActionPlugin
# Content:
com.minewright.plugin.CoreActionsPlugin
```

**Step 5: Update Main Mod Initialization**

```java
// File: src/main/java/com/minewright/MineWrightMod.java
@Mod("minewright")
public class MineWrightMod {
    public MineWrightMod() {
        // Existing initialization...

        // Load plugins
        ActionRegistry registry = new ActionRegistry();
        PluginManager pluginManager = new PluginManager(registry);
        pluginManager.loadPlugins();
    }
}
```

**Estimated Time:** 1-2 hours

---

## Testing the Changes

After implementing each pattern, add tests:

```java
// Test LLMCache
@Test
void testCacheHit() {
    LLMCache cache = new LLMCache();
    CacheKey key = new CacheKey("openai", "gpt-4", "sys", "user");
    cache.put(key, "response");
    assertEquals("response", cache.get(key));
}

// Test Resilience
@Test
void testCircuitBreaker() {
    LLMClient mockClient = createFailingClient();
    ResilientLLMClient resilient = new ResilientLLMClient(mockClient);

    // Trigger failures
    assertThrows(Exception.class, () -> resilient.query(request));

    // Check circuit state
    assertEquals(CircuitBreaker.State.OPEN,
        resilient.getCircuitBreakerState());
}

// Test SPI
@Test
void testPluginDiscovery() {
    ActionRegistry registry = new ActionRegistry();
    PluginManager manager = new PluginManager(registry);
    manager.loadPlugins();

    assertTrue(registry.hasAction("mine"));
    assertTrue(registry.hasAction("build"));
}
```

---

## Rollout Plan

### Week 1: Foundation
- [ ] Implement LLMCache
- [ ] Add tests for cache
- [ ] Integrate into CascadeRouter
- [ ] Monitor cache hit rates

### Week 2: Resilience
- [ ] Implement ResilienceConfig
- [ ] Create ResilientLLMClient
- [ ] Wrap existing clients
- [ ] Add circuit breaker monitoring

### Week 3: Extensibility
- [ ] Define ActionPlugin interface
- [ ] Update PluginManager
- [ ] Create CoreActionsPlugin
- [ ] Add SPI configuration
- [ ] Test plugin loading

### Week 4: Documentation & Polish
- [ ] Update documentation
- [ ] Add metrics collection
- [ ] Performance testing
- [ ] Deploy to production

---

## Success Metrics

### Cache Effectiveness
- Hit rate target: >20%
- API cost reduction: >15%
- Response time improvement: >100ms for cached queries

### Resilience
- Circuit breaker activation: <5% of time
- Retry success rate: >30%
- Rate limiter effectiveness: Zero quota errors

### Extensibility
- Plugin load time: <100ms
- Zero core modifications for new actions
- Third-party plugins possible

---

## References

- Upstream repository: https://github.com/YuvDwi/Steve
- Caffeine cache: https://github.com/ben-manes/caffeine
- Resilience4j: https://resilience4j.readme.io/
- Java SPI: https://docs.oracle.com/javase/tutorial/ext/basics/spi.html

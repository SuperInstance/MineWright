# SmartCascadeRouter Usage Guide

## Overview

The `SmartCascadeRouter` is an intelligent LLM request router that prioritizes FREE local models while maintaining fallback to cloud models when needed. It implements a sophisticated cascading routing strategy based on task complexity and vision requirements.

## Key Features

1. **FREE Local Processing**: Uses local SmolVLM (localhost:8000) as first priority
2. **Intelligent Complexity Assessment**: Automatically determines if a task needs cloud processing
3. **Vision Support**: Handles screenshots and image analysis with local vision models
4. **Smart Fallback Chain**: Gracefully degrades through cloud models on failures
5. **Cost Optimization**: Maximizes local processing to minimize API costs

## Model Hierarchy

```
┌─────────────────────────────────────────────────────────────────┐
│                    PRIORITY ORDER (Highest to Lowest)           │
├─────────────────────────────────────────────────────────────────┤
│ 1. Local SmolVLM (localhost:8000)      │ FREE      │ ~50-200ms  │
│    - Has vision capabilities                                           │
│    - Used for TRIVIAL and SIMPLE tasks                                │
│                                                                       │
│ 2. glm-4.7-flashx (z.ai)                │ ~$0.00001 │ ~100-300ms │
│    - Fastest cloud model                                            │
│    - Used for MODERATE tasks when local unavailable                  │
│                                                                       │
│ 3. glm-4.7-flash (z.ai)                 │ ~$0.00020 │ ~300-800ms │
│    - Balanced speed/quality                                        │
│    - Fallback when flashx fails                                     │
│                                                                       │
│ 4. GLM-5 (z.ai)                         │ ~$0.01000 │ ~1000-3000ms│
│    - Best quality                                                     │
│    - Used for COMPLEX tasks                                          │
│                                                                       │
│ 5. glm-4.6v (z.ai)                     │ ~$0.00150 │ ~800-1500ms │
│    - Vision model for complex image analysis                          │
│    - Fallback when local vision fails                                │
└─────────────────────────────────────────────────────────────────┘
```

## Complexity Levels

### TRIVIAL
- **Examples**: "follow me", "stop", "wait", "hello"
- **Routing**: Local only (never uses cloud)
- **Expected Success Rate**: 95%+

### SIMPLE
- **Examples**: "mine 10 stone", "go to x,y,z", "place torch"
- **Routing**: Local model preferred, flashx fallback
- **Expected Success Rate**: 85%+

### MODERATE
- **Examples**: "build a small house", "gather resources for crafting"
- **Routing**: Try local first, fallback to flashx -> flash -> GLM-5
- **Expected Success Rate**: 70%+

### COMPLEX
- **Examples**: "coordinate crew to build castle", "setup automated farm", "debug redstone"
- **Routing**: Direct to GLM-5, fallback to flash
- **Expected Success Rate**: 90%+

## Usage Examples

### Basic Text Processing

```java
// Create router instance
SmartCascadeRouter router = new SmartCascadeRouter();

// Process a simple command
String systemPrompt = "You are Steve, a Minecraft AI assistant.";
String userMessage = "mine 10 stone and craft a furnace";

String response = router.processWithCascade(systemPrompt, userMessage);
System.out.println(response);
```

### Vision Processing with Screenshot

```java
// Process a vision request with screenshot
String systemPrompt = "You are Steve. Analyze the screenshot and respond.";
String userMessage = "What do you see in front of you?";
String imageBase64 = captureScreenshotAsBase64(); // Your screenshot capture method

String response = router.processVisionRequest(systemPrompt, userMessage, imageBase64);
System.out.println(response);
```

### Async Processing

```java
// For non-blocking processing
router.processWithCascadeAsync(systemPrompt, userMessage)
    .thenAccept(response -> {
        // Handle response asynchronously
        System.out.println("Response: " + response);
    })
    .exceptionally(throwable -> {
        // Handle errors
        System.err.println("Error: " + throwable.getMessage());
        return null;
    });
```

## Monitoring and Metrics

### Check Router Statistics

```java
// Log current statistics
router.logStats();

// Output example:
// === Smart Cascade Router Statistics ===
// Total Requests: 150
// Local Hits: 120 (80.0%)
// Cloud Requests: 30
// Vision Requests: 15
// Estimated Cost: $0.03
// Model Usage:
//   smolvlm: 100 requests
//   llama3.2: 50 requests
//   glm-4.7-flashx: 25 requests
//   glm-4.7-flash: 5 requests
```

### Get Specific Metrics

```java
long totalRequests = router.getTotalRequests();
long localHits = router.getLocalHits();
long cloudRequests = router.getCloudRequests();
double localHitRate = router.getLocalHitRate();

System.out.printf("Local hit rate: %.1f%%%n", localHitRate * 100);
```

### Reset Metrics

```java
// Reset all metrics (e.g., between test runs)
router.resetMetrics();
```

## Configuration

### Local Model Setup

To use the free local SmolVLM model:

1. **Install vLLM**:
   ```bash
   pip install vllm
   ```

2. **Start SmolVLM server**:
   ```bash
   vllm serve smolvlm-256m-instruct --port 8000
   ```

   Or for a more capable model:
   ```bash
   vllm serve smolvlm-500m-instruct --port 8000
   ```

3. **Verify availability**:
   ```bash
   curl http://localhost:8000/v1/models
   ```

### Cloud Model Configuration

Set your z.ai API key in the config file:

```toml
# config/steve-common.toml
[openai]
apiKey = "your-zai-api-key-here"
```

## Advanced Features

### Custom Complexity Assessment

The router uses both local model assessment and heuristic analysis:

```java
// Local model preprocessing (when available)
PreprocessResult result = router.preprocessWithLocal(message);

// Heuristic fallback (when local unavailable)
PreprocessResult result = router.heuristicPreprocess(message);
```

### Failure Tracking

The router automatically tracks model failures and implements skip logic:

- **MAX_FAILURES_BEFORE_SKIP**: 3 consecutive failures
- **FAILURE_RESET_MS**: 60000ms (1 minute)
- After skip period, model is retried

### Vision Request Flow

```
1. Check if image data present
   ↓
2. Try local SmolVLM (FREE)
   ↓ (if fails)
3. Fallback to glm-4.6v (cloud vision)
   ↓ (if fails)
4. Process as text-only with note about vision unavailability
```

## Best Practices

1. **Always check local model availability** before production deployment
2. **Monitor local hit rate** - should be >70% for cost efficiency
3. **Use async processing** for non-blocking operations
4. **Handle vision failures gracefully** - not all tasks require vision
5. **Log statistics regularly** to track cost optimization

## Troubleshooting

### Local Model Not Available

```
[SmartCascade] Local SmolVLM: NOT AVAILABLE
```

**Solution**: Start your local vLLM server:
```bash
vllm serve smolvlm-256m-instruct --port 8000
```

### All Models Failing

```
[SmartCascade] All models failed
```

**Solution**: Check API key configuration and network connectivity.

### Vision Requests Failing

```
[SmartCascade] Local vision failed: Model doesn't support vision
```

**Solution**: Ensure your local model has vision capabilities (smolvlm) or use cloud vision fallback.

## Performance Optimization

1. **Warm up local model**: Send a few test requests before production use
2. **Batch similar requests**: The router caches preprocessing results
3. **Adjust timeout settings**: Modify TIMEOUT_SECONDS for your network conditions
4. **Monitor latency**: Use logStats() to identify bottlenecks

## Migration from GLMCascadeRouter

To migrate from the old GLMCascadeRouter:

```java
// Old code
GLMCascadeRouter oldRouter = new GLMCascadeRouter();
String response = oldRouter.processWithCascade(systemPrompt, userMessage);

// New code (drop-in replacement)
SmartCascadeRouter newRouter = new SmartCascadeRouter();
String response = newRouter.processWithCascade(systemPrompt, userMessage);
```

The new router is fully backward compatible but adds:
- FREE local processing
- Better complexity assessment
- Improved vision support
- Enhanced metrics tracking

## License

Same as parent project (Steve AI / MineWright)

## Contributing

When adding new features to SmartCascadeRouter:
1. Maintain the priority: Local → Cloud
2. Update complexity assessment logic
3. Add appropriate metrics tracking
4. Test fallback chains thoroughly
5. Document new complexity levels

---

**Last Updated**: 2025-02-27
**Version**: 1.4.0
**Status**: Production Ready

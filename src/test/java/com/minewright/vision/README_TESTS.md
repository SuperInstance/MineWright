# Vision Integration Test Suite

This directory contains comprehensive test coverage for the vision integration components of the Steve AI Minecraft mod. These tests are based on the research documented in `src/main/java/com/minewright/research/`.

## Test Files

### VisionResponseParserTest.java
Tests parsing of vision model API responses in OpenAI-compatible format.
- Content extraction from valid responses
- Usage statistics parsing
- Truncation detection
- Error classification and handling
- Edge cases (empty responses, special characters, nested JSON)

**Test Count:** 40+ tests

### ImageEncoderTest.java
Tests image encoding to base64 with proper MIME type detection.
- PNG, JPEG, WEBP format encoding
- MIME type detection from file extensions
- Raw base64 encoding (without prefix)
- Streaming encoding for large images
- Error handling (corrupted files, empty files)

**Test Count:** 30+ tests

### VisionRequestBuilderTest.java
Tests building vision API request bodies for multimodal LLM requests.
- Basic request building with text and images
- Multi-image comparison requests
- System prompt integration
- Minecraft-specific vision prompts (OCR, biome analysis, threat detection, etc.)
- JSON serialization validation

**Test Count:** 35+ tests

### VisionCacheTest.java
Tests caching mechanism for vision analysis results.
- Cache storage and retrieval
- Cache key generation and consistency
- Expiration and cleanup
- Concurrent access handling
- Performance testing (1000+ operations)

**Test Count:** 40+ tests

### StructuredVisionParserTest.java
Tests parsing of structured JSON responses from vision models.
- JSON extraction from markdown code blocks
- Biome analysis parsing
- Nested JSON structures
- Type conversions (string, number, boolean, array, object)
- Real-world Minecraft response formats

**Test Count:** 30+ tests

### VisionOptimizationTest.java
Tests optimization strategies for vision API performance.
- Resolution suggestions (LOW_RES, MEDIUM_RES, HIGH_RES)
- Detail level recommendations (low, auto, high)
- Task-specific optimizations (OCR, biome ID, build verification, etc.)
- Performance implications (token usage, processing speed)
- Aspect ratio maintenance

**Test Count:** 30+ tests

## Running Tests

### Run all vision tests:
```bash
./gradlew test --tests com.minewright.vision.*
```

### Run specific test class:
```bash
./gradlew test --tests VisionResponseParserTest
```

### Run specific test method:
```bash
./gradlew test --tests VisionResponseParserTest.testExtractContent_ValidResponse
```

## Test Coverage

The test suite provides comprehensive coverage of:
- Basic functionality (happy path)
- Edge cases (null inputs, empty strings, special characters)
- Error handling (invalid JSON, corrupted images, API errors)
- Concurrent access (multi-threaded operations)
- Performance (large images, high-volume operations)
- Integration scenarios (real Minecraft analysis workflows)

## Dependencies

These tests use:
- JUnit 5 for testing framework
- Gson for JSON parsing
- Java standard library for image I/O
- TempDir for temporary file management

## Research Basis

These tests are based on the research documented in:
- `src/main/java/com/minewright/research/SmolVLM_Research.md`
- `src/main/java/com/minewright/research/Vision_Integration_Guide.md`

The research documents provide:
- SmolVLM vision model specifications
- OpenAI-compatible API format
- Minecraft-specific use cases
- Performance benchmarks
- Best practices for vision integration

## Vision Components Under Test

The following components (documented in the research guides) are tested:

1. **VisionResponseParser** - Parse vision model API responses
2. **ImageEncoder** - Encode images to base64 data URLs
3. **VisionRequestBuilder** - Build multimodal API requests
4. **VisionCache** - Cache vision analysis results
5. **StructuredVisionParser** - Parse structured JSON outputs
6. **VisionOptimization** - Optimize resolution and detail settings

## Vision Tasks Supported

The test suite covers these Minecraft vision tasks:
- **OCR** - Reading text from signs, books, chat
- **Biome Identification** - Detecting biome types
- **Resource Scanning** - Finding ores, trees, items
- **Threat Detection** - Spotting hostile mobs, dangers
- **Build Verification** - Comparing builds to blueprints
- **General Analysis** - Overall scene understanding

## Integration Notes

These tests are designed to work with:
- Local vision models (SmolVLM via vLLM)
- Cloud vision APIs (GLM-4.6v, GPT-4V)
- Hybrid routing strategies

## Test Data

Tests use:
- Sample base64-encoded images
- Mock API responses
- Temporary test files
- Synthetic vision model outputs

## Future Enhancements

Potential additions:
- Integration tests with actual vision API
- Performance benchmarks on target hardware
- Memory usage profiling
- Real Minecraft screenshot tests
- Model accuracy validation

---

**Test Suite Version:** 1.0
**Last Updated:** 2026-03-04
**Based On:** SmolVLM Research & Vision Integration Guide

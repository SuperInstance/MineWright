# Voice System Architecture

**Project:** MineWright AI - Minecraft Forge Mod
**Document Version:** 1.0
**Last Updated:** 2025-02-27
**Status:** Architecture Analysis Complete

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [Current Voice System Analysis](#current-voice-system-analysis)
3. [Architecture Overview](#architecture-overview)
4. [TTS/STT Integration Patterns](#ttstt-integration-patterns)
5. [Audio Pipeline Design](#audio-pipeline-design)
6. [Latency Optimization Strategies](#latency-optimization-strategies)
7. [Error Handling and Fallbacks](#error-handling-and-fallbacks)
8. [Platform-Specific Considerations](#platform-specific-considerations)
9. [Implementation Roadmap](#implementation-roadmap)
10. [Code Examples](#code-examples)
11. [Testing Strategy](#testing-strategy)
12. [Security and Privacy](#security-and-privacy)
13. [Cost Analysis](#cost-analysis)
14. [References](#references)

---

## Executive Summary

The MineWright voice system provides a clean, extensible architecture for integrating Text-to-Speech (TTS) and Speech-to-Text (STT) functionality into the Minecraft Forge mod. The current implementation provides a well-designed interface structure with logging and disabled fallback modes, but requires implementation of actual TTS/STT providers.

### Key Findings

| Aspect | Status | Priority |
|--------|--------|----------|
| **Interface Design** | Excellent - Clean separation of concerns | - |
| **Configuration System** | Complete - Forge config integration | - |
| **STT Implementation** | Logging stub only - Needs real implementation | High |
| **TTS Implementation** | Logging stub only - Needs real implementation | High |
| **Audio Capture** | Not implemented | High |
| **Voice Activity Detection** | Not implemented | Medium |
| **Positional Audio** | Not implemented | Medium |

### Recommended Tech Stack

| Component | Recommendation | Latency | Cost |
|-----------|---------------|---------|------|
| **STT (Primary)** | OpenAI Whisper API | 200-800ms | $0.006/10min |
| **STT (Offline)** | whisper.cpp via JNI | 50-200ms | Free (local) |
| **TTS (Primary)** | OpenAI TTS API | 200-300ms | $15/1M chars |
| **TTS (Premium)** | ElevenLabs API | 300-500ms | $5-50/1M chars |
| **Audio Capture** | Java Sound API | <10ms | Free (built-in) |
| **Voice Activation** | Silero VAD | <5ms | Free (ONNX) |

---

## Current Voice System Analysis

### Architecture Strengths

The current voice system demonstrates excellent software engineering practices:

1. **Clean Interface Segregation**: Separate interfaces for STT and TTS
2. **Strategy Pattern**: Multiple voice system implementations (Logging, Disabled)
3. **Configuration-Driven**: Forge config integration for all settings
4. **Singleton Pattern**: VoiceManager provides global access
5. **Async Operations**: CompletableFuture for non-blocking operations
6. **Proper Error Handling**: Custom VoiceException with factory methods

### Package Structure

```
src/main/java/com/minewright/voice/
├── VoiceManager.java              # Singleton facade
├── VoiceSystem.java               # Main interface
├── VoiceConfig.java               # Configuration holder
├── VoiceException.java            # Custom exception
├── TextToSpeech.java              # TTS interface
├── SpeechToText.java              # STT interface
├── LoggingVoiceSystem.java        # Testing implementation
└── DisabledVoiceSystem.java       # No-op implementation
```

### Configuration System

**File:** `C:\Users\casey\steve\src\main\java\com\minewright\config\MineWrightConfig.java`

```java
// Voice Configuration (Lines 76-122)
public static final ForgeConfigSpec.BooleanValue VOICE_ENABLED;
public static final ForgeConfigSpec.ConfigValue<String> VOICE_MODE;
public static final ForgeConfigSpec.ConfigValue<String> VOICE_STT_LANGUAGE;
public static final ForgeConfigSpec.ConfigValue<String> VOICE_TTS_VOICE;
public static final ForgeConfigSpec.DoubleValue VOICE_TTS_VOLUME;
public static final ForgeConfigSpec.DoubleValue VOICE_TTS_RATE;
public static final ForgeConfigSpec.DoubleValue VOICE_TTS_PITCH;
public static final ForgeConfigSpec.DoubleValue VOICE_STT_SENSITIVITY;
public static final ForgeConfigSpec.BooleanValue VOICE_PUSH_TO_TALK;
public static final ForgeConfigSpec.IntValue VOICE_LISTENING_TIMEOUT;
public static final ForgeConfigSpec.BooleanValue VOICE_DEBUG_LOGGING;
```

### Current Implementation Status

#### VoiceManager (Singleton Facade)

**Purpose:** Central coordinator for all voice operations

**Key Methods:**
- `getInstance()` - Singleton access
- `initialize()` - System initialization
- `isEnabled()` - Check if voice is enabled
- `listenForCommand()` - Start STT (returns CompletableFuture<String>)
- `speak(String text)` - Start TTS
- `stopAll()` - Stop all voice operations
- `test()` - System health check

**Design Quality:** Excellent - Proper encapsulation with clean API

#### VoiceSystem Interface

**Purpose:** Abstraction for different voice system implementations

**Key Methods:**
- `initialize()` - Initialize the voice system
- `startListening()` - Begin capturing audio
- `speak(String text)` - Synthesize speech
- `stopListening()` / `stopSpeaking()` - Cancel operations
- `getSpeechToText()` / `getTextToSpeech()` - Access subsystems
- `test()` - Health check returning VoiceTestResult

**Implementations:**
1. **LoggingVoiceSystem** - Logs operations instead of real audio (testing)
2. **DisabledVoiceSystem** - No-op implementation (when voice disabled)

**Design Quality:** Excellent - Clean interface with clear contracts

#### TextToSpeech Interface

**Purpose:** Abstraction for TTS providers

**Configuration Options:**
- Voice selection (name, displayName, language, gender)
- Speech rate (0.5-2.0 multiplier)
- Pitch adjustment (0.5-2.0 multiplier)
- Volume (0.0-1.0)

**Key Methods:**
- `speak(String text)` - Immediate speech (interrupts current)
- `speakQueued(String text)` - Add to queue
- `speakAsync(String text)` - Returns CompletableFuture<Void>
- `getAvailableVoices()` - List available voices
- `setVoice(Voice)` / `setVoice(String)` - Select voice

**Design Quality:** Excellent - Supports both immediate and queued speech

#### SpeechToText Interface

**Purpose:** Abstraction for STT providers

**Configuration Options:**
- Language/locale (e.g., "en-US", "en-GB")
- Sensitivity threshold (0.0-1.0)

**Key Methods:**
- `startListening(Consumer<String>)` - Continuous listening with callback
- `listenOnce()` - One-shot transcription (returns CompletableFuture<String>)
- `getPreferredAudioFormat()` - Audio format requirements
- `cancel()` - Cancel current operation

**Design Quality:** Excellent - Supports both continuous and one-shot modes

### Current Gaps

1. **No Audio Capture**: Missing microphone capture implementation
2. **No Real TTS**: Only logging stub exists
3. **No Real STT**: Only logging stub exists
4. **No VAD**: Voice activity detection not implemented
5. **No Positional Audio**: TTS playback not integrated with Minecraft sound system
6. **No Key Bindings**: Push-to-talk not integrated with client controls

---

## Architecture Overview

### System Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                         CLIENT (Minecraft)                      │
│                                                                  │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐      │
│  │   Audio      │───▶│     VAD      │───▶│     STT      │      │
│  │   Capture    │    │ (Silero)     │    │ (Whisper)    │      │
│  └──────────────┘    └──────────────┘    └──────────────┘      │
│       │                                      │                  │
│       │        Push-to-Talk Key (V)         │                  │
│       │                 ▼                   ▼                  │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐      │
│  │    Key       │    │  Voice       │    │  Transcribed │      │
│  │  Bindings    │    │  Manager     │    │     Text     │      │
│  └──────────────┘    └──────────────┘    └──────────────┘      │
│                                                    │            │
│                                                    ▼            │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐      │
│  │    Audio     │◀───│     TTS      │◀───│     LLM      │      │
│  │   Playback   │    │ (OpenAI)     │    │ (Existing)   │      │
│  └──────────────┘    └──────────────┘    └──────────────┘      │
│         │                                                        │
│         ▼                                                        │
│  ┌──────────────┐                                               │
│  │  Minecraft   │                                               │
│  │ Sound System │                                               │
│  │ (Positional) │                                               │
│  └──────────────┘                                               │
└─────────────────────────────────────────────────────────────────┘
                            │
                            │ HTTPS (Optional)
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                      CLOUD API (Optional)                       │
│                                                                  │
│  ┌──────────────┐              ┌──────────────┐                │
│  │  Whisper API │              │  TTS API     │                │
│  │  (OpenAI)    │              │  (OpenAI/    │                │
│  │              │              │   ElevenLabs)│                │
│  └──────────────┘              └──────────────┘                │
└─────────────────────────────────────────────────────────────────┘
```

### Data Flow Diagrams

#### Player Speaking to MineWright

```
1. Player presses PTT key (V)
   ↓
2. KeyBindingsHandler.onKeyPress()
   ↓
3. VoiceManager.listenForCommand()
   ↓
4. VoiceSystem.startListening()
   ↓
5. AudioCapture.startCapture()
   ↓
6. AudioCapture.captureChunk() [continuous 30ms chunks]
   ↓
7. VoiceActivationDetector.isSpeech() [optional]
   ↓
8. SpeechToText.startListening()
   ↓
9. Transcribed text returned via CompletableFuture
   ↓
10. ClientEventHandler.onTranscriptionReceived()
   ↓
11. Route to ActionExecutor.processNaturalLanguageCommand()
   ↓
12. LLM generates response
   ↓
13. Response sent to VoiceManager.speak()
```

#### MineWright Speaking to Player

```
1. LLM generates response text
   ↓
2. ForemanEntity.generateResponse()
   ↓
3. VoiceManager.speak(responseText)
   ↓
4. VoiceSystem.speak(text)
   ↓
5. TextToSpeech.speak(text)
   ↓
6. TTS API call (OpenAI/ElevenLabs)
   ↓
7. Receive audio bytes (MP3/OGG)
   ↓
8. Convert to Minecraft-compatible format
   ↓
9. MinecraftSoundSystem.playSound()
   ↓
10. Positional audio at entity location
   ↓
11. Player hears from correct direction
```

### Component Interactions

```
┌──────────────────────────────────────────────────────────────┐
│                     VoiceManager                              │
│  ┌────────────────────────────────────────────────────────┐  │
│  │  Responsibilities:                                      │  │
│  │  - Singleton access point                              │  │
│  │  - Voice system lifecycle (init/shutdown)              │  │
│  │  - Configuration management                            │  │
│  │  - High-level API (speak, listen)                      │  │
│  └────────────────────────────────────────────────────────┘  │
│                              │                                │
│           ┌──────────────────┼──────────────────┐            │
│           ▼                  ▼                  ▼            │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐       │
│  │  VoiceSystem │  │  VoiceConfig │  │VoiceException│       │
│  │  (interface) │  │              │  │              │       │
│  └──────────────┘  └──────────────┘  └──────────────┘       │
│                              │                                │
│           ┌──────────────────┴──────────────────┐            │
│           ▼                                     ▼            │
│  ┌──────────────────────────┐      ┌──────────────────────┐  │
│  │     TextToSpeech         │      │    SpeechToText      │  │
│  │  ┌────────────────────┐  │      │  ┌────────────────┐  │  │
│  │  │ OpenAITTS         │  │      │  │ WhisperSTT      │  │  │
│  │  │ ElevenLabsTTS     │  │      │  │ LocalWhisperSTT │  │  │
│  │  │ LocalTTS          │  │      │  │ GoogleCloudSTT  │  │  │
│  │  └────────────────────┘  │      │  └────────────────┘  │  │
│  └──────────────────────────┘      └──────────────────────┘  │
└──────────────────────────────────────────────────────────────┘
```

---

## TTS/STT Integration Patterns

### Provider Selection Strategy

The voice system uses a strategy pattern for TTS/STT providers:

```java
// Factory method in VoiceManager
private VoiceSystem createVoiceSystem() {
    String mode = MineWrightConfig.VOICE_MODE.get();

    return switch (mode.toLowerCase()) {
        case "logging" -> new LoggingVoiceSystem();
        case "real" -> createRealVoiceSystem();  // TODO: Implement
        case "disabled", "off", "false" -> new DisabledVoiceSystem();
        default -> new DisabledVoiceSystem();
    };
}
```

### Recommended Provider Implementations

#### 1. OpenAI Whisper STT

**Maven Dependency:**
```xml
<dependency>
    <groupId>com.squareup.okhttp3</groupId>
    <artifactId>okhttp</artifactId>
    <version>4.12.0</version>
</dependency>
```

**Implementation Sketch:**
```java
package com.minewright.voice.stt;

import com.minewright.voice.SpeechToText;
import com.minewright.voice.VoiceException;
import okhttp3.*;

import javax.sound.sampled.AudioFormat;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class WhisperSTT implements SpeechToText {
    private static final String API_URL = "https://api.openai.com/v1/audio/transcriptions";
    private final OkHttpClient client;
    private final String apiKey;
    private String language = "en-US";

    public WhisperSTT(String apiKey) {
        this.client = new OkHttpClient();
        this.apiKey = apiKey;
    }

    @Override
    public void initialize() throws VoiceException {
        // Validate API key
        if (apiKey == null || apiKey.isBlank()) {
            throw VoiceException.configurationError("OpenAI API key is required");
        }
    }

    @Override
    public void startListening(Consumer<String> resultConsumer) throws VoiceException {
        // For Whisper, we typically use listenOnce() and then call it again
        // This is a wrapper that provides continuous listening
        CompletableFuture.runAsync(() -> {
            while (true) {  // Use a flag for actual control
                try {
                    String result = listenOnce().get();
                    resultConsumer.accept(result);
                } catch (Exception e) {
                    if (e instanceof InterruptedException) break;
                    // Log error and continue
                }
            }
        });
    }

    @Override
    public CompletableFuture<String> listenOnce() throws VoiceException {
        // This would be called with actual audio data
        // For now, return a failed future as audio capture is not integrated
        return CompletableFuture.failedFuture(
            VoiceException.audioCaptureFailed("Audio capture not integrated", null)
        );
    }

    // Actual API call implementation
    private String transcribe(byte[] audioData) throws Exception {
        RequestBody requestBody = new MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("model", "whisper-1")
            .addFormDataPart("file", "audio.wav",
                RequestBody.create(audioData, MediaType.parse("audio/wav")))
            .addFormDataPart("language", language.split("-")[0])  // "en" from "en-US"
            .build();

        Request request = new Request.Builder()
            .url(API_URL)
            .header("Authorization", "Bearer " + apiKey)
            .post(requestBody)
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw VoiceException.transcriptionFailed(
                    "API returned " + response.code(), null
                );
            }

            // Parse JSON response
            JsonObject json = JsonParser.parseString(
                response.body().string()
            ).getAsJsonObject();

            return json.get("text").getAsString();
        }
    }

    @Override
    public AudioFormat getPreferredAudioFormat() {
        // Whisper prefers 16kHz, 16-bit, mono PCM
        return new AudioFormat(16000, 16, 1, true, false);
    }

    @Override
    public String getLanguage() {
        return language;
    }

    @Override
    public void setLanguage(String language) {
        this.language = language;
    }

    @Override
    public double getSensitivity() {
        return 0.5;  // Not applicable for Whisper API
    }

    @Override
    public void setSensitivity(double sensitivity) {
        // Not applicable for Whisper API
    }

    @Override
    public void stopListening() {
        // Cancel any ongoing operations
    }

    @Override
    public boolean isListening() {
        return false;
    }

    @Override
    public void cancel() {
        stopListening();
    }

    @Override
    public void shutdown() {
        // Cleanup
    }
}
```

#### 2. OpenAI TTS

**Implementation Sketch:**
```java
package com.minewright.voice.tts;

import com.minewright.voice.TextToSpeech;
import com.minewright.voice.VoiceException;
import okhttp3.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class OpenAITTS implements TextToSpeech {
    private static final String API_URL = "https://api.openai.com/v1/audio/speech";
    private final OkHttpClient client;
    private final String apiKey;
    private Voice currentVoice = Voice.of("nova", "Nova", "en-US", "female");
    private double volume = 1.0;
    private double rate = 1.0;
    private double pitch = 1.0;

    public OpenAITTS(String apiKey) {
        this.client = new OkHttpClient();
        this.apiKey = apiKey;
    }

    @Override
    public void initialize() throws VoiceException {
        if (apiKey == null || apiKey.isBlank()) {
            throw VoiceException.configurationError("OpenAI API key is required");
        }
    }

    @Override
    public void speak(String text) throws VoiceException {
        byte[] audio = synthesize(text);
        playAudio(audio);
    }

    @Override
    public CompletableFuture<Void> speakAsync(String text) throws VoiceException {
        return CompletableFuture.runAsync(() -> {
            try {
                speak(text);
            } catch (VoiceException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private byte[] synthesize(String text) throws VoiceException {
        try {
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("model", "tts-1");
            requestBody.addProperty("input", text);
            requestBody.addProperty("voice", currentVoice.name());

            Request request = new Request.Builder()
                .url(API_URL)
                .header("Authorization", "Bearer " + apiKey)
                .post(RequestBody.create(
                    requestBody.toString(),
                    MediaType.parse("application/json")
                ))
                .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw VoiceException.synthesisFailed(
                        "API returned " + response.code(), null
                    );
                }

                return response.body().bytes();
            }
        } catch (Exception e) {
            throw VoiceException.synthesisFailed("Synthesis failed", e);
        }
    }

    private void playAudio(byte[] audio) {
        // Integrate with Minecraft sound system
        // See Audio Pipeline Design section
    }

    @Override
    public List<Voice> getAvailableVoices() {
        return List.of(
            Voice.of("alloy", "Alloy", "en-US", "neutral"),
            Voice.of("echo", "Echo", "en-US", "male"),
            Voice.of("fable", "Fable", "en-US", "male"),
            Voice.of("onyx", "Onyx", "en-US", "male"),
            Voice.of("nova", "Nova", "en-US", "female"),
            Voice.of("shimmer", "Shimmer", "en-US", "female")
        );
    }

    @Override
    public Voice getCurrentVoice() {
        return currentVoice;
    }

    @Override
    public void setVoice(Voice voice) throws VoiceException {
        this.currentVoice = voice;
    }

    @Override
    public void setVoice(String voiceName) throws VoiceException {
        getAvailableVoices().stream()
            .filter(v -> v.name().equals(voiceName))
            .findFirst()
            .orElseThrow(() -> VoiceException.configurationError(
                "Voice not found: " + voiceName
            ));
    }

    @Override
    public double getRate() {
        return rate;
    }

    @Override
    public void setRate(double rate) {
        this.rate = Math.max(0.5, Math.min(2.0, rate));
    }

    @Override
    public double getPitch() {
        return pitch;
    }

    @Override
    public void setPitch(double pitch) {
        this.pitch = Math.max(0.5, Math.min(2.0, pitch));
    }

    @Override
    public double getVolume() {
        return volume;
    }

    @Override
    public void setVolume(double volume) {
        this.volume = Math.max(0.0, Math.min(1.0, volume));
    }

    @Override
    public void shutdown() {
        // Cleanup
    }

    // Other interface methods...
}
```

#### 3. Local Whisper STT (Offline)

**Maven Dependency:**
```xml
<dependency>
    <groupId>io.github.givimad</groupId>
    <artifactId>whisper-jni</artifactId>
    <version>1.7.1</version>
</dependency>
```

**Implementation Sketch:**
```java
package com.minewright.voice.stt;

import com.github.givimad.whisperjni.WhisperContext;
import com.minewright.voice.SpeechToText;
import com.minewright.voice.VoiceException;

import javax.sound.sampled.AudioFormat;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class LocalWhisperSTT implements SpeechToText {
    private WhisperContext context;
    private String modelPath = "models/whisper/small.ggml";
    private String language = "en";

    @Override
    public void initialize() throws VoiceException {
        try {
            context = new WhisperContext(modelPath);
        } catch (Exception e) {
            throw VoiceException.initializationFailed(
                "Failed to load Whisper model", e
            );
        }
    }

    @Override
    public CompletableFuture<String> listenOnce() throws VoiceException {
        return CompletableFuture.supplyAsync(() -> {
            try {
                byte[] audio = captureAudio();  // From AudioCapture
                // Ensure audio is 16kHz, 16-bit, mono PCM
                String result = context.transcribe(audio);
                return result;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public AudioFormat getPreferredAudioFormat() {
        return new AudioFormat(16000, 16, 1, true, false);
    }

    // Other interface methods...
}
```

---

## Audio Pipeline Design

### Audio Capture Pipeline

```
┌─────────────────────────────────────────────────────────────┐
│                    Audio Capture Pipeline                    │
│                                                              │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐  │
│  │  Microphone  │───▶│   Audio      │───▶│     VAD      │  │
│  │   Hardware   │    │   Buffer     │    │  (Optional)  │  │
│  └──────────────┘    └──────────────┘    └──────────────┘  │
│                             │                   │           │
│                             │                   ▼           │
│                             │            ┌──────────────┐   │
│                             │            │  Speech      │   │
│                             │            │  Detected?   │   │
│                             │            └──────────────┘   │
│                             │                   │           │
│                             │            Yes    │   No      │
│                             │                   ▼           │
│                             │            ┌──────────────┐   │
│                             └───────────▶│   Audio      │   │
│                                          │   Chunk      │   │
│                                          └──────────────┘   │
│                                                 │           │
│                                                 ▼           │
│                                          ┌──────────────┐   │
│                                          │     STT      │   │
│                                          │  Service     │   │
│                                          └──────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

### Java Sound API Audio Capture

**Implementation:**
```java
package com.minewright.voice.capture;

import javax.sound.sampled.*;
import java.io.ByteArrayOutputStream;

public class AudioCapture {
    private TargetDataLine microphone;
    private AudioFormat format;
    private boolean isRecording = false;

    // Whisper-compatible format
    private static final AudioFormat WHISPER_FORMAT = new AudioFormat(
        16000,  // Sample rate (16kHz)
        16,     // Sample size (16-bit)
        1,      // Channels (mono)
        true,   // Signed
        false   // Little-endian
    );

    public void startCapture() throws LineUnavailableException {
        DataLine.Info info = new DataLine.Info(
            TargetDataLine.class,
            WHISPER_FORMAT
        );

        microphone = (TargetDataLine) AudioSystem.getLine(info);
        microphone.open(WHISPER_FORMAT);
        microphone.start();
        isRecording = true;
    }

    public byte[] captureChunk(int durationMs) {
        int bufferSize = (int) (WHISPER_FORMAT.getSampleRate() *
            WHISPER_FORMAT.getFrameSize() * (durationMs / 1000.0));
        byte[] buffer = new byte[bufferSize];
        microphone.read(buffer, 0, buffer.length);
        return buffer;
    }

    public void stopCapture() {
        isRecording = false;
        if (microphone != null) {
            microphone.stop();
            microphone.close();
        }
    }

    public static void listMicrophones() {
        Mixer.Info[] mixers = AudioSystem.getMixerInfo();
        for (Mixer.Info mixer : mixers) {
            Mixer m = AudioSystem.getMixer(mixer);
            if (m.getTargetLineInfo().length > 0) {
                System.out.println("Microphone: " + mixer.getName());
            }
        }
    }
}
```

### TTS Audio Playback Pipeline

```
┌─────────────────────────────────────────────────────────────┐
│                      TTS Playback Pipeline                    │
│                                                              │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐  │
│  │   Text       │───▶│     TTS      │───▶│  Audio Data  │  │
│  │   Input      │    │   Provider   │    │   (MP3/OGG)  │  │
│  └──────────────┘    └──────────────┘    └──────────────┘  │
│                                                  │           │
│                                                  ▼           │
│                                         ┌──────────────┐    │
│                                         │   Format     │    │
│                                         │  Conversion  │    │
│                                         │ (to OGG/WAV) │    │
│                                         └──────────────┘    │
│                                                  │           │
│                                                  ▼           │
│                                         ┌──────────────┐    │
│                                         │  Minecraft   │    │
│                                         │  Sound       │    │
│                                         │  System      │    │
│                                         └──────────────┘    │
│                                                  │           │
│                                                  ▼           │
│                                         ┌──────────────┐    │
│                                         │  Positional  │    │
│                                         │  3D Audio    │    │
│                                         └──────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

### Minecraft Sound System Integration

**Custom Sound Event Registration:**
```java
package com.minewright.sound;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class MineWrightSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
        DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, "minewright");

    public static final RegistryObject<SoundEvent> MINESAFT_SPEAK = SOUNDS.register(
        "minesaft_speak",
        () -> new SoundEvent(new ResourceLocation("minewright", "minesaft_speak"))
    );

    public static final RegistryObject<SoundEvent> MINESAFT_ACKNOWLEDGE = SOUNDS.register(
        "minesaft_acknowledge",
        () -> new SoundEvent(new ResourceLocation("minewright", "minesaft_acknowledge"))
    );
}
```

**Sound Definition:**
```json
// src/main/resources/assets/minewright/sounds.json
{
  "minewright.minesaft_speak": {
    "sounds": [
      {
        "name": "minewright:tts_output",
        "stream": true
      }
    ]
  }
}
```

**Positional Audio Playback:**
```java
package com.minewright.sound;

import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.BlockPos;

public class PositionalAudioPlayer {
    public static void playSpeechAtEntity(
        Level level,
        Entity entity,
        byte[] audioData
    ) {
        // Convert audio to OGG format if needed
        byte[] oggAudio = convertToOGG(audioData);

        // Save to temp file in sounds directory
        Path tempSoundFile = saveTempSound(oggAudio);

        // Play at entity position
        level.playSound(
            null,  // Player (null = all players)
            entity.blockPosition(),
            MineWrightSounds.MINESAFT_SPEAK.get(),
            SoundSource.NEUTRAL,
            1.0f,  // Volume
            1.0f   // Pitch
        );
    }

    private static byte[] convertToOGG(byte[] audio) {
        // Use JOrbis or similar library
        // For now, assume TTS returns OGG
        return audio;
    }

    private static Path saveTempSound(byte[] audio) {
        try {
            Path soundsDir = Paths.get("sounds/minewright");
            Files.createDirectories(soundsDir);

            Path tempFile = soundsDir.resolve("tts_" +
                System.currentTimeMillis() + ".ogg");
            Files.write(tempFile, audio);

            return tempFile;
        } catch (IOException e) {
            throw new RuntimeException("Failed to save temp sound", e);
        }
    }
}
```

### Voice Activity Detection (VAD)

**Silero VAD Integration (Recommended):**
```java
package com.minewright.voice.vad;

import ai.onnxruntime.*;
import com.minewright.voice.VoiceException;

import java.nio.FloatBuffer;
import java.util.Arrays;

public class SileroVAD {
    private OrtSession session;
    private OrtEnvironment env;
    private static final int SAMPLE_RATE = 16000;
    private static final int CHUNK_SIZE = 512;  // 32ms chunks

    public SileroVAD(String modelPath) throws VoiceException {
        try {
            env = OrtEnvironment.getEnvironment();
            OrtSession.SessionOptions options = new OrtSession.SessionOptions();
            session = env.createSession(modelPath, options);
        } catch (OrtException e) {
            throw VoiceException.initializationFailed(
                "Failed to load Silero VAD model", e
            );
        }
    }

    public boolean isSpeech(byte[] audioChunk) {
        try {
            // Convert bytes to float array
            float[] audio = bytesToFloats(audioChunk);

            // Create input tensor
            OnnxTensor inputTensor = OnnxTensor.createTensor(
                env,
                FloatBuffer.wrap(audio),
                new long[]{1, audio.length}
            );

            // Run inference
            OrtSession.Result result = session.run(
                Map.of("input", inputTensor)
            );

            // Get output (speech probability)
            float[][] output = (float[][]) result.get(0).getValue();
            float probability = output[0][0];

            result.close();
            inputTensor.close();

            return probability > 0.5;  // Threshold

        } catch (OrtException e) {
            return false;
        }
    }

    private float[] bytesToFloats(byte[] audio) {
        // Convert 16-bit PCM to float [-1, 1]
        float[] floats = new float[audio.length / 2];
        for (int i = 0; i < floats.length; i++) {
            short sample = (short) ((audio[i * 2 + 1] << 8) | (audio[i * 2] & 0xFF));
            floats[i] = sample / 32768.0f;
        }
        return floats;
    }

    public void shutdown() {
        try {
            if (session != null) session.close();
            if (env != null) env.close();
        } catch (OrtException e) {
            // Ignore
        }
    }
}
```

**Fallback Energy-Based VAD:**
```java
package com.minewright.voice.vad;

public class EnergyVAD {
    private static final double THRESHOLD = 1000.0;
    private static final int SPEECH_TIMEOUT_MS = 1000;
    private long lastSpeechTime = 0;

    public boolean isSpeechActive(byte[] audioChunk) {
        double energy = calculateEnergy(audioChunk);
        if (energy > THRESHOLD) {
            lastSpeechTime = System.currentTimeMillis();
            return true;
        }
        return (System.currentTimeMillis() - lastSpeechTime) < SPEECH_TIMEOUT_MS;
    }

    private double calculateEnergy(byte[] audio) {
        double sum = 0;
        for (int i = 0; i < audio.length; i += 2) {
            short sample = (short) ((audio[i + 1] << 8) | (audio[i] & 0xFF));
            sum += sample * sample;
        }
        return Math.sqrt(sum / (audio.length / 2));
    }
}
```

---

## Latency Optimization Strategies

### Latency Budget

| Component | Target | Optimization |
|-----------|--------|--------------|
| Audio Capture | <10ms | Small buffer sizes |
| VAD Processing | <5ms | ONNX Runtime |
| STT (API) | 200-800ms | Streaming API |
| STT (Local) | 50-200ms | GPU acceleration |
| LLM Processing | 500-2000ms | Async, caching |
| TTS (API) | 200-300ms | Streaming playback |
| TTS (Local) | 500-2000ms | Model quantization |
| Audio Playback | <50ms | Minecraft sound system |
| **Total Target** | **<1000ms** | Pipeline optimization |

### Strategy 1: Streaming TTS

**Concept:** Start playing audio before entire response is generated

```java
package com.minewright.voice.tts;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface StreamingTextToSpeech extends TextToSpeech {
    /**
     * Synthesize audio in chunks for streaming playback.
     *
     * @param text Text to synthesize
     * @return Future completing with list of audio chunks
     */
    CompletableFuture<List<byte[]>> synthesizeStreaming(String text);

    /**
     * Callback interface for progressive playback.
     */
    interface StreamingPlayback {
        void onChunk(byte[] audioChunk);
        void onComplete();
        void onError(Throwable t);
    }

    /**
     * Synthesize and play with streaming callback.
     */
    void speakStreaming(String text, StreamingPlayback callback);
}
```

**OpenAI Streaming TTS Implementation:**
```java
public class OpenAIStreamingTTS extends OpenAITTS implements StreamingTextToSpeech {

    @Override
    public CompletableFuture<List<byte[]>> synthesizeStreaming(String text) {
        return CompletableFuture.supplyAsync(() -> {
            List<byte[]> chunks = new ArrayList<>();

            // Split text into sentences/phrases
            String[] sentences = text.split("(?<=[.!?])\\s+");

            for (String sentence : sentences) {
                try {
                    byte[] audio = super.synthesize(sentence);
                    chunks.add(audio);
                } catch (VoiceException e) {
                    // Handle error
                }
            }

            return chunks;
        });
    }

    @Override
    public void speakStreaming(String text, StreamingPlayback callback) {
        synthesizeStreaming(text).thenAccept(chunks -> {
            for (byte[] chunk : chunks) {
                callback.onChunk(chunk);
                playAudio(chunk);
            }
            callback.onComplete();
        }).exceptionally(e -> {
            callback.onError(e);
            return null;
        });
    }
}
```

### Strategy 2: Parallel Processing

**Concept:** Overlap LLM generation with TTS synthesis

```java
package com.minewright.voice.pipeline;

import java.util.concurrent.CompletableFuture;

public class VoicePipeline {
    private final LLMService llm;
    private final TextToSpeech tts;

    public CompletableFuture<Void> processCommand(String input) {
        // Start LLM generation
        CompletableFuture<String> llmFuture = llm.generateAsync(input);

        // When LLM produces first sentence, start TTS
        return llmFuture.thenCompose(response -> {
            // Split into sentences for streaming
            String[] sentences = response.split("(?<=[.!?])\\s+");

            // Stream TTS output
            return streamSentences(sentences);
        });
    }

    private CompletableFuture<Void> streamSentences(String[] sentences) {
        CompletableFuture<Void> pipeline = CompletableFuture.completedFuture(null);

        for (String sentence : sentences) {
            pipeline = pipeline.thenCompose(v -> {
                try {
                    return tts.speakAsync(sentence);
                } catch (VoiceException e) {
                    return CompletableFuture.failedFuture(e);
                }
            });
        }

        return pipeline;
    }
}
```

### Strategy 3: Response Caching

**Concept:** Cache TTS output for common responses

```java
package com.minewright.voice.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.minewright.voice.TextToSpeech;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class CachedTTS implements TextToSpeech {
    private final TextToSpeech delegate;
    private final Cache<String, byte[]> audioCache;

    public CachedTTS(TextToSpeech delegate) {
        this.delegate = delegate;
        this.audioCache = Caffeine.newBuilder()
            .maximumSize(100)
            .expireAfterAccess(Duration.ofHours(1))
            .build();
    }

    @Override
    public void speak(String text) throws VoiceException {
        byte[] audio = audioCache.get(text, t -> {
            try {
                // This is a simplification - actual caching would need async
                return new byte[0];  // Placeholder
            } catch (Exception e) {
                return null;
            }
        });

        playAudio(audio);
    }

    @Override
    public CompletableFuture<Void> speakAsync(String text) throws VoiceException {
        byte[] cached = audioCache.getIfPresent(text);

        if (cached != null) {
            playAudio(cached);
            return CompletableFuture.completedFuture(null);
        }

        // Cache miss - generate and cache
        return delegate.speakAsync(text).thenAccept(v -> {
            // Would need to capture audio for caching
        });
    }
}
```

### Strategy 4: Thread Pool Optimization

```java
package com.minewright.voice.concurrent;

import java.util.concurrent.*;

public class VoiceExecutors {
    private final ExecutorService captureExecutor;
    private final ExecutorService sttExecutor;
    private final ExecutorService ttsExecutor;
    private final ExecutorService playbackExecutor;

    public VoiceExecutors() {
        // Single thread for capture (audio requires timing precision)
        this.captureExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "VoiceCapture");
            t.setPriority(Thread.MAX_PRIORITY);
            return t;
        });

        // Multiple threads for STT (network I/O bound)
        this.sttExecutor = newFixedThreadPoolWithPriority(2, "WhisperSTT", Thread.NORM_PRIORITY);

        // Single thread for TTS (API rate limits)
        this.ttsExecutor = newSingleThreadExecutor("OpenAITTS", Thread.NORM_PRIORITY);

        // Single thread for playback (audio mixing)
        this.playbackExecutor = newSingleThreadExecutor("AudioPlayback", Thread.NORM_PRIORITY - 1);
    }

    private ExecutorService newSingleThreadExecutor(String name, int priority) {
        return Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, name);
            t.setPriority(priority);
            t.setDaemon(true);
            return t;
        });
    }

    private ExecutorService newFixedThreadPoolWithPriority(int threads, String name, int priority) {
        return Executors.newFixedThreadPool(threads, r -> {
            Thread t = new Thread(r, name);
            t.setPriority(priority);
            t.setDaemon(true);
            return t;
        });
    }

    public ExecutorService getCaptureExecutor() {
        return captureExecutor;
    }

    public ExecutorService getSttExecutor() {
        return sttExecutor;
    }

    public ExecutorService getTtsExecutor() {
        return ttsExecutor;
    }

    public ExecutorService getPlaybackExecutor() {
        return playbackExecutor;
    }

    public void shutdown() {
        captureExecutor.shutdown();
        sttExecutor.shutdown();
        ttsExecutor.shutdown();
        playbackExecutor.shutdown();
    }
}
```

---

## Error Handling and Fallbacks

### Error Handling Strategy

The voice system implements a multi-layered fallback strategy:

```
┌─────────────────────────────────────────────────────────────┐
│                    Error Handling Layers                    │
│                                                              │
│  Layer 1: Provider-level retry                              │
│    ├─ Network timeout retry (3x)                            │
│    ├─ Exponential backoff                                   │
│    └─ Circuit breaker (Resilience4j)                        │
│                                                              │
│  Layer 2: Provider fallback                                 │
│    ├─ OpenAI API → Local Whisper                            │
│    ├─ ElevenLabs → OpenAI TTS                               │
│    └─ Real TTS → Logging mode                               │
│                                                              │
│  Layer 3: Feature degradation                               │
│    ├─ Voice → Text input                                    │
│    ├─ Full TTS → Sound effects only                         │
│    └─ Positional audio → Stereo                             │
│                                                              │
│  Layer 4: Graceful shutdown                                 │
│    ├─ Notify user of failure                                │
│    ├─ Provide alternative input method                      │
│    └─ Log detailed error for debugging                      │
└─────────────────────────────────────────────────────────────┘
```

### Resilience4j Integration

**Dependencies (already in build.gradle):**
```gradle
implementation 'io.github.resilience4j:resilience4j-circuitbreaker:2.1.0'
implementation 'io.github.resilience4j:resilience4j-retry:2.1.0'
implementation 'io.github.resilience4j:resilience4j-ratelimiter:2.1.0'
```

**STT with Resilience:**
```java
package com.minewright.voice.stt;

import com.minewright.voice.SpeechToText;
import com.minewright.voice.VoiceException;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;

import javax.sound.sampled.AudioFormat;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class ResilientWhisperSTT implements SpeechToText {
    private final WhisperSTT delegate;
    private final Retry retry;
    private final CircuitBreaker circuitBreaker;

    public ResilientWhisperSTT(WhisperSTT delegate) {
        this.delegate = delegate;

        // Configure retry with exponential backoff
        this.retry = Retry.of("whisper-stt",
            RetryConfig.custom()
                .maxAttempts(3)
                .intervalFunction(IntervalFunction.ofExponentialBackoff(500, 2))
                .retryExceptions(VoiceException.class)
                .build()
        );

        // Configure circuit breaker
        this.circuitBreaker = CircuitBreaker.of("whisper-stt",
            CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(java.time.Duration.ofSeconds(30))
                .slidingWindowSize(10)
                .build()
        );
    }

    @Override
    public CompletableFuture<String> listenOnce() throws VoiceException {
        return CompletableFuture.supplyAsync(() -> {
            return Retry.decorateCallable(retry,
                CircuitBreaker.decorateCallable(circuitBreaker,
                    () -> {
                        try {
                            return delegate.listenOnce().get();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                )
            ).call();
        }).exceptionally(e -> {
            // Fallback to local processing or error message
            return "[Voice unavailable: " + e.getMessage() + "]";
        });
    }

    // Delegate other methods...
}
```

### Fallback Voice System

```java
package com.minewright.voice.fallback;

import com.minewright.voice.*;
import java.util.concurrent.CompletableFuture;

public class FallbackVoiceSystem implements VoiceSystem {
    private VoiceSystem primary;
    private VoiceSystem fallback;

    public FallbackVoiceSystem(VoiceSystem primary, VoiceSystem fallback) {
        this.primary = primary;
        this.fallback = fallback;
    }

    @Override
    public CompletableFuture<String> startListening() throws VoiceException {
        try {
            return primary.startListening();
        } catch (VoiceException e) {
            LOGGER.warn("Primary voice system failed, using fallback", e);
            return fallback.startListening();
        }
    }

    @Override
    public void speak(String text) throws VoiceException {
        try {
            primary.speak(text);
        } catch (VoiceException e) {
            LOGGER.warn("Primary TTS failed, using fallback", e);
            fallback.speak(text);
        }
    }

    @Override
    public CompletableFuture<VoiceTestResult> test() {
        return primary.test()
            .exceptionallyCompose(result -> {
                LOGGER.warn("Primary test failed, testing fallback");
                return fallback.test();
            });
    }

    // Other methods delegate similarly...
}
```

### Graceful Degradation

```java
package com.minewright.voice.degradation;

public class VoiceDegradationManager {
    private enum DegradationLevel {
        FULL,           // All features available
        DEGRADED,       // Limited features
        MINIMAL,        // Sound effects only
        DISABLED        // Voice disabled
    }

    private DegradationLevel currentLevel = DegradationLevel.FULL;

    public CompletableFuture<String> listenForCommand() {
        switch (currentLevel) {
            case FULL:
                return voiceManager.listenForCommand();

            case DEGRADED:
                // Show text input prompt
                return showTextPrompt("Voice degraded - Enter command:");

            case MINIMAL:
                // Show chat notification
                notifyPlayer("Voice temporarily unavailable");
                return showTextPrompt("Enter command:");

            case DISABLED:
                notifyPlayer("Voice is disabled");
                return CompletableFuture.completedFuture("");

            default:
                return CompletableFuture.completedFuture("");
        }
    }

    public void speak(String text) {
        switch (currentLevel) {
            case FULL:
                voiceManager.speak(text);
                break;

            case DEGRADED:
                // Play acknowledgment sound only
                playSound(SoundEvents.AMETHYST_BLOCK_CHIME);
                // Still show text
                showChatMessage(text);
                break;

            case MINIMAL:
                // Just show text
                showChatMessage(text);
                break;

            case DISABLED:
                // Silent
                break;
        }
    }

    private void adjustDegradationLevel(Exception error) {
        if (error instanceof NetworkException) {
            currentLevel = DegradationLevel.DEGRADED;
        } else if (error instanceof AudioHardwareException) {
            currentLevel = DegradationLevel.MINIMAL;
        } else if (error instanceof ConfigurationException) {
            currentLevel = DegradationLevel.DISABLED;
        }
    }
}
```

### User Communication

```java
package com.minewright.voice.notification;

import net.minecraft.network.chat.Component;

public class VoiceNotificationManager {
    public void notifyVoiceError(VoiceException error, Player player) {
        Component message = switch (error.getType()) {
            case AUDIO_CAPTURE_FAILED ->
                Component.literal("Microphone error: " + error.getMessage())
                    .withStyle(ChatFormatting.RED);

            case TRANSCRIPTION_FAILED ->
                Component.literal("Could not understand speech. Please try again.")
                    .withStyle(ChatFormatting.YELLOW);

            case SYNTHESIS_FAILED ->
                Component.literal("Voice response unavailable.")
                    .withStyle(ChatFormatting.YELLOW);

            case CONFIGURATION_ERROR ->
                Component.literal("Voice configuration error. Check settings.")
                    .withStyle(ChatFormatting.RED);

            default ->
                Component.literal("Voice system error: " + error.getMessage())
                    .withStyle(ChatFormatting.RED);
        };

        player.displayClientMessage(message, false);
    }

    public void notifyFallbackActivated(Player player) {
        player.displayClientMessage(
            Component.literal("Voice unavailable - using text input")
                .withStyle(ChatFormatting.YELLOW),
            false
        );
    }
}
```

---

## Platform-Specific Considerations

### Cross-Platform Audio Challenges

| Platform | Audio System | Challenges | Solutions |
|----------|-------------|------------|-----------|
| **Windows** | WASAPI/DirectSound | Device enumeration, format conversion | Use Java Sound API, test with multiple devices |
| **macOS** | CoreAudio | Permission requests, sandboxing | Request microphone permission explicitly |
| **Linux** | ALSA/PulseAudio/JACK | Multiple audio systems, device naming | Test with PulseAudio, provide device selection |

### Microphone Device Selection

```java
package com.minewright.voice.capture;

import javax.sound.sampled.*;
import java.util.ArrayList;
import java.util.List;

public class AudioDeviceManager {
    public record AudioDevice(String name, String description, Mixer mixer) {}

    public List<AudioDevice> getAvailableMicrophones() {
        List<AudioDevice> devices = new ArrayList<>();
        Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();

        for (Mixer.Info info : mixerInfos) {
            Mixer mixer = AudioSystem.getMixer(info);

            // Check if mixer supports audio input
            if (mixer.getTargetLineInfo().length > 0) {
                devices.add(new AudioDevice(
                    info.getName(),
                    info.getDescription(),
                    mixer
                ));
            }
        }

        return devices;
    }

    public AudioDevice getBestMicrophone() {
        List<AudioDevice> devices = getAvailableMicrophones();
        String os = System.getProperty("os.name").toLowerCase();

        // Platform-specific priority lists
        String[] priorityNames = os.contains("win")
            ? new String[]{"Primary", "Microphone", "Realtek", "Nahimic"}
            : os.contains("mac")
                ? new String[]{"Built-in", "Input", "Apple"}
                : new String[]{"default", "pulse", "alsa", "USB"};

        // Try to find device by priority
        for (String priority : priorityNames) {
            for (AudioDevice device : devices) {
                if (device.name().toLowerCase().contains(priority.toLowerCase())) {
                    return device;
                }
            }
        }

        // Fallback to first available
        return devices.isEmpty() ? null : devices.get(0);
    }

    public TargetDataLine openMicrophone(AudioDevice device, AudioFormat format)
            throws LineUnavailableException {
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        return (TargetDataLine) device.mixer().getLine(info);
    }
}
```

### macOS Permissions

**Info.plist Addition:**
```xml
<key>NSMicrophoneUsageDescription</key>
<string>MineWright needs microphone access for voice commands</string>
```

**Permission Request Handler:**
```java
package com.minewright.voice.permission;

public class MacosPermissionHandler {
    public static boolean requestMicrophonePermission() {
        String os = System.getProperty("os.name").toLowerCase();

        if (!os.contains("mac")) {
            return true;  // Not applicable
        }

        // On macOS, permission is requested automatically on first access
        // We just need to handle the potential denial gracefully
        try {
            AudioCapture test = new AudioCapture();
            test.startCapture();
            test.stopCapture();
            return true;
        } catch (LineUnavailableException e) {
            if (e.getMessage().contains("Permission denied")) {
                return false;
            }
            throw new RuntimeException(e);
        }
    }
}
```

### Linux Audio Backend Detection

```java
package com.minewright.voice.platform;

public class LinuxAudioDetector {
    public enum AudioBackend {
        PULSEAUDIO,
        ALSA,
        JACK,
        UNKNOWN
    }

    public static AudioBackend detectBackend() {
        String os = System.getProperty("os.name").toLowerCase();

        if (!os.contains("nux") && !os.contains("nix")) {
            return AudioBackend.UNKNOWN;
        }

        // Check for PulseAudio
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"which", "pulseaudio"});
            if (p.waitFor() == 0) {
                return AudioBackend.PULSEAUDIO;
            }
        } catch (Exception e) {
            // Ignore
        }

        // Check for ALSA
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"which", "aplay"});
            if (p.waitFor() == 0) {
                return AudioBackend.ALSA;
            }
        } catch (Exception e) {
            // Ignore
        }

        return AudioBackend.UNKNOWN;
    }

    public static String getDeviceHint(AudioBackend backend) {
        return switch (backend) {
            case PULSEAUDIO -> "default";
            case ALSA -> "hw:0,0";
            case JACK -> "jack";
            case UNKNOWN -> "default";
        };
    }
}
```

---

## Implementation Roadmap

### Phase 1: Foundation (Week 1-2)

**Tasks:**
1. Add audio dependencies to build.gradle
2. Create audio capture package structure
3. Implement AudioCapture with Java Sound API
4. Implement SimpleVAD (energy-based)
5. Add push-to-talk key binding (V key)
6. Create device selection UI

**Deliverables:**
- Working microphone capture
- PTT functionality
- Config screen for device selection

**Files to Create:**
```
src/main/java/com/minewright/voice/capture/
├── AudioCapture.java
├── AudioDeviceManager.java
└── AudioConfig.java

src/main/java/com/minewright/voice/vad/
└── EnergyVAD.java

src/main/java/com/minewright/client/
└── VoiceKeyBindings.java
```

### Phase 2: Speech-to-Text (Week 3-4)

**Tasks:**
1. Implement WhisperSTT (OpenAI API)
2. Add API key configuration
3. Integrate with VoiceManager
4. Test transcription accuracy
5. Implement streaming audio support
6. Add error handling and retries

**Deliverables:**
- Working STT with OpenAI Whisper
- Transcription displayed in chat
- Fallback to text input on failure

**Files to Create:**
```
src/main/java/com/minewright/voice/stt/
├── WhisperSTT.java
├── ResilientWhisperSTT.java
└── STTConfig.java

src/main/resources/
└── config/minewright-common.toml (update)
```

### Phase 3: Text-to-Speech (Week 5-6)

**Tasks:**
1. Implement OpenAITTS
2. Add voice selection (alloy, echo, fable, etc.)
3. Convert TTS output to OGG format
4. Register custom sound events
5. Implement positional audio playback
6. Add voice profiles per agent

**Deliverables:**
- MineWright speaks with TTS
- 3D positional audio
- Multiple voice options

**Files to Create:**
```
src/main/java/com/minewright/voice/tts/
├── OpenAITTS.java
├── TTSConfig.java
└── VoiceProfile.java

src/main/java/com/minewright/sound/
├── MineWrightSounds.java
└── PositionalAudioPlayer.java

src/main/resources/assets/minewright/
└── sounds.json
```

### Phase 4: Polish & Optimization (Week 7-8)

**Tasks:**
1. Implement SileroVAD for better activation
2. Add noise suppression
3. Optimize latency (streaming, caching)
4. Add interruption handling
5. Implement voice profiles per agent
6. Add UI indicators (recording, processing, speaking)

**Deliverables:**
- Voice activation mode
- Visual feedback
- Optimized performance

**Files to Create:**
```
src/main/java/com/minewright/voice/vad/
├── SileroVAD.java
└── VADConfig.java

src/main/java/com/minewright/client/
├── VoiceOverlay.java
└── VoiceConfigScreen.java

src/main/java/com/minewright/voice/cache/
└── CachedTTS.java
```

### Phase 5: Advanced Features (Week 9-10)

**Tasks:**
1. Implement LocalWhisperSTT (offline mode)
2. Add ElevenLabs TTS support
3. Implement voice cloning (optional)
4. Add multi-language support
5. Create voice tutorial and documentation

**Deliverables:**
- Offline STT mode
- Premium TTS option
- Complete documentation

---

## Code Examples

### Complete Voice Command Flow

```java
package com.minewright.command;

import com.minewright.voice.VoiceManager;
import com.minewright.action.ActionExecutor;
import net.minecraft.client.player.LocalPlayer;

public class VoiceCommandHandler {
    private final VoiceManager voiceManager;
    private final ActionExecutor actionExecutor;

    public VoiceCommandHandler() {
        this.voiceManager = VoiceManager.getInstance();
        this.actionExecutor = ActionExecutor.getInstance();
    }

    public void onPushToTalkPressed(LocalPlayer player) {
        if (!voiceManager.isEnabled()) {
            player.displayClientMessage(
                Component.literal("Voice is not enabled")
                    .withStyle(ChatFormatting.YELLOW),
                true
            );
            return;
        }

        // Start listening
        voiceManager.listenForCommand()
            .thenAccept(transcript -> {
                // Show what was heard
                player.displayClientMessage(
                    Component.literal("Heard: \"" + transcript + "\"")
                        .withStyle(ChatFormatting.GREEN),
                    false
                );

                // Process command
                processCommand(transcript, player);
            })
            .exceptionally(error -> {
                player.displayClientMessage(
                    Component.literal("Voice error: " + error.getMessage())
                        .withStyle(ChatFormatting.RED),
                    false
                );
                return null;
            });
    }

    private void processCommand(String command, LocalPlayer player) {
        // Route to action executor
        actionExecutor.processNaturalLanguageCommand(
            player,
            command
        );
    }
}
```

### Complete TTS Response Flow

```java
package com.minewright.dialogue;

import com.minewright.voice.VoiceManager;
import com.minewright.entity.ForemanEntity;

public class ForemanDialogueManager {
    private final VoiceManager voiceManager;

    public void generateAndSpeakResponse(ForemanEntity foreman, String prompt) {
        // Generate response via LLM
        String response = foreman.getBrain().generateResponse(prompt);

        // Show in chat
        foreman.level().players().forEach(player -> {
            player.displayClientMessage(
                Component.literal("Foreman: " + response)
                    .withStyle(ChatFormatting.WHITE),
                false
            );
        });

        // Speak via TTS
        voiceManager.speakIfEnabled(response);
    }

    public void generateAndSpeakComment(ForemanEntity foreman, String comment) {
        // Show in chat
        foreman.level().players().forEach(player -> {
            player.displayClientMessage(
                Component.literal("*" + foreman.getName() + "*: " + comment)
                    .withStyle(ChatFormatting.GRAY)
                    .withStyle(ChatFormatting.ITALIC),
                false
            );
        });

        // Speak via TTS (lower volume for ambient comments)
        voiceManager.speakIfEnabled(comment);
    }
}
```

---

## Testing Strategy

### Unit Testing

**Audio Capture Test:**
```java
@Test
public void testAudioCapture() throws LineUnavailableException {
    AudioCapture capture = new AudioCapture();
    capture.startCapture();

    byte[] audio = capture.captureChunk(1000);  // 1 second

    assertNotNull(audio);
    assertTrue(audio.length > 0);

    capture.stopCapture();
}
```

**VAD Test:**
```java
@Test
public void testVAD() {
    EnergyVAD vad = new EnergyVAD(0.5);

    // Silence
    byte[] silence = generateSilence(1000);
    assertFalse(vad.isSpeechActive(silence));

    // Speech
    byte[] speech = generateSpeech(1000);
    assertTrue(vad.isSpeechActive(speech));
}
```

### Integration Testing

**End-to-End Voice Test:**
```java
@Test
public void testVoiceToEndEnd() throws Exception {
    // Record test audio
    byte[] testAudio = loadTestAudio();

    // Transcribe
    String transcript = sttService.transcribe(testAudio);
    assertEquals("hello minesaft", transcript.toLowerCase());

    // Process with LLM
    String response = llmService.generate(transcript);
    assertNotNull(response);

    // Synthesize
    byte[] ttsAudio = ttsService.synthesize(response);
    assertNotNull(ttsAudio);
    assertTrue(ttsAudio.length > 0);
}
```

---

## Security and Privacy

### API Key Management

**Never hardcode API keys:**
```java
// BAD
private static final String API_KEY = "sk-...";

// GOOD
private final String apiKey = VoiceConfig.OPENAI_API_KEY.get();
```

### Privacy Considerations

1. **Informed Consent:** Always indicate when recording
2. **Local Storage:** Don't store raw audio unless opted in
3. **Encryption:** Use HTTPS for all API calls
4. **Data Retention:** Delete temporary audio files immediately

### Rate Limiting

```java
public class RateLimitedTTS {
    private final RateLimiter rateLimiter;

    public RateLimitedTTS() {
        // Limit to 3 requests per second
        this.rateLimiter = RateLimiter.create(3.0);
    }

    public byte[] synthesize(String text) {
        rateLimiter.acquire();
        return ttsService.synthesize(text);
    }
}
```

---

## Cost Analysis

### OpenAI API Costs (per 1000 voice conversations)

**Whisper STT:**
- Average command: 5 seconds
- Cost per command: $0.00005 (0.5 cents)
- 1000 commands: $0.05

**TTS:**
- Average response: 100 characters
- Cost per response: $0.0015 (0.15 cents)
- 1000 responses: $1.50

**Monthly Estimate (1000 conversations):**
- STT: $0.05
- TTS: $1.50
- **Total: ~$1.55/month**

---

## References

### Documentation
- [OpenAI Whisper API](https://platform.openai.com/docs/guides/speech-to-text)
- [OpenAI TTS API](https://platform.openai.com/docs/guides/text-to-speech)
- [Minecraft Forge Documentation](https://docs.minecraftforge.net/)
- [Java Sound API](https://docs.oracle.com/javase/tutorial/sound/)

### Java Libraries
- [whisper-jni](https://github.com/givimad/whisper-jni)
- [Resilience4j](https://resilience4j.readme.io/)
- [Caffeine Cache](https://github.com/ben-manes/caffeine)

### Existing Mods (Reference)
- [Simple Voice Chat](https://github.com/henkelmax/simple-voice-chat)
- [Plasmo Voice](https://github.com/Plasmo-voice/plasmo-voice)

---

## Conclusion

The MineWright voice system provides a solid architectural foundation for TTS/STT integration. The current implementation demonstrates excellent software engineering practices with clean interfaces, proper configuration management, and extensible design patterns.

### Next Steps

1. **Immediate Priority:** Implement audio capture with Java Sound API
2. **High Priority:** Integrate OpenAI Whisper STT
3. **High Priority:** Integrate OpenAI TTS
4. **Medium Priority:** Add positional audio playback
5. **Low Priority:** Implement local/offline alternatives

### Key Strengths

- Clean, extensible architecture
- Proper separation of concerns
- Configuration-driven design
- Async/non-blocking operations
- Multiple fallback modes

### Areas for Improvement

- Real TTS/STT implementations needed
- Audio capture integration required
- Positional audio not implemented
- Voice activity detection missing
- Platform-specific handling needed

---

**Document Status:** Complete
**Next Review:** After Phase 1 implementation
**Maintainer:** MineWright Development Team

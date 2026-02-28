# Voice Integration Research Report
## MineWright AI Mod - Natural Conversation with Agents

**Date:** 2025-02-26
**Project:** MineWright AI (Cursor for Minecraft)
**Research Focus:** STT/TTS Integration for Natural Voice Conversations

---

## Executive Summary

This report provides a comprehensive analysis of voice integration options for the MineWright AI Minecraft Forge mod. The goal is to enable natural voice conversations with AI-controlled MineWright agents using Speech-to-Text (STT) and Text-to-Speech (TTS) technologies.

### Key Recommendations

| Component | Recommendation | Latency | Cost |
|-----------|---------------|---------|------|
| **STT (Primary)** | OpenAI Whisper API | 200-800ms | $0.006/10min |
| **STT (Offline)** | whisper.cpp via JNI | 50-200ms | Free (local) |
| **TTS (Primary)** | OpenAI TTS API | 200-300ms | $15/1M chars |
| **TTS (Premium)** | ElevenLabs API | 300-500ms | $5-50/1M chars |
| **TTS (Offline)** | Coqui XTTS-v2 | 500-2000ms | Free (local) |
| **Audio Capture** | Java Sound API | <10ms | Free (built-in) |
| **Voice Activation** | Silero VAD | <5ms | Free (ONNX) |

### Recommended Architecture

**Hybrid Approach:**
- **Client-Side Audio Capture** using Java Sound API
- **Client-Side STT** with local whisper.cpp (low latency)
- **Server-Side LLM** (existing OpenAI/Groq integration)
- **Client-Side TTS** with OpenAI/ElevenLabs API
- **Positional Audio** using Minecraft's sound system

---

## Table of Contents

1. [Speech-to-Text (STT) Options](#1-speech-to-text-stt-options)
2. [Text-to-Speech (TTS) Options](#2-text-to-speech-tts-options)
3. [Java Audio Libraries](#3-java-audio-libraries)
4. [Existing Minecraft Voice Mods](#4-existing-minecraft-voice-mods)
5. [Integration Challenges](#5-integration-challenges)
6. [Recommended Architecture](#6-recommended-architecture)
7. [Implementation Roadmap](#7-implementation-roadmap)
8. [Code Examples](#8-code-examples)
9. [Performance & Optimization](#9-performance--optimization)
10. [Security Considerations](#10-security-considerations)

---

## 1. Speech-to-Text (STT) Options

### 1.1 OpenAI Whisper API

**Overview:** State-of-the-art speech recognition model with excellent multilingual support.

**Pros:**
- Highest accuracy (WER ~1-2%)
- Supports 99+ languages
- Excellent with accents and background noise
- Simple REST API integration
- No infrastructure overhead

**Cons:**
- Requires internet connection
- Latency: 200-800ms
- Cost: $0.006 per 10 minutes
- Privacy considerations (audio sent to OpenAI)

**Java Integration:**

```java
// Using Spring AI with Whisper
@Service
public class SpeechToTextService {
    private final OpenAiWhisperClient whisperClient;

    public Mono<String> transcribe(byte[] audioData, String language) {
        WhisperRequest request = WhisperRequest.builder()
            .model("whisper-1")
            .file(audioData)
            .language("en")  // Optional: auto-detect
            .responseFormat("text")
            .build();

        return whisperClient.transcribe(request)
            .map(WhisperResponse::getText)
            .defaultIfEmpty("No valid speech detected");
    }
}
```

**Maven Dependency:**
```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-openai</artifactId>
    <version>1.0.0-M4</version>
</dependency>
```

**API Endpoint:**
```
POST https://api.openai.com/v1/audio/transcriptions
```

---

### 1.2 whisper.cpp (Local/Offline)

**Overview:** C++ implementation of Whisper with Java bindings via JNI. 4x faster than original.

**Pros:**
- Completely offline (privacy)
- Low latency: 50-200ms
- No API costs
- Cross-platform (Windows/Mac/Linux)
- Official Java bindings available

**Cons:**
- Requires native library setup
- Model download (39MB - 3GB depending on model)
- Slightly lower accuracy than API
- CPU intensive

**Java Integration:**

**Option A: Maven Dependency (Recommended)**
```xml
<dependency>
    <groupId>io.github.givimad</groupId>
    <artifactId>whisper-jni</artifactId>
    <version>1.7.1</version>
</dependency>
```

**Option B: Build from Source**
```bash
# Build whisper.cpp as shared library
cmake -B build -DBUILD_SHARED_LIBS=OFF
cmake --build build --config Release -j

# Use Java bindings in bindings/java/
```

**Code Example:**
```java
import com.github.givimad.whisperjni.WhisperContext;

public class LocalSpeechRecognition {
    private WhisperContext context;

    public void initialize(String modelPath) {
        context = new WhisperContext(modelPath);
    }

    public String transcribe(byte[] audioData) {
        // audioData should be 16kHz, 16-bit, mono PCM
        String result = context.transcribe(audioData);
        return result;
    }
}
```

**Model Sizes:**
| Model | Size | VRAM | Speed | Relative Accuracy |
|-------|------|------|-------|-------------------|
| tiny | 39 MB | ~1 GB | ~32x | 74% |
| base | 74 MB | ~1 GB | ~16x | 82% |
| small | 244 MB | ~2 GB | ~6x | 88% |
| medium | 769 MB | ~5 GB | ~2x | 92% |
| large-v3 | 1550 MB | ~10 GB | 1x | 100% |

**Recommended:** `small` model for balance of speed and accuracy.

---

### 1.3 Google Cloud Speech-to-Text

**Pros:**
- 125+ languages
- Real-time streaming API
- Enhanced models for specific use cases
- Auto punctuation

**Cons:**
- Complex setup
- Pricing: $0.006-$0.036 per 15 seconds
- Higher latency than Whisper

---

### 1.4 Azure Speech Services

**Pros:**
- Excellent accuracy
- Real-time transcription
- Custom speech models
- Good SDK support

**Cons:**
- Pricing: $1 per hour
- Azure subscription required

---

### 1.5 Web Speech API

**Overview:** Browser-based API (JavaScript only).

**Pros:**
- Free (Chrome/Edge)
- No server infrastructure
- Built into modern browsers

**Cons:**
- Client-side only (JavaScript)
- Requires external bridge for Java
- Limited to Chromium browsers
- Privacy concerns (Google)

**Not Recommended** for Minecraft Forge mod due to Java requirement.

---

## 2. Text-to-Speech (TTS) Options

### 2.1 OpenAI TTS API

**Overview:** High-quality TTS with 6 voice options.

**Pros:**
- Natural sounding (MOS 4.5+)
- Low latency (~200ms)
- Simple API
- Multiple voices (alloy, echo, fable, onyx, nova, shimmer)
- 40+ languages

**Cons:**
- Requires internet
- Cost: $15 per 1M characters (~$2-3 per hour of audio)
- Limited voice customization

**Java Integration:**

```java
public class OpenAITTSClient {
    private static final String TTS_API_URL = "https://api.openai.com/v1/audio/speech";
    private final HttpClient client;
    private final String apiKey;

    public byte[] synthesize(String text, String voice) throws Exception {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", "tts-1");
        requestBody.addProperty("input", text);
        requestBody.addProperty("voice", voice); // alloy, echo, fable, onyx, nova, shimmer

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(TTS_API_URL))
            .header("Authorization", "Bearer " + apiKey)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
            .build();

        HttpResponse<byte[]> response = client.send(request,
            HttpResponse.BodyHandlers.ofByteArray());

        return response.body();
    }
}
```

**Voice Characteristics:**
| Voice | Gender | Description |
|-------|--------|-------------|
| alloy | Neutral | Balanced and clear |
| echo | Male | Knowledgeable and calm |
| fable | Male | Expressive and storytelling |
| onyx | Male | Deep and authoritative |
| nova | Female | Friendly and warm |
| shimmer | Female | Clear and bright |

---

### 2.2 ElevenLabs TTS

**Overview:** Premium TTS with voice cloning and emotional control.

**Pros:**
- Highest quality (near-human)
- 40,000+ voice options in Voice Library
- Voice cloning (30s sample)
- Emotional control
- Multiple languages
- Streaming support

**Cons:**
- Higher cost: $5-50 per 1M characters
- Higher latency: 300-500ms
- Internet required

**Java Integration (Spring AI):**

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-elevenlabs</artifactId>
</dependency>
```

```java
ElevenLabsApi elevenLabsApi = ElevenLabsApi.builder()
    .apiKey(System.getenv("ELEVEN_LABS_API_KEY"))
    .build();

ElevenLabsTextToSpeechModel tts = ElevenLabsTextToSpeechModel.builder()
    .elevenLabsApi(elevenLabsApi)
    .defaultOptions(ElevenLabsTextToSpeechOptions.builder()
        .model("eleven_turbo_v2_5")
        .voiceId("your_voice_id")
        .outputFormat("mp3_44100_128")
        .build())
    .build();

TextToSpeechResponse response = tts.call(
    new TextToSpeechPrompt("Hello MineWright, build me a house!")
);
byte[] audio = response.getResult().getOutput();
```

**Popular Models:**
- `eleven_multilingual_v2` - Best quality, 29 languages
- `eleven_turbo_v2_5` - Low latency, 29 languages

---

### 2.3 Coqui TTS / XTTS-v2 (Local/Offline)

**Overview:** Open-source TTS with voice cloning capabilities.

**Pros:**
- Completely offline
- Voice cloning (6s sample)
- Cross-language voice cloning
- Multiple models: Tacotron, Glow-TTS, VITS, XTTS
- Free

**Cons:**
- Higher latency: 500-2000ms
- Python-based (requires HTTP API or ONNX)
- Lower quality than commercial options
- More complex setup

**Java Integration Options:**

**Option A: HTTP API (Recommended)**
```python
# Python Flask server
from TTS.api import TTS
from flask import Flask, send_file

app = Flask(__name__)
tts = TTS("tts_models/multilingual/multi-dataset/xtts_v2")

@app.route('/tts')
def text_to_speech():
    text = request.args.get('text')
    tts.tts_to_file(text=text, file_path="output.wav")
    return send_file("output.wav")
```

```java
// Java client
public byte[] synthesize(String text) throws Exception {
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create("http://localhost:5000/tts?text=" +
            URLEncoder.encode(text, StandardCharsets.UTF_8)))
        .GET()
        .build();

    return client.send(request,
        HttpResponse.BodyHandlers.ofByteArray()).body();
}
```

**Option B: ONNX Runtime Java**
```xml
<dependency>
    <groupId>com.microsoft.onnxruntime</groupId>
    <artifactId>onnxruntime</artifactId>
    <version>1.17.0</version>
</dependency>
```

---

### 2.4 Azure Speech Synthesis

**Pros:**
- High quality neural voices
- 400+ neural voices across 140+ languages
- SSML support for fine control
- Custom neural voices

**Cons:**
- Pricing: $15 per 1M characters
- Azure subscription required

---

### 2.5 Google Cloud TTS

**Pros:**
- WaveNet voices (high quality)
- 400+ voices
- SSML support

**Cons:**
- Pricing: $4-16 per 1M characters
- Standard voices lower quality

---

### 2.6 Minecraft Native Sounds

**Overview:** Using Minecraft's note block system for synthetic speech.

**Pros:**
- Completely within Minecraft
- No external dependencies
- Fun/quirky aesthetic

**Cons:**
- Very limited intelligibility
- High development effort
- Not suitable for conversation

**Not Recommended** for serious conversation.

---

## 3. Java Audio Libraries

### 3.1 Java Sound API (javax.sound.sampled)

**Overview:** Built-in Java standard library for audio I/O.

**Pros:**
- No external dependencies
- Cross-platform (Windows/Mac/Linux)
- Sufficient for basic capture/playback
- Already part of JDK

**Cons:**
- Basic functionality only
- No advanced DSP
- Higher latency than native solutions

**Microphone Capture Example:**

```java
import javax.sound.sampled.*;

public class AudioCapture {
    private TargetDataLine microphone;
    private AudioFormat format;
    private boolean isRecording = false;

    // Recommended format for Whisper
    private static final AudioFormat WHISPER_FORMAT = new AudioFormat(
        16000,  // Sample rate (16kHz for Whisper)
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

    public byte[] captureAudio(int durationMs) {
        int bufferSize = (int) (WHISPER_FORMAT.getSampleRate() *
            WHISPER_FORMAT.getFrameSize() * (durationMs / 1000.0));
        byte[] buffer = new byte[bufferSize];
        microphone.read(buffer, 0, buffer.length);
        return buffer;
    }

    public void stopCapture() {
        isRecording = false;
        microphone.stop();
        microphone.close();
    }

    // List available microphones
    public static void listMicrophones() {
        Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();
        for (Mixer.Info info : mixerInfos) {
            Mixer mixer = AudioSystem.getMixer(info);
            if (mixer.getTargetLineInfo().length > 0) {
                System.out.println("Microphone: " + info.getName());
            }
        }
    }
}
```

**Audio Playback Example:**

```java
public class AudioPlayback {
    private SourceDataLine speaker;

    public void playAudio(byte[] audioData, AudioFormat format)
            throws LineUnavailableException {
        DataLine.Info info = new DataLine.Info(
            SourceDataLine.class,
            format
        );

        speaker = (SourceDataLine) AudioSystem.getLine(info);
        speaker.open(format);
        speaker.start();
        speaker.write(audioData, 0, audioData.length);
        speaker.drain();
        speaker.close();
    }

    // Play with position (3D audio)
    public void playPositionalAudio(byte[] audioData, float x, float y, float z) {
        // Use Minecraft's sound system instead
        // See section 6.3
    }
}
```

---

### 3.2 Minecraft Sound System

**Overview:** Forge's built-in sound system for positional audio.

**Playing Sound at Entity Position:**

```java
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.resources.ResourceLocation;

public class MineWrightEntity extends PathfinderMob {

    // Register custom sound event
    public static final DeferredRegister<SoundEvent> SOUNDS =
        DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, "minewright");

    public static final RegistryObject<SoundEvent> STEVE_SPEAK = SOUNDS.register(
        "minewright_speak",
        () -> new SoundEvent(new ResourceLocation("minewright", "minewright_speak"))
    );

    public void speak(String message) {
        // Convert TTS audio to OGG format
        byte[] oggAudio = convertToOGG(message);

        // Save to temp file
        Path tempFile = Files.createTempFile("minewright_tts_", ".ogg");
        Files.write(tempFile, oggAudio);

        // Play at entity position with distance attenuation
        this.level().playSound(
            null,  // Player (null = all players)
            this.blockPosition(),  // Position
            STEVE_SPEAK.get(),  // Sound event
            SoundSource.NEUTRAL,  // Category
            1.0f,  // Volume
            1.0f   // Pitch
        );
    }
}
```

**Registering Custom Sounds:**

```json
// src/main/resources/assets/minewright/sounds.json
{
  "minewright.minewright_speak": {
    "sounds": [
      {
        "name": "minewright:tts_output",
        "stream": true
      }
    ]
  }
}
```

---

### 3.3 Advanced Audio Libraries

**TarsosDSP**
- Pure Java audio processing
- VAD (Voice Activity Detection)
- Pitch detection
- Audio effects

```xml
<dependency>
    <groupId>be.tarsos.dsp</groupId>
    <artifactId>core</artifactId>
    <version>2.5</version>
</dependency>
```

**JavaCV**
- FFmpeg wrapper
- Advanced audio/video processing
- Cross-platform

```xml
<dependency>
    <groupId>org.bytedeco</groupId>
    <artifactId>javacv-platform</artifactId>
    <version>1.5.9</version>
</dependency>
```

---

## 4. Existing Minecraft Voice Mods

### 4.1 Simple Voice Chat (by Henkelmax)

**GitHub:** https://github.com/henkelmax/simple-voice-chat
**Versions:** Forge, Fabric, Quilt, NeoForge
**MC Versions:** 1.16.2 - 1.20.4

**Key Features:**
- Proximity voice chat
- Opus codec for audio encoding
- RNNoise (RNN noise suppression)
- OpenAL for 3D positional audio
- Push-to-talk and voice activation
- AES encryption
- Audio recording capability

**Technical Implementation:**
- Uses Java Sound API for microphone capture
- Opus codec for compression
- Custom networking for audio streaming
- OpenAL for 3D audio positioning

**Relevance:** Excellent reference for audio capture and networking code.

---

### 4.2 Plasmo Voice

**Features:**
- Voice activation with configurable threshold
- Visual voice indicator
- Distance-based audio
- Multiple audio codecs

---

### 4.3 Audio Libraries in Existing Mods

**Auudio** - Library mod for playing audio
- URL: https://www.mcmod.cn/class/7489.html
- Features:
  - No sound asset registration required
  - External audio via URL
  - Individual volume control
  - Stop/pause/resume support

---

## 5. Integration Challenges

### 5.1 Cross-Platform Audio Capture

**Challenge:** Microphone access varies across operating systems.

**Solutions:**
- Use Java Sound API (works on all platforms)
- Test on Windows, macOS, and Linux
- Handle different microphone naming conventions
- Provide device selection in config

```java
public AudioCaptureConfig getBestMicrophone() {
    Mixer.Info[] mixers = AudioSystem.getMixerInfo();

    // Priority list by platform
    String[] priorityNames = isWindows()
        ? new String[]{"Primary", "Microphone", "Realtek"}
        : isMac()
            ? new String[]{"Built-in", "Input"}
            : new String[]{"default", "pulse", "alsa"};

    for (String priority : priorityNames) {
        for (Mixer.Info mixer : mixers) {
            if (mixer.getName().toLowerCase().contains(priority.toLowerCase())) {
                return new AudioCaptureConfig(mixer);
            }
        }
    }

    // Fallback to first available
    return new AudioCaptureConfig(mixers[0]);
}
```

---

### 5.2 Push-to-Talk vs Voice Activation

**Push-to-Talk (PTT):**
- Pros: No false activations, cleaner audio
- Cons: Requires key binding, less natural

**Voice Activation Detection (VAD):**
- Pros: Hands-free, natural conversation
- Cons: False activations, background noise

**Recommended:** Support both with config option.

**VAD Implementation Options:**

**Option A: Silero VAD (Recommended)**
- High accuracy
- Low latency (<5ms)
- Java ONNX implementation available

```xml
<dependency>
    <groupId>com.microsoft.onnxruntime</groupId>
    <artifactId>onnxruntime</artifactId>
    <version>1.17.0</version>
</dependency>
```

```java
// Silero VAD Java wrapper
public class SileroVAD {
    private OrtSession session;
    private OrtEnvironment env;

    public boolean isSpeech(byte[] audioChunk) {
        // Process 30ms chunks at 16kHz
        // Returns probability of speech
        return speechProbability > 0.5;
    }
}
```

**Option B: Simple Energy-Based VAD**
```java
public class SimpleVAD {
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
        // RMS energy calculation
        double sum = 0;
        for (int i = 0; i < audio.length; i += 2) {
            short sample = (short) ((audio[i+1] << 8) | (audio[i] & 0xFF));
            sum += sample * sample;
        }
        return Math.sqrt(sum / (audio.length / 2));
    }
}
```

---

### 5.3 Noise Cancellation

**Challenge:** Minecraft game audio (music, sounds, other players) interferes with voice input.

**Solutions:**

**Option A: Acoustic Echo Cancellation (AEC)**
- Requires echo reference (Minecraft audio output)
- Complex to implement
- WebRTC provides AEC algorithms

**Option B: Noise Suppression**
- RNNoise (used by Simple Voice Chat)
- Removes background noise
- Available as native library

**Option C: Headset with Noise Cancelling**
- Hardware solution
- Most effective
- Requires user investment

**Option D: Push-to-Talk**
- Simplest solution
- No false activations
- Recommended for initial implementation

---

### 5.4 Latency Requirements

**Target:** End-to-end latency under 1 second for natural conversation.

**Latency Breakdown:**
| Component | Target | Optimization |
|-----------|--------|--------------|
| Audio Capture | <10ms | Buffered streaming |
| VAD | <5ms | Silero ONNX |
| STT | 200-800ms | Whisper API/local |
| LLM | 500-2000ms | Existing, optimize prompts |
| TTS | 200-500ms | OpenAI/ElevenLabs |
| Audio Playback | <50ms | Minecraft sound system |
| **Total** | **915-3365ms** | Streaming TTS helps |

**Optimizations:**
- Stream audio chunks while recording (pipelining)
- Use local Whisper for STT (lower latency)
- Use streaming TTS (play while generating)
- Cache common responses

---

### 5.5 Multiple Players/Agents

**Challenge:** Multiple MineWrights responding to multiple players.

**Solutions:**

**Option A: Individual Voice Channels**
- Each MineWright has unique voice/ID
- Spatial audio (3D positioning)
- Players hear closest MineWrights

**Option B: Voice Configuration per Agent**
```java
public class MineWrightVoiceProfile {
    private String voiceId;      // TTS voice ID
    private float pitch;         // Pitch modifier (0.5-2.0)
    private float speed;         // Speed modifier (0.5-2.0)
    private String language;     // Language code

    // Example configurations
    public static final MineWrightVoiceProfile DEFAULT = new MineWrightVoiceProfile(
        "nova", 1.0f, 1.0f, "en"
    );

    public static final MineWrightVoiceProfile DEEP = new MineWrightVoiceProfile(
        "onyx", 0.8f, 0.9f, "en"
    );

    public static final MineWrightVoiceProfile FAST = new MineWrightVoiceProfile(
        "alloy", 1.0f, 1.2f, "en"
    );
}
```

---

### 5.6 Interruption Handling

**Challenge:** Player interrupts while MineWright is speaking.

**Solutions:**
```java
public class MineWrightAudioManager {
    private SourceDataLine currentPlayback;
    private volatile boolean interrupted = false;

    public void speak(String text) {
        interrupted = false;

        // Generate TTS in chunks
        List<byte[]> audioChunks = ttsClient.generateStreaming(text);

        for (byte[] chunk : audioChunks) {
            if (interrupted) {
                stopPlayback();
                return;
            }
            playChunk(chunk);
        }
    }

    public void interrupt() {
        interrupted = true;
        stopPlayback();
    }

    // Detect interruption (e.g., player starts speaking)
    public void onPlayerSpeechStart() {
        if (isSpeaking()) {
            interrupt();
        }
    }
}
```

---

## 6. Recommended Architecture

### 6.1 System Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         CLIENT (Minecraft)                      │
│                                                                  │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐      │
│  │   Audio      │───▶│     VAD      │───▶│     STT      │      │
│  │   Capture    │    │ (Silero)     │    │ (Whisper)    │      │
│  └──────────────┘    └──────────────┘    └──────────────┘      │
│                                                     │            │
│                                                     ▼            │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐      │
│  │    Audio     │◀───│     TTS      │◀───│     LLM      │      │
│  │   Playback   │    │ (OpenAI)     │    │ (Existing)   │      │
│  └──────────────┘    └──────────────┘    └──────────────┘      │
│         │                                                        │
│         ▼                                                        │
│  ┌──────────────┐                                               │
│  │  Minecraft   │                                               │
│  │ Sound System │                                               │
│  └──────────────┘                                               │
└─────────────────────────────────────────────────────────────────┘
                            │
                            │ Network (if using cloud STT/TTS)
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

---

### 6.2 Package Structure

```
src/main/java/com/minewright/ai/
├── voice/
│   ├── capture/
│   │   ├── AudioCapture.java          # Microphone capture
│   │   ├── AudioConfig.java           # Device configuration
│   │   └── CaptureManager.java        # Lifecycle management
│   ├── stt/
│   │   ├── SpeechToTextService.java   # STT interface
│   │   ├── WhisperSTT.java            # OpenAI Whisper API
│   │   ├── LocalWhisperSTT.java       # whisper.cpp JNI
│   │   └── STTConfig.java             # STT configuration
│   ├── tts/
│   │   ├── TextToSpeechService.java   # TTS interface
│   │   ├── OpenAITTS.java             # OpenAI TTS API
│   │   ├── ElevenLabsTTS.java         # ElevenLabs API
│   │   ├── VoiceProfile.java          # Voice configuration
│   │   └── TTSConfig.java             # TTS configuration
│   ├── vad/
│   │   ├── VoiceActivationDetector.java  # VAD interface
│   │   ├── SileroVAD.java                # Silero VAD
│   │   └── SimpleVAD.java                # Fallback energy-based
│   └── VoiceManager.java              # Main voice coordinator
├── client/
│   ├── VoiceKeyBindings.java          # PTT keys
│   ├── VoiceOverlay.java              # Voice status UI
│   └── VoiceConfigScreen.java         # Config GUI
└── config/
    └── VoiceConfig.java               # TOML config values
```

---

### 6.3 Client-Side vs Server-Side Decision

| Component | Location | Rationale |
|-----------|----------|-----------|
| Audio Capture | Client | Microphone access only on client |
| VAD | Client | Low latency, reduces network traffic |
| STT | Client | Privacy, lower latency |
| LLM | Server | Existing implementation |
| TTS | Client | Reduces server load, local playback |
| Audio Playback | Client | Minecraft sound system |

**Key Point:** All voice processing happens client-side. Only the transcribed text is sent to the server for LLM processing, then the response is synthesized client-side.

---

### 6.4 Data Flow

**Player Speaking to MineWright:**

```
1. Player presses PTT key (or VAD detects speech)
2. AudioCapture starts recording
3. Audio chunks sent to VAD
4. When speech detected, chunks sent to STT
5. STT returns transcribed text
6. Text sent to server via existing command system
7. Server processes with LLM (existing flow)
8. LLM response returned to client
9. Client sends response to TTS
10. TTS returns audio
11. Audio played at MineWright's position (3D audio)
```

**MineWright Speaking to Player:**

```
1. MineWright generates response (LLM)
2. Response sent to client
3. Client creates TTS request
4. TTS returns audio
5. Audio played at MineWright's entity position
6. Player hears MineWright from correct direction
```

---

## 7. Implementation Roadmap

### Phase 1: Foundation (Week 1-2)

**Tasks:**
1. Add voice dependencies to build.gradle
2. Create voice package structure
3. Implement AudioCapture with Java Sound API
4. Implement SimpleVAD (energy-based)
5. Add push-to-talk key binding
6. Create voice config options

**Deliverables:**
- Working microphone capture
- PTT functionality
- Config screen for voice settings

---

### Phase 2: Speech-to-Text (Week 3-4)

**Tasks:**
1. Implement WhisperSTT (OpenAI API)
2. Add API key configuration
3. Test transcription accuracy
4. Implement streaming audio support
5. Add transcription caching
6. Implement error handling and retries

**Deliverables:**
- Working STT with OpenAI Whisper
- Transcription displayed in chat
- Fallback to text input on failure

---

### Phase 3: Text-to-Speech (Week 5-6)

**Tasks:**
1. Implement OpenAITTS
2. Add voice selection (alloy, echo, fable, etc.)
3. Convert TTS output to OGG format
4. Register custom sound events
5. Implement positional audio playback
6. Add MineWright voice profiles

**Deliverables:**
- MineWright speaks with TTS
- 3D positional audio
- Multiple voice options

---

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

---

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

## 8. Code Examples

### 8.1 Voice Manager (Main Coordinator)

```java
package com.minewright.ai.voice;

import com.minewright.ai.config.VoiceConfig;
import com.minewright.ai.entity.MineWrightEntity;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

public class VoiceManager {
    private final AudioCapture capture;
    private final VoiceActivationDetector vad;
    private final SpeechToTextService stt;
    private final TextToSpeechService tts;
    private final VoiceConfig config;

    private volatile boolean isRecording = false;
    private LocalPlayer currentPlayer;

    public VoiceManager(VoiceConfig config) {
        this.config = config;
        this.capture = new AudioCapture(config);
        this.vad = createVAD(config);
        this.stt = createSTT(config);
        this.tts = createTTS(config);
    }

    public void startRecording(LocalPlayer player) {
        this.currentPlayer = player;
        this.isRecording = true;

        capture.startCapture();

        // Start recording thread
        new Thread(this::recordingLoop).start();
    }

    public void stopRecording() {
        isRecording = false;
        capture.stopCapture();
    }

    private void recordingLoop() {
        StringBuilder transcript = new StringBuilder();
        boolean speechDetected = false;
        int silenceCount = 0;

        while (isRecording) {
            // Capture audio chunk (30ms)
            byte[] audioChunk = capture.captureChunk(30);

            // Check for speech
            if (vad.isSpeech(audioChunk)) {
                speechDetected = true;
                silenceCount = 0;

                // Accumulate audio for transcription
                transcript.append(transcribeChunk(audioChunk));
            } else if (speechDetected) {
                silenceCount++;

                // Stop after 1 second of silence
                if (silenceCount > 33) { // 33 * 30ms = ~1s
                    break;
                }
            }
        }

        // Process transcript
        if (speechDetected && transcript.length() > 0) {
            handleTranscript(transcript.toString());
        }
    }

    private void handleTranscript(String transcript) {
        // Send to MineWright as command
        if (currentPlayer != null) {
            String command = "/minewright command " + transcript;
            currentPlayer.connection.send(command);
        }
    }

    public void speak(MineWrightEntity minewright, String text) {
        // Generate TTS audio
        byte[] audio = tts.synthesize(text, minewright.getVoiceProfile());

        // Play at MineWright's position
        playAudioAtEntity(minewright, audio);
    }

    private void playAudioAtEntity(MineWrightEntity minewright, byte[] audio) {
        // Convert to Minecraft sound and play
        // Implementation depends on chosen approach
        minewright.level().playSound(
            null,
            minewright.blockPosition(),
            minewright.getSpeakSound(),
            net.minecraft.sounds.SoundSource.NEUTRAL,
            1.0f,
            1.0f
        );
    }
}
```

---

### 8.2 Configuration

```toml
[voice]
# Voice system enabled
enabled = true

# Push-to-talk or voice activation
activation_mode = "push_to_talk"  # or "voice_activation"

# Push-to-talk key binding (keycode)
ptt_key = 86  # 'V' key

# Voice activation threshold (0.0 - 1.0)
vad_threshold = 0.5

# Silence timeout in ms before stopping recording
silence_timeout = 1000

[voice.stt]
# STT provider: "openai", "local"
provider = "openai"

# Whisper settings
model = "whisper-1"
language = "auto"  # or "en", "es", etc.

# Local Whisper settings
local_model_path = "models/whisper/small.ggml"

[voice.tts]
# TTS provider: "openai", "elevenlabs", "local"
provider = "openai"

# OpenAI TTS settings
model = "tts-1"
voice = "nova"  # alloy, echo, fable, onyx, nova, shimmer

# ElevenLabs settings
elevenlabs_api_key = "your_key_here"
elevenlabs_model = "eleven_turbo_v2_5"
elevenlabs_voice_id = "your_voice_id"

[voice.audio]
# Audio capture settings
sample_rate = 16000  # 16kHz for Whisper
sample_size = 16     # 16-bit
channels = 1         # mono

# Microphone device (empty = default)
microphone_device = ""

# Noise reduction
noise_suppression = true
```

---

### 8.3 Key Bindings

```java
package com.minewright.ai.client;

import com.minewright.ai.voice.VoiceManager;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.ClientRegistry;
import org.lwjgl.glfw.GLFW;

public class VoiceKeyBindings {
    private static KeyMapping pushToTalk;
    private static KeyMapping voiceToggle;

    public static void register(VoiceManager voiceManager) {
        pushToTalk = new KeyMapping(
            "key.minewright.push_to_talk",
            GLFW.GLFW_KEY_V,
            "key.categories.minewright"
        );

        voiceToggle = new KeyMapping(
            "key.minewright.voice_toggle",
            GLFW.GLFW_KEY_B,
            "key.categories.minewright"
        );

        ClientRegistry.registerKeyBinding(pushToTalk);
        ClientRegistry.registerKeyBinding(voiceToggle);

        // Register input handler
        // ... (in client event handler)
    }

    public static boolean isPushToTalkPressed() {
        return pushToTalk.isDown();
    }
}
```

---

## 9. Performance & Optimization

### 9.1 Latency Optimization Strategies

**1. Streaming TTS**
```java
public interface StreamingTTS {
    // Generate audio in chunks
    CompletableFuture<List<byte[]>> synthesizeStreaming(String text);

    // Callback interface for streaming playback
    interface PlaybackCallback {
        void onAudioChunk(byte[] chunk);
        void onComplete();
    }
}
```

**2. Parallel Processing**
```java
// Start TTS while LLM is still generating
public CompletableFuture<Void> processVoiceCommand(String input) {
    // LLM processing
    CompletableFuture<String> llmFuture = llmClient.generateAsync(input);

    // When LLM completes, start TTS
    return llmFuture.thenCompose(response -> {
        // Start TTS immediately
        return tts.synthesizeAsync(response)
            .thenAccept(audio -> playAudio(audio));
    });
}
```

**3. Response Caching**
```java
public class TTSCache {
    private final Cache<String, byte[]> cache = Caffeine.newBuilder()
        .maximumSize(100)
        .expireAfterAccess(Duration.ofHours(1))
        .build();

    public byte[] getCachedOrGenerate(String text) {
        return cache.get(text, t -> ttsService.synthesize(t));
    }
}
```

---

### 9.2 Memory Management

**Audio Buffering:**
```java
public class AudioBuffer {
    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    private final int maxSize;

    public AudioBuffer(int maxSizeBytes) {
        this.maxSize = maxSizeBytes;
    }

    public void write(byte[] data) {
        if (buffer.size() + data.length > maxSize) {
            // Clear old data or throw exception
            buffer.reset();
        }
        buffer.writeBytes(data);
    }

    public byte[] toByteArray() {
        return buffer.toByteArray();
    }
}
```

---

### 9.3 Thread Pool Management

```java
public class VoiceExecutorService {
    private final ExecutorService captureExecutor;
    private final ExecutorService sttExecutor;
    private final ExecutorService ttsExecutor;

    public VoiceExecutorService() {
        // Single thread for capture (audio requires timing precision)
        captureExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "VoiceCapture");
            t.setPriority(Thread.MAX_PRIORITY);
            return t;
        });

        // Multiple threads for STT (network I/O bound)
        sttExecutor = Executors.newFixedThreadPool(2, r ->
            new Thread(r, "WhisperSTT"));

        // Single thread for TTS (API rate limits)
        ttsExecutor = Executors.newSingleThreadExecutor(r ->
            new Thread(r, "OpenAITTS"));
    }
}
```

---

## 10. Security Considerations

### 10.1 API Key Management

**Never hardcode API keys:**

```java
// BAD
private static final String API_KEY = "sk-...";

// GOOD
private final String apiKey = VoiceConfig.OPENAI_API_KEY.get();
```

**Use environment variables or config file:**

```toml
# config/minewright-common.toml
[voice.stt]
api_key_env = "OPENAI_API_KEY"  # Read from environment variable
```

---

### 10.2 Privacy Considerations

**Data Privacy:**
- Inform users when audio is being recorded
- Store transcripts locally only (configurable)
- Provide option to disable voice features
- Clear privacy policy for API usage

**Audio Data:**
- Don't store raw audio (unless user opts in)
- Delete temporary audio files after processing
- Encrypt audio in transit (HTTPS)

---

### 10.3 Rate Limiting

**Prevent API abuse:**

```java
public class RateLimitedTTS {
    private final RateLimiter rateLimiter;

    public RateLimitedTTS() {
        // Limit to 3 requests per second
        this.rateLimiter = RateLimiter.create(3.0);
    }

    public byte[] synthesize(String text) {
        // Wait for permit
        rateLimiter.acquire();
        return ttsService.synthesize(text);
    }
}
```

---

## 11. Cost Estimation

### 11.1 OpenAI API Costs

**Whisper STT:**
- $0.006 per 10 minutes
- Average command: 5 seconds
- Cost per command: $0.00005 (0.5 cents)
- 1000 commands: $0.05

**TTS:**
- $15 per 1M characters
- Average response: 100 characters
- Cost per response: $0.0015 (0.15 cents)
- 1000 responses: $1.50

**Monthly Estimates (1000 voice conversations):**
- STT: $0.05
- TTS: $1.50
- **Total: ~$1.55/month**

### 11.2 ElevenLabs TTS Costs

**Starter Plan:** $5/month
- 30,000 characters included
- $0.30 per 1K characters after

**Quality/Tier Comparison:**
| Plan | Characters | Cost |
|------|------------|------|
| Free | 10,000 | $0 (limited quality) |
| Starter | 30,000 | $5/month |
| Creator | 100,000 | $22/month |
| Pro | 500,000 | $99/month |

---

## 12. Dependencies

### 12.1 Maven Dependencies

```xml
<dependencies>
    <!-- Whisper JNI (local STT) -->
    <dependency>
        <groupId>io.github.givimad</groupId>
        <artifactId>whisper-jni</artifactId>
        <version>1.7.1</version>
    </dependency>

    <!-- ONNX Runtime (for Silero VAD) -->
    <dependency>
        <groupId>com.microsoft.onnxruntime</groupId>
        <artifactId>onnxruntime</artifactId>
        <version>1.17.0</version>
    </dependency>

    <!-- OkHttp (for API calls) -->
    <dependency>
        <groupId>com.squareup.okhttp3</groupId>
        <artifactId>okhttp</artifactId>
        <version>4.12.0</version>
    </dependency>

    <!-- Caffeine (caching) -->
    <dependency>
        <groupId>com.github.ben-manes.caffeine</groupId>
        <artifactId>caffeine</artifactId>
        <version>3.1.8</version>
    </dependency>

    <!-- Resilience4j (retry, circuit breaker) -->
    <dependency>
        <groupId>io.github.resilience4j</groupId>
        <artifactId>resilience4j-retry</artifactId>
        <version>2.1.0</version>
    </dependency>

    <!-- TarsosDSP (audio processing, optional) -->
    <dependency>
        <groupId>be.tarsos.dsp</groupId>
        <artifactId>core</artifactId>
        <version>2.5</version>
    </dependency>
</dependencies>
```

---

## 13. Testing Strategy

### 13.1 Unit Testing

```java
@Test
public void testAudioCapture() throws LineUnavailableException {
    AudioCapture capture = new AudioCapture(config);
    capture.startCapture();

    byte[] audio = capture.captureAudio(1000); // 1 second

    assertNotNull(audio);
    assertTrue(audio.length > 0);

    capture.stopCapture();
}

@Test
public void testVAD() {
    SimpleVAD vad = new SimpleVAD(0.5);

    // Silence
    byte[] silence = generateSilence(1000);
    assertFalse(vad.isSpeech(silence));

    // Speech
    byte[] speech = generateSpeech(1000);
    assertTrue(vad.isSpeech(speech));
}
```

### 13.2 Integration Testing

```java
@Test
public void testVoiceToEndToEnd() throws Exception {
    // Record test audio
    byte[] testAudio = loadTestAudio();

    // Transcribe
    String transcript = sttService.transcribe(testAudio);
    assertEquals("hello minewright", transcript.toLowerCase());

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

## 14. Troubleshooting

### 14.1 Common Issues

**Issue: Microphone not detected**
- Solution: Run `AudioCapture.listMicrophones()` and verify device name in config

**Issue: Poor transcription accuracy**
- Solution: Use closer microphone position, reduce background noise, or upgrade to larger Whisper model

**Issue: High latency**
- Solution: Use local Whisper, enable streaming TTS, or reduce audio buffer size

**Issue: TTS sounds robotic**
- Solution: Switch to ElevenLabs or use different OpenAI voice

**Issue: Audio crackling**
- Solution: Increase audio buffer size, check sample rate mismatch

---

## 15. Future Enhancements

### 15.1 Advanced Features

1. **Voice Cloning**
   - Record user's voice for 30 seconds
   - Create custom ElevenLabs voice
   - MineWright responds with user's voice

2. **Emotional Speech**
   - Detect sentiment in LLM response
   - Adjust TTS pitch/speed accordingly
   - Happy = faster/higher, Sad = slower/lower

3. **Multi-Language Support**
   - Auto-detect player language
   - Switch MineWright's language dynamically
   - Translation between languages

4. **Voice Commands for Actions**
   - "MineWright, build a house"
   - "MineWright, come here"
   - "MineWright, follow me"

5. **Ambient Dialogue**
   - MineWright comments on environment
   - Small talk while idle
   - Reactive to game events

---

## 16. Conclusion

### Recommended Implementation Priority

1. **Start Simple:**
   - Java Sound API for capture
   - OpenAI Whisper API for STT
   - OpenAI TTS API for speech
   - Push-to-talk activation

2. **Add Polish:**
   - Silero VAD for voice activation
   - Positional 3D audio
   - Multiple voice profiles
   - UI feedback

3. **Optimize:**
   - Local Whisper for lower latency
   - Streaming TTS for faster response
   - Response caching
   - Thread pool optimization

4. **Enhance:**
   - ElevenLabs for premium voices
   - Voice cloning
   - Emotional speech
   - Multi-language support

---

## 17. References & Resources

### Documentation
- [OpenAI Whisper API](https://platform.openai.com/docs/guides/speech-to-text)
- [OpenAI TTS API](https://platform.openai.com/docs/guides/text-to-speech)
- [ElevenLabs Documentation](https://elevenlabs.io/docs)
- [whisper.cpp GitHub](https://github.com/ggerganov/whisper.cpp)
- [Minecraft Forge Documentation](https://docs.minecraftforge.net/)

### Java Libraries
- [whisper-jni](https://github.com/givimad/whisper-jni)
- [Spring AI](https://docs.spring.io/spring-ai/reference/)
- [TarsosDSP](https://github.com/JorenSix/TarsosDSP)

### Existing Mods
- [Simple Voice Chat](https://github.com/henkelmax/simple-voice-chat)
- [Plasmo Voice](https://github.com/Plasmo-voice/plasmo-voice)

### Community
- [Minecraft Modding Discord](https://discord.gg/minecraft-modding)
- [Forge Forums](https://forums.minecraftforge.net/)

---

**Document Version:** 1.0
**Last Updated:** 2025-02-26
**Status:** Research Complete


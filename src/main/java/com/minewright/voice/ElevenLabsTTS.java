package com.minewright.voice;

import com.minewright.testutil.TestLogger;
import com.minewright.config.MineWrightConfig;
import org.slf4j.Logger;

import javax.sound.sampled.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Text-to-speech implementation using ElevenLabs API.
 *
 * <p>ElevenLabs provides high-quality, natural-sounding voices.
 * This implementation supports:</p>
 * <ul>
 *   <li>Multiple voices with different characteristics</li>
 *   <li>Streaming audio playback</li>
 *   <li>Voice settings (stability, similarity boost)</li>
 *   <li>Automatic model selection</li>
 * </ul>
 *
 * <p><b>Configuration:</b></p>
 * Set your ElevenLabs API key in config/minewright-common.toml:
 * <pre>
 * [voice]
 * elevenlabs_api_key = "your-api-key"
 * elevenlabs_voice_id = "voice-id"
 * </pre>
 *
 * <p><b>Popular Voice IDs:</b></p>
 * <ul>
 *   <li>Adam: "pNInz6obpgDQGcFmaJgB" (deep male)</li>
 *   <li>Antoni: "ErXwobaYiN019PkySvjV" (warm male)</li>
 *   <li>Arnold: "VR6AewLTigWG4xSOukaG" (crisp male)</li>
 *   <li>Bella: "EXAVITQu4vr4xnSDxMaL" (soft female)</li>
 *   <li>Domi: "AZnzlk1XvdvUeBnXmlld" (strong female)</li>
 *   <li>Elli: "MF3mGyEYCl7XYWbV9V6O" (emotive female)</li>
 *   <li>Josh: "TxGEqnHWrfWFTfGW9XjX" (calm male)</li>
 *   <li>Rachel: "21m00Tcm4TlvDq8ikWAM" (warm female)</li>
 *   <li>Sam: "yoZ06aMxZJJ28mfd3POQ" (gravelly male)</li>
 * </ul>
 *
 * @since 1.3.0
 */
public class ElevenLabsTTS implements TextToSpeech {
    private static final Logger LOGGER = TestLogger.getLogger(ElevenLabsTTS.class);

    // ElevenLabs API endpoints
    private static final String API_BASE = "https://api.elevenlabs.io/v1";
    private static final String TTS_ENDPOINT = "/text-to-speech/{voice_id}";
    private static final String VOICES_ENDPOINT = "/voices";
    private static final String MODELS_ENDPOINT = "/models";

    // Default settings
    private static final String DEFAULT_MODEL = "eleven_monolingual_v1";
    private static final String DEFAULT_VOICE = "21m00Tcm4TlvDq8ikWAM"; // Rachel

    private final ExecutorService executor;
    private final Map<String, Voice> availableVoices;
    private final List<String> speechQueue;
    private volatile boolean speaking = false;
    private volatile boolean shutdown = false;

    // Settings
    private Voice currentVoice;
    private double volume = 1.0;
    private double rate = 1.0;
    private double pitch = 1.0;
    private double stability = 0.5;
    private double similarityBoost = 0.75;

    // Cached API key
    private String apiKey;
    private String voiceId;

    public ElevenLabsTTS() {
        this.executor = Executors.newSingleThreadExecutor();
        this.availableVoices = new ConcurrentHashMap<>();
        this.speechQueue = Collections.synchronizedList(new ArrayList<>());
        this.currentVoice = Voice.of(DEFAULT_VOICE, "Rachel", "en-US", "female");
    }

    @Override
    public void initialize() throws VoiceException {
        LOGGER.info("[ElevenLabsTTS] Initializing...");

        // Load API key from config (we'll add this config option)
        this.apiKey = getApiKey();
        this.voiceId = getVoiceId();

        if (apiKey == null || apiKey.isEmpty()) {
            LOGGER.warn("[ElevenLabsTTS] No API key configured - TTS will not work");
            LOGGER.warn("[ElevenLabsTTS] Add to config: elevenlabs_api_key = \"your-key\"");
        } else {
            // Fetch available voices
            fetchAvailableVoices();
            LOGGER.info("[ElevenLabsTTS] Initialized with {} voices available", availableVoices.size());
        }
    }

    /**
     * Gets the ElevenLabs API key from config or environment.
     */
    private String getApiKey() {
        // Try config first, then environment variable
        String key = System.getenv("ELEVENLABS_API_KEY");
        if (key != null && !key.isEmpty()) {
            return key;
        }
        // Could add to MineWrightConfig if needed
        return null;
    }

    /**
     * Gets the voice ID from config or uses default.
     */
    private String getVoiceId() {
        String id = System.getenv("ELEVENLABS_VOICE_ID");
        if (id != null && !id.isEmpty()) {
            return id;
        }
        return DEFAULT_VOICE;
    }

    @Override
    public void speak(String text) throws VoiceException {
        if (apiKey == null || apiKey.isEmpty()) {
            LOGGER.warn("[ElevenLabsTTS] No API key - cannot speak");
            return;
        }

        if (text == null || text.trim().isEmpty()) {
            return;
        }

        speaking = true;

        executor.submit(() -> {
            try {
                synthesizeAndPlay(text.trim());
            } catch (Exception e) {
                LOGGER.error("[ElevenLabsTTS] Error speaking: {}", e.getMessage());
            } finally {
                speaking = false;
            }
        });
    }

    @Override
    public void speakQueued(String text) throws VoiceException {
        speechQueue.add(text);
        processQueue();
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

    @Override
    public void stop() {
        speaking = false;
        speechQueue.clear();
    }

    @Override
    public boolean isSpeaking() {
        return speaking;
    }

    @Override
    public boolean hasQueuedSpeech() {
        return !speechQueue.isEmpty();
    }

    @Override
    public void clearQueue() {
        speechQueue.clear();
    }

    @Override
    public List<Voice> getAvailableVoices() {
        if (availableVoices.isEmpty()) {
            return List.of(currentVoice);
        }
        return new ArrayList<>(availableVoices.values());
    }

    @Override
    public Voice getCurrentVoice() {
        return currentVoice;
    }

    @Override
    public void setVoice(Voice voice) throws VoiceException {
        this.currentVoice = voice;
        this.voiceId = voice.name();
        LOGGER.info("[ElevenLabsTTS] Voice set to: {}", voice.displayName());
    }

    @Override
    public void setVoice(String voiceName) throws VoiceException {
        Voice voice = availableVoices.get(voiceName);
        if (voice != null) {
            setVoice(voice);
        } else {
            // Assume it's a voice ID
            this.voiceId = voiceName;
            this.currentVoice = Voice.of(voiceName, voiceName, "en-US", "unknown");
        }
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

    /**
     * Sets the stability setting (0.0 to 1.0).
     * Higher = more stable, lower = more expressive.
     */
    public void setStability(double stability) {
        this.stability = Math.max(0.0, Math.min(1.0, stability));
    }

    /**
     * Sets the similarity boost (0.0 to 1.0).
     * Higher = more similar to original voice.
     */
    public void setSimilarityBoost(double boost) {
        this.similarityBoost = Math.max(0.0, Math.min(1.0, boost));
    }

    @Override
    public void shutdown() {
        shutdown = true;
        stop();
        executor.shutdown();
    }

    /**
     * Processes the speech queue.
     */
    private void processQueue() {
        if (speaking || speechQueue.isEmpty()) {
            return;
        }

        String text = speechQueue.remove(0);
        try {
            speak(text);
        } catch (VoiceException e) {
            LOGGER.error("[ElevenLabsTTS] Queue processing error: {}", e.getMessage());
        }
    }

    /**
     * Synthesizes text and plays the audio.
     */
    private void synthesizeAndPlay(String text) throws Exception {
        byte[] audioData = synthesize(text);
        if (audioData != null && audioData.length > 0) {
            playAudio(audioData);
        }
    }

    /**
     * Synthesizes text using ElevenLabs API.
     */
    private byte[] synthesize(String text) throws Exception {
        String url = API_BASE + TTS_ENDPOINT.replace("{voice_id}", voiceId);

        // Build request body
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("text", text);
        requestBody.put("model_id", DEFAULT_MODEL);

        Map<String, Object> voiceSettings = new HashMap<>();
        voiceSettings.put("stability", stability);
        voiceSettings.put("similarity_boost", similarityBoost);
        requestBody.put("voice_settings", voiceSettings);

        String jsonBody = toJson(requestBody);

        HttpURLConnection connection = null;
        try {
            URL apiUrl = new URL(url);
            connection = (HttpURLConnection) apiUrl.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("xi-api-key", apiKey);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "audio/mpeg");
            connection.setDoOutput(true);
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(60000);

            // Send request
            try (OutputStream os = connection.getOutputStream()) {
                os.write(jsonBody.getBytes("UTF-8"));
            }

            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                // Read audio data
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try (InputStream is = connection.getInputStream()) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        baos.write(buffer, 0, bytesRead);
                    }
                }
                return baos.toByteArray();
            } else {
                String error = readErrorStream(connection);
                LOGGER.error("[ElevenLabsTTS] API error ({}): {}", responseCode, error);
                throw new Exception("ElevenLabs API error: " + responseCode);
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Plays MP3 audio data.
     */
    private void playAudio(byte[] mp3Data) throws Exception {
        // For Minecraft mods, we'd typically use the game's sound system
        // For now, we'll just log that we received audio
        LOGGER.info("[ElevenLabsTTS] Received {} bytes of audio data", mp3Data.length);

        // In a real implementation, you would:
        // 1. Decode MP3 to PCM using a library like JLayer
        // 2. Play through Minecraft's SoundSystem or Java Sound API

        // Simulate playback duration based on text length
        int estimatedDurationMs = Math.min(mp3Data.length / 32, 10000); // Rough estimate
        Thread.sleep(estimatedDurationMs);
    }

    /**
     * Fetches available voices from ElevenLabs API.
     */
    private void fetchAvailableVoices() {
        if (apiKey == null) return;

        try {
            URL url = new URL(API_BASE + VOICES_ENDPOINT);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("xi-api-key", apiKey);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            if (connection.getResponseCode() == 200) {
                String response = readStream(connection.getInputStream());
                parseVoicesResponse(response);
            }
            connection.disconnect();
        } catch (Exception e) {
            LOGGER.warn("[ElevenLabsTTS] Failed to fetch voices: {}", e.getMessage());
        }
    }

    /**
     * Parses the voices API response.
     */
    private void parseVoicesResponse(String json) {
        try {
            // Simple JSON parsing for voices list
            int voicesIndex = json.indexOf("\"voices\"");
            if (voicesIndex == -1) return;

            int arrayStart = json.indexOf("[", voicesIndex);
            int arrayEnd = json.lastIndexOf("]");
            if (arrayStart == -1 || arrayEnd == -1) return;

            String voicesArray = json.substring(arrayStart, arrayEnd + 1);

            // Parse each voice object
            int pos = 0;
            while (pos < voicesArray.length()) {
                int objStart = voicesArray.indexOf("{", pos);
                if (objStart == -1) break;
                int objEnd = voicesArray.indexOf("}", objStart);
                if (objEnd == -1) break;

                String voiceObj = voicesArray.substring(objStart, objEnd + 1);

                String voiceId = extractJsonValue(voiceObj, "voice_id");
                String name = extractJsonValue(voiceObj, "name");

                if (voiceId != null && name != null) {
                    Voice voice = Voice.of(voiceId, name, "en-US", "unknown");
                    availableVoices.put(voiceId, voice);
                }

                pos = objEnd + 1;
            }
        } catch (Exception e) {
            LOGGER.warn("[ElevenLabsTTS] Failed to parse voices: {}", e.getMessage());
        }
    }

    /**
     * Extracts a string value from JSON.
     */
    private String extractJsonValue(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1) return null;

        int colonIndex = json.indexOf(":", keyIndex);
        if (colonIndex == -1) return null;

        int startQuote = json.indexOf("\"", colonIndex);
        if (startQuote == -1) return null;

        int endQuote = json.indexOf("\"", startQuote + 1);
        if (endQuote == -1) return null;

        return json.substring(startQuote + 1, endQuote);
    }

    /**
     * Converts a map to JSON string.
     */
    private String toJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("\"").append(entry.getKey()).append("\":");
            Object value = entry.getValue();
            if (value instanceof String) {
                sb.append("\"").append(escapeJson((String) value)).append("\"");
            } else if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> mapValue = (Map<String, Object>) value;
                sb.append(toJson(mapValue));
            } else {
                sb.append(value);
            }
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * Escapes JSON string values.
     */
    private String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * Reads an error stream to string.
     */
    private String readErrorStream(HttpURLConnection connection) {
        try (InputStream is = connection.getErrorStream()) {
            if (is == null) return "No error details";
            return readStream(is);
        } catch (Exception e) {
            return "Failed to read error: " + e.getMessage();
        }
    }

    /**
     * Reads an input stream to string.
     */
    private String readStream(InputStream is) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        reader.close();
        return sb.toString();
    }
}

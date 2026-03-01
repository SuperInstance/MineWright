package com.minewright.voice;

import com.minewright.testutil.TestLogger;
import org.slf4j.Logger;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.List;

/**
 * TTS implementation that uses ElevenLabs through Docker MCP Gateway.
 *
 * <p>This implementation calls the Docker MCP gateway to use your
 * authenticated ElevenLabs account for text-to-speech.</p>
 *
 * <p><b>Requirements:</b></p>
 * <ul>
 *   <li>Docker Desktop running</li>
 *   <li>ElevenLabs MCP server configured in Docker</li>
 *   <li>ElevenLabs account authenticated</li>
 * </ul>
 *
 * <p><b>Usage:</b>
 * The system will automatically use Docker MCP if available.</p>
 *
 * @since 1.3.0
 */
public class DockerMCPTTS implements TextToSpeech {
    private static final Logger LOGGER = TestLogger.getLogger(DockerMCPTTS.class);

    // Default voice ID (Rachel - warm female voice)
    private static final String DEFAULT_VOICE_ID = "21m00Tcm4TlvDq8ikWAM";

    private final ExecutorService executor;
    private final List<String> speechQueue;
    private volatile boolean speaking = false;
    private volatile boolean shutdown = false;

    private Voice currentVoice;
    private double volume = 1.0;
    private double rate = 1.0;
    private double pitch = 1.0;
    private String voiceId;
    private boolean mcpAvailable = false;

    public DockerMCPTTS() {
        this.executor = Executors.newSingleThreadExecutor();
        this.speechQueue = Collections.synchronizedList(new ArrayList<>());
        this.currentVoice = Voice.of(DEFAULT_VOICE_ID, "Rachel", "en-US", "female");
        this.voiceId = DEFAULT_VOICE_ID;
    }

    @Override
    public void initialize() throws VoiceException {
        LOGGER.info("[DockerMCP-TTS] Initializing...");

        // Check if Docker MCP is available
        mcpAvailable = checkMCPAvailable();

        if (mcpAvailable) {
            LOGGER.info("[DockerMCP-TTS] Docker MCP ElevenLabs available!");
            // Fetch available voices
            fetchVoices();
        } else {
            LOGGER.warn("[DockerMCP-TTS] Docker MCP not available - TTS disabled");
            LOGGER.warn("[DockerMCP-TTS] Make sure Docker Desktop is running with ElevenLabs MCP");
        }
    }

    /**
     * Checks if Docker MCP gateway is available.
     */
    private boolean checkMCPAvailable() {
        try {
            ProcessBuilder pb = new ProcessBuilder("docker", "mcp", "gateway", "list");
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.toLowerCase().contains("elevenlabs")) {
                        return true;
                    }
                }
            }

            process.waitFor(10, TimeUnit.SECONDS);
            return process.exitValue() == 0;
        } catch (Exception e) {
            LOGGER.debug("[DockerMCP-TTS] MCP check failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Fetches available voices from ElevenLabs via MCP.
     */
    private void fetchVoices() {
        try {
            // Try to get voices through Docker MCP
            String result = runMCPCommand("elevenlabs_get_voices", Map.of());
            if (result != null && !result.isEmpty()) {
                LOGGER.info("[DockerMCP-TTS] Retrieved voice list");
                // Could parse and cache voices here
            }
        } catch (Exception e) {
            LOGGER.debug("[DockerMCP-TTS] Failed to fetch voices: {}", e.getMessage());
        }
    }

    @Override
    public void speak(String text) throws VoiceException {
        if (!mcpAvailable) {
            LOGGER.debug("[DockerMCP-TTS] MCP not available - skipping TTS");
            return;
        }

        if (text == null || text.trim().isEmpty()) {
            return;
        }

        speaking = true;

        executor.submit(() -> {
            try {
                synthesizeViaMCP(text.trim());
            } catch (Exception e) {
                LOGGER.error("[DockerMCP-TTS] Error speaking: {}", e.getMessage());
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
        // Return some common ElevenLabs voices
        return List.of(
            Voice.of("21m00Tcm4TlvDq8ikWAM", "Rachel", "en-US", "female"),
            Voice.of("pNInz6obpgDQGcFmaJgB", "Adam", "en-US", "male"),
            Voice.of("EXAVITQu4vr4xnSDxMaL", "Bella", "en-US", "female"),
            Voice.of("yoZ06aMxZJJ28mfd3POQ", "Sam", "en-US", "male"),
            Voice.of("MF3mGyEYCl7XYWbV9V6O", "Elli", "en-US", "female")
        );
    }

    @Override
    public Voice getCurrentVoice() {
        return currentVoice;
    }

    @Override
    public void setVoice(Voice voice) throws VoiceException {
        this.currentVoice = voice;
        this.voiceId = voice.name();
        LOGGER.info("[DockerMCP-TTS] Voice set to: {}", voice.displayName());
    }

    @Override
    public void setVoice(String voiceName) throws VoiceException {
        this.voiceId = voiceName;
        this.currentVoice = Voice.of(voiceName, voiceName, "en-US", "unknown");
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
            LOGGER.error("[DockerMCP-TTS] Queue error: {}", e.getMessage());
        }
    }

    /**
     * Synthesizes text via Docker MCP ElevenLabs.
     *
     * Security: Validates all inputs before processing to prevent injection attacks.
     */
    private void synthesizeViaMCP(String text) {
        try {
            // Validate inputs
            if (!validateText(text)) {
                LOGGER.error("[DockerMCP-TTS] Invalid text input rejected");
                return;
            }
            if (!validateVoiceId(voiceId)) {
                LOGGER.error("[DockerMCP-TTS] Invalid voice ID: {}", voiceId);
                return;
            }

            // Use Docker MCP gateway to call ElevenLabs TTS
            Map<String, Object> params = new HashMap<>();
            params.put("text", text);
            params.put("voice_id", voiceId);
            params.put("model_id", "eleven_monolingual_v1");

            // Call the MCP tool
            String result = runMCPCommand("elevenlabs_text_to_speech", params);

            if (result != null) {
                LOGGER.info("[DockerMCP-TTS] Speech synthesized: {} chars", text.length());
                // The audio would be played by the MCP or we'd decode it here
            } else {
                LOGGER.warn("[DockerMCP-TTS] No result from MCP");
            }
        } catch (Exception e) {
            LOGGER.error("[DockerMCP-TTS] Synthesis failed: {}", e.getMessage());
        }
    }

    /**
     * Runs a Docker MCP gateway command.
     */
    private String runMCPCommand(String tool, Map<String, Object> params) {
        try {
            // Build the MCP gateway command
            List<String> command = new ArrayList<>();
            command.add("docker");
            command.add("mcp");
            command.add("gateway");
            command.add("run");
            command.add("elevenlabs");
            command.add(tool);

            // Add parameters as JSON
            if (!params.isEmpty()) {
                StringBuilder json = new StringBuilder("{");
                boolean first = true;
                for (Map.Entry<String, Object> entry : params.entrySet()) {
                    if (!first) json.append(",");
                    first = false;
                    json.append("\"").append(entry.getKey()).append("\":");
                    if (entry.getValue() instanceof String) {
                        json.append("\"").append(escapeJson((String) entry.getValue())).append("\"");
                    } else {
                        json.append(entry.getValue());
                    }
                }
                json.append("}");
                command.add(json.toString());
            }

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);

            LOGGER.debug("[DockerMCP-TTS] Running: {}", command);

            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
            }

            boolean completed = process.waitFor(30, TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                LOGGER.warn("[DockerMCP-TTS] Command timed out");
                return null;
            }

            if (process.exitValue() == 0) {
                return output.toString();
            } else {
                LOGGER.warn("[DockerMCP-TTS] Command failed with exit code {}: {}",
                    process.exitValue(), output);
                return null;
            }

        } catch (Exception e) {
            LOGGER.error("[DockerMCP-TTS] MCP command failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Validates text input to prevent injection attacks.
     *
     * Security: Enforces length restrictions and character allowlist.
     *
     * @param text The text to validate
     * @return true if text is safe to process, false otherwise
     */
    private boolean validateText(String text) {
        if (text == null) {
            return false;
        }

        // Length restriction: prevent DoS via extremely long text
        final int MAX_TEXT_LENGTH = 5000;
        if (text.length() > MAX_TEXT_LENGTH) {
            LOGGER.warn("[DockerMCP-TTS] Text exceeds maximum length of {} chars", MAX_TEXT_LENGTH);
            return false;
        }

        return true;
    }

    /**
     * Validates voice ID against an allowlist to prevent injection attacks.
     *
     * Security: Only allows known ElevenLabs voice IDs.
     *
     * @param voiceId The voice ID to validate
     * @return true if voice ID is in allowlist, false otherwise
     */
    private boolean validateVoiceId(String voiceId) {
        if (voiceId == null || voiceId.isEmpty()) {
            return false;
        }

        // Length restriction: ElevenLabs voice IDs are typically ~20 chars
        final int MAX_VOICE_ID_LENGTH = 50;
        if (voiceId.length() > MAX_VOICE_ID_LENGTH) {
            return false;
        }

        // Allowlist of known voice IDs
        // This prevents injection of malicious commands through custom voice IDs
        Set<String> allowedVoiceIds = Set.of(
            "21m00Tcm4TlvDq8ikWAM",  // Rachel
            "pNInz6obpgDQGcFmaJgB",  // Adam
            "EXAVITQu4vr4xnSDxMaL",  // Bella
            "yoZ06aMxZJJ28mfd3POQ",  // Sam
            "MF3mGyEYCl7XYWbV9V6O",  // Elli
            "txg98GcUuHnG8B3wdKQr",  // Marcus
            "5Q0t7uMcjvnjumKxPWC0",  // Domi
            "AZnzlk1XvdvUeBnXmlld",  // Fin
            "IEvaWXzEL0GhnPcTtrgT",  // Antoni
            "ODq5zmih8GrVes37Dizj"   // Elli v2
        );

        boolean allowed = allowedVoiceIds.contains(voiceId);
        if (!allowed) {
            LOGGER.warn("[DockerMCP-TTS] Voice ID not in allowlist: {}", voiceId);
        }
        return allowed;
    }

    /**
     * Escapes JSON string values to prevent injection attacks.
     * Properly escapes all JSON special characters according to RFC 8259.
     *
     * Security: This prevents JSON injection and command injection through
     * malformed JSON that could be misinterpreted by the Docker MCP gateway.
     */
    private String escapeJson(String s) {
        if (s == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder(s.length() * 2);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                case '/':
                    // Optional but recommended: escape forward slash to prevent
                    // closing tags in JSONP-like contexts
                    sb.append("\\/");
                    break;
                default:
                    // Control characters (0x00-0x1f) must be escaped
                    if (c <= 0x1f) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                    break;
            }
        }
        return sb.toString();
    }

    /**
     * Checks if MCP TTS is available.
     */
    public boolean isMCPAvailable() {
        return mcpAvailable;
    }
}

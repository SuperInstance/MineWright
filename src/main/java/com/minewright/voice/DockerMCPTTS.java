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
     */
    private void synthesizeViaMCP(String text) {
        try {
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
     * Checks if MCP TTS is available.
     */
    public boolean isMCPAvailable() {
        return mcpAvailable;
    }
}

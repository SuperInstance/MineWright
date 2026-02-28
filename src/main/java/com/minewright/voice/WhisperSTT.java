package com.minewright.voice;

import com.minewright.testutil.TestLogger;
import com.minewright.config.MineWrightConfig;
import org.slf4j.Logger;

import javax.sound.sampled.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Speech-to-text implementation using OpenAI's Whisper API.
 *
 * <p>This implementation captures audio from the microphone and sends it
 * to OpenAI's Whisper API for transcription.</p>
 *
 * <p><b>Configuration:</b></p>
 * <ul>
 *   <li>Uses OPENAI_API_KEY from MineWrightConfig</li>
 *   <li>Can be configured for different Whisper models</li>
 *   <li>Supports multiple languages</li>
 * </ul>
 *
 * <p><b>Audio Format:</b></p>
 * <ul>
 *   <li>16kHz sample rate (Whisper optimal)</li>
 *   <li>16-bit PCM</li>
 *   <li>Mono channel</li>
 * </ul>
 *
 * @since 1.2.0
 */
public class WhisperSTT implements SpeechToText {
    private static final Logger LOGGER = TestLogger.getLogger(WhisperSTT.class);

    // OpenAI Whisper API endpoint
    private static final String WHISPER_API_URL = "https://api.openai.com/v1/audio/transcriptions";
    private static final String WHISPER_MODEL = "whisper-1";

    // Audio configuration
    private static final AudioFormat AUDIO_FORMAT = new AudioFormat(
        16000.0f,  // 16kHz sample rate (Whisper optimal)
        16,        // 16-bit samples
        1,         // Mono
        true,      // Signed
        false      // Little-endian
    );

    private static final int MAX_RECORDING_MS = 60000;  // Max 60 seconds per request
    private static final int SILENCE_THRESHOLD = 500;    // Silence detection threshold
    private static final int SILENCE_DURATION_MS = 1500; // Stop after 1.5s silence

    private final ExecutorService executor;
    private final AtomicBoolean listening;
    private final AtomicBoolean cancelled;

    private TargetDataLine microphone;
    private String language = "en";
    private double sensitivity = 0.5;
    private Consumer<String> resultConsumer;

    public WhisperSTT() {
        this.executor = Executors.newSingleThreadExecutor();
        this.listening = new AtomicBoolean(false);
        this.cancelled = new AtomicBoolean(false);
    }

    @Override
    public void initialize() throws VoiceException {
        LOGGER.info("[WhisperSTT] Initializing speech-to-text...");

        // Check for API key
        String apiKey = MineWrightConfig.OPENAI_API_KEY.get();
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("your-api-key-here")) {
            LOGGER.warn("[WhisperSTT] No valid OpenAI API key configured - STT will not work");
            LOGGER.warn("[WhisperSTT] Set OPENAI_API_KEY in config/minewright-common.toml");
        }

        // Check microphone availability
        try {
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, AUDIO_FORMAT);
            if (!AudioSystem.isLineSupported(info)) {
                throw new VoiceException("Microphone not available or format not supported");
            }
            LOGGER.info("[WhisperSTT] Microphone detected and available");
        } catch (Exception e) {
            throw new VoiceException("Failed to initialize microphone: " + e.getMessage(), e);
        }

        LOGGER.info("[WhisperSTT] Speech-to-text initialized successfully");
    }

    @Override
    public void startListening(Consumer<String> resultConsumer) throws VoiceException {
        if (listening.get()) {
            throw new VoiceException("Already listening");
        }

        this.resultConsumer = resultConsumer;
        listening.set(true);
        cancelled.set(false);

        executor.submit(() -> {
            try {
                recordAndTranscribe();
            } catch (Exception e) {
                LOGGER.error("[WhisperSTT] Error during recording/transcription", e);
                if (!cancelled.get() && resultConsumer != null) {
                    resultConsumer.accept("");  // Empty result on error
                }
            } finally {
                listening.set(false);
            }
        });
    }

    @Override
    public void stopListening() {
        LOGGER.debug("[WhisperSTT] Stopping listening");
        listening.set(false);

        if (microphone != null) {
            microphone.stop();
            microphone.close();
            microphone = null;
        }
    }

    @Override
    public boolean isListening() {
        return listening.get();
    }

    @Override
    public CompletableFuture<String> listenOnce() throws VoiceException {
        CompletableFuture<String> future = new CompletableFuture<>();

        startListening(result -> {
            if (result != null && !result.isEmpty()) {
                future.complete(result);
            } else {
                future.complete("");
            }
        });

        return future;
    }

    @Override
    public void cancel() {
        cancelled.set(true);
        stopListening();
    }

    @Override
    public AudioFormat getPreferredAudioFormat() {
        return AUDIO_FORMAT;
    }

    @Override
    public String getLanguage() {
        return language;
    }

    @Override
    public void setLanguage(String language) {
        this.language = language;
        LOGGER.debug("[WhisperSTT] Language set to: {}", language);
    }

    @Override
    public double getSensitivity() {
        return sensitivity;
    }

    @Override
    public void setSensitivity(double sensitivity) {
        this.sensitivity = Math.max(0.0, Math.min(1.0, sensitivity));
        LOGGER.debug("[WhisperSTT] Sensitivity set to: {}", this.sensitivity);
    }

    @Override
    public void shutdown() {
        LOGGER.info("[WhisperSTT] Shutting down");
        stopListening();
        executor.shutdown();
    }

    /**
     * Records audio and sends it for transcription.
     */
    private void recordAndTranscribe() throws VoiceException {
        LOGGER.info("[WhisperSTT] Starting audio capture...");

        // Open microphone
        try {
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, AUDIO_FORMAT);
            microphone = (TargetDataLine) AudioSystem.getLine(info);
            microphone.open(AUDIO_FORMAT);
            microphone.start();
        } catch (LineUnavailableException e) {
            throw new VoiceException("Failed to open microphone: " + e.getMessage(), e);
        }

        // Record audio
        ByteArrayOutputStream audioBuffer = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        long startTime = System.currentTimeMillis();
        long lastSoundTime = startTime;
        int silenceThreshold = (int) (SILENCE_THRESHOLD * (1.0 - sensitivity * 0.5));

        LOGGER.info("[WhisperSTT] Recording... (speak now)");

        while (listening.get()) {
            int bytesRead = microphone.read(buffer, 0, buffer.length);
            if (bytesRead > 0) {
                audioBuffer.write(buffer, 0, bytesRead);

                // Check for sound level (simple RMS calculation)
                long sum = 0;
                for (int i = 0; i < bytesRead; i += 2) {
                    short sample = (short) ((buffer[i + 1] << 8) | (buffer[i] & 0xFF));
                    sum += sample * sample;
                }
                double rms = Math.sqrt(sum / (bytesRead / 2.0));

                // Track when we last heard sound
                if (rms > silenceThreshold) {
                    lastSoundTime = System.currentTimeMillis();
                }

                // Check for silence timeout (stop recording after silence)
                long elapsed = System.currentTimeMillis() - lastSoundTime;
                if (elapsed > SILENCE_DURATION_MS && audioBuffer.size() > 16000) {
                    LOGGER.info("[WhisperSTT] Silence detected, stopping recording");
                    break;
                }

                // Check for max recording time
                if (System.currentTimeMillis() - startTime > MAX_RECORDING_MS) {
                    LOGGER.info("[WhisperSTT] Max recording time reached");
                    break;
                }
            }
        }

        // Stop microphone
        if (microphone != null) {
            microphone.stop();
            microphone.close();
            microphone = null;
        }

        // If cancelled, don't transcribe
        if (cancelled.get()) {
            LOGGER.debug("[WhisperSTT] Recording cancelled");
            return;
        }

        // Send audio for transcription
        byte[] audioData = audioBuffer.toByteArray();
        if (audioData.length > 16000) {  // At least 0.5 seconds of audio
            LOGGER.info("[WhisperSTT] Sending {} bytes for transcription", audioData.length);
            String transcription = transcribeAudio(audioData);
            LOGGER.info("[WhisperSTT] Transcription: \"{}\"", transcription);

            if (resultConsumer != null && !cancelled.get()) {
                resultConsumer.accept(transcription);
            }
        } else {
            LOGGER.warn("[WhisperSTT] Recording too short, not transcribing");
            if (resultConsumer != null) {
                resultConsumer.accept("");
            }
        }
    }

    /**
     * Sends audio data to OpenAI Whisper API for transcription.
     */
    private String transcribeAudio(byte[] audioData) {
        String apiKey = MineWrightConfig.OPENAI_API_KEY.get();
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("your-api-key-here")) {
            LOGGER.error("[WhisperSTT] No API key configured");
            return "";
        }

        HttpURLConnection connection = null;
        try {
            // Create multipart form data
            String boundary = "----MineWrightBoundary" + System.currentTimeMillis();
            URL url = new URL(WHISPER_API_URL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(60000);

            // Build multipart body
            ByteArrayOutputStream bodyStream = new ByteArrayOutputStream();
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(bodyStream, "UTF-8"));

            // Model parameter
            writer.append("--").append(boundary).append("\r\n");
            writer.append("Content-Disposition: form-data; name=\"model\"\r\n\r\n");
            writer.append(WHISPER_MODEL).append("\r\n");

            // Language parameter
            writer.append("--").append(boundary).append("\r\n");
            writer.append("Content-Disposition: form-data; name=\"language\"\r\n\r\n");
            writer.append(language).append("\r\n");

            // Audio file
            writer.append("--").append(boundary).append("\r\n");
            writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"audio.wav\"\r\n");
            writer.append("Content-Type: audio/wav\r\n\r\n");
            writer.flush();

            // Write WAV header + audio data
            writeWavHeader(bodyStream, audioData.length);
            bodyStream.write(audioData);

            writer.append("\r\n--").append(boundary).append("--\r\n");
            writer.flush();
            writer.close();

            // Send request
            connection.getOutputStream().write(bodyStream.toByteArray());
            connection.getOutputStream().flush();

            // Read response
            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                String response = readStream(connection.getInputStream());
                // Parse JSON response: {"text": "transcribed text"}
                return parseTranscription(response);
            } else {
                String errorResponse = readStream(connection.getErrorStream());
                LOGGER.error("[WhisperSTT] API error ({}): {}", responseCode, errorResponse);
                return "";
            }
        } catch (Exception e) {
            LOGGER.error("[WhisperSTT] Transcription failed", e);
            return "";
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Writes a WAV file header to the output stream.
     */
    private void writeWavHeader(ByteArrayOutputStream out, int audioLength) throws IOException {
        int sampleRate = (int) AUDIO_FORMAT.getSampleRate();
        int channels = AUDIO_FORMAT.getChannels();
        int bitsPerSample = AUDIO_FORMAT.getSampleSizeInBits();
        int byteRate = sampleRate * channels * bitsPerSample / 8;
        int blockAlign = channels * bitsPerSample / 8;
        int totalLength = 36 + audioLength;

        // RIFF header
        out.write("RIFF".getBytes());
        out.write(intToBytes(totalLength, 4));  // File size - 8
        out.write("WAVE".getBytes());

        // fmt chunk
        out.write("fmt ".getBytes());
        out.write(intToBytes(16, 4));           // Chunk size
        out.write(intToBytes(1, 2));            // Audio format (PCM)
        out.write(intToBytes(channels, 2));     // Channels
        out.write(intToBytes(sampleRate, 4));   // Sample rate
        out.write(intToBytes(byteRate, 4));     // Byte rate
        out.write(intToBytes(blockAlign, 2));   // Block align
        out.write(intToBytes(bitsPerSample, 2)); // Bits per sample

        // data chunk
        out.write("data".getBytes());
        out.write(intToBytes(audioLength, 4));  // Data size
    }

    /**
     * Converts an integer to bytes (little-endian).
     */
    private byte[] intToBytes(int value, int bytes) {
        byte[] result = new byte[bytes];
        for (int i = 0; i < bytes; i++) {
            result[i] = (byte) ((value >> (8 * i)) & 0xFF);
        }
        return result;
    }

    /**
     * Reads an input stream to a string.
     */
    private String readStream(InputStream is) throws IOException {
        if (is == null) return "";
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        reader.close();
        return sb.toString();
    }

    /**
     * Parses the transcription from the JSON response.
     */
    private String parseTranscription(String json) {
        // Simple JSON parsing for {"text": "..."}
        if (json == null || json.isEmpty()) return "";

        int textIndex = json.indexOf("\"text\"");
        if (textIndex == -1) return "";

        int colonIndex = json.indexOf(":", textIndex);
        if (colonIndex == -1) return "";

        int startQuote = json.indexOf("\"", colonIndex);
        if (startQuote == -1) return "";

        int endQuote = json.indexOf("\"", startQuote + 1);
        if (endQuote == -1) return "";

        return json.substring(startQuote + 1, endQuote)
            .replace("\\n", "\n")
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")
            .trim();
    }
}

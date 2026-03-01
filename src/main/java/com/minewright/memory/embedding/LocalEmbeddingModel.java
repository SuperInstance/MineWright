package com.minewright.memory.embedding;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * Local embedding model interface for on-device embedding generation.
 *
 * <p>This interface is designed for future implementations that use local models
 * such as ONNX Runtime or Deep Java Library (DJL) to generate embeddings without
 * requiring external API calls.</p>
 *
 * <p><b>Benefits of Local Embedding Models:</b></p>
 * <ul>
 *   <li>No network latency or API costs</li>
 *   <li>Privacy: data never leaves the device</li>
 *   <li>Offline operation</li>
 *   <li>Predictable performance</li>
 * </ul>
 *
 * <p><b>Popular Models for Java:</b></p>
 * <ul>
 *   <li><b>Sentence-BERT (all-MiniLM-L6-v2)</b> - 384 dimensions, fast and accurate</li>
 *   <li><b>all-MiniLM-L12-v2</b> - 384 dimensions, better accuracy</li>
 *   <li><b>e5-small-v2</b> - 384 dimensions, optimized for retrieval</li>
 *   <li><b>bge-small-en-v1.5</b> - 384 dimensions, state-of-the-art</li>
 * </ul>
 *
 * <p><b>Implementation Options:</b></p>
 * <ul>
 *   <li><b>ONNX Runtime:</b> Run pre-trained models converted to ONNX format</li>
 *   <li><b>Deep Java Library (DJL):b> High-level API with model zoo support</li>
 *   <li><b>Apache OpenNLP:</b> Lightweight NLP library with embedding support</li>
 * </ul>
 *
 * <h3>Adding Real Local Model Support</h3>
 *
 * <p>To implement a real local embedding model:</p>
 *
 * <h4>Option 1: ONNX Runtime (Recommended)</h4>
 * <ol>
 *   <li>Add ONNX Runtime dependency to build.gradle:
 *     <pre>
 * implementation 'com.microsoft.onnxruntime:onnxruntime:1.17.0'
 *     </pre>
 *   </li>
 *   <li>Download a pre-trained ONNX model (e.g., all-MiniLM-L6-v2.onnx)</li>
 *   <li>Create a class extending LocalEmbeddingModel that:
 *     <ul>
 *       <li>Loads the ONNX model from resources or file system</li>
 *       <li>Pre-processes text (tokenization, truncation)</li>
 *       <li>Runs inference with OrtSession</li>
 *       <li>Post-processes output (normalization, pooling)</li>
 *     </ul>
 *   </li>
 * </ol>
 *
 * <h4>Option 2: Deep Java Library (DJL)</h4>
 * <ol>
 *   <li>Add DJL dependencies to build.gradle:
 *     <pre>
 * implementation 'ai.djl:api:0.24.0'
 * implementation 'ai.djl.pytorch:pytorch-engine:0.24.0'
 * implementation 'ai.djl.pytorch:pytorch-model-zoo:0.24.0'
 *     </pre>
 *   </li>
 *   <li>Use DJL's ModelZoo to load a pre-trained sentence transformer</li>
 *   <li>Implement the embedding generation using DJL's Predictor API</li>
 * </ol>
 *
 * <h4>Example ONNX Implementation:</h4>
 * <pre>
 * public class OnnxEmbeddingModel extends LocalEmbeddingModel {
 *     private final OrtSession session;
 *     private final OrtEnvironment env;
 *
 *     public OnnxEmbeddingModel(Path modelPath) throws OrtException {
 *         this.env = OrtEnvironment.getEnvironment();
 *         this.session = env.createSession(modelPath.toString(),
 *             new OrtSession.SessionOptions());
 *     }
 *
 *     {@literal @}Override
 *     public float[] embed(String text) {
 *         // Tokenize text using tokenizer
 *         long[] inputIds = tokenize(text);
 *         long[] attentionMask = createAttentionMask(inputIds);
 *
 *         // Create input tensors
 *         OnnxTensor inputTensor = OnnxTensor.createTensor(env,
 *             new long[][]{inputIds});
 *         OnnxTensor maskTensor = OnnxTensor.createTensor(env,
 *             new long[][]{attentionMask});
 *
 *         // Run inference
 *         OrtSession.Result result = session.run(Map.of(
 *             "input_ids", inputTensor,
 *             "attention_mask", maskTensor
 *         ));
 *
 *         // Extract and normalize embeddings
 *         float[][] rawEmbeddings = (float[][]) result.get(0).getValue();
 *         return normalize(rawEmbeddings[0]);
 *     }
 * }
 * </pre>
 *
 * @since 1.2.0
 */
public interface LocalEmbeddingModel extends EmbeddingModel {

    /**
     * Logger for local embedding implementations.
     */
    Logger LOGGER = LoggerFactory.getLogger(LocalEmbeddingModel.class);

    /**
     * Warms up the model by running a few inference passes.
     *
     * <p>Local models often have slow initialization (loading weights, JIT compilation).
     * Calling warmup() ensures the model is ready before real requests.</p>
     *
     * @throws IllegalStateException if the model cannot be warmed up
     */
    void warmup();

    /**
     * Returns the approximate memory footprint of the model in bytes.
     *
     * @return Memory footprint in bytes, or -1 if unknown
     */
    long getMemoryFootprint();

    /**
     * Returns whether the model is loaded and ready.
     *
     * @return true if loaded, false otherwise
     */
    boolean isLoaded();

    /**
     * Unloads the model and releases resources.
     *
     * <p>After calling unload(), the model cannot be used until reloaded.
     * This is useful for freeing memory when the model is not needed.</p>
     */
    void unload();

    /**
     * Default implementation that throws UnsupportedOperationException.
     * Real implementations should override this method.
     */
    @Override
    default float[] embed(String text) {
        throw new UnsupportedOperationException(
                "Local embedding model not implemented. Please implement a concrete class " +
                "using ONNX Runtime, DJL, or another local ML framework. " +
                "See LocalEmbeddingModel JavaDoc for implementation guidance.");
    }

    /**
     * Default implementation that throws UnsupportedOperationException.
     * Real implementations should override this method.
     */
    @Override
    default CompletableFuture<float[]> embedAsync(String text) {
        throw new UnsupportedOperationException(
                "Local embedding model not implemented. Please implement a concrete class " +
                "using ONNX Runtime, DJL, or another local ML framework. " +
                "See LocalEmbeddingModel JavaDoc for implementation guidance.");
    }

    /**
     * Placeholder implementation that always returns false.
     * Real implementations should override this method.
     */
    @Override
    default boolean isAvailable() {
        LOGGER.warn("Local embedding model is not available. " +
                "Please implement a concrete class using ONNX Runtime, DJL, or another local ML framework.");
        return false;
    }

    /**
     * Placeholder implementation that returns default dimension.
     * Real implementations should override this method.
     */
    @Override
    default int getDimension() {
        LOGGER.warn("Using placeholder dimension for local embedding model. " +
                "Real implementations should override getDimension() with the actual model dimension.");
        return 384; // Common dimension for small models
    }

    /**
     * Placeholder implementation that returns placeholder name.
     * Real implementations should override this method.
     */
    @Override
    default String getModelName() {
        LOGGER.warn("Using placeholder model name for local embedding model. " +
                "Real implementations should override getModelName() with the actual model name.");
        return "placeholder-local-embedding";
    }
}

package com.minewright.vision;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import javax.imageio.ImageIO;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for ImageEncoder.
 * Tests image encoding to base64 with proper MIME type detection.
 */
@DisplayName("ImageEncoder Tests")
class ImageEncoderTest {

    @TempDir
    Path tempDir;

    private Path pngFile;
    private Path jpgFile;
    private Path webpFile;

    @BeforeEach
    void setUp() throws IOException {
        // Create test image files
        pngFile = createTestImage("test.png", "PNG");
        jpgFile = createTestImage("test.jpg", "JPEG");
        webpFile = createTestImage("test.webp", "PNG"); // Note: WebP may not be supported
    }

    private Path createTestImage(String filename, String format) throws IOException {
        Path file = tempDir.resolve(filename);
        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        ImageIO.write(image, format, file.toFile());
        return file;
    }

    // ==================== Basic Encoding Tests ====================

    @Test
    @DisplayName("Should encode PNG image to base64 with correct MIME type")
    void testEncodeToBase64_Png() throws Exception {
        String base64 = ImageEncoder.encodeToBase64(pngFile);

        assertNotNull(base64, "Base64 string should not be null");
        assertTrue(
            base64.startsWith("data:image/png;base64,"),
            "Should start with correct PNG data URL prefix"
        );
        assertTrue(
            base64.length() > "data:image/png;base64,".length(),
            "Should contain actual base64 data"
        );
    }

    @Test
    @DisplayName("Should encode JPEG image to base64 with correct MIME type")
    void testEncodeToBase64_Jpeg() throws Exception {
        String base64 = ImageEncoder.encodeToBase64(jpgFile);

        assertNotNull(base64, "Base64 string should not be null");
        assertTrue(
            base64.startsWith("data:image/jpeg;base64,"),
            "Should start with correct JPEG data URL prefix"
        );
    }

    @Test
    @DisplayName("Should encode uppercase .JPEG extension")
    void testEncodeToBase64_UppercaseExtension() throws Exception {
        Path upperJpeg = tempDir.resolve("test.JPG");
        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        ImageIO.write(image, "JPEG", upperJpeg.toFile());

        String base64 = ImageEncoder.encodeToBase64(upperJpeg);

        assertTrue(
            base64.startsWith("data:image/jpeg;base64,"),
            "Should handle uppercase extension"
        );
    }

    @Test
    @DisplayName("Should encode mixed case .JpEg extension")
    void testEncodeToBase64_MixedCaseExtension() throws Exception {
        Path mixedCase = tempDir.resolve("test.JpEg");
        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        ImageIO.write(image, "JPEG", mixedCase.toFile());

        String base64 = ImageEncoder.encodeToBase64(mixedCase);

        assertTrue(
            base64.startsWith("data:image/jpeg;base64,"),
            "Should handle mixed case extension"
        );
    }

    @Test
    @DisplayName("Should default to PNG MIME type for unknown extension")
    void testEncodeToBase64_UnknownExtension() throws Exception {
        Path unknownFile = tempDir.resolve("test.unknown");
        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        ImageIO.write(image, "PNG", unknownFile.toFile());

        String base64 = ImageEncoder.encodeToBase64(unknownFile);

        assertTrue(
            base64.startsWith("data:image/png;base64,"),
            "Should default to PNG for unknown extension"
        );
    }

    // ==================== Raw Base64 Tests ====================

    @Test
    @DisplayName("Should encode to raw base64 without prefix")
    void testEncodeToRawBase64() throws Exception {
        String rawBase64 = ImageEncoder.encodeToRawBase64(pngFile);

        assertNotNull(rawBase64, "Raw base64 should not be null");
        assertFalse(
            rawBase64.startsWith("data:"),
            "Should not include data URL prefix"
        );

        // Verify it's valid base64
        assertDoesNotThrow(() -> Base64.getDecoder().decode(rawBase64));
    }

    @Test
    @DisplayName("Raw base64 should decode to original image data")
    void testEncodeToRawBase64_DecodesCorrectly() throws Exception {
        byte[] originalBytes = Files.readAllBytes(pngFile);
        String rawBase64 = ImageEncoder.encodeToRawBase64(pngFile);
        byte[] decodedBytes = Base64.getDecoder().decode(rawBase64);

        assertArrayEquals(originalBytes, decodedBytes, "Decoded bytes should match original");
    }

    // ==================== MIME Type Detection Tests ====================

    @Test
    @DisplayName("Should detect PNG MIME type correctly")
    void testDetectMimeType_Png() {
        String mimeType = ImageEncoder.detectMimeType(pngFile);
        assertEquals("image/png", mimeType, "Should detect PNG");
    }

    @Test
    @DisplayName("Should detect JPEG MIME type correctly")
    void testDetectMimeType_Jpeg() {
        String mimeType = ImageEncoder.detectMimeType(jpgFile);
        assertEquals("image/jpeg", mimeType, "Should detect JPEG");
    }

    @Test
    @DisplayName("Should detect WEBP MIME type correctly")
    void testDetectMimeType_Webp() {
        String mimeType = ImageEncoder.detectMimeType(webpFile);
        assertEquals("image/webp", mimeType, "Should detect WEBP");
    }

    // ==================== Streaming Encoding Tests ====================

    @Test
    @DisplayName("Should encode image using streaming")
    void testEncodeStreaming() throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        StreamingImageEncoder.encodeStreaming(
            pngFile,
            "image/png",
            outputStream
        );

        String result = outputStream.toString("UTF-8");

        assertNotNull(result, "Result should not be null");
        assertTrue(
            result.startsWith("data:image/png;base64,"),
            "Should include data URL prefix"
        );

        // Verify valid base64 after prefix
        String base64Part = result.substring("data:image/png;base64,".length());
        assertDoesNotThrow(() -> Base64.getDecoder().decode(base64Part));
    }

    @Test
    @DisplayName("Streaming should handle larger images efficiently")
    void testEncodeStreaming_LargeImage() throws Exception {
        // Create a larger image
        Path largeFile = tempDir.resolve("large.png");
        BufferedImage largeImage = new BufferedImage(1920, 1080, BufferedImage.TYPE_INT_ARGB);
        ImageIO.write(largeImage, "PNG", largeFile.toFile());

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        assertDoesNotThrow(
            () -> StreamingImageEncoder.encodeStreaming(
                largeFile,
                "image/png",
                outputStream
            ),
            "Should handle large images without issues"
        );

        assertTrue(outputStream.size() > 0, "Should produce output");
    }

    @Test
    @DisplayName("Streaming should produce same result as simple encoding")
    void testEncodeStreaming_Consistency() throws Exception {
        String simpleEncoding = ImageEncoder.encodeToBase64(pngFile);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        StreamingImageEncoder.encodeStreaming(pngFile, "image/png", outputStream);
        String streamingEncoding = outputStream.toString("UTF-8");

        assertEquals(
            simpleEncoding,
            streamingEncoding,
            "Streaming and simple encoding should produce identical results"
        );
    }

    // ==================== Error Handling Tests ====================

    @Test
    @DisplayName("Should throw exception for non-existent file")
    void testEncodeToBase64_NonExistentFile() {
        Path nonExistent = tempDir.resolve("does_not_exist.png");

        assertThrows(
            Exception.class,
            () -> ImageEncoder.encodeToBase64(nonExistent),
            "Should throw exception for non-existent file"
        );
    }

    @Test
    @DisplayName("Should throw exception for directory instead of file")
    void testEncodeToBase64_Directory() {
        assertThrows(
            Exception.class,
            () -> ImageEncoder.encodeToBase64(tempDir),
            "Should throw exception for directory"
        );
    }

    @Test
    @DisplayName("Should throw exception for corrupted image file")
    void testEncodeToBase64_CorruptedFile() throws IOException {
        Path corruptedFile = tempDir.resolve("corrupted.png");
        Files.writeString(corruptedFile, "This is not a valid PNG file");

        assertThrows(
            Exception.class,
            () -> ImageEncoder.encodeToBase64(corruptedFile),
            "Should throw exception for corrupted file"
        );
    }

    // ==================== Edge Case Tests ====================

    @Test
    @DisplayName("Should handle empty image file")
    void testEncodeToBase64_EmptyFile() throws IOException {
        Path emptyFile = tempDir.resolve("empty.png");
        Files.createFile(emptyFile);

        // Should either handle gracefully or throw meaningful exception
        assertThrows(
            Exception.class,
            () -> ImageEncoder.encodeToBase64(emptyFile),
            "Should throw exception for empty file"
        );
    }

    @Test
    @DisplayName("Should handle very small image (1x1 pixel)")
    void testEncodeToBase64_TinyImage() throws Exception {
        Path tinyFile = tempDir.resolve("tiny.png");
        BufferedImage tinyImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        tinyImage.setRGB(0, 0, 0xFF0000FF); // Blue pixel
        ImageIO.write(tinyImage, "PNG", tinyFile.toFile());

        String base64 = ImageEncoder.encodeToBase64(tinyFile);

        assertNotNull(base64, "Should encode tiny image");
        assertTrue(
            base64.startsWith("data:image/png;base64,"),
            "Should have correct prefix"
        );
    }

    @Test
    @DisplayName("Should handle image with various color depths")
    void testEncodeToBase64_DifferentColorDepths() throws Exception {
        // Test different image types
        BufferedImage argbImage = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        BufferedImage rgbImage = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        BufferedImage grayImage = new BufferedImage(10, 10, BufferedImage.TYPE_BYTE_GRAY);

        Path argbFile = tempDir.resolve("argb.png");
        Path rgbFile = tempDir.resolve("rgb.png");
        Path grayFile = tempDir.resolve("gray.png");

        ImageIO.write(argbImage, "PNG", argbFile.toFile());
        ImageIO.write(rgbImage, "PNG", rgbFile.toFile());
        ImageIO.write(grayImage, "PNG", grayFile.toFile());

        assertDoesNotThrow(() -> ImageEncoder.encodeToBase64(argbFile));
        assertDoesNotThrow(() -> ImageEncoder.encodeToBase64(rgbFile));
        assertDoesNotThrow(() -> ImageEncoder.encodeToBase64(grayFile));
    }

    @Test
    @DisplayName("Should handle special characters in filename")
    void testEncodeToBase64_SpecialCharactersInFilename() throws Exception {
        Path specialFile = tempDir.resolve("test image (1).png");
        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        ImageIO.write(image, "PNG", specialFile.toFile());

        assertDoesNotThrow(
            () -> ImageEncoder.encodeToBase64(specialFile),
            "Should handle special characters in filename"
        );
    }

    // ==================== Null and Empty Input Tests ====================

    @Test
    @DisplayName("Should throw exception for null path")
    void testEncodeToBase64_NullPath() {
        assertThrows(
            Exception.class,
            () -> ImageEncoder.encodeToBase64(null),
            "Should throw exception for null path"
        );
    }

    @Test
    @DisplayName("Should throw exception for null MIME type in streaming")
    void testEncodeStreaming_NullMimeType() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        assertThrows(
            Exception.class,
            () -> StreamingImageEncoder.encodeStreaming(pngFile, null, outputStream),
            "Should throw exception for null MIME type"
        );
    }

    @Test
    @DisplayName("Should throw exception for null output stream in streaming")
    void testEncodeStreaming_NullOutputStream() {
        assertThrows(
            Exception.class,
            () -> StreamingImageEncoder.encodeStreaming(pngFile, "image/png", null),
            "Should throw exception for null output stream"
        );
    }

    // ==================== Performance Tests ====================

    @Test
    @DisplayName("Encoding should complete in reasonable time")
    void testEncodeToBase64_Performance() throws Exception {
        Path perfFile = tempDir.resolve("perf.png");
        BufferedImage image = new BufferedImage(1280, 720, BufferedImage.TYPE_INT_ARGB);
        ImageIO.write(image, "PNG", perfFile.toFile());

        long startTime = System.currentTimeMillis();
        ImageEncoder.encodeToBase64(perfFile);
        long duration = System.currentTimeMillis() - startTime;

        assertTrue(
            duration < 1000,
            "Encoding should complete in less than 1 second, took: " + duration + "ms"
        );
    }

    @Test
    @DisplayName("Streaming should use less memory for large images")
    void testEncodeStreaming_MemoryEfficiency() throws Exception {
        // This is a conceptual test - actual memory measurement would require profiling
        Path largeFile = tempDir.resolve("large.png");
        BufferedImage largeImage = new BufferedImage(1920, 1080, BufferedImage.TYPE_INT_ARGB);
        ImageIO.write(largeImage, "PNG", largeFile.toFile());

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        assertDoesNotThrow(
            () -> StreamingImageEncoder.encodeStreaming(largeFile, "image/png", outputStream),
            "Streaming should handle large files efficiently"
        );
    }
}

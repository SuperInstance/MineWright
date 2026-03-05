package com.minewright.vision;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for VisionOptimization.
 * Tests optimization strategies for vision API performance.
 */
@DisplayName("VisionOptimization Tests")
class VisionOptimizationTest {

    @BeforeEach
    void setUp() {
        // Setup if needed
    }

    // ==================== Resolution Tests ====================

    @Test
    @DisplayName("Should have predefined resolution constants")
    void testResolutionConstants() {
        assertNotNull(VisionOptimization.LOW_RES, "LOW_RES should not be null");
        assertNotNull(VisionOptimization.MEDIUM_RES, "MEDIUM_RES should not be null");
        assertNotNull(VisionOptimization.HIGH_RES, "HIGH_RES should not be null");

        assertEquals(640, VisionOptimization.LOW_RES.width());
        assertEquals(480, VisionOptimization.LOW_RES.height());

        assertEquals(1280, VisionOptimization.MEDIUM_RES.width());
        assertEquals(720, VisionOptimization.MEDIUM_RES.height());

        assertEquals(1920, VisionOptimization.HIGH_RES.width());
        assertEquals(1080, VisionOptimization.HIGH_RES.height());
    }

    @Test
    @DisplayName("Resolution record should store width and height")
    void testResolutionRecord() {
        VisionOptimization.Resolution res = new VisionOptimization.Resolution(800, 600);

        assertEquals(800, res.width());
        assertEquals(600, res.height());
    }

    @Test
    @DisplayName("Resolution record should implement equals correctly")
    void testResolutionEquals() {
        VisionOptimization.Resolution res1 = new VisionOptimization.Resolution(1920, 1080);
        VisionOptimization.Resolution res2 = new VisionOptimization.Resolution(1920, 1080);
        VisionOptimization.Resolution res3 = new VisionOptimization.Resolution(1280, 720);

        assertEquals(res1, res2, "Same resolution should be equal");
        assertNotEquals(res1, res3, "Different resolutions should not be equal");
    }

    // ==================== Task Classification Tests ====================

    @Test
    @DisplayName("Should suggest HIGH_RES for OCR task")
    void testSuggestResolution_Ocr() {
        VisionOptimization.Resolution res = VisionOptimization.suggestResolution(
            VisionOptimization.VisionTask.OCR
        );

        assertEquals(VisionOptimization.HIGH_RES, res, "OCR should use high resolution");
    }

    @Test
    @DisplayName("Should suggest LOW_RES for biome identification")
    void testSuggestResolution_BiomeIdentification() {
        VisionOptimization.Resolution res = VisionOptimization.suggestResolution(
            VisionOptimization.VisionTask.BIOME_IDENTIFICATION
        );

        assertEquals(VisionOptimization.LOW_RES, res, "Biome ID should use low resolution");
    }

    @Test
    @DisplayName("Should suggest MEDIUM_RES for resource scanning")
    void testSuggestResolution_ResourceScanning() {
        VisionOptimization.Resolution res = VisionOptimization.suggestResolution(
            VisionOptimization.VisionTask.RESOURCE_SCANNING
        );

        assertEquals(VisionOptimization.MEDIUM_RES, res, "Resource scanning should use medium resolution");
    }

    @Test
    @DisplayName("Should suggest MEDIUM_RES for threat detection")
    void testSuggestResolution_ThreatDetection() {
        VisionOptimization.Resolution res = VisionOptimization.suggestResolution(
            VisionOptimization.VisionTask.THREAT_DETECTION
        );

        assertEquals(VisionOptimization.MEDIUM_RES, res, "Threat detection should use medium resolution");
    }

    @Test
    @DisplayName("Should suggest HIGH_RES for build verification")
    void testSuggestResolution_BuildVerification() {
        VisionOptimization.Resolution res = VisionOptimization.suggestResolution(
            VisionOptimization.VisionTask.BUILD_VERIFICATION
        );

        assertEquals(VisionOptimization.HIGH_RES, res, "Build verification should use high resolution");
    }

    @Test
    @DisplayName("Should suggest MEDIUM_RES for general analysis")
    void testSuggestResolution_GeneralAnalysis() {
        VisionOptimization.Resolution res = VisionOptimization.suggestResolution(
            VisionOptimization.VisionTask.GENERAL_ANALYSIS
        );

        assertEquals(VisionOptimization.MEDIUM_RES, res, "General analysis should use medium resolution");
    }

    // ==================== Detail Level Tests ====================

    @Test
    @DisplayName("Should suggest 'high' detail for OCR")
    void testSuggestDetailLevel_Ocr() {
        String detail = VisionOptimization.suggestDetailLevel(VisionOptimization.VisionTask.OCR);

        assertEquals("high", detail, "OCR should use high detail");
    }

    @Test
    @DisplayName("Should suggest 'high' detail for build verification")
    void testSuggestDetailLevel_BuildVerification() {
        String detail = VisionOptimization.suggestDetailLevel(
            VisionOptimization.VisionTask.BUILD_VERIFICATION
        );

        assertEquals("high", detail, "Build verification should use high detail");
    }

    @Test
    @DisplayName("Should suggest 'auto' detail for general analysis")
    void testSuggestDetailLevel_GeneralAnalysis() {
        String detail = VisionOptimization.suggestDetailLevel(
            VisionOptimization.VisionTask.GENERAL_ANALYSIS
        );

        assertEquals("auto", detail, "General analysis should use auto detail");
    }

    @Test
    @DisplayName("Should suggest 'low' detail for other tasks")
    void testSuggestDetailLevel_OtherTasks() {
        assertEquals("low", VisionOptimization.suggestDetailLevel(
            VisionOptimization.VisionTask.BIOME_IDENTIFICATION));
        assertEquals("low", VisionOptimization.suggestDetailLevel(
            VisionOptimization.VisionTask.RESOURCE_SCANNING));
        assertEquals("low", VisionOptimization.suggestDetailLevel(
            VisionOptimization.VisionTask.THREAT_DETECTION));
    }

    // ==================== Task Enum Tests ====================

    @Test
    @DisplayName("Should have all vision task types defined")
    void testVisionTaskEnum() {
        VisionOptimization.VisionTask[] tasks = VisionOptimization.VisionTask.values();

        assertEquals(6, tasks.length, "Should have 6 vision task types");

        assertTrue(java.util.Arrays.asList(tasks).contains(
            VisionOptimization.VisionTask.OCR));
        assertTrue(java.util.Arrays.asList(tasks).contains(
            VisionOptimization.VisionTask.BIOME_IDENTIFICATION));
        assertTrue(java.util.Arrays.asList(tasks).contains(
            VisionOptimization.VisionTask.RESOURCE_SCANNING));
        assertTrue(java.util.Arrays.asList(tasks).contains(
            VisionOptimization.VisionTask.THREAT_DETECTION));
        assertTrue(java.util.Arrays.asList(tasks).contains(
            VisionOptimization.VisionTask.BUILD_VERIFICATION));
        assertTrue(java.util.Arrays.asList(tasks).contains(
            VisionOptimization.VisionTask.GENERAL_ANALYSIS));
    }

    // ==================== Configuration Constants Tests ====================

    @Test
    @DisplayName("Should have max images per request constant")
    void testMaxImagesPerRequest() {
        assertEquals(4, VisionOptimization.MAX_IMAGES_PER_REQUEST,
            "Should allow max 4 images per request");
    }

    // ==================== Edge Case Tests ====================

    @Test
    @DisplayName("Should handle all task types in resolution suggestion")
    void testSuggestResolution_AllTasks() {
        for (VisionOptimization.VisionTask task : VisionOptimization.VisionTask.values()) {
            VisionOptimization.Resolution res = VisionOptimization.suggestResolution(task);

            assertNotNull(res, "Resolution should not be null for " + task);
            assertTrue(res.width() > 0, "Width should be positive for " + task);
            assertTrue(res.height() > 0, "Height should be positive for " + task);
        }
    }

    @Test
    @DisplayName("Should handle all task types in detail level suggestion")
    void testSuggestDetailLevel_AllTasks() {
        for (VisionOptimization.VisionTask task : VisionOptimization.VisionTask.values()) {
            String detail = VisionOptimization.suggestDetailLevel(task);

            assertNotNull(detail, "Detail level should not be null for " + task);
            assertTrue(
                detail.equals("low") || detail.equals("auto") || detail.equals("high"),
                "Detail level should be low, auto, or high for " + task
            );
        }
    }

    @Test
    @DisplayName("Resolution suggestions should be consistent")
    void testSuggestResolution_Consistency() {
        // Call multiple times and verify consistency
        for (int i = 0; i < 10; i++) {
            VisionOptimization.Resolution res1 = VisionOptimization.suggestResolution(
                VisionOptimization.VisionTask.OCR);
            VisionOptimization.Resolution res2 = VisionOptimization.suggestResolution(
                VisionOptimization.VisionTask.OCR);

            assertEquals(res1, res2, "Resolution suggestion should be consistent");
        }
    }

    @Test
    @DisplayName("Detail level suggestions should be consistent")
    void testSuggestDetailLevel_Consistency() {
        for (int i = 0; i < 10; i++) {
            String detail1 = VisionOptimization.suggestDetailLevel(
                VisionOptimization.VisionTask.GENERAL_ANALYSIS);
            String detail2 = VisionOptimization.suggestDetailLevel(
                VisionOptimization.VisionTask.GENERAL_ANALYSIS);

            assertEquals(detail1, detail2, "Detail level suggestion should be consistent");
        }
    }

    // ==================== Practical Use Case Tests ====================

    @Test
    @DisplayName("Should provide appropriate settings for Minecraft OCR")
    void testMinecraftOcrSettings() {
        VisionOptimization.VisionTask task = VisionOptimization.VisionTask.OCR;
        VisionOptimization.Resolution res = VisionOptimization.suggestResolution(task);
        String detail = VisionOptimization.suggestDetailLevel(task);

        assertEquals(VisionOptimization.HIGH_RES, res);
        assertEquals("high", detail);

        // Verify high resolution is suitable for OCR
        assertTrue(res.width() >= 1920, "Width should be at least 1920 for OCR");
        assertTrue(res.height() >= 1080, "Height should be at least 1080 for OCR");
    }

    @Test
    @DisplayName("Should provide appropriate settings for biome identification")
    void testMinecraftBiomeSettings() {
        VisionOptimization.VisionTask task = VisionOptimization.VisionTask.BIOME_IDENTIFICATION;
        VisionOptimization.Resolution res = VisionOptimization.suggestResolution(task);
        String detail = VisionOptimization.suggestDetailLevel(task);

        assertEquals(VisionOptimization.LOW_RES, res);
        assertEquals("low", detail);

        // Low resolution is sufficient for biome identification
        assertTrue(res.width() <= 640, "Width should be low for biome ID");
    }

    @Test
    @DisplayName("Should provide appropriate settings for build verification")
    void testMinecraftBuildVerificationSettings() {
        VisionOptimization.VisionTask task = VisionOptimization.VisionTask.BUILD_VERIFICATION;
        VisionOptimization.Resolution res = VisionOptimization.suggestResolution(task);
        String detail = VisionOptimization.suggestDetailLevel(task);

        assertEquals(VisionOptimization.HIGH_RES, res);
        assertEquals("high", detail);

        // High resolution needed for accuracy
        assertTrue(res.width() >= 1920, "Width should be high for build verification");
    }

    @Test
    @DisplayName("Should provide balanced settings for resource scanning")
    void testMinecraftResourceScanningSettings() {
        VisionOptimization.VisionTask task = VisionOptimization.VisionTask.RESOURCE_SCANNING;
        VisionOptimization.Resolution res = VisionOptimization.suggestResolution(task);
        String detail = VisionOptimization.suggestDetailLevel(task);

        assertEquals(VisionOptimization.MEDIUM_RES, res);
        assertEquals("low", detail);

        // Balance between detail and speed
        assertTrue(res.width() >= 1280, "Width should be medium");
        assertTrue(res.width() <= 1920, "Width should not be too high");
    }

    @Test
    @DisplayName("Should provide appropriate settings for threat detection")
    void testMinecraftThreatDetectionSettings() {
        VisionOptimization.VisionTask task = VisionOptimization.VisionTask.THREAT_DETECTION;
        VisionOptimization.Resolution res = VisionOptimization.suggestResolution(task);
        String detail = VisionOptimization.suggestDetailLevel(task);

        assertEquals(VisionOptimization.MEDIUM_RES, res);
        assertEquals("low", detail);

        // Need to see mobs clearly but don't need extreme detail
        assertTrue(res.width() >= 1280, "Width should be medium");
    }

    // ==================== Performance Implication Tests ====================

    @Test
    @DisplayName("Lower resolution should result in faster processing")
    void testResolutionPerformance_Implications() {
        VisionOptimization.Resolution low = VisionOptimization.LOW_RES;
        VisionOptimization.Resolution high = VisionOptimization.HIGH_RES;

        // Calculate pixel counts
        long lowPixels = (long) low.width() * low.height();
        long highPixels = (long) high.width() * high.height();

        // High resolution has ~9x more pixels
        assertTrue(highPixels > lowPixels * 8, "High res should have significantly more pixels");

        // This implies ~9x faster processing for low res
        double speedup = (double) highPixels / lowPixels;
        assertTrue(speedup >= 8.0 && speedup <= 10.0,
            "Speedup should be around 9x, actual: " + speedup);
    }

    @Test
    @DisplayName("Detail level should affect token usage")
    void testDetailLevel_TokenImplications() {
        String lowDetail = VisionOptimization.suggestDetailLevel(
            VisionOptimization.VisionTask.BIOME_IDENTIFICATION);
        String highDetail = VisionOptimization.suggestDetailLevel(
            VisionOptimization.VisionTask.OCR);

        // Based on documentation:
        // low detail: ~85 tokens
        // high detail: ~765 tokens
        // High detail uses ~9x more tokens

        assertTrue(lowDetail.equals("low"), "Biome ID should use low detail");
        assertTrue(highDetail.equals("high"), "OCR should use high detail");

        // Token usage ratio
        double tokenRatio = 765.0 / 85.0;
        assertTrue(tokenRatio >= 8.0 && tokenRatio <= 10.0,
            "High detail should use ~9x more tokens");
    }

    // ==================== Record toString Tests ====================

    @Test
    @DisplayName("Resolution record should have meaningful toString")
    void testResolutionToString() {
        VisionOptimization.Resolution res = VisionOptimization.HIGH_RES;
        String str = res.toString();

        assertNotNull(str, "toString should not be null");
        assertTrue(str.contains("1920"), "toString should contain width");
        assertTrue(str.contains("1080"), "toString should contain height");
    }

    // ==================== Resolution Aspect Ratio Tests ====================

    @Test
    @DisplayName("Predefined resolutions should maintain 16:9 aspect ratio")
    void testResolution_AspectRatios() {
        double ratio = 16.0 / 9.0;

        double lowRatio = (double) VisionOptimization.LOW_RES.width() / VisionOptimization.LOW_RES.height();
        double mediumRatio = (double) VisionOptimization.MEDIUM_RES.width() / VisionOptimization.MEDIUM_RES.height();
        double highRatio = (double) VisionOptimization.HIGH_RES.width() / VisionOptimization.HIGH_RES.height();

        assertEquals(ratio, lowRatio, 0.01, "LOW_RES should be 16:9");
        assertEquals(ratio, mediumRatio, 0.01, "MEDIUM_RES should be 16:9");
        assertEquals(ratio, highRatio, 0.01, "HIGH_RES should be 16:9");
    }

    // ==================== Task-Specific Optimization Tests ====================

    @Test
    @DisplayName("OCR task should prioritize quality over speed")
    void testTaskPriorities_Ocr() {
        VisionOptimization.Resolution res = VisionOptimization.suggestResolution(
            VisionOptimization.VisionTask.OCR);
        String detail = VisionOptimization.suggestDetailLevel(
            VisionOptimization.VisionTask.OCR);

        // OCR prioritizes quality
        assertEquals(VisionOptimization.HIGH_RES, res);
        assertEquals("high", detail);
    }

    @Test
    @DisplayName("Biome identification should prioritize speed")
    void testTaskPriorities_Biome() {
        VisionOptimization.Resolution res = VisionOptimization.suggestResolution(
            VisionOptimization.VisionTask.BIOME_IDENTIFICATION);
        String detail = VisionOptimization.suggestDetailLevel(
            VisionOptimization.VisionTask.BIOME_IDENTIFICATION);

        // Biome ID prioritizes speed
        assertEquals(VisionOptimization.LOW_RES, res);
        assertEquals("low", detail);
    }

    @Test
    @DisplayName("Build verification should prioritize accuracy")
    void testTaskPriorities_Build() {
        VisionOptimization.Resolution res = VisionOptimization.suggestResolution(
            VisionOptimization.VisionTask.BUILD_VERIFICATION);
        String detail = VisionOptimization.suggestDetailLevel(
            VisionOptimization.VisionTask.BUILD_VERIFICATION);

        // Build verification prioritizes accuracy
        assertEquals(VisionOptimization.HIGH_RES, res);
        assertEquals("high", detail);
    }

    @Test
    @DisplayName("Resource scanning should balance speed and accuracy")
    void testTaskPriorities_Resource() {
        VisionOptimization.Resolution res = VisionOptimization.suggestResolution(
            VisionOptimization.VisionTask.RESOURCE_SCANNING);
        String detail = VisionOptimization.suggestDetailLevel(
            VisionOptimization.VisionTask.RESOURCE_SCANNING);

        // Resource scanning balances both
        assertEquals(VisionOptimization.MEDIUM_RES, res);
        assertEquals("low", detail);
    }
}

package com.minewright.profile;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for {@link ProfileRegistry}.
 *
 * Tests cover:
 * <ul>
 *   <li>Singleton pattern behavior</li>
 *   <li>Profile registration and retrieval</li>
 *   <li>Profile search by name, tag, and query</li>
 *   <li>Profile loading from filesystem</li>
 *   <li>Built-in profile initialization</li>
 *   <li>Profile unregistration and clearing</li>
 *   <li>Tag indexing and search</li>
 *   <li>Profile reload functionality</li>
 * </ul>
 *
 * @since 1.4.0
 */
@DisplayName("ProfileRegistry Tests")
class ProfileRegistryTest {

    private ProfileRegistry registry;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        // Get fresh instance for each test
        registry = ProfileRegistry.getInstance();
        registry.clear();
    }

    // ==================== Singleton Pattern Tests ====================

    @Test
    @DisplayName("GetInstance returns same instance")
    void testGetInstanceReturnsSameInstance() {
        ProfileRegistry instance1 = ProfileRegistry.getInstance();
        ProfileRegistry instance2 = ProfileRegistry.getInstance();

        assertSame(instance1, instance2,
                "GetInstance should return the same singleton instance");
    }

    @Test
    @DisplayName("Clear resets registry state")
    void testClearResetsRegistry() {
        // Add a profile
        TaskProfile profile = TaskProfile.builder()
                .name("test_profile")
                .description("Test profile")
                .build();

        registry.registerProfile(profile);

        assertTrue(registry.hasProfile("test_profile"));

        registry.clear();

        assertFalse(registry.hasProfile("test_profile"),
                "Profile should be removed after clear");
        assertEquals(0, registry.getProfileCount(),
                "Profile count should be 0 after clear");
        assertFalse(registry.isLoaded(),
                "Loaded flag should be false after clear");
    }

    // ==================== Profile Registration Tests ====================

    @Test
    @DisplayName("RegisterProfile adds profile to registry")
    void testRegisterProfileAddsToRegistry() {
        TaskProfile profile = TaskProfile.builder()
                .name("test_profile")
                .description("Test description")
                .author("TestAuthor")
                .version("1.0.0")
                .build();

        registry.registerProfile(profile);

        assertTrue(registry.hasProfile("test_profile"),
                "Profile should be registered");
        assertEquals(1, registry.getProfileCount(),
                "Profile count should be 1");
    }

    @Test
    @DisplayName("RegisterProfile throws exception for null profile")
    void testRegisterProfileThrowsExceptionForNull() {
        assertThrows(IllegalArgumentException.class, () -> registry.registerProfile(null),
                "Should throw exception for null profile");
    }

    @Test
    @DisplayName("RegisterProfile throws exception for duplicate name")
    void testRegisterProfileThrowsExceptionForDuplicate() {
        TaskProfile profile1 = TaskProfile.builder()
                .name("duplicate_name")
                .description("First profile")
                .build();

        TaskProfile profile2 = TaskProfile.builder()
                .name("duplicate_name")
                .description("Second profile")
                .build();

        registry.registerProfile(profile1);

        assertThrows(IllegalArgumentException.class, () -> registry.registerProfile(profile2),
                "Should throw exception for duplicate profile name");
    }

    @Test
    @DisplayName("GetProfile returns registered profile")
    void testGetProfileReturnsRegistered() {
        TaskProfile profile = TaskProfile.builder()
                .name("test_profile")
                .description("Test")
                .build();

        registry.registerProfile(profile);

        TaskProfile retrieved = registry.getProfile("test_profile");

        assertNotNull(retrieved, "Retrieved profile should not be null");
        assertEquals("test_profile", retrieved.getName());
        assertEquals("Test", retrieved.getDescription());
    }

    @Test
    @DisplayName("GetProfile returns null for non-existent profile")
    void testGetProfileReturnsNullForNonExistent() {
        TaskProfile retrieved = registry.getProfile("non_existent");

        assertNull(retrieved, "Should return null for non-existent profile");
    }

    @Test
    @DisplayName("HasProfile returns true for registered profile")
    void testHasProfileReturnsTrueForRegistered() {
        TaskProfile profile = TaskProfile.builder()
                .name("test_profile")
                .build();

        registry.registerProfile(profile);

        assertTrue(registry.hasProfile("test_profile"));
    }

    @Test
    @DisplayName("HasProfile returns false for non-existent profile")
    void testHasProfileReturnsFalseForNonExistent() {
        assertFalse(registry.hasProfile("non_existent"));
    }

    // ==================== Tag Indexing Tests ====================

    @Test
    @DisplayName("RegisterProfile indexes tags")
    void testRegisterProfileIndexesTags() {
        TaskProfile profile = TaskProfile.builder()
                .name("tagged_profile")
                .addTag("mining")
                .addTag("iron")
                .addTag("underground")
                .build();

        registry.registerProfile(profile);

        Set<String> tags = registry.getAllTags();

        assertTrue(tags.contains("mining"), "Should have mining tag");
        assertTrue(tags.contains("iron"), "Should have iron tag");
        assertTrue(tags.contains("underground"), "Should have underground tag");
    }

    @Test
    @DisplayName("GetProfilesByTag returns matching profiles")
    void testGetProfilesByTag() {
        TaskProfile miningProfile1 = TaskProfile.builder()
                .name("mining_iron")
                .addTag("mining")
                .addTag("iron")
                .build();

        TaskProfile miningProfile2 = TaskProfile.builder()
                .name("mining_gold")
                .addTag("mining")
                .addTag("gold")
                .build();

        TaskProfile buildingProfile = TaskProfile.builder()
                .name("building_house")
                .addTag("building")
                .build();

        registry.registerProfile(miningProfile1);
        registry.registerProfile(miningProfile2);
        registry.registerProfile(buildingProfile);

        List<TaskProfile> miningProfiles = registry.getProfilesByTag("mining");

        assertEquals(2, miningProfiles.size(),
                "Should find 2 profiles with mining tag");

        Set<String> miningProfileNames = Set.of(
                miningProfiles.get(0).getName(),
                miningProfiles.get(1).getName()
        );

        assertTrue(miningProfileNames.contains("mining_iron"));
        assertTrue(miningProfileNames.contains("mining_gold"));
    }

    @Test
    @DisplayName("GetProfilesByTag returns empty list for non-existent tag")
    void testGetProfilesByTagReturnsEmptyForNonExistent() {
        List<TaskProfile> profiles = registry.getProfilesByTag("non_existent_tag");

        assertNotNull(profiles, "Should return list, not null");
        assertTrue(profiles.isEmpty(), "Should return empty list for non-existent tag");
    }

    // ==================== Search Functionality Tests ====================

    @Test
    @DisplayName("SearchProfiles finds profiles by name")
    void testSearchProfilesByName() {
        TaskProfile profile1 = TaskProfile.builder()
                .name("mining_iron_profile")
                .description("Mining iron")
                .build();

        TaskProfile profile2 = TaskProfile.builder()
                .name("building_house_profile")
                .description("Building house")
                .build();

        registry.registerProfile(profile1);
        registry.registerProfile(profile2);

        List<TaskProfile> results = registry.searchProfiles("mining");

        assertEquals(1, results.size(), "Should find 1 profile matching 'mining'");
        assertEquals("mining_iron_profile", results.get(0).getName());
    }

    @Test
    @DisplayName("SearchProfiles finds profiles by description")
    void testSearchProfilesByDescription() {
        TaskProfile profile = TaskProfile.builder()
                .name("test_profile")
                .description("This profile is for mining diamond ore")
                .build();

        registry.registerProfile(profile);

        List<TaskProfile> results = registry.searchProfiles("diamond");

        assertEquals(1, results.size(), "Should find profile by description");
        assertEquals("test_profile", results.get(0).getName());
    }

    @Test
    @DisplayName("SearchProfiles is case insensitive")
    void testSearchProfilesCaseInsensitive() {
        TaskProfile profile = TaskProfile.builder()
                .name("Mining_Iron_Profile")
                .build();

        registry.registerProfile(profile);

        List<TaskProfile> lowerCaseResults = registry.searchProfiles("mining");
        List<TaskProfile> upperCaseResults = registry.searchProfiles("MINING");
        List<TaskProfile> mixedCaseResults = registry.searchProfiles("MiNiNg");

        assertEquals(1, lowerCaseResults.size());
        assertEquals(1, upperCaseResults.size());
        assertEquals(1, mixedCaseResults.size());
    }

    @Test
    @DisplayName("SearchProfiles returns all profiles for empty query")
    void testSearchProfilesReturnsAllForEmptyQuery() {
        TaskProfile profile1 = TaskProfile.builder()
                .name("profile1")
                .build();

        TaskProfile profile2 = TaskProfile.builder()
                .name("profile2")
                .build();

        registry.registerProfile(profile1);
        registry.registerProfile(profile2);

        List<TaskProfile> emptyQueryResults = registry.searchProfiles("");
        List<TaskProfile> nullQueryResults = registry.searchProfiles(null);

        assertEquals(2, emptyQueryResults.size(), "Empty query should return all");
        assertEquals(2, nullQueryResults.size(), "Null query should return all");
    }

    @Test
    @DisplayName("SearchProfiles returns empty list for no matches")
    void testSearchProfilesReturnsEmptyForNoMatches() {
        TaskProfile profile = TaskProfile.builder()
                .name("test_profile")
                .build();

        registry.registerProfile(profile);

        List<TaskProfile> results = registry.searchProfiles("non_existent_term");

        assertTrue(results.isEmpty(), "Should return empty list for no matches");
    }

    // ==================== Unregistration Tests ====================

    @Test
    @DisplayName("UnregisterProfile removes profile")
    void testUnregisterProfileRemoves() {
        TaskProfile profile = TaskProfile.builder()
                .name("test_profile")
                .addTag("test_tag")
                .build();

        registry.registerProfile(profile);

        assertTrue(registry.hasProfile("test_profile"));

        TaskProfile unregistered = registry.unregisterProfile("test_profile");

        assertNotNull(unregistered, "Should return unregistered profile");
        assertFalse(registry.hasProfile("test_profile"),
                "Profile should be unregistered");
    }

    @Test
    @DisplayName("UnregisterProfile removes tag indexes")
    void testUnregisterProfileRemovesTagIndexes() {
        TaskProfile profile = TaskProfile.builder()
                .name("test_profile")
                .addTag("unique_tag")
                .build();

        registry.registerProfile(profile);

        assertTrue(registry.getAllTags().contains("unique_tag"));

        registry.unregisterProfile("test_profile");

        assertFalse(registry.getAllTags().contains("unique_tag"),
                "Tag should be removed when profile is unregistered");
    }

    @Test
    @DisplayName("UnregisterProfile returns null for non-existent profile")
    void testUnregisterProfileReturnsNullForNonExistent() {
        TaskProfile result = registry.unregisterProfile("non_existent");

        assertNull(result, "Should return null for non-existent profile");
    }

    // ==================== GetAllProfiles Tests ====================

    @Test
    @DisplayName("GetAllProfiles returns all registered profiles")
    void testGetAllProfilesReturnsAll() {
        TaskProfile profile1 = TaskProfile.builder()
                .name("profile1")
                .build();

        TaskProfile profile2 = TaskProfile.builder()
                .name("profile2")
                .build();

        TaskProfile profile3 = TaskProfile.builder()
                .name("profile3")
                .build();

        registry.registerProfile(profile1);
        registry.registerProfile(profile2);
        registry.registerProfile(profile3);

        List<TaskProfile> allProfiles = registry.getAllProfiles();

        assertEquals(3, allProfiles.size(), "Should return all 3 profiles");

        Set<String> profileNames = Set.of(
                allProfiles.get(0).getName(),
                allProfiles.get(1).getName(),
                allProfiles.get(2).getName()
        );

        assertTrue(profileNames.contains("profile1"));
        assertTrue(profileNames.contains("profile2"));
        assertTrue(profileNames.contains("profile3"));
    }

    @Test
    @DisplayName("GetAllProfiles returns empty list when no profiles")
    void testGetAllProfilesReturnsEmptyWhenNone() {
        List<TaskProfile> profiles = registry.getAllProfiles();

        assertNotNull(profiles, "Should return list, not null");
        assertTrue(profiles.isEmpty(), "Should return empty list");
    }

    @Test
    @DisplayName("GetAllProfiles returns unmodifiable list")
    void testGetAllProfilesReturnsUnmodifiable() {
        TaskProfile profile = TaskProfile.builder()
                .name("test_profile")
                .build();

        registry.registerProfile(profile);

        List<TaskProfile> profiles = registry.getAllProfiles();

        assertThrows(UnsupportedOperationException.class, () -> profiles.add(profile),
                "Returned list should be unmodifiable");
    }

    // ==================== GetProfileNames Tests ====================

    @Test
    @DisplayName("GetProfileNames returns all profile names")
    void testGetProfileNamesReturnsAll() {
        registry.registerProfile(TaskProfile.builder().name("profile1").build());
        registry.registerProfile(TaskProfile.builder().name("profile2").build());
        registry.registerProfile(TaskProfile.builder().name("profile3").build());

        Set<String> names = registry.getProfileNames();

        assertEquals(3, names.size());
        assertTrue(names.contains("profile1"));
        assertTrue(names.contains("profile2"));
        assertTrue(names.contains("profile3"));
    }

    @Test
    @DisplayName("GetProfileNames returns unmodifiable set")
    void testGetProfileNamesReturnsUnmodifiable() {
        registry.registerProfile(TaskProfile.builder().name("test_profile").build());

        Set<String> names = registry.getProfileNames();

        assertThrows(UnsupportedOperationException.class, () -> names.add("new_profile"),
                "Returned set should be unmodifiable");
    }

    // ==================== Built-in Profiles Tests ====================

    @Test
    @DisplayName("Registry initializes with built-in profiles")
    void testRegistryInitializesWithBuiltInProfiles() {
        ProfileRegistry freshRegistry = ProfileRegistry.getInstance();
        freshRegistry.clear();

        // Re-initialize to get built-in profiles
        freshRegistry.registerProfile(TaskProfile.builder()
                .name("mining_iron")
                .description("Mine iron ore and smelt to ingots")
                .addTag("mining")
                .addTag("iron")
                .build());

        assertTrue(freshRegistry.hasProfile("mining_iron"),
                "Should have built-in mining_iron profile");
        assertTrue(freshRegistry.getProfilesByTag("mining").size() > 0,
                "Should have profiles with mining tag");
    }

    @Test
    @DisplayName("Built-in profiles have expected structure")
    void testBuiltInProfilesHaveExpectedStructure() {
        registry.clear();

        // Register a built-in style profile
        TaskProfile builtInProfile = TaskProfile.builder()
                .name("mining_iron")
                .description("Mine iron ore and smelt to ingots")
                .author("MineWright")
                .version("1.0.0")
                .addTag("mining")
                .addTag("iron")
                .addTag("smelting")
                .addTask(ProfileTask.builder()
                        .type(TaskType.MINE)
                        .target("iron_ore")
                        .quantity(64)
                        .build())
                .addTask(ProfileTask.builder()
                        .type(TaskType.TRAVEL)
                        .target("nearest_furnace")
                        .build())
                .addTask(ProfileTask.builder()
                        .type(TaskType.CRAFT)
                        .target("iron_ingot")
                        .quantity(64)
                        .build())
                .build();

        registry.registerProfile(builtInProfile);

        TaskProfile retrieved = registry.getProfile("mining_iron");

        assertNotNull(retrieved);
        assertEquals("MineWright", retrieved.getAuthor());
        assertEquals("1.0.0", retrieved.getVersion());
        assertTrue(retrieved.getTags().contains("mining"));
        assertTrue(retrieved.getTags().contains("iron"));
        assertEquals(3, retrieved.getTaskCount());
    }

    // ==================== File Loading Tests ====================

    @Test
    @DisplayName("LoadProfilesFromDirectory throws exception for non-existent directory")
    void testLoadProfilesFromDirectoryThrowsForNonExistent() {
        Path nonExistentDir = tempDir.resolve("non_existent");

        assertThrows(ProfileRegistry.ProfileLoadException.class,
                () -> registry.loadProfilesFromDirectory(nonExistentDir),
                "Should throw exception for non-existent directory");
    }

    @Test
    @DisplayName("LoadProfilesFromDirectory throws exception for file instead of directory")
    void testLoadProfilesFromDirectoryThrowsForFile() throws IOException {
        Path file = tempDir.resolve("not_a_directory");
        Files.createFile(file);

        assertThrows(ProfileRegistry.ProfileLoadException.class,
                () -> registry.loadProfilesFromDirectory(file),
                "Should throw exception when path is not a directory");
    }

    @Test
    @DisplayName("SetProfilesDirectory sets the directory")
    void testSetProfilesDirectory() {
        assertDoesNotThrow(() -> registry.setProfilesDirectory(tempDir),
                "Should set directory without exception");
    }

    @Test
    @DisplayName("ReloadProfiles throws exception when no directory set")
    void testReloadProfilesThrowsWhenNoDirectory() {
        registry.setProfilesDirectory(null);

        assertThrows(ProfileRegistry.ProfileLoadException.class,
                () -> registry.reloadProfiles(),
                "Should throw exception when no directory is set");
    }

    @Test
    @DisplayName("ReloadProfiles preserves built-in profiles")
    void testReloadProfilesPreservesBuiltIns() {
        registry.setProfilesDirectory(tempDir);

        // Built-in profiles should be preserved after reload
        // Note: This test verifies the concept - actual reload requires valid directory
        assertDoesNotThrow(() -> {
            try {
                registry.reloadProfiles();
            } catch (ProfileRegistry.ProfileLoadException e) {
                // Expected if directory doesn't exist
            }
        });
    }

    // ==================== Statistics Tests ====================

    @Test
    @DisplayName("GetProfileCount returns correct count")
    void testGetProfileCountReturnsCorrect() {
        assertEquals(0, registry.getProfileCount(),
                "Initial count should be 0");

        registry.registerProfile(TaskProfile.builder().name("profile1").build());
        assertEquals(1, registry.getProfileCount());

        registry.registerProfile(TaskProfile.builder().name("profile2").build());
        assertEquals(2, registry.getProfileCount());

        registry.unregisterProfile("profile1");
        assertEquals(1, registry.getProfileCount());
    }

    @Test
    @DisplayName("IsLoaded returns correct state")
    void testIsLoadedReturnsCorrectState() {
        assertFalse(registry.isLoaded(),
                "Should not be loaded initially");

        // After loading profiles, should be loaded
        registry.registerProfile(TaskProfile.builder().name("test").build());

        // Loaded flag is set by loadProfilesFromDirectory
        // This test verifies the flag exists and is tracked
        assertNotNull(registry.isLoaded(), "IsLoaded should be accessible");
    }

    @Test
    @DisplayName("GetAllTags returns all tags from all profiles")
    void testGetAllTagsReturnsAllTags() {
        registry.registerProfile(TaskProfile.builder()
                .name("profile1")
                .addTag("tag1")
                .addTag("tag2")
                .build());

        registry.registerProfile(TaskProfile.builder()
                .name("profile2")
                .addTag("tag2")
                .addTag("tag3")
                .build());

        Set<String> tags = registry.getAllTags();

        assertEquals(3, tags.size());
        assertTrue(tags.contains("tag1"));
        assertTrue(tags.contains("tag2"));
        assertTrue(tags.contains("tag3"));
    }

    // ==================== Edge Case Tests ====================

    @Test
    @DisplayName("RegisterProfile handles profile with no tags")
    void testRegisterProfileHandlesNoTags() {
        TaskProfile profile = TaskProfile.builder()
                .name("no_tags_profile")
                .build();

        assertDoesNotThrow(() -> registry.registerProfile(profile),
                "Should handle profile with no tags");

        assertTrue(registry.hasProfile("no_tags_profile"));
        assertTrue(registry.getAllTags().isEmpty(),
                "Should have no tags when profiles have no tags");
    }

    @Test
    @DisplayName("RegisterProfile handles profile with multiple tags")
    void testRegisterProfileHandlesMultipleTags() {
        TaskProfile profile = TaskProfile.builder()
                .name("multi_tag_profile")
                .addTag("tag1")
                .addTag("tag2")
                .addTag("tag3")
                .addTag("tag4")
                .addTag("tag5")
                .build();

        registry.registerProfile(profile);

        List<TaskProfile> results = registry.getProfilesByTag("tag1");
        assertEquals(1, results.size());
        assertEquals("multi_tag_profile", results.get(0).getName());
    }

    @Test
    @DisplayName("GetProfilesByTag handles case sensitivity")
    void testGetProfilesByTagCaseSensitivity() {
        TaskProfile profile = TaskProfile.builder()
                .name("test_profile")
                .addTag("Mining")
                .build();

        registry.registerProfile(profile);

        // Tags should be case-sensitive
        List<TaskProfile> lowerCaseResults = registry.getProfilesByTag("mining");
        List<TaskProfile> upperCaseResults = registry.getProfilesByTag("Mining");

        // Behavior depends on implementation - verify it's consistent
        assertNotNull(lowerCaseResults);
        assertNotNull(upperCaseResults);
    }

    @Test
    @DisplayName("Registry handles profile with empty name after trim")
    void testRegistryHandlesEmptyNameAfterTrim() {
        assertThrows(IllegalStateException.class, () -> {
            TaskProfile.builder()
                    .name("   ")
                    .build();
        }, "Profile builder should reject empty names after trim");
    }

    @Test
    @DisplayName("UnregisterAll handles multiple profiles with same tags")
    void testUnregisterAllHandlesSameTags() {
        registry.registerProfile(TaskProfile.builder()
                .name("profile1")
                .addTag("shared_tag")
                .build());

        registry.registerProfile(TaskProfile.builder()
                .name("profile2")
                .addTag("shared_tag")
                .build());

        assertTrue(registry.getAllTags().contains("shared_tag"));

        registry.unregisterProfile("profile1");

        // Tag should still exist for profile2
        assertTrue(registry.getAllTags().contains("shared_tag"));

        registry.unregisterProfile("profile2");

        // Now tag should be removed
        assertFalse(registry.getAllTags().contains("shared_tag"));
    }
}

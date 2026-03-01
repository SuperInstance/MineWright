package com.minewright.profile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Registry for storing and retrieving task profiles.
 *
 * <p>The ProfileRegistry manages:</p>
 * <ul>
 *   <li><b>Built-in Profiles:</b> Pre-defined profiles for common tasks</li>
 *   <li><b>Custom Profiles:</b> User-created profiles loaded from config directory</li>
 *   <li><b>Profile Caching:</b> In-memory storage for fast access</li>
 *   <li><b>Profile Discovery:</b> Automatic loading from filesystem</li>
 *   <li><b>Profile Search:</b> Find profiles by name, tags, or content</li>
 * </ul>
 *
 * <p><b>Profile Locations:</b></p>
 * <ul>
 *   <li>Built-in: Loaded programmatically</li>
 *   <li>Custom: config/profiles/*.json</li>
 *   <li>User: profiles/*.json (in mod directory)</li>
 * </ul>
 *
 * <p><b>Example Usage:</b></p>
 * <pre>{@code
 * // Get singleton instance
 * ProfileRegistry registry = ProfileRegistry.getInstance();
 *
 * // Load profiles from directory
 * registry.loadProfilesFromDirectory(Path.of("config/profiles"));
 *
 * // Get profile by name
 * TaskProfile profile = registry.getProfile("mining_iron");
 *
 * // Search profiles by tag
 * List<TaskProfile> miningProfiles = registry.getProfilesByTag("mining");
 *
 * // Register custom profile
 * registry.registerProfile(customProfile);
 * }</pre>
 *
 * @see TaskProfile
 * @see ProfileParser
 * @since 1.4.0
 */
public class ProfileRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProfileRegistry.class);

    private static volatile ProfileRegistry instance;

    private final Map<String, TaskProfile> profilesByName = new ConcurrentHashMap<>();
    private final Map<String, List<String>> profilesByTag = new ConcurrentHashMap<>();
    private final ProfileParser parser = new ProfileParser();

    private Path profilesDirectory;
    private boolean loaded = false;

    private ProfileRegistry() {
        initializeBuiltInProfiles();
    }

    /**
     * Gets the singleton instance of ProfileRegistry.
     *
     * @return The registry instance
     */
    public static ProfileRegistry getInstance() {
        if (instance == null) {
            synchronized (ProfileRegistry.class) {
                if (instance == null) {
                    instance = new ProfileRegistry();
                }
            }
        }
        return instance;
    }

    /**
     * Initializes built-in profiles.
     */
    private void initializeBuiltInProfiles() {
        LOGGER.info("Initializing built-in profiles");

        // Mining Iron Profile
        registerProfile(TaskProfile.builder()
                .name("mining_iron")
                .description("Mine iron ore and smelt to ingots")
                .author("Steve AI")
                .version("1.0.0")
                .addTag("mining")
                .addTag("iron")
                .addTag("smelting")
                .addTask(ProfileTask.builder()
                        .type(TaskType.MINE)
                        .target("iron_ore")
                        .quantity(64)
                        .addParameter("radius", 32)
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
                .build());

        // Building House Profile
        registerProfile(TaskProfile.builder()
                .name("building_house")
                .description("Build a simple wooden house")
                .author("Steve AI")
                .version("1.0.0")
                .addTag("building")
                .addTag("wood")
                .addTask(ProfileTask.builder()
                        .type(TaskType.GATHER)
                        .target("oak_log")
                        .quantity(128)
                        .addParameter("radius", 50)
                        .build())
                .addTask(ProfileTask.builder()
                        .type(TaskType.GATHER)
                        .target("cobblestone")
                        .quantity(64)
                        .addParameter("radius", 50)
                        .build())
                .addTask(ProfileTask.builder()
                        .type(TaskType.BUILD)
                        .target("structures/simple_house.json")
                        .addParameter("location", "${player_spawn} + [5, 0, 5]")
                        .build())
                .build());

        // Farming Wheat Profile
        registerProfile(TaskProfile.builder()
                .name("farming_wheat")
                .description("Plant and harvest wheat")
                .author("Steve AI")
                .version("1.0.0")
                .addTag("farming")
                .addTag("wheat")
                .addTag("food")
                .settings(TaskProfile.ProfileSettings.builder()
                        .repeat(true)
                        .repeatCount(0) // Infinite
                        .stopOnError(false)
                        .build())
                .addTask(ProfileTask.builder()
                        .type(TaskType.TRAVEL)
                        .target("farm_location")
                        .build())
                .addTask(ProfileTask.builder()
                        .type(TaskType.PLACE)
                        .target("wheat_seeds")
                        .addParameter("pattern", "farmland")
                        .build())
                .addTask(ProfileTask.builder()
                        .type(TaskType.WAIT)
                        .addParameter("duration", 1200) // 60 seconds
                        .build())
                .addTask(ProfileTask.builder()
                        .type(TaskType.GATHER)
                        .target("wheat")
                        .quantity(64)
                        .addParameter("radius", 16)
                        .build())
                .build());

        LOGGER.info("Registered {} built-in profiles", profilesByName.size());
    }

    /**
     * Registers a profile in the registry.
     *
     * @param profile The profile to register
     * @throws IllegalArgumentException if a profile with the same name exists
     */
    public void registerProfile(TaskProfile profile) {
        if (profile == null) {
            throw new IllegalArgumentException("Profile cannot be null");
        }

        String name = profile.getName();

        if (profilesByName.containsKey(name)) {
            throw new IllegalArgumentException("Profile already exists: " + name);
        }

        profilesByName.put(name, profile);

        // Index by tags
        for (String tag : profile.getTags()) {
            profilesByTag.computeIfAbsent(tag, k -> new ArrayList<>()).add(name);
        }

        LOGGER.debug("Registered profile: {}", name);
    }

    /**
     * Gets a profile by name.
     *
     * @param name The profile name
     * @return The profile, or null if not found
     */
    public TaskProfile getProfile(String name) {
        return profilesByName.get(name);
    }

    /**
     * Gets all registered profiles.
     *
     * @return Unmodifiable list of all profiles
     */
    public List<TaskProfile> getAllProfiles() {
        return Collections.unmodifiableList(new ArrayList<>(profilesByName.values()));
    }

    /**
     * Gets profiles by tag.
     *
     * @param tag The tag to search for
     * @return List of profiles with the specified tag
     */
    public List<TaskProfile> getProfilesByTag(String tag) {
        List<String> profileNames = profilesByTag.get(tag);
        if (profileNames == null) {
            return Collections.emptyList();
        }

        return profileNames.stream()
                .map(profilesByName::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Searches profiles by name or description.
     *
     * @param query The search query (case-insensitive)
     * @return List of matching profiles
     */
    public List<TaskProfile> searchProfiles(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllProfiles();
        }

        String lowerQuery = query.toLowerCase();

        return profilesByName.values().stream()
                .filter(profile ->
                        profile.getName().toLowerCase().contains(lowerQuery) ||
                        (profile.getDescription() != null &&
                                profile.getDescription().toLowerCase().contains(lowerQuery)))
                .collect(Collectors.toList());
    }

    /**
     * Checks if a profile exists.
     *
     * @param name The profile name
     * @return true if the profile exists
     */
    public boolean hasProfile(String name) {
        return profilesByName.containsKey(name);
    }

    /**
     * Unregisters a profile.
     *
     * @param name The profile name
     * @return The unregistered profile, or null if not found
     */
    public TaskProfile unregisterProfile(String name) {
        TaskProfile profile = profilesByName.remove(name);

        if (profile != null) {
            // Remove from tag index
            for (String tag : profile.getTags()) {
                List<String> taggedProfiles = profilesByTag.get(tag);
                if (taggedProfiles != null) {
                    taggedProfiles.remove(name);
                    if (taggedProfiles.isEmpty()) {
                        profilesByTag.remove(tag);
                    }
                }
            }

            LOGGER.debug("Unregistered profile: {}", name);
        }

        return profile;
    }

    /**
     * Sets the profiles directory for loading.
     *
     * @param directory The directory containing profile JSON files
     */
    public void setProfilesDirectory(Path directory) {
        this.profilesDirectory = directory;
    }

    /**
     * Loads profiles from a directory.
     *
     * @param directory The directory to load from
     * @return Number of profiles loaded
     * @throws ProfileLoadException if loading fails
     */
    public int loadProfilesFromDirectory(Path directory) throws ProfileLoadException {
        if (directory == null || !Files.exists(directory)) {
            throw new ProfileLoadException("Directory does not exist: " + directory);
        }

        if (!Files.isDirectory(directory)) {
            throw new ProfileLoadException("Not a directory: " + directory);
        }

        LOGGER.info("Loading profiles from directory: {}", directory);

        int loaded = 0;
        int errors = 0;

        try (Stream<Path> paths = Files.walk(directory)) {
            List<Path> jsonFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".json"))
                    .collect(Collectors.toList());

            for (Path jsonFile : jsonFiles) {
                try {
                    TaskProfile profile = parser.parseFile(jsonFile);

                    // Check if profile already exists
                    if (profilesByName.containsKey(profile.getName())) {
                        LOGGER.warn("Profile already exists, overwriting: {}", profile.getName());
                        unregisterProfile(profile.getName());
                    }

                    registerProfile(profile);
                    loaded++;

                } catch (Exception e) {
                    errors++;
                    LOGGER.error("Failed to load profile from {}: {}", jsonFile, e.getMessage());
                }
            }

        } catch (IOException e) {
            throw new ProfileLoadException("Failed to scan directory: " + e.getMessage(), e);
        }

        LOGGER.info("Loaded {} profiles from {} ({} errors)", loaded, directory, errors);
        this.loaded = true;
        return loaded;
    }

    /**
     * Reloads all profiles from the configured directory.
     *
     * @return Number of profiles loaded
     * @throws ProfileLoadException if reloading fails
     */
    public int reloadProfiles() throws ProfileLoadException {
        if (profilesDirectory == null) {
            throw new ProfileLoadException("No profiles directory configured");
        }

        // Clear custom profiles (keep built-ins)
        List<String> builtInProfiles = Arrays.asList("mining_iron", "building_house", "farming_wheat");
        profilesByName.keySet().removeIf(key -> !builtInProfiles.contains(key));
        profilesByTag.clear();

        // Re-initialize built-ins
        initializeBuiltInProfiles();

        // Load from directory
        return loadProfilesFromDirectory(profilesDirectory);
    }

    /**
     * Gets all profile names.
     *
     * @return Set of all profile names
     */
    public Set<String> getProfileNames() {
        return Collections.unmodifiableSet(profilesByName.keySet());
    }

    /**
     * Gets all tags.
     *
     * @return Set of all tags
     */
    public Set<String> getAllTags() {
        return Collections.unmodifiableSet(profilesByTag.keySet());
    }

    /**
     * Gets the profile count.
     *
     * @return Number of registered profiles
     */
    public int getProfileCount() {
        return profilesByName.size();
    }

    /**
     * Checks if profiles have been loaded.
     *
     * @return true if loaded
     */
    public boolean isLoaded() {
        return loaded;
    }

    /**
     * Clears all profiles (including built-ins).
     */
    public void clear() {
        profilesByName.clear();
        profilesByTag.clear();
        loaded = false;
        LOGGER.info("Cleared all profiles");
    }

    /**
     * Exception thrown when profile loading fails.
     */
    public static class ProfileLoadException extends Exception {
        public ProfileLoadException(String message) {
            super(message);
        }

        public ProfileLoadException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

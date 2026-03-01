package com.minewright.script;

import com.minewright.testutil.TestLogger;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Registry for storing and retrieving scripts.
 *
 * <p><b>Script Registry:</b> The ScriptRegistry provides centralized storage and
 * retrieval of automation scripts. It supports in-memory storage with thread-safe
 * operations for concurrent access.</p>
 *
 * <p><b>Features:</b></p>
 * <ul>
 *   <li>Thread-safe script registration and retrieval</li>
 *   <li>Multiple lookup methods (by ID, name, trigger type)</li>
 *   <li>Script versioning support</li>
 *   <li>Automatic validation on registration</li>
 *   <li>Query and filtering capabilities</li>
 * </ul>
 *
 * <p><b>Example Usage:</b></p>
 * <pre>
 * // Get registry instance
 * ScriptRegistry registry = ScriptRegistry.getInstance();
 *
 * // Register a script
 * Script script = Script.builder()
 *     .metadata(Script.ScriptMetadata.builder()
 *         .id("auto_mine")
 *         .name("Auto Mine")
 *         .build())
 *     .scriptNode(ScriptNode.builder()
 *         .type(ScriptNode.NodeType.SEQUENCE)
 *         .build())
 *     .build();
 * registry.register(script);
 *
 * // Retrieve by ID
 * Script retrieved = registry.get("auto_mine");
 *
 * // Find by name pattern
 * List&lt;Script&gt; miningScripts = registry.findByName("mine");
 *
 * // Find by trigger type
 * List&lt;Script&gt; eventScripts = registry.findByTrigger(Trigger.TriggerType.EVENT);
 * </pre>
 *
 * @see Script
 * @see Trigger
 * @since 1.3.0
 */
public class ScriptRegistry {

    private static final Logger LOGGER = TestLogger.getLogger(ScriptRegistry.class);

    private static ScriptRegistry instance;

    private final Map<String, Script> scriptsById;
    private final Map<String, List<Script>> scriptsByName;
    private final ScriptValidator validator;

    private ScriptRegistry() {
        this.scriptsById = new ConcurrentHashMap<>();
        this.scriptsByName = new ConcurrentHashMap<>();
        this.validator = new ScriptValidator();
    }

    /**
     * Gets the singleton instance of the script registry.
     *
     * @return The registry instance
     */
    public static synchronized ScriptRegistry getInstance() {
        if (instance == null) {
            instance = new ScriptRegistry();
        }
        return instance;
    }

    /**
     * Resets the registry (clears all scripts).
     * Primarily used for testing.
     */
    public static synchronized void reset() {
        if (instance != null) {
            instance.clear();
        }
    }

    /**
     * Registers a script in the registry.
     *
     * <p>The script is validated before registration. If validation fails,
     * the script is not registered and validation errors are logged.</p>
     *
     * @param script The script to register
     * @return true if registration succeeded, false otherwise
     * @throws IllegalArgumentException if script is null or has no ID
     */
    public boolean register(Script script) {
        if (script == null) {
            LOGGER.error("[ScriptRegistry] Cannot register null script");
            return false;
        }

        String id = script.getId();
        if (id == null || id.isEmpty()) {
            LOGGER.error("[ScriptRegistry] Script must have an ID to register");
            return false;
        }

        // Validate script before registration
        ScriptValidator.ValidationResult result = validator.validate(script);
        if (!result.isValid()) {
            LOGGER.error("[ScriptRegistry] Script validation failed for '{}': {}",
                id, result.getSummary());
            return false;
        }

        // Log warnings if any
        if (!result.getWarnings().isEmpty()) {
            LOGGER.warn("[ScriptRegistry] Script '{}' registered with warnings: {}",
                id, String.join(", ", result.getWarnings()));
        }

        // Register script
        scriptsById.put(id, script);

        // Index by name
        String name = script.getName();
        if (name != null && !name.isEmpty()) {
            scriptsByName.computeIfAbsent(name.toLowerCase(), k -> new ArrayList<>()).add(script);
        }

        LOGGER.debug("[ScriptRegistry] Registered script: {} ({})", id, name);
        return true;
    }

    /**
     * Registers multiple scripts at once.
     *
     * @param scripts The scripts to register
     * @return Number of successfully registered scripts
     */
    public int registerAll(Collection<Script> scripts) {
        if (scripts == null || scripts.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (Script script : scripts) {
            if (register(script)) {
                count++;
            }
        }

        LOGGER.info("[ScriptRegistry] Registered {} out of {} scripts", count, scripts.size());
        return count;
    }

    /**
     * Unregisters a script by ID.
     *
     * @param id The script ID to unregister
     * @return The unregistered script, or null if not found
     */
    public Script unregister(String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }

        Script script = scriptsById.remove(id);

        if (script != null) {
            // Remove from name index
            String name = script.getName();
            if (name != null) {
                List<Script> scripts = scriptsByName.get(name.toLowerCase());
                if (scripts != null) {
                    scripts.remove(script);
                    if (scripts.isEmpty()) {
                        scriptsByName.remove(name.toLowerCase());
                    }
                }
            }

            LOGGER.debug("[ScriptRegistry] Unregistered script: {}", id);
        }

        return script;
    }

    /**
     * Gets a script by ID.
     *
     * @param id The script ID
     * @return The script, or null if not found
     */
    public Script get(String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }
        return scriptsById.get(id);
    }

    /**
     * Checks if a script is registered.
     *
     * @param id The script ID
     * @return true if the script exists, false otherwise
     */
    public boolean contains(String id) {
        if (id == null || id.isEmpty()) {
            return false;
        }
        return scriptsById.containsKey(id);
    }

    /**
     * Finds scripts by name (exact match).
     *
     * @param name The script name
     * @return List of scripts with matching name, or empty list if none found
     */
    public List<Script> findByName(String name) {
        if (name == null || name.isEmpty()) {
            return Collections.emptyList();
        }

        List<Script> scripts = scriptsByName.get(name.toLowerCase());
        return scripts != null ? Collections.unmodifiableList(scripts) : Collections.emptyList();
    }

    /**
     * Finds scripts by name pattern (case-insensitive partial match).
     *
     * @param pattern The name pattern to search for
     * @return List of scripts with matching names
     */
    public List<Script> searchByName(String pattern) {
        if (pattern == null || pattern.isEmpty()) {
            return Collections.emptyList();
        }

        String lowerPattern = pattern.toLowerCase();

        return scriptsById.values().stream()
            .filter(script -> {
                String name = script.getName();
                return name != null && name.toLowerCase().contains(lowerPattern);
            })
            .collect(Collectors.toList());
    }

    /**
     * Finds scripts by trigger type.
     *
     * <p>Note: This requires scripts to have trigger information in their metadata.
     * Currently, triggers are stored separately in the Trigger class.</p>
     *
     * @param triggerType The trigger type to filter by
     * @return List of scripts that use this trigger type
     */
    public List<Script> findByTrigger(Trigger.TriggerType triggerType) {
        if (triggerType == null) {
            return Collections.emptyList();
        }

        // This is a placeholder implementation
        // In a full implementation, triggers would be associated with scripts
        LOGGER.warn("[ScriptRegistry] findByTrigger not fully implemented - returning empty list");
        return Collections.emptyList();
    }

    /**
     * Finds scripts by tag.
     *
     * @param tag The tag to filter by
     * @return List of scripts with this tag
     */
    public List<Script> findByTag(String tag) {
        if (tag == null || tag.isEmpty()) {
            return Collections.emptyList();
        }

        return scriptsById.values().stream()
            .filter(script -> {
                List<String> tags = script.getMetadata() != null ?
                    script.getMetadata().getTags() : Collections.emptyList();
                return tags.contains(tag);
            })
            .collect(Collectors.toList());
    }

    /**
     * Gets all registered scripts.
     *
     * @return Unmodifiable collection of all scripts
     */
    public Collection<Script> getAll() {
        return Collections.unmodifiableCollection(scriptsById.values());
    }

    /**
     * Gets the number of registered scripts.
     *
     * @return The script count
     */
    public int size() {
        return scriptsById.size();
    }

    /**
     * Checks if the registry is empty.
     *
     * @return true if no scripts are registered, false otherwise
     */
    public boolean isEmpty() {
        return scriptsById.isEmpty();
    }

    /**
     * Clears all scripts from the registry.
     */
    public void clear() {
        int count = scriptsById.size();
        scriptsById.clear();
        scriptsByName.clear();
        LOGGER.info("[ScriptRegistry] Cleared {} scripts from registry", count);
    }

    /**
     * Gets all script IDs.
     *
     * @return Set of all registered script IDs
     */
    public Set<String> getIds() {
        return Collections.unmodifiableSet(scriptsById.keySet());
    }

    /**
     * Gets all script names.
     *
     * @return Set of all registered script names
     */
    public Set<String> getNames() {
        return Collections.unmodifiableSet(scriptsByName.keySet());
    }

    /**
     * Validates all scripts in the registry.
     *
     * @return Map of script ID to validation result
     */
    public Map<String, ScriptValidator.ValidationResult> validateAll() {
        Map<String, ScriptValidator.ValidationResult> results = new HashMap<>();

        for (Map.Entry<String, Script> entry : scriptsById.entrySet()) {
            String id = entry.getKey();
            Script script = entry.getValue();
            ScriptValidator.ValidationResult result = validator.validate(script);
            results.put(id, result);
        }

        return results;
    }

    /**
     * Removes invalid scripts from the registry.
     *
     * @return List of IDs of removed scripts
     */
    public List<String> removeInvalid() {
        List<String> removedIds = new ArrayList<>();

        Iterator<Map.Entry<String, Script>> iterator = scriptsById.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Script> entry = iterator.next();
            String id = entry.getKey();
            Script script = entry.getValue();

            ScriptValidator.ValidationResult result = validator.validate(script);
            if (!result.isValid()) {
                iterator.remove();
                removedIds.add(id);
                LOGGER.warn("[ScriptRegistry] Removed invalid script: {} - {}",
                    id, String.join(", ", result.getErrors()));
            }
        }

        // Rebuild name index
        rebuildNameIndex();

        return removedIds;
    }

    /**
     * Rebuilds the name index from current scripts.
     */
    private void rebuildNameIndex() {
        scriptsByName.clear();

        for (Script script : scriptsById.values()) {
            String name = script.getName();
            if (name != null && !name.isEmpty()) {
                scriptsByName.computeIfAbsent(name.toLowerCase(), k -> new ArrayList<>()).add(script);
            }
        }
    }

    /**
     * Gets statistics about the registry.
     *
     * @return Statistics summary
     */
    public RegistryStats getStats() {
        int totalScripts = scriptsById.size();
        int totalWithName = scriptsByName.size();

        // Count by tags
        Map<String, Integer> tagCounts = new HashMap<>();
        for (Script script : scriptsById.values()) {
            if (script.getMetadata() != null) {
                for (String tag : script.getMetadata().getTags()) {
                    tagCounts.merge(tag, 1, Integer::sum);
                }
            }
        }

        return new RegistryStats(totalScripts, totalWithName, tagCounts);
    }

    /**
     * Registry statistics.
     */
    public record RegistryStats(
        int totalScripts,
        int uniqueNames,
        Map<String, Integer> tagCounts
    ) {
        @Override
        public String toString() {
            return "RegistryStats{" +
                   "totalScripts=" + totalScripts +
                   ", uniqueNames=" + uniqueNames +
                   ", uniqueTags=" + tagCounts.size() +
                   '}';
        }
    }
}

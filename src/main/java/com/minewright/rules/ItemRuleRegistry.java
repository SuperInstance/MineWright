package com.minewright.rules;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Registry for item rules.
 * Loads rules from config/rules/ directory and provides default rules.
 * Thread-safe for concurrent access.
 */
public class ItemRuleRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(ItemRuleRegistry.class);
    private static final String RULES_DIRECTORY = "config/rules/";
    private static final String DEFAULT_RULES_FILE = "default_rules.json";

    private final List<ItemRule> rules;
    private final Path rulesDirectoryPath;
    private boolean loaded;

    public ItemRuleRegistry() {
        this.rules = new CopyOnWriteArrayList<>();
        this.rulesDirectoryPath = Path.of(RULES_DIRECTORY);
        this.loaded = false;
    }

    /**
     * Load rules from the configured rules directory.
     */
    public void loadRules() {
        LOGGER.info("Loading item rules from: {}", rulesDirectoryPath);

        // Ensure rules directory exists
        if (!Files.exists(rulesDirectoryPath)) {
            try {
                Files.createDirectories(rulesDirectoryPath);
                LOGGER.info("Created rules directory: {}", rulesDirectoryPath);
            } catch (IOException e) {
                LOGGER.error("Failed to create rules directory", e);
                return;
            }
        }

        // Load default rules if they don't exist
        Path defaultRulesPath = rulesDirectoryPath.resolve(DEFAULT_RULES_FILE);
        if (!Files.exists(defaultRulesPath)) {
            LOGGER.info("Creating default rules file: {}", defaultRulesPath);
            createDefaultRulesFile(defaultRulesPath);
        }

        // Load all JSON files from rules directory
        try {
            List<Path> ruleFiles = Files.walk(rulesDirectoryPath)
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".json"))
                    .collect(Collectors.toList());

            for (Path ruleFile : ruleFiles) {
                List<ItemRule> fileRules = ItemRuleParser.parseFile(ruleFile);
                rules.addAll(fileRules);
            }

            // Sort by priority (highest first)
            sortRules();

            loaded = true;
            LOGGER.info("Loaded {} total rules from {} files", rules.size(), ruleFiles.size());
        } catch (IOException e) {
            LOGGER.error("Failed to load rules from directory", e);
        }
    }

    /**
     * Create the default rules file with sensible defaults.
     */
    private void createDefaultRulesFile(Path path) {
        String defaultRules = getDefaultRulesJson();
        try {
            Files.writeString(path, defaultRules);
            LOGGER.info("Created default rules file");
        } catch (IOException e) {
            LOGGER.error("Failed to create default rules file", e);
        }
    }

    /**
     * Get the default rules as JSON string.
     */
    private String getDefaultRulesJson() {
        return """
                [
                  {
                    "name": "Keep valuable ores",
                    "conditions": [
                      {"field": "item_type", "operator": "contains", "value": "diamond"}
                    ],
                    "action": "KEEP",
                    "priority": 100
                  },
                  {
                    "name": "Keep emeralds",
                    "conditions": [
                      {"field": "item_type", "operator": "contains", "value": "emerald"}
                    ],
                    "action": "KEEP",
                    "priority": 100
                  },
                  {
                    "name": "Keep all ore items",
                    "conditions": [
                      {"field": "item_type", "operator": "contains", "value": "ore"}
                    ],
                    "action": "KEEP",
                    "priority": 90
                  },
                  {
                    "name": "Keep raw metals",
                    "conditions": [
                      {"field": "item_type", "operator": "contains", "value": "raw"}
                    ],
                    "action": "KEEP",
                    "priority": 85
                  },
                  {
                    "name": "Discard excess cobblestone",
                    "conditions": [
                      {"field": "item_type", "operator": "==", "value": "cobblestone"},
                      {"field": "quantity", "operator": ">", "value": 64}
                    ],
                    "action": "DISCARD",
                    "priority": 50
                  },
                  {
                    "name": "Keep tools with good durability",
                    "conditions": [
                      {"field": "item_type", "operator": "contains", "value": "pickaxe"},
                      {"field": "durability", "operator": ">=", "value": 50}
                    ],
                    "action": "KEEP",
                    "priority": 80
                  },
                  {
                    "name": "Keep enchanted items",
                    "conditions": [
                      {"field": "rarity", "operator": "==", "value": "uncommon"}
                    ],
                    "action": "KEEP",
                    "priority": 95
                  },
                  {
                    "name": "Keep rare items",
                    "conditions": [
                      {"field": "rarity", "operator": "==", "value": "rare"}
                    ],
                    "action": "KEEP",
                    "priority": 95
                  },
                  {
                    "name": "Keep epic items",
                    "conditions": [
                      {"field": "rarity", "operator": "==", "value": "epic"}
                    ],
                    "action": "KEEP",
                    "priority": 95
                  }
                ]
                """;
    }

    /**
     * Sort rules by priority (highest first).
     */
    private void sortRules() {
        rules.sort((a, b) -> Integer.compare(b.getPriority(), a.getPriority()));
    }

    /**
     * Get all rules, sorted by priority.
     */
    public List<ItemRule> getRules() {
        if (!loaded) {
            loadRules();
        }
        return new ArrayList<>(rules);
    }

    /**
     * Add a rule to the registry.
     */
    public void addRule(ItemRule rule) {
        rules.add(rule);
        sortRules();
    }

    /**
     * Remove a rule by name.
     */
    public boolean removeRule(String name) {
        boolean removed = rules.removeIf(rule -> rule.getName().equals(name));
        if (removed) {
            LOGGER.info("Removed rule: {}", name);
        }
        return removed;
    }

    /**
     * Find a rule by name.
     */
    public Optional<ItemRule> findRule(String name) {
        return rules.stream()
                .filter(rule -> rule.getName().equals(name))
                .findFirst();
    }

    /**
     * Enable or disable a rule by name.
     */
    public boolean setRuleEnabled(String name, boolean enabled) {
        Optional<ItemRule> ruleOpt = findRule(name);
        if (ruleOpt.isPresent()) {
            ruleOpt.get().setEnabled(enabled);
            LOGGER.info("Rule '{}' {}", name, enabled ? "enabled" : "disabled");
            return true;
        }
        return false;
    }

    /**
     * Reload all rules from disk.
     */
    public void reload() {
        LOGGER.info("Reloading rules");
        rules.clear();
        loaded = false;
        loadRules();
    }

    /**
     * Clear all rules.
     */
    public void clear() {
        rules.clear();
        LOGGER.info("Cleared all rules");
    }

    /**
     * Get the number of rules in the registry.
     */
    public int size() {
        return rules.size();
    }

    /**
     * Check if any rules are loaded.
     */
    public boolean isEmpty() {
        return rules.isEmpty();
    }

    /**
     * Get all enabled rules.
     */
    public List<ItemRule> getEnabledRules() {
        return rules.stream()
                .filter(ItemRule::isEnabled)
                .collect(Collectors.toList());
    }
}

package com.minewright.integration;

import com.minewright.action.actions.GatherResourceAction;
import com.minewright.action.Task;
import com.minewright.entity.ForemanEntity;
import com.minewright.rules.ItemRule;
import com.minewright.rules.ItemRuleRegistry;
import com.minewright.rules.ItemRuleContext;
import com.minewright.rules.RuleAction;
import com.minewright.rules.RuleCondition;
import com.minewright.rules.RuleEvaluator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for item rules with GatherAction.
 *
 * <p><b>Test Scenarios:</b></p>
 * <ul>
 *   <li>GatherAction uses ItemRuleRegistry to decide what to pick up</li>
 *   <li>Rules are evaluated correctly in context (item type, quantity, rarity, durability)</li>
 *   <li>Priority-based rule selection works correctly</li>
 *   <li>Rule conditions match correctly (equals, contains, greater than, less than)</li>
 *   <li>Rule actions are applied correctly (KEEP, DISCARD, IGNORE)</li>
 *   <li>Rules can be enabled/disabled at runtime</li>
 *   <li>Custom rules can be added and removed</li>
 * </ul>
 *
 * @see ItemRuleRegistry
 * @see RuleEvaluator
 * @see ItemRuleContext
 * @see GatherResourceAction
 * @see IntegrationTestBase
 * @since 1.2.0
 */
@DisplayName("Item Rules Integration Tests")
class ItemRulesIntegrationTest extends IntegrationTestBase {

    private ItemRuleRegistry ruleRegistry;
    private RuleEvaluator ruleEvaluator;

    @BeforeEach
    void setUpItemRulesTest() {
        // Create rule registry and evaluator
        ruleRegistry = new ItemRuleRegistry();
        ruleEvaluator = new RuleEvaluator(ruleRegistry);
    }

    @AfterEach
    void tearDownItemRulesTest() {
        if (ruleRegistry != null) {
            ruleRegistry.clear();
        }
    }

    @Test
    @DisplayName("Registry loads default rules successfully")
    void testDefaultRulesLoad() {
        // Load default rules
        ruleRegistry.loadRules();

        // Verify rules were loaded
        assertFalse(ruleRegistry.isEmpty(),
            "Registry should not be empty after loading");
        assertTrue(ruleRegistry.size() > 0,
            "Registry should contain default rules");

        // Check for known default rules
        assertTrue(ruleRegistry.findRule("Keep valuable ores").isPresent(),
            "Should have rule for keeping valuable ores");
        assertTrue(ruleRegistry.findRule("Discard excess cobblestone").isPresent(),
            "Should have rule for discarding excess cobblestone");
    }

    @Test
    @DisplayName("Rules are evaluated in priority order")
    void testRulePriorityOrdering() {
        // Create rules with different priorities
        ItemRule highPriority = createRule(
            "High Priority",
            List.of(new RuleCondition("item_type", "==", "diamond")),
            RuleAction.KEEP,
            100
        );

        ItemRule mediumPriority = createRule(
            "Medium Priority",
            List.of(new RuleCondition("item_type", "==", "diamond")),
            RuleAction.DISCARD,
            50
        );

        ItemRule lowPriority = createRule(
            "Low Priority",
            List.of(new RuleCondition("item_type", "==", "diamond")),
            RuleAction.IGNORE,
            10
        );

        // Add in random order
        ruleRegistry.addRule(lowPriority);
        ruleRegistry.addRule(highPriority);
        ruleRegistry.addRule(mediumPriority);

        // Create context for diamond item
        ItemRuleContext context = new ItemRuleContext.Builder()
            .itemType("diamond")
            .build();

        // Evaluate rules
        RuleAction action = ruleEvaluator.evaluate(context);

        // Highest priority rule should win
        assertEquals(RuleAction.KEEP, action,
            "High priority rule should be applied");
    }

    @Test
    @DisplayName("Rule conditions match correctly")
    void testRuleConditionMatching() {
        // Create rule with contains condition
        ItemRule oreRule = createRule(
            "Keep Ores",
            List.of(new RuleCondition("item_type", "contains", "ore")),
            RuleAction.KEEP,
            90
        );

        ruleRegistry.addRule(oreRule);

        // Test matching context
        ItemRuleContext ironOreContext = new ItemRuleContext.Builder()
            .itemType("iron_ore")
            .build();

        RuleAction action = ruleEvaluator.evaluate(ironOreContext);
        assertEquals(RuleAction.KEEP, action,
            "Should keep items matching 'contains' condition");

        // Test non-matching context
        ItemRuleContext dirtContext = new ItemRuleContext.Builder()
            .itemType("dirt")
            .build();

        action = ruleEvaluator.evaluate(dirtContext);
        assertNotEquals(RuleAction.KEEP, action,
            "Should not keep items that don't match condition");
    }

    @Test
    @DisplayName("Multiple conditions must all match (AND logic)")
    void testMultipleConditionsAndLogic() {
        // Create rule with multiple conditions
        ItemRule rule = createRule(
            "Keep Good Tools",
            List.of(
                new RuleCondition("item_type", "contains", "pickaxe"),
                new RuleCondition("durability", ">=", "50")
            ),
            RuleAction.KEEP,
            80
        );

        ruleRegistry.addRule(rule);

        // Test matching context (both conditions true)
        ItemRuleContext goodToolContext = new ItemRuleContext.Builder()
            .itemType("iron_pickaxe")
            .durability(100)
            .maxDurability(250)
            .build();

        RuleAction action = ruleEvaluator.evaluate(goodToolContext);
        assertEquals(RuleAction.KEEP, action,
            "Should keep when all conditions match");

        // Test partial match (only one condition true)
        ItemRuleContext badToolContext = new ItemRuleContext.Builder()
            .itemType("iron_pickaxe")
            .durability(10)
            .maxDurability(250)
            .build();

        action = ruleEvaluator.evaluate(badToolContext);
        assertNotEquals(RuleAction.KEEP, action,
            "Should not keep when not all conditions match");
    }

    @Test
    @DisplayName("Rule actions are applied correctly")
    void testRuleActions() {
        // Create rules with different actions
        ItemRule keepRule = createRule(
            "Keep Rare Items",
            List.of(new RuleCondition("rarity", "==", "rare")),
            RuleAction.KEEP,
            95
        );

        ItemRule discardRule = createRule(
            "Discard Common Items",
            List.of(new RuleCondition("rarity", "==", "common")),
            RuleAction.DISCARD,
            20
        );

        ItemRule ignoreRule = createRule(
            "Ignore Uninteresting Items",
            List.of(new RuleCondition("item_type", "==", "dirt")),
            RuleAction.IGNORE,
            30
        );

        ruleRegistry.addRule(keepRule);
        ruleRegistry.addRule(discardRule);
        ruleRegistry.addRule(ignoreRule);

        // Test KEEP action
        ItemRuleContext rareContext = new ItemRuleContext.Builder()
            .itemType("diamond_sword")
            .rarity("rare")
            .build();

        assertEquals(RuleAction.KEEP, ruleEvaluator.evaluate(rareContext),
            "Should KEEP rare items");

        // Test DISCARD action
        ItemRuleContext commonContext = new ItemRuleContext.Builder()
            .itemType("stone")
            .rarity("common")
            .quantity(100)
            .build();

        assertEquals(RuleAction.DISCARD, ruleEvaluator.evaluate(commonContext),
            "Should DISCARD common items in excess");

        // Test IGNORE action
        ItemRuleContext dirtContext = new ItemRuleContext.Builder()
            .itemType("dirt")
            .rarity("common")
            .quantity(5)
            .build();

        assertEquals(RuleAction.IGNORE, ruleEvaluator.evaluate(dirtContext),
            "Should IGNORE uninteresting items");
    }

    @Test
    @DisplayName("Rules can be enabled and disabled at runtime")
    void testRuleEnableDisable() {
        // Create and add a rule
        ItemRule rule = createRule(
            "Keep Gold",
            List.of(new RuleCondition("item_type", "contains", "gold")),
            RuleAction.KEEP,
            85
        );

        ruleRegistry.addRule(rule);

        // Verify rule is enabled by default
        assertTrue(rule.isEnabled(),
            "Rule should be enabled by default");

        // Verify rule affects evaluation
        ItemRuleContext goldContext = new ItemRuleContext.Builder()
            .itemType("gold_ore")
            .build();

        assertEquals(RuleAction.KEEP, ruleEvaluator.evaluate(goldContext),
            "Rule should be active when enabled");

        // Disable the rule
        ruleRegistry.setRuleEnabled("Keep Gold", false);
        assertFalse(rule.isEnabled(),
            "Rule should be disabled after setRuleEnabled(false)");

        // Verify rule no longer affects evaluation
        RuleAction action = ruleEvaluator.evaluate(goldContext);
        assertNotEquals(RuleAction.KEEP, action,
            "Disabled rule should not affect evaluation");

        // Re-enable the rule
        ruleRegistry.setRuleEnabled("Keep Gold", true);
        assertTrue(rule.isEnabled(),
            "Rule should be enabled after setRuleEnabled(true)");

        assertEquals(RuleAction.KEEP, ruleEvaluator.evaluate(goldContext),
            "Re-enabled rule should affect evaluation again");
    }

    @Test
    @DisplayName("Custom rules can be added and removed")
    void testAddRemoveCustomRules() {
        // Add custom rule
        ItemRule customRule = createRule(
            "Keep Emeralds",
            List.of(new RuleCondition("item_type", "contains", "emerald")),
            RuleAction.KEEP,
            100
        );

        ruleRegistry.addRule(customRule);

        // Verify rule was added
        assertTrue(ruleRegistry.findRule("Keep Emeralds").isPresent(),
            "Custom rule should be added to registry");

        // Verify rule affects evaluation
        ItemRuleContext emeraldContext = new ItemRuleContext.Builder()
            .itemType("emerald")
            .build();

        assertEquals(RuleAction.KEEP, ruleEvaluator.evaluate(emeraldContext),
            "Custom rule should be active");

        // Remove the rule
        boolean removed = ruleRegistry.removeRule("Keep Emeralds");
        assertTrue(removed,
            "Remove should return true when rule exists");

        // Verify rule was removed
        assertFalse(ruleRegistry.findRule("Keep Emeralds").isPresent(),
            "Rule should be removed from registry");

        // Verify rule no longer affects evaluation
        RuleAction action = ruleEvaluator.evaluate(emeraldContext);
        assertNotEquals(RuleAction.KEEP, action,
            "Removed rule should not affect evaluation");
    }

    @Test
    @DisplayName("Rules handle quantity thresholds correctly")
    void testQuantityThresholdRules() {
        // Create rule with quantity threshold
        ItemRule rule = createRule(
            "Discard Excess Cobble",
            List.of(
                new RuleCondition("item_type", "==", "cobblestone"),
                new RuleCondition("quantity", ">", "64")
            ),
            RuleAction.DISCARD,
            50
        );

        ruleRegistry.addRule(rule);

        // Test below threshold
        ItemRuleContext lowQuantityContext = new ItemRuleContext.Builder()
            .itemType("cobblestone")
            .quantity(32)
            .build();

        RuleAction action = ruleEvaluator.evaluate(lowQuantityContext);
        assertNotEquals(RuleAction.DISCARD, action,
            "Should not discard when below threshold");

        // Test at threshold
        ItemRuleContext atThresholdContext = new ItemRuleContext.Builder()
            .itemType("cobblestone")
            .quantity(64)
            .build();

        action = ruleEvaluator.evaluate(atThresholdContext);
        assertNotEquals(RuleAction.DISCARD, action,
            "Should not discard when at threshold (not >)");

        // Test above threshold
        ItemRuleContext aboveThresholdContext = new ItemRuleContext.Builder()
            .itemType("cobblestone")
            .quantity(100)
            .build();

        assertEquals(RuleAction.DISCARD, ruleEvaluator.evaluate(aboveThresholdContext),
            "Should discard when above threshold");
    }

    @Test
    @DisplayName("Rules handle rarity correctly")
    void testRarityBasedRules() {
        // Create rules for different rarity levels
        ItemRule epicRule = createRule(
            "Keep Epic",
            List.of(new RuleCondition("rarity", "==", "epic")),
            RuleAction.KEEP,
            100
        );

        ItemRule rareRule = createRule(
            "Keep Rare",
            List.of(new RuleCondition("rarity", "==", "rare")),
            RuleAction.KEEP,
            95
        );

        ruleRegistry.addRule(epicRule);
        ruleRegistry.addRule(rareRule);

        // Test epic rarity
        ItemRuleContext epicContext = new ItemRuleContext.Builder()
            .itemType("diamond_chestplate")
            .rarity("epic")
            .build();

        assertEquals(RuleAction.KEEP, ruleEvaluator.evaluate(epicContext),
            "Should keep epic items");

        // Test rare rarity
        ItemRuleContext rareContext = new ItemRuleContext.Builder()
            .itemType("iron_sword")
            .rarity("rare")
            .build();

        assertEquals(RuleAction.KEEP, ruleEvaluator.evaluate(rareContext),
            "Should keep rare items");

        // Test common rarity (no rule matches)
        ItemRuleContext commonContext = new ItemRuleContext.Builder()
            .itemType("stone_pickaxe")
            .rarity("common")
            .build();

        RuleAction action = ruleEvaluator.evaluate(commonContext);
        assertNotEquals(RuleAction.KEEP, action,
            "Should not automatically keep common items");
    }

    @Test
    @DisplayName("Registry reload updates rules")
    void testRegistryReload() {
        // Add a custom rule
        ItemRule customRule = createRule(
            "Custom Rule",
            List.of(new RuleCondition("item_type", "==", "test")),
            RuleAction.KEEP,
            50
        );

        ruleRegistry.addRule(customRule);
        int initialCount = ruleRegistry.size();

        // Reload registry
        ruleRegistry.reload();

        // Reloaded registry should have default rules
        assertFalse(ruleRegistry.isEmpty(),
            "Registry should not be empty after reload");
        // Size may be different (custom rule cleared, defaults loaded)
    }

    // ==================== Helper Methods ====================

    /**
     * Creates an ItemRule with the specified properties.
     */
    private ItemRule createRule(String name, List<RuleCondition> conditions,
                                RuleAction action, int priority) {
        ItemRule rule = new ItemRule();
        rule.setName(name);
        rule.setConditions(conditions);
        rule.setAction(action);
        rule.setPriority(priority);
        rule.setEnabled(true);
        return rule;
    }

    // ==================== Assertion Helpers ====================

    private void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private void assertFalse(boolean condition, String message) {
        if (condition) {
            throw new AssertionError(message);
        }
    }

    private void assertEquals(Object expected, Object actual, String message) {
        if (expected == null && actual == null) {
            return;
        }
        if (expected == null || !expected.equals(actual)) {
            throw new AssertionError(message + " (expected: " + expected + ", actual: " + actual + ")");
        }
    }

    private void assertNotEquals(Object unexpected, Object actual, String message) {
        if (unexpected == null && actual == null) {
            throw new AssertionError(message);
        }
        if (unexpected == null || unexpected.equals(actual)) {
            throw new AssertionError(message + " (expected: not " + unexpected + ", actual: " + actual + ")");
        }
    }
}

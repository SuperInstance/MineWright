package com.minewright.integration;

import com.minewright.rules.ItemRule;
import com.minewright.rules.ItemRuleContext;
import com.minewright.rules.RuleAction;
import com.minewright.rules.RuleCondition;
import com.minewright.rules.RuleEvaluator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the Item Rules Engine.
 * Tests rule evaluation, priority ordering, condition matching, and action application.
 *
 * <p><b>Test Coverage:</b></p>
 * <ul>
 *   <li>Rule priority ordering (highest first)</li>
 *   <li>Condition matching with various operators</li>
 *   <li>Action application for matching rules</li>
 *   <li>Disabled rules are skipped</li>
 *   <li>Empty rule lists return null</li>
 *   <li>Multiple conditions (AND logic)</li>
 *   <li>Enchantment matching</li>
 *   <li>Custom field matching</li>
 * </ul>
 *
 * @see RuleEvaluator
 * @see ItemRule
 * @see ItemRuleContext
 * @since 1.0.0
 */
@DisplayName("Item Rules Integration Tests")
public class ItemRulesIntegrationTest extends IntegrationTestBase {

    @Test
    @DisplayName("Empty rule list returns null action")
    void testEmptyRulesReturnsNull() {
        // Arrange
        List<ItemRule> rules = Collections.emptyList();
        ItemRuleContext context = new ItemRuleContext.Builder()
                .itemType("diamond_sword")
                .rarity("rare")
                .build();

        // Act
        RuleAction result = RuleEvaluator.evaluate(rules, context);

        // Assert
        assertNull(result, "Empty rule list should return null action");
    }

    @Test
    @DisplayName("Null rule list returns null action")
    void testNullRulesReturnsNull() {
        // Arrange
        ItemRuleContext context = new ItemRuleContext.Builder()
                .itemType("diamond_sword")
                .build();

        // Act
        RuleAction result = RuleEvaluator.evaluate(null, context);

        // Assert
        assertNull(result, "Null rule list should return null action");
    }

    @Test
    @DisplayName("Single matching rule returns its action")
    void testSingleMatchingRule() {
        // Arrange
        List<RuleCondition> conditions = Arrays.asList(
                new RuleCondition("item_type", "==", "diamond_sword")
        );

        ItemRule rule = new ItemRule(
                "Keep Diamond Sword",
                conditions,
                RuleAction.KEEP,
                10
        );

        List<ItemRule> rules = Collections.singletonList(rule);

        ItemRuleContext context = new ItemRuleContext.Builder()
                .itemType("diamond_sword")
                .rarity("rare")
                .build();

        // Act
        RuleAction result = RuleEvaluator.evaluate(rules, context);

        // Assert
        assertEquals(RuleAction.KEEP, result, "Matching rule should return KEEP action");
    }

    @Test
    @DisplayName("Non-matching rule returns null")
    void testNonMatchingRule() {
        // Arrange
        List<RuleCondition> conditions = Arrays.asList(
                new RuleCondition("item_type", "==", "diamond_sword")
        );

        ItemRule rule = new ItemRule(
                "Keep Diamond Sword",
                conditions,
                RuleAction.KEEP,
                10
        );

        List<ItemRule> rules = Collections.singletonList(rule);

        ItemRuleContext context = new ItemRuleContext.Builder()
                .itemType("iron_sword")
                .rarity("common")
                .build();

        // Act
        RuleAction result = RuleEvaluator.evaluate(rules, context);

        // Assert
        assertNull(result, "Non-matching rule should return null");
    }

    @Test
    @DisplayName("Higher priority rule is evaluated first")
    void testRulePriorityOrdering() {
        // Arrange
        // Low priority rule (matches diamond_sword)
        List<RuleCondition> lowConditions = Arrays.asList(
                new RuleCondition("item_type", "==", "diamond_sword")
        );
        ItemRule lowRule = new ItemRule(
                "Low Priority Rule",
                lowConditions,
                RuleAction.DISCARD,
                1
        );

        // High priority rule (matches diamond_sword)
        List<RuleCondition> highConditions = Arrays.asList(
                new RuleCondition("item_type", "==", "diamond_sword")
        );
        ItemRule highRule = new ItemRule(
                "High Priority Rule",
                highConditions,
                RuleAction.KEEP,
                100
        );

        // Add in random order
        List<ItemRule> rules = new ArrayList<>();
        rules.add(lowRule);
        rules.add(highRule);

        // Sort by priority (as the system would)
        rules.sort((a, b) -> Integer.compare(b.getPriority(), a.getPriority()));

        ItemRuleContext context = new ItemRuleContext.Builder()
                .itemType("diamond_sword")
                .rarity("rare")
                .build();

        // Act
        RuleAction result = RuleEvaluator.evaluate(rules, context);

        // Assert
        assertEquals(RuleAction.KEEP, result,
                "Higher priority rule should match first, returning KEEP not DISCARD");
    }

    @Test
    @DisplayName("Disabled rules are skipped")
    void testDisabledRuleSkipped() {
        // Arrange
        List<RuleCondition> conditions = Arrays.asList(
                new RuleCondition("item_type", "==", "diamond_sword")
        );

        ItemRule disabledRule = new ItemRule(
                "Disabled Rule",
                conditions,
                RuleAction.DISCARD,
                100
        );
        disabledRule.setEnabled(false);

        List<RuleCondition> conditions2 = Arrays.asList(
                new RuleCondition("item_type", "==", "diamond_sword")
        );

        ItemRule enabledRule = new ItemRule(
                "Enabled Rule",
                conditions2,
                RuleAction.KEEP,
                1
        );

        List<ItemRule> rules = Arrays.asList(disabledRule, enabledRule);
        rules.sort((a, b) -> Integer.compare(b.getPriority(), a.getPriority()));

        ItemRuleContext context = new ItemRuleContext.Builder()
                .itemType("diamond_sword")
                .build();

        // Act
        RuleAction result = RuleEvaluator.evaluate(rules, context);

        // Assert
        assertEquals(RuleAction.KEEP, result,
                "Disabled rule should be skipped, enabled rule should match");
    }

    @Test
    @DisplayName("Rule with multiple conditions (AND logic)")
    void testMultipleConditionsAndLogic() {
        // Arrange
        List<RuleCondition> conditions = Arrays.asList(
                new RuleCondition("item_type", "==", "diamond_sword"),
                new RuleCondition("rarity", "==", "rare"),
                new RuleCondition("quantity", ">=", 1)
        );

        ItemRule rule = new ItemRule(
                "Keep Rare Diamond Sword",
                conditions,
                RuleAction.KEEP,
                10
        );

        List<ItemRule> rules = Collections.singletonList(rule);

        // Test matching context
        ItemRuleContext matchingContext = new ItemRuleContext.Builder()
                .itemType("diamond_sword")
                .rarity("rare")
                .quantity(5)
                .build();

        // Act & Assert - Matching
        RuleAction matchingResult = RuleEvaluator.evaluate(rules, matchingContext);
        assertEquals(RuleAction.KEEP, matchingResult,
                "All conditions match should return action");

        // Test non-matching context (wrong rarity)
        ItemRuleContext nonMatchingContext = new ItemRuleContext.Builder()
                .itemType("diamond_sword")
                .rarity("common")
                .quantity(5)
                .build();

        // Act & Assert - Non-matching
        RuleAction nonMatchingResult = RuleEvaluator.evaluate(rules, nonMatchingContext);
        assertNull(nonMatchingResult,
                "Not all conditions match should return null");
    }

    @Test
    @DisplayName("Rule with no conditions matches everything")
    void testNoConditionsMatchesAll() {
        // Arrange
        ItemRule rule = new ItemRule(
                "Match All Rule",
                Collections.emptyList(), // No conditions
                RuleAction.DISCARD,
                10
        );

        List<ItemRule> rules = Collections.singletonList(rule);

        ItemRuleContext context = new ItemRuleContext.Builder()
                .itemType("any_item")
                .build();

        // Act
        RuleAction result = RuleEvaluator.evaluate(rules, context);

        // Assert
        assertEquals(RuleAction.DISCARD, result,
                "Rule with no conditions should match any context");
    }

    @Test
    @DisplayName("String operators: equals, not equals, contains")
    void testStringOperators() {
        // Arrange - Test equals operator
        List<RuleCondition> equalsConditions = Arrays.asList(
                new RuleCondition("item_type", "==", "diamond_sword")
        );
        ItemRule equalsRule = new ItemRule(
                "Equals Rule",
                equalsConditions,
                RuleAction.KEEP,
                10
        );

        // Test not equals operator
        List<RuleCondition> notEqualsConditions = Arrays.asList(
                new RuleCondition("item_type", "!=", "wooden_sword")
        );
        ItemRule notEqualsRule = new ItemRule(
                "Not Equals Rule",
                notEqualsConditions,
                RuleAction.KEEP,
                10
        );

        // Test contains operator
        List<RuleCondition> containsConditions = Arrays.asList(
                new RuleCondition("display_name", "contains", "Diamond")
        );
        ItemRule containsRule = new ItemRule(
                "Contains Rule",
                containsConditions,
                RuleAction.KEEP,
                10
        );

        // Act & Assert - Equals
        List<ItemRule> rules = Collections.singletonList(equalsRule);
        ItemRuleContext context = new ItemRuleContext.Builder()
                .itemType("diamond_sword")
                .build();
        assertEquals(RuleAction.KEEP, RuleEvaluator.evaluate(rules, context),
                "Equals operator should match exact string");

        // Act & Assert - Not Equals
        rules = Collections.singletonList(notEqualsRule);
        assertEquals(RuleAction.KEEP, RuleEvaluator.evaluate(rules, context),
                "Not equals operator should match different string");

        // Act & Assert - Contains
        rules = Collections.singletonList(containsRule);
        ItemRuleContext diamondContext = new ItemRuleContext.Builder()
                .itemType("sword")
                .displayName("Enchanted Diamond Sword")
                .build();
        assertEquals(RuleAction.KEEP, RuleEvaluator.evaluate(rules, diamondContext),
                "Contains operator should match substring");

        ItemRuleContext ironContext = new ItemRuleContext.Builder()
                .itemType("sword")
                .displayName("Iron Sword")
                .build();
        assertNull(RuleEvaluator.evaluate(rules, ironContext),
                "Contains operator should not match non-containing string");
    }

    @Test
    @DisplayName("Numeric operators: greater than, less than, greater or equal, less or equal")
    void testNumericOperators() {
        // Arrange
        List<RuleCondition> greaterConditions = Arrays.asList(
                new RuleCondition("quantity", ">", 5)
        );
        ItemRule greaterRule = new ItemRule(
                "Greater Than Rule",
                greaterConditions,
                RuleAction.DISCARD,
                10
        );

        List<RuleCondition> lessConditions = Arrays.asList(
                new RuleCondition("durability", "<", 10)
        );
        ItemRule lessRule = new ItemRule(
                "Less Than Rule",
                lessConditions,
                RuleAction.DISCARD,
                10
        );

        List<RuleCondition> greaterEqualConditions = Arrays.asList(
                new RuleCondition("quantity", ">=", 10)
        );
        ItemRule greaterEqualRule = new ItemRule(
                "Greater Equal Rule",
                greaterEqualConditions,
                RuleAction.KEEP,
                10
        );

        List<RuleCondition> lessEqualConditions = Arrays.asList(
                new RuleCondition("durability", "<=", 50)
        );
        ItemRule lessEqualRule = new ItemRule(
                "Less Equal Rule",
                lessEqualConditions,
                RuleAction.KEEP,
                10
        );

        // Act & Assert - Greater Than
        List<ItemRule> rules = Collections.singletonList(greaterRule);
        ItemRuleContext highQuantityContext = new ItemRuleContext.Builder()
                .itemType("dirt")
                .quantity(10)
                .build();
        assertEquals(RuleAction.DISCARD, RuleEvaluator.evaluate(rules, highQuantityContext),
                "Greater than operator should match larger value");

        ItemRuleContext lowQuantityContext = new ItemRuleContext.Builder()
                .itemType("dirt")
                .quantity(3)
                .build();
        assertNull(RuleEvaluator.evaluate(rules, lowQuantityContext),
                "Greater than operator should not match smaller value");

        // Act & Assert - Less Than
        rules = Collections.singletonList(lessRule);
        ItemRuleContext lowDurabilityContext = new ItemRuleContext.Builder()
                .itemType("pickaxe")
                .durability(5)
                .build();
        assertEquals(RuleAction.DISCARD, RuleEvaluator.evaluate(rules, lowDurabilityContext),
                "Less than operator should match smaller value");

        // Act & Assert - Greater or Equal
        rules = Collections.singletonList(greaterEqualRule);
        ItemRuleContext exactQuantityContext = new ItemRuleContext.Builder()
                .itemType("dirt")
                .quantity(10)
                .build();
        assertEquals(RuleAction.KEEP, RuleEvaluator.evaluate(rules, exactQuantityContext),
                "Greater or equal operator should match equal value");

        // Act & Assert - Less or Equal
        rules = Collections.singletonList(lessEqualRule);
        ItemRuleContext mediumDurabilityContext = new ItemRuleContext.Builder()
                .itemType("pickaxe")
                .durability(50)
                .build();
        assertEquals(RuleAction.KEEP, RuleEvaluator.evaluate(rules, mediumDurabilityContext),
                "Less or equal operator should match equal value");
    }

    @Test
    @DisplayName("Enchantment matching with dot notation")
    void testEnchantmentMatching() {
        // Arrange
        List<RuleCondition> conditions = Arrays.asList(
                new RuleCondition("enchantment.sharpness", ">=", 3)
        );

        ItemRule rule = new ItemRule(
                "Keep Sharp Weapons",
                conditions,
                RuleAction.KEEP,
                10
        );

        List<ItemRule> rules = Collections.singletonList(rule);

        // Test with enchantment
        ItemRuleContext enchantedContext = new ItemRuleContext.Builder()
                .itemType("diamond_sword")
                .enchantment("sharpness", 5)
                .build();

        // Act & Assert
        RuleAction result = RuleEvaluator.evaluate(rules, enchantedContext);
        assertEquals(RuleAction.KEEP, result,
                "Should match item with sufficient enchantment level");

        // Test without enchantment
        ItemRuleContext nonEnchantedContext = new ItemRuleContext.Builder()
                .itemType("diamond_sword")
                .enchantment("sharpness", 1)
                .build();

        RuleAction nonEnchantedResult = RuleEvaluator.evaluate(rules, nonEnchantedContext);
        assertNull(nonEnchantedResult,
                "Should not match item with insufficient enchantment level");
    }

    @Test
    @DisplayName("Custom field matching")
    void testCustomFieldMatching() {
        // Arrange
        List<RuleCondition> conditions = Arrays.asList(
                new RuleCondition("custom_source", "==", "fishing")
        );

        ItemRule rule = new ItemRule(
                "Store Fished Items",
                conditions,
                RuleAction.STORE,
                10
        );

        List<ItemRule> rules = Collections.singletonList(rule);

        // Test with custom field
        ItemRuleContext fishedContext = new ItemRuleContext.Builder()
                .itemType("fish")
                .addCustomField("source", "fishing")
                .addCustomField("custom_source", "fishing") // Prefix for custom
                .build();

        // Act & Assert
        RuleAction result = RuleEvaluator.evaluate(rules, fishedContext);
        assertEquals(RuleAction.STORE, result,
                "Should match custom field value");

        // Test with different custom field value
        ItemRuleContext minedContext = new ItemRuleContext.Builder()
                .itemType("fish")
                .addCustomField("source", "mining")
                .addCustomField("custom_source", "mining")
                .build();

        RuleAction minedResult = RuleEvaluator.evaluate(rules, minedContext);
        assertNull(minedResult,
                "Should not match different custom field value");
    }

    @Test
    @DisplayName("First matching rule wins (stop after first match)")
    void testFirstMatchingRuleWins() {
        // Arrange
        List<RuleCondition> conditions = Arrays.asList(
                new RuleCondition("item_type", "==", "diamond")
        );

        ItemRule firstRule = new ItemRule(
                "First Rule",
                conditions,
                RuleAction.KEEP,
                100
        );

        ItemRule secondRule = new ItemRule(
                "Second Rule",
                conditions,
                RuleAction.STORE,
                50
        );

        ItemRule thirdRule = new ItemRule(
                "Third Rule",
                conditions,
                RuleAction.DISCARD,
                10
        );

        List<ItemRule> rules = Arrays.asList(firstRule, secondRule, thirdRule);

        ItemRuleContext context = new ItemRuleContext.Builder()
                .itemType("diamond")
                .build();

        // Act
        RuleAction result = RuleEvaluator.evaluate(rules, context);

        // Assert
        assertEquals(RuleAction.KEEP, result,
                "Should return action from first matching rule, not later ones");
    }

    @Test
    @DisplayName("Complex rule set with multiple item types")
    void testComplexRuleSet() {
        // Arrange - Create a realistic rule set for inventory management
        List<ItemRule> rules = Arrays.asList(
                // Rule 1: Keep rare items (highest priority)
                new ItemRule(
                        "Keep Rare Items",
                        Arrays.asList(new RuleCondition("rarity", "==", "rare")),
                        RuleAction.KEEP,
                        100
                ),
                // Rule 2: Store common items with high quantity
                new ItemRule(
                        "Store Common Items",
                        Arrays.asList(
                                new RuleCondition("rarity", "==", "common"),
                                new RuleCondition("quantity", ">", 32)
                        ),
                        RuleAction.STORE,
                        50
                ),
                // Rule 3: Discard damaged tools
                new ItemRule(
                        "Discard Damaged Tools",
                        Arrays.asList(
                                new RuleCondition("item_type", "contains", "pickaxe"),
                                new RuleCondition("durability", "<", 10)
                        ),
                        RuleAction.DISCARD,
                        75
                ),
                // Rule 4: Default - keep everything else
                new ItemRule(
                        "Default Keep",
                        Collections.emptyList(),
                        RuleAction.KEEP,
                        1
                )
        );

        // Sort by priority
        rules.sort((a, b) -> Integer.compare(b.getPriority(), a.getPriority()));

        // Act & Assert - Rare item should be kept (Rule 1)
        ItemRuleContext rareItemContext = new ItemRuleContext.Builder()
                .itemType("diamond_sword")
                .rarity("rare")
                .quantity(1)
                .build();
        assertEquals(RuleAction.KEEP, RuleEvaluator.evaluate(rules, rareItemContext),
                "Rare items should be kept");

        // Act & Assert - Common items with high quantity should be stored (Rule 2)
        ItemRuleContext commonStackContext = new ItemRuleContext.Builder()
                .itemType("dirt")
                .rarity("common")
                .quantity(64)
                .build();
        assertEquals(RuleAction.STORE, RuleEvaluator.evaluate(rules, commonStackContext),
                "Common items with high quantity should be stored");

        // Act & Assert - Damaged pickaxe should be discarded (Rule 3)
        ItemRuleContext damagedPickaxeContext = new ItemRuleContext.Builder()
                .itemType("wooden_pickaxe")
                .rarity("common")
                .durability(5)
                .build();
        assertEquals(RuleAction.DISCARD, RuleEvaluator.evaluate(rules, damagedPickaxeContext),
                "Damaged tools should be discarded");

        // Act & Assert - Default case should keep (Rule 4)
        ItemRuleContext defaultContext = new ItemRuleContext.Builder()
                .itemType("stone")
                .rarity("common")
                .quantity(10)
                .build();
        assertEquals(RuleAction.KEEP, RuleEvaluator.evaluate(rules, defaultContext),
                "Items matching no specific rule should use default KEEP");
    }

    @Test
    @DisplayName("Case insensitive contains operator")
    void testCaseInsensitiveContains() {
        // Arrange
        List<RuleCondition> conditions = Arrays.asList(
                new RuleCondition("display_name", "contains", "DIAMOND") // Uppercase
        );

        ItemRule rule = new ItemRule(
                "Contains Rule",
                conditions,
                RuleAction.KEEP,
                10
        );

        List<ItemRule> rules = Collections.singletonList(rule);

        ItemRuleContext lowercaseContext = new ItemRuleContext.Builder()
                .itemType("sword")
                .displayName("diamond sword") // Lowercase
                .build();

        // Act
        RuleAction result = RuleEvaluator.evaluate(rules, lowercaseContext);

        // Assert
        assertEquals(RuleAction.KEEP, result,
                "Contains operator should be case insensitive");
    }

    @Test
    @DisplayName("FindMatchingRule returns rule not just action")
    void testFindMatchingRule() {
        // Arrange
        List<RuleCondition> conditions = Arrays.asList(
                new RuleCondition("item_type", "==", "diamond_sword")
        );

        ItemRule expectedRule = new ItemRule(
                "Test Rule",
                conditions,
                RuleAction.KEEP,
                10
        );

        List<ItemRule> rules = Collections.singletonList(expectedRule);

        ItemRuleContext context = new ItemRuleContext.Builder()
                .itemType("diamond_sword")
                .build();

        // Act
        ItemRule result = RuleEvaluator.findMatchingRule(rules, context);

        // Assert
        assertNotNull(result, "Should find matching rule");
        assertEquals(expectedRule.getName(), result.getName(),
                "Should return the actual matching rule");
        assertEquals(expectedRule.getAction(), result.getAction(),
                "Returned rule should have correct action");
    }

    @Test
    @DisplayName("Matches method validates rule conditions")
    void testMatchesMethod() {
        // Arrange
        List<RuleCondition> conditions = Arrays.asList(
                new RuleCondition("item_type", "==", "diamond_sword"),
                new RuleCondition("rarity", "==", "rare")
        );

        ItemRule rule = new ItemRule(
                "Test Rule",
                conditions,
                RuleAction.KEEP,
                10
        );

        ItemRuleContext matchingContext = new ItemRuleContext.Builder()
                .itemType("diamond_sword")
                .rarity("rare")
                .build();

        ItemRuleContext nonMatchingContext = new ItemRuleContext.Builder()
                .itemType("iron_sword")
                .rarity("rare")
                .build();

        // Act & Assert
        assertTrue(RuleEvaluator.matches(rule, matchingContext),
                "Rule should match context that meets all conditions");

        assertFalse(RuleEvaluator.matches(rule, nonMatchingContext),
                "Rule should not match context that fails a condition");
    }
}

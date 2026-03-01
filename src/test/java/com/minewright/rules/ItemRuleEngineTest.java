package com.minewright.rules;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for the item rules engine.
 */
class ItemRuleEngineTest {

    private ItemRuleRegistry registry;
    private ItemRuleContext diamondContext;
    private ItemRuleContext cobblestoneContext;
    private ItemRuleContext damagedToolContext;

    @BeforeEach
    void setUp() {
        registry = new ItemRuleRegistry();

        // Create test contexts
        diamondContext = new ItemRuleContext.Builder()
                .itemType("diamond")
                .rarity("uncommon")
                .quantity(5)
                .durability(0)
                .maxDurability(0)
                .displayName("Diamond")
                .build();

        cobblestoneContext = new ItemRuleContext.Builder()
                .itemType("cobblestone")
                .rarity("common")
                .quantity(100)
                .durability(0)
                .maxDurability(0)
                .displayName("Cobblestone")
                .build();

        damagedToolContext = new ItemRuleContext.Builder()
                .itemType("iron_pickaxe")
                .rarity("common")
                .quantity(1)
                .durability(30) // 30% durability
                .maxDurability(250)
                .displayName("Iron Pickaxe")
                .build();
    }

    @Test
    @DisplayName("RuleCondition should store field, operator, and value")
    void testRuleConditionCreation() {
        RuleCondition condition = new RuleCondition("item_type", "==", "diamond");

        assertEquals("item_type", condition.getField());
        assertEquals("==", condition.getOperator());
        assertEquals("diamond", condition.getValue());
    }

    @Test
    @DisplayName("RuleCondition should implement equals and hashCode correctly")
    void testRuleConditionEquality() {
        RuleCondition condition1 = new RuleCondition("item_type", "==", "diamond");
        RuleCondition condition2 = new RuleCondition("item_type", "==", "diamond");
        RuleCondition condition3 = new RuleCondition("item_type", "!=", "diamond");

        assertEquals(condition1, condition2);
        assertEquals(condition1.hashCode(), condition2.hashCode());
        assertNotEquals(condition1, condition3);
    }

    @Test
    @DisplayName("ItemRule should be created with all properties")
    void testItemRuleCreation() {
        List<RuleCondition> conditions = Arrays.asList(
                new RuleCondition("item_type", "==", "diamond")
        );

        ItemRule rule = new ItemRule("Keep diamonds", conditions, RuleAction.KEEP, 100);

        assertEquals("Keep diamonds", rule.getName());
        assertEquals(1, rule.getConditions().size());
        assertEquals(RuleAction.KEEP, rule.getAction());
        assertEquals(100, rule.getPriority());
        assertTrue(rule.isEnabled());
    }

    @Test
    @DisplayName("ItemRule should support enabling/disabling")
    void testItemRuleEnabled() {
        ItemRule rule = new ItemRule();
        rule.setEnabled(true);
        assertTrue(rule.isEnabled());

        rule.setEnabled(false);
        assertFalse(rule.isEnabled());
    }

    @Test
    @DisplayName("ItemRuleContext should be created with Builder")
    void testContextCreation() {
        ItemRuleContext context = new ItemRuleContext.Builder()
                .itemType("diamond")
                .rarity("uncommon")
                .quantity(5)
                .durability(0)
                .maxDurability(0)
                .build();

        assertEquals("diamond", context.getItemType());
        assertEquals("uncommon", context.getRarity());
        assertEquals(5, context.getQuantity());
    }

    @Test
    @DisplayName("ItemRuleContext should support custom fields")
    void testContextCustomFields() {
        ItemRuleContext context = new ItemRuleContext.Builder()
                .itemType("sword")
                .addCustomField("damage", 10)
                .addCustomField("speed", 1.5)
                .build();

        assertEquals(10, context.getCustomFields().get("damage"));
        assertEquals(1.5, context.getCustomFields().get("speed"));
    }

    @Test
    @DisplayName("RuleEvaluator should match rule with equals operator")
    void testRuleEvaluatorEquals() {
        List<RuleCondition> conditions = Arrays.asList(
                new RuleCondition("item_type", "==", "diamond")
        );
        ItemRule rule = new ItemRule("Keep diamonds", conditions, RuleAction.KEEP, 100);

        assertTrue(RuleEvaluator.matches(rule, diamondContext));
        assertFalse(RuleEvaluator.matches(rule, cobblestoneContext));
    }

    @Test
    @DisplayName("RuleEvaluator should match rule with contains operator")
    void testRuleEvaluatorContains() {
        List<RuleCondition> conditions = Arrays.asList(
                new RuleCondition("item_type", "contains", "mond")
        );
        ItemRule rule = new ItemRule("Keep diamonds", conditions, RuleAction.KEEP, 100);

        assertTrue(RuleEvaluator.matches(rule, diamondContext));
        assertFalse(RuleEvaluator.matches(rule, cobblestoneContext));
    }

    @Test
    @DisplayName("RuleEvaluator should match rule with greater than operator")
    void testRuleEvaluatorGreaterThan() {
        List<RuleCondition> conditions = Arrays.asList(
                new RuleCondition("quantity", ">", "64")
        );
        ItemRule rule = new ItemRule("Discard excess", conditions, RuleAction.DISCARD, 50);

        assertTrue(RuleEvaluator.matches(rule, cobblestoneContext)); // quantity = 100
        assertFalse(RuleEvaluator.matches(rule, diamondContext)); // quantity = 5
    }

    @Test
    @DisplayName("RuleEvaluator should match rule with greater than or equal operator")
    void testRuleEvaluatorGreaterThanOrEqual() {
        List<RuleCondition> conditions = Arrays.asList(
                new RuleCondition("quantity", ">=", "100")
        );
        ItemRule rule = new ItemRule("Discard excess", conditions, RuleAction.DISCARD, 50);

        assertTrue(RuleEvaluator.matches(rule, cobblestoneContext)); // quantity = 100
        assertFalse(RuleEvaluator.matches(rule, diamondContext)); // quantity = 5
    }

    @Test
    @DisplayName("RuleEvaluator should match rule with less than operator")
    void testRuleEvaluatorLessThan() {
        List<RuleCondition> conditions = Arrays.asList(
                new RuleCondition("durability", "<", "50")
        );
        ItemRule rule = new ItemRule("Discard damaged", conditions, RuleAction.DISCARD, 50);

        assertTrue(RuleEvaluator.matches(rule, damagedToolContext)); // durability = 30
        assertFalse(RuleEvaluator.matches(rule, diamondContext)); // durability = 0
    }

    @Test
    @DisplayName("RuleEvaluator should match all conditions (AND logic)")
    void testRuleEvaluatorMultipleConditions() {
        List<RuleCondition> conditions = Arrays.asList(
                new RuleCondition("item_type", "==", "cobblestone"),
                new RuleCondition("quantity", ">", "64")
        );
        ItemRule rule = new ItemRule("Discard excess cobble", conditions, RuleAction.DISCARD, 50);

        assertTrue(RuleEvaluator.matches(rule, cobblestoneContext));

        // Create context with low quantity
        ItemRuleContext lowQuantityContext = new ItemRuleContext.Builder()
                .itemType("cobblestone")
                .quantity(10)
                .build();
        assertFalse(RuleEvaluator.matches(rule, lowQuantityContext));
    }

    @Test
    @DisplayName("RuleEvaluator should return first matching rule's action")
    void testRuleEvaluatorPriority() {
        List<ItemRule> rules = Arrays.asList(
                new ItemRule("Low priority", Collections.singletonList(
                        new RuleCondition("item_type", "==", "diamond")
                ), RuleAction.DISCARD, 10),
                new ItemRule("High priority", Collections.singletonList(
                        new RuleCondition("item_type", "==", "diamond")
                ), RuleAction.KEEP, 100)
        );

        RuleAction action = RuleEvaluator.evaluate(rules, diamondContext);
        assertEquals(RuleAction.KEEP, action); // High priority should match first
    }

    @Test
    @DisplayName("RuleEvaluator should skip disabled rules")
    void testRuleEvaluatorDisabledRules() {
        List<RuleCondition> conditions = Arrays.asList(
                new RuleCondition("item_type", "==", "diamond")
        );
        ItemRule disabledRule = new ItemRule("Disabled rule", conditions, RuleAction.DISCARD, 100);
        disabledRule.setEnabled(false);

        List<ItemRule> rules = Collections.singletonList(disabledRule);

        RuleAction action = RuleEvaluator.evaluate(rules, diamondContext);
        assertNull(action); // No matching rules (only disabled one)
    }

    @Test
    @DisplayName("RuleEvaluator should handle rules with no conditions")
    void testRuleEvaluatorNoConditions() {
        ItemRule catchAllRule = new ItemRule("Catch all", Collections.emptyList(), RuleAction.KEEP, 0);

        assertTrue(RuleEvaluator.matches(catchAllRule, diamondContext));
        assertTrue(RuleEvaluator.matches(catchAllRule, cobblestoneContext));
    }

    @Test
    @DisplayName("ItemRuleParser should parse rule from JSON string")
    void testParseRuleFromString() {
        String json = """
                {
                  "name": "Keep diamonds",
                  "conditions": [
                    {"field": "item_type", "operator": "==", "value": "diamond"}
                  ],
                  "action": "KEEP",
                  "priority": 100
                }
                """;

        ItemRule rule = ItemRuleParser.parseRule(json);

        assertNotNull(rule);
        assertEquals("Keep diamonds", rule.getName());
        assertEquals(1, rule.getConditions().size());
        assertEquals(RuleAction.KEEP, rule.getAction());
        assertEquals(100, rule.getPriority());
        assertTrue(rule.isEnabled());
    }

    @Test
    @DisplayName("ItemRuleParser should parse rule with enabled flag")
    void testParseRuleWithEnabled() {
        String json = """
                {
                  "name": "Disabled rule",
                  "conditions": [],
                  "action": "DISCARD",
                  "priority": 50,
                  "enabled": false
                }
                """;

        ItemRule rule = ItemRuleParser.parseRule(json);

        assertNotNull(rule);
        assertFalse(rule.isEnabled());
    }

    @Test
    @DisplayName("ItemRuleParser should parse multiple conditions")
    void testParseMultipleConditions() {
        String json = """
                {
                  "name": "Multi-condition rule",
                  "conditions": [
                    {"field": "item_type", "operator": "==", "value": "cobblestone"},
                    {"field": "quantity", "operator": ">", "value": 64}
                  ],
                  "action": "DISCARD",
                  "priority": 50
                }
                """;

        ItemRule rule = ItemRuleParser.parseRule(json);

        assertNotNull(rule);
        assertEquals(2, rule.getConditions().size());
        assertEquals("item_type", rule.getConditions().get(0).getField());
        assertEquals("quantity", rule.getConditions().get(1).getField());
    }

    @Test
    @DisplayName("ItemRuleParser should parse numeric values")
    void testParseNumericValues() {
        String json = """
                {
                  "name": "Numeric rule",
                  "conditions": [
                    {"field": "quantity", "operator": ">=", "value": 100},
                    {"field": "durability", "operator": "<", "value": 50}
                  ],
                  "action": "DISCARD",
                  "priority": 50
                }
                """;

        ItemRule rule = ItemRuleParser.parseRule(json);

        assertNotNull(rule);
        assertEquals(100, rule.getConditions().get(0).getValue());
        assertEquals(50, rule.getConditions().get(1).getValue());
    }

    @Test
    @DisplayName("ItemRuleRegistry should add and retrieve rules")
    void testRegistryAddAndGet() {
        ItemRule rule = new ItemRule("Test rule", Collections.emptyList(), RuleAction.KEEP, 50);

        registry.addRule(rule);

        assertEquals(1, registry.size());
        assertFalse(registry.isEmpty());

        Optional<ItemRule> found = registry.findRule("Test rule");
        assertTrue(found.isPresent());
        assertEquals("Test rule", found.get().getName());
    }

    @Test
    @DisplayName("ItemRuleRegistry should remove rules by name")
    void testRegistryRemove() {
        ItemRule rule = new ItemRule("Test rule", Collections.emptyList(), RuleAction.KEEP, 50);
        registry.addRule(rule);

        boolean removed = registry.removeRule("Test rule");

        assertTrue(removed);
        assertTrue(registry.isEmpty());
    }

    @Test
    @DisplayName("ItemRuleRegistry should enable/disable rules")
    void testRegistryEnableDisable() {
        ItemRule rule = new ItemRule("Test rule", Collections.emptyList(), RuleAction.KEEP, 50);
        registry.addRule(rule);

        assertTrue(registry.setRuleEnabled("Test rule", false));
        assertFalse(registry.findRule("Test rule").get().isEnabled());

        assertTrue(registry.setRuleEnabled("Test rule", true));
        assertTrue(registry.findRule("Test rule").get().isEnabled());
    }

    @Test
    @DisplayName("ItemRuleRegistry should sort rules by priority")
    void testRegistryPrioritySort() {
        registry.addRule(new ItemRule("Low", Collections.emptyList(), RuleAction.KEEP, 10));
        registry.addRule(new ItemRule("High", Collections.emptyList(), RuleAction.KEEP, 100));
        registry.addRule(new ItemRule("Medium", Collections.emptyList(), RuleAction.KEEP, 50));

        List<ItemRule> rules = registry.getRules();

        assertEquals("High", rules.get(0).getName());
        assertEquals("Medium", rules.get(1).getName());
        assertEquals("Low", rules.get(2).getName());
    }

    @Test
    @DisplayName("ItemRuleRegistry should get only enabled rules")
    void testRegistryGetEnabled() {
        ItemRule enabledRule = new ItemRule("Enabled", Collections.emptyList(), RuleAction.KEEP, 50);
        ItemRule disabledRule = new ItemRule("Disabled", Collections.emptyList(), RuleAction.KEEP, 50);
        disabledRule.setEnabled(false);

        registry.addRule(enabledRule);
        registry.addRule(disabledRule);

        List<ItemRule> enabledRules = registry.getEnabledRules();

        assertEquals(1, enabledRules.size());
        assertEquals("Enabled", enabledRules.get(0).getName());
    }

    @Test
    @DisplayName("ItemRuleRegistry should clear all rules")
    void testRegistryClear() {
        registry.addRule(new ItemRule("Rule1", Collections.emptyList(), RuleAction.KEEP, 50));
        registry.addRule(new ItemRule("Rule2", Collections.emptyList(), RuleAction.KEEP, 50));

        assertEquals(2, registry.size());

        registry.clear();

        assertTrue(registry.isEmpty());
    }

    @Test
    @DisplayName("RuleEvaluator should find matching rule")
    void testFindMatchingRule() {
        List<RuleCondition> conditions = Arrays.asList(
                new RuleCondition("item_type", "==", "diamond")
        );
        ItemRule rule = new ItemRule("Keep diamonds", conditions, RuleAction.KEEP, 100);

        List<ItemRule> rules = Collections.singletonList(rule);

        ItemRule matching = RuleEvaluator.findMatchingRule(rules, diamondContext);

        assertNotNull(matching);
        assertEquals("Keep diamonds", matching.getName());
    }

    @Test
    @DisplayName("RuleEvaluator should return null when no rule matches")
    void testFindMatchingRuleNone() {
        List<RuleCondition> conditions = Arrays.asList(
                new RuleCondition("item_type", "==", "gold")
        );
        ItemRule rule = new ItemRule("Keep gold", conditions, RuleAction.KEEP, 100);

        List<ItemRule> rules = Collections.singletonList(rule);

        ItemRule matching = RuleEvaluator.findMatchingRule(rules, diamondContext);

        assertNull(matching);
    }

    @Test
    @DisplayName("RuleAction enum should have all expected values")
    void testRuleActionEnum() {
        RuleAction[] actions = RuleAction.values();

        assertTrue(Arrays.asList(actions).contains(RuleAction.KEEP));
        assertTrue(Arrays.asList(actions).contains(RuleAction.DISCARD));
        assertTrue(Arrays.asList(actions).contains(RuleAction.STORE));
        assertTrue(Arrays.asList(actions).contains(RuleAction.EQUIP));
        assertTrue(Arrays.asList(actions).contains(RuleAction.SMELT));
        assertTrue(Arrays.asList(actions).contains(RuleAction.CRAFT));

        assertEquals(6, actions.length);
    }

    @Test
    @DisplayName("RuleContext should support enchantment field queries")
    void testContextEnchantmentField() {
        ItemRuleContext context = new ItemRuleContext.Builder()
                .itemType("sword")
                .enchantment("sharpness", 5)
                .enchantment("looting", 3)
                .build();

        assertEquals(5, context.getFieldValue("enchantment.sharpness"));
        assertEquals(3, context.getFieldValue("enchantment.looting"));
        assertEquals(0, context.getFieldValue("enchantment.unbreaking"));
    }

    @Test
    @DisplayName("RuleContext builder should support enchantments")
    void testContextBuilderEnchantments() {
        java.util.Map<String, Integer> enchantments = new java.util.HashMap<>();
        enchantments.put("sharpness", 5);
        enchantments.put("efficiency", 3);

        ItemRuleContext context = new ItemRuleContext.Builder()
                .itemType("pickaxe")
                .enchantments(enchantments)
                .build();

        assertEquals(2, context.getEnchantments().size());
        assertEquals(5, context.getEnchantments().get("sharpness"));
    }

    @Test
    @DisplayName("RuleEvaluator should handle not equals operator")
    void testRuleEvaluatorNotEquals() {
        List<RuleCondition> conditions = Arrays.asList(
                new RuleCondition("item_type", "!=", "dirt")
        );
        ItemRule rule = new ItemRule("Not dirt", conditions, RuleAction.KEEP, 50);

        assertTrue(RuleEvaluator.matches(rule, diamondContext));
        assertFalse(RuleEvaluator.matches(rule, new ItemRuleContext.Builder()
                .itemType("dirt")
                .build()));
    }

    @Test
    @DisplayName("RuleEvaluator should handle less than or equal operator")
    void testRuleEvaluatorLessThanOrEqual() {
        List<RuleCondition> conditions = Arrays.asList(
                new RuleCondition("durability", "<=", "50")
        );
        ItemRule rule = new ItemRule("Low durability", conditions, RuleAction.DISCARD, 50);

        assertTrue(RuleEvaluator.matches(rule, damagedToolContext)); // 30 durability
        assertTrue(RuleEvaluator.matches(rule, new ItemRuleContext.Builder()
                .itemType("tool")
                .durability(50)
                .build()));
        assertFalse(RuleEvaluator.matches(rule, new ItemRuleContext.Builder()
                .itemType("tool")
                .durability(51)
                .build()));
    }

    @Test
    @DisplayName("RuleCondition toString should contain all fields")
    void testRuleConditionToString() {
        RuleCondition condition = new RuleCondition("item_type", "==", "diamond");
        String str = condition.toString();

        assertTrue(str.contains("item_type"));
        assertTrue(str.contains("=="));
        assertTrue(str.contains("diamond"));
    }

    @Test
    @DisplayName("ItemRule toString should contain all fields")
    void testItemRuleToString() {
        ItemRule rule = new ItemRule("Test", Collections.emptyList(), RuleAction.KEEP, 50);
        String str = rule.toString();

        assertTrue(str.contains("Test"));
        assertTrue(str.contains("KEEP"));
        assertTrue(str.contains("50"));
    }

    @Test
    @DisplayName("RuleContext should return correct values for standard fields")
    void testContextStandardFields() {
        ItemRuleContext context = new ItemRuleContext.Builder()
                .itemType("diamond_sword")
                .rarity("rare")
                .quantity(1)
                .durability(100)
                .maxDurability(500)
                .displayName("Diamond Sword")
                .build();

        assertEquals("diamond_sword", context.getFieldValue("item_type"));
        assertEquals("rare", context.getFieldValue("rarity"));
        assertEquals(1, context.getFieldValue("quantity"));
        assertEquals(100, context.getFieldValue("durability"));
        assertEquals(500, context.getFieldValue("max_durability"));
        assertEquals("Diamond Sword", context.getFieldValue("display_name"));
    }

    @Test
    @DisplayName("RuleEvaluator should return null for empty rule list")
    void testRuleEvaluatorEmptyRules() {
        RuleAction action = RuleEvaluator.evaluate(Collections.emptyList(), diamondContext);
        assertNull(action);
    }

    @Test
    @DisplayName("RuleEvaluator should return null for null rule list")
    void testRuleEvaluatorNullRules() {
        RuleAction action = RuleEvaluator.evaluate(null, diamondContext);
        assertNull(action);
    }

    @Test
    @DisplayName("ItemRuleParser should return null for invalid JSON")
    void testParseInvalidJson() {
        ItemRule rule = ItemRuleParser.parseRule("invalid json");
        assertNull(rule);
    }

    @Test
    @DisplayName("ItemRuleParser should parse value as number when appropriate")
    void testParseValueNumber() {
        String json = """
                {
                  "name": "Numeric",
                  "conditions": [
                    {"field": "quantity", "operator": ">", "value": 64.5}
                  ],
                  "action": "KEEP",
                  "priority": 50
                }
                """;

        ItemRule rule = ItemRuleParser.parseRule(json);
        assertNotNull(rule);
        assertEquals(64.5, rule.getConditions().get(0).getValue());
    }

    @Test
    @DisplayName("ItemRuleContext builder should have sensible defaults")
    void testContextBuilderDefaults() {
        ItemRuleContext context = new ItemRuleContext.Builder().build();

        assertNull(context.getItemType());
        assertEquals(1, context.getQuantity()); // Default quantity
        assertTrue(context.getEnchantments().isEmpty());
        assertTrue(context.getCustomFields().isEmpty());
    }

    @Test
    @DisplayName("ItemRule should handle null conditions gracefully")
    void testItemRuleNullConditions() {
        ItemRule rule = new ItemRule();
        rule.setConditions(null);

        assertNotNull(rule.getConditions());
        assertTrue(rule.getConditions().isEmpty());
    }

    @Test
    @DisplayName("ItemRule should implement equals correctly")
    void testItemRuleEquals() {
        List<RuleCondition> conditions = Collections.singletonList(
                new RuleCondition("item_type", "==", "diamond")
        );

        ItemRule rule1 = new ItemRule("Test", conditions, RuleAction.KEEP, 50);
        ItemRule rule2 = new ItemRule("Test", conditions, RuleAction.KEEP, 50);
        ItemRule rule3 = new ItemRule("Different", conditions, RuleAction.DISCARD, 50);

        assertEquals(rule1, rule2);
        assertEquals(rule1.hashCode(), rule2.hashCode());
        assertNotEquals(rule1, rule3);
    }

    @Test
    @DisplayName("ItemRuleRegistry should handle removing non-existent rule")
    void testRegistryRemoveNonExistent() {
        boolean removed = registry.removeRule("Non-existent rule");
        assertFalse(removed);
    }

    @Test
    @DisplayName("ItemRuleRegistry should handle enabling non-existent rule")
    void testRegistryEnableNonExistent() {
        boolean result = registry.setRuleEnabled("Non-existent rule", true);
        assertFalse(result);
    }
}

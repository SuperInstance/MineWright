/**
 * Declarative item rules engine for intelligent inventory management.
 *
 * <p>This package provides a rule-based system for automatically deciding
 * what to do with items (keep, drop, pickup, ignore). Inspired by game bot
 * research from Honorbuddy and similar projects.</p>
 *
 * <h2>Key Components</h2>
 * <ul>
*   <li>{@link com.minewright.rules.ItemRule} - Declarative rule definition</li>
*   <li>{@link com.minewright.rules.RuleCondition} - Rule predicates (name, type, tag, etc.)</li>
*   <li>{@link com.minewright.rules.RuleAction} - Actions (KEEP, DROP, PICKUP, IGNORE)</li>
*   <li>{@link com.minewright.rules.RuleEvaluator} - Evaluate rules against items</li>
*   <li>{@link com.minewright.rules.ItemRuleParser} - Parse rules from configuration</li>
*   <li>{@link com.minewright.rules.ItemRuleRegistry} - Rule storage and lookup</li>
* </ul>
 *
 * <h2>Design Philosophy</h2>
 * <p>Rules are evaluated in priority order. First matching rule wins.
 * Rules can be defined in configuration files:</p>
 *
 * <pre>
 * rules:
 *   - name: "Keep diamonds"
 *     conditions:
 *       - type: "diamond"
 *     action: KEEP
 *     priority: 100
 *
 *   - name: "Drop cobblestone"
 *     conditions:
*       - type: "cobblestone"
 *       - count: "> 64"
 *     action: DROP
 *     priority: 50
 * </pre>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Evaluate an item against rules
 * ItemRule rule = registry.findMatchingRule(itemStack);
* if (rule != null) {
 *     switch (rule.getAction()) {
 *         case KEEP -> // Keep in inventory
 *         case DROP -> // Throw away
 *         case PICKUP -> // Auto-pickup if dropped
 *     }
 * }
 * }</pre>
 *
 * @since 1.7.0
 * @see com.minewright.rules.ItemRule
 * @see com.minewright.rules.RuleEvaluator
 */
package com.minewright.rules;

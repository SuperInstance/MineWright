package com.minewright.rules;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Evaluates item rules against a context to determine matching actions.
 * Rules are evaluated in priority order (highest first).
 * Returns the first matching rule's action.
 */
public class RuleEvaluator {
    private static final Logger LOGGER = LoggerFactory.getLogger(RuleEvaluator.class);

    /**
     * Evaluate a list of rules against the given context.
     * Returns the action of the first matching rule, or null if no rules match.
     *
     * @param rules List of rules to evaluate (should be sorted by priority)
     * @param context Context containing item information
     * @return The action of the first matching rule, or null if none match
     */
    public static RuleAction evaluate(List<ItemRule> rules, ItemRuleContext context) {
        if (rules == null || rules.isEmpty()) {
            LOGGER.debug("No rules to evaluate");
            return null;
        }

        for (ItemRule rule : rules) {
            if (!rule.isEnabled()) {
                LOGGER.debug("Skipping disabled rule: {}", rule.getName());
                continue;
            }

            if (matches(rule, context)) {
                LOGGER.debug("Rule '{}' matched for item '{}', action: {}",
                        rule.getName(), context.getItemType(), rule.getAction());
                return rule.getAction();
            }
        }

        LOGGER.debug("No rules matched for item '{}'", context.getItemType());
        return null;
    }

    /**
     * Check if a rule's conditions all match the given context.
     */
    public static boolean matches(ItemRule rule, ItemRuleContext context) {
        if (rule.getConditions() == null || rule.getConditions().isEmpty()) {
            // Rules with no conditions match everything
            return true;
        }

        for (RuleCondition condition : rule.getConditions()) {
            if (!evaluateCondition(condition, context)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Evaluate a single condition against the context.
     */
    @SuppressWarnings("unchecked")
    private static boolean evaluateCondition(RuleCondition condition, ItemRuleContext context) {
        String field = condition.getField();
        String operator = condition.getOperator();
        Object expectedValue = condition.getValue();
        Object actualValue = context.getFieldValue(field);

        if (actualValue == null) {
            // Handle null actual value
            return operator.equals("==") && expectedValue == null;
        }

        // String operators
        switch (operator) {
            case "==":
                return actualValue.equals(expectedValue);
            case "!=":
                return !actualValue.equals(expectedValue);
            case "contains":
                if (actualValue instanceof String && expectedValue instanceof String) {
                    return ((String) actualValue).toLowerCase()
                            .contains(((String) expectedValue).toLowerCase());
                }
                return false;
            case ">":
                return compare(actualValue, expectedValue) > 0;
            case "<":
                return compare(actualValue, expectedValue) < 0;
            case ">=":
                return compare(actualValue, expectedValue) >= 0;
            case "<=":
                return compare(actualValue, expectedValue) <= 0;
            default:
                LOGGER.warn("Unknown operator: {}", operator);
                return false;
        }
    }

    /**
     * Compare two values for numeric operators.
     * Supports Integers and comparables.
     */
    @SuppressWarnings("unchecked")
    private static int compare(Object actual, Object expected) {
        if (actual instanceof Number && expected instanceof Number) {
            return Double.compare(((Number) actual).doubleValue(), ((Number) expected).doubleValue());
        }
        if (actual instanceof Comparable && expected instanceof Comparable) {
            return ((Comparable<Object>) actual).compareTo(expected);
        }
        throw new IllegalArgumentException("Cannot compare " + actual + " with " + expected);
    }

    /**
     * Find the matching rule for a context (returns the rule, not just the action).
     */
    public static ItemRule findMatchingRule(List<ItemRule> rules, ItemRuleContext context) {
        if (rules == null || rules.isEmpty()) {
            return null;
        }

        for (ItemRule rule : rules) {
            if (!rule.isEnabled()) {
                continue;
            }

            if (matches(rule, context)) {
                return rule;
            }
        }

        return null;
    }
}

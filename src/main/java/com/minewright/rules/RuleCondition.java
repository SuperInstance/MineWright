package com.minewright.rules;

import java.util.Objects;

/**
 * A single condition in a rule that must be satisfied for the rule to match.
 */
public class RuleCondition {
    /**
     * The field to check (e.g., "item_type", "rarity", "quantity", "durability", "enchantment").
     */
    private final String field;

    /**
     * The comparison operator (e.g., "==", "!=", ">", "<", ">=", "<=", "contains").
     */
    private final String operator;

    /**
     * The value to compare against.
     */
    private final Object value;

    public RuleCondition(String field, String operator, Object value) {
        this.field = field;
        this.operator = operator;
        this.value = value;
    }

    public String getField() {
        return field;
    }

    public String getOperator() {
        return operator;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RuleCondition that = (RuleCondition) o;
        return Objects.equals(field, that.field) &&
                Objects.equals(operator, that.operator) &&
                Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(field, operator, value);
    }

    @Override
    public String toString() {
        return "RuleCondition{" +
                "field='" + field + '\'' +
                ", operator='" + operator + '\'' +
                ", value=" + value +
                '}';
    }
}

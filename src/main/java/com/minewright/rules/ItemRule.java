package com.minewright.rules;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A declarative rule for filtering and handling items.
 * Rules are evaluated in priority order (highest first).
 */
public class ItemRule {
    /**
     * Human-readable name for this rule.
     */
    private String name;

    /**
     * All conditions must match for this rule to apply.
     */
    private List<RuleCondition> conditions;

    /**
     * The action to take when this rule matches.
     */
    private RuleAction action;

    /**
     * Higher priority rules are evaluated first.
     */
    private int priority;

    /**
     * Whether this rule is currently enabled.
     */
    private boolean enabled;

    public ItemRule() {
        this.conditions = new ArrayList<>();
        this.priority = 0;
        this.enabled = true;
    }

    public ItemRule(String name, List<RuleCondition> conditions, RuleAction action, int priority) {
        this.name = name;
        this.conditions = conditions != null ? conditions : new ArrayList<>();
        this.action = action;
        this.priority = priority;
        this.enabled = true;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<RuleCondition> getConditions() {
        return conditions;
    }

    public void setConditions(List<RuleCondition> conditions) {
        this.conditions = conditions != null ? conditions : new ArrayList<>();
    }

    public RuleAction getAction() {
        return action;
    }

    public void setAction(RuleAction action) {
        this.action = action;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ItemRule itemRule = (ItemRule) o;
        return priority == itemRule.priority &&
                enabled == itemRule.enabled &&
                Objects.equals(name, itemRule.name) &&
                Objects.equals(conditions, itemRule.conditions) &&
                action == itemRule.action;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, conditions, action, priority, enabled);
    }

    @Override
    public String toString() {
        return "ItemRule{" +
                "name='" + name + '\'' +
                ", conditions=" + conditions +
                ", action=" + action +
                ", priority=" + priority +
                ", enabled=" + enabled +
                '}';
    }
}

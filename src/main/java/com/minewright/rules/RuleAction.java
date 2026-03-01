package com.minewright.rules;

/**
 * Actions that can be taken when an item rule matches.
 */
public enum RuleAction {
    /**
     * Keep the item in inventory.
     */
    KEEP,

    /**
     * Discard the item (drop on ground).
     */
    DISCARD,

    /**
     * Store the item in a designated chest.
     */
    STORE,

    /**
     * Equip the item (if wearable/holdable).
     */
    EQUIP,

    /**
     * Smelt the item (if smeltable).
     */
    SMELT,

    /**
     * Craft with the item.
     */
    CRAFT
}

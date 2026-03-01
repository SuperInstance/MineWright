package com.minewright.rules;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.HashMap;
import java.util.Map;

/**
 * Context for evaluating item rules.
 * Provides all relevant information about an item for rule matching.
 */
public class ItemRuleContext {
    private final String itemType;
    private final String rarity;
    private final int quantity;
    private final int durability;
    private final int maxDurability;
    private final Map<String, Integer> enchantments;
    private final String displayName;
    private final Map<String, Object> customFields;

    private ItemRuleContext(Builder builder) {
        this.itemType = builder.itemType;
        this.rarity = builder.rarity;
        this.quantity = builder.quantity;
        this.durability = builder.durability;
        this.maxDurability = builder.maxDurability;
        this.enchantments = builder.enchantments;
        this.displayName = builder.displayName;
        this.customFields = builder.customFields;
    }

    public String getItemType() {
        return itemType;
    }

    public String getRarity() {
        return rarity;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getDurability() {
        return durability;
    }

    public int getMaxDurability() {
        return maxDurability;
    }

    public Map<String, Integer> getEnchantments() {
        return enchantments;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Map<String, Object> getCustomFields() {
        return customFields;
    }

    /**
     * Get a field value for rule evaluation.
     * Supports standard fields and custom fields.
     */
    public Object getFieldValue(String field) {
        switch (field) {
            case "item_type":
                return itemType;
            case "rarity":
                return rarity;
            case "quantity":
                return quantity;
            case "durability":
                return durability;
            case "max_durability":
                return maxDurability;
            case "display_name":
                return displayName;
            default:
                if (field.startsWith("enchantment.")) {
                    String enchantmentName = field.substring("enchantment.".length());
                    return enchantments.getOrDefault(enchantmentName, 0);
                }
                return customFields.get(field);
        }
    }

    /**
     * Create context from a Minecraft ItemStack.
     */
    public static ItemRuleContext fromItemStack(ItemStack itemStack) {
        Item item = itemStack.getItem();
        String itemType = item.toString().toLowerCase();
        String rarity = item.getRarity(itemStack).toString().toLowerCase();
        int quantity = itemStack.getCount();
        int durability = itemStack.isDamageableItem() ?
                itemStack.getMaxDamage() - itemStack.getDamageValue() : 0;
        int maxDurability = itemStack.getMaxDamage();
        Map<Enchantment, Integer> mcEnchantments = EnchantmentHelper.deserializeEnchantments(
                itemStack.getEnchantmentTags()
        );

        // Convert Minecraft enchantment map to string-based map
        Map<String, Integer> enchantments = new HashMap<>();
        mcEnchantments.forEach((enchantment, level) -> {
            String enchantmentName = enchantment.getDescriptionId()
                    .replace("enchantment.minecraft.", "")
                    .toLowerCase();
            enchantments.put(enchantmentName, level);
        });

        return new Builder()
                .itemType(itemType)
                .rarity(rarity)
                .quantity(quantity)
                .durability(durability)
                .maxDurability(maxDurability)
                .enchantments(enchantments)
                .displayName(itemStack.getHoverName().getString())
                .build();
    }

    /**
     * Builder for creating ItemRuleContext instances.
     */
    public static class Builder {
        private String itemType;
        private String rarity;
        private int quantity = 1;
        private int durability;
        private int maxDurability;
        private Map<String, Integer> enchantments = new HashMap<>();
        private String displayName;
        private Map<String, Object> customFields = new HashMap<>();

        public Builder itemType(String itemType) {
            this.itemType = itemType;
            return this;
        }

        public Builder rarity(String rarity) {
            this.rarity = rarity;
            return this;
        }

        public Builder quantity(int quantity) {
            this.quantity = quantity;
            return this;
        }

        public Builder durability(int durability) {
            this.durability = durability;
            return this;
        }

        public Builder maxDurability(int maxDurability) {
            this.maxDurability = maxDurability;
            return this;
        }

        public Builder enchantments(Map<String, Integer> enchantments) {
            this.enchantments = enchantments != null ? enchantments : new HashMap<>();
            return this;
        }

        public Builder enchantment(String name, int level) {
            this.enchantments.put(name, level);
            return this;
        }

        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder addCustomField(String name, Object value) {
            this.customFields.put(name, value);
            return this;
        }

        public ItemRuleContext build() {
            return new ItemRuleContext(this);
        }
    }
}

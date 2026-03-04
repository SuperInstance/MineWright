package com.minewright.mentorship;

import com.minewright.mentorship.MentorshipModels.SkillLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Worker skill and progress tracking.
 *
 * <p>Profiles track individual worker capabilities, improvement over time,
 * mistake patterns, and relationship metrics.</p>
 *
 * @since 1.5.0
 */
public class WorkerProfile {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorkerProfile.class);

    private final String name;
    private final String role;
    private final Map<String, SkillLevel> skills;
    private final Map<String, Integer> taskHistory;
    private final Set<String> repeatedMistakes;
    private int rapportLevel;
    private double stressLevel;
    private double focusLevel;

    public WorkerProfile(String name, String role) {
        this.name = name;
        this.role = role;
        this.skills = new ConcurrentHashMap<>();
        this.taskHistory = new ConcurrentHashMap<>();
        this.repeatedMistakes = ConcurrentHashMap.newKeySet();
        this.rapportLevel = 10;
        this.stressLevel = 0.0;
        this.focusLevel = 1.0;
    }

    public SkillLevel getSkillLevel(String context) {
        // Extract skill category from context
        String category = extractSkillCategory(context);
        return skills.getOrDefault(category, SkillLevel.NOVICE);
    }

    private String extractSkillCategory(String context) {
        String lower = context.toLowerCase();

        if (lower.contains("redstone") || lower.contains("circuit")) return "redstone";
        if (lower.contains("build") || lower.contains("construct")) return "building";
        if (lower.contains("mine") || lower.contains("dig")) return "mining";
        if (lower.contains("farm") || lower.contains("crop")) return "farming";
        if (lower.contains("craft")) return "crafting";

        return "general";
    }

    public void improveSkill(String context) {
        String category = extractSkillCategory(context);
        SkillLevel current = getSkillLevel(context);

        if (current.ordinal() < SkillLevel.values().length - 1) {
            skills.put(category, SkillLevel.values()[current.ordinal() + 1]);
            LOGGER.debug("Worker '{}' improved skill '{}' to {}",
                name, category, skills.get(category));
        }
    }

    public void trackMistake(String context) {
        String key = "mistake:" + context;
        int count = taskHistory.getOrDefault(key, 0) + 1;

        if (count >= 3) {
            repeatedMistakes.add(context);
        }

        taskHistory.put(key, count);
    }

    public boolean hasRepeatedMistake(String context) {
        return repeatedMistakes.contains(context);
    }

    public void recordSuccess(String context) {
        String category = extractSkillCategory(context);
        String key = category + "_success";
        int count = taskHistory.getOrDefault(key, 0) + 1;
        taskHistory.put(key, count);

        // Improve skill every 3 successes
        if (count % 3 == 0) {
            improveSkill(category);
        }
    }

    public int getSuccessCount(String context) {
        String category = extractSkillCategory(context);
        return taskHistory.getOrDefault(category + "_success", 0);
    }

    public boolean hasImprovedSpeed(String context) {
        String key = extractSkillCategory(context) + "_fast";
        return taskHistory.getOrDefault(key, 0) > 1;
    }

    public boolean hasImprovedQuality(String context) {
        String key = extractSkillCategory(context) + "_quality";
        return taskHistory.getOrDefault(key, 0) > 1;
    }

    public boolean recentlyCompletedWithoutHelp(String context) {
        String key = extractSkillCategory(context) + "_independent";
        return taskHistory.getOrDefault(key, 0) > 0;
    }

    public void recordIndependentCompletion(String context) {
        String key = extractSkillCategory(context) + "_independent";
        taskHistory.put(key, taskHistory.getOrDefault(key, 0) + 1);
    }

    public boolean usedCreativeApproach(String context) {
        return taskHistory.containsKey(extractSkillCategory(context) + "_creative");
    }

    // Getters and setters
    public String getName() { return name; }
    public String getRole() { return role; }
    public int getRapportLevel() { return rapportLevel; }
    public void setRapportLevel(int level) { this.rapportLevel = Math.max(0, Math.min(100, level)); }
    public double getStressLevel() { return stressLevel; }
    public void setStressLevel(double level) { this.stressLevel = Math.max(0.0, Math.min(1.0, level)); }
    public double getFocusLevel() { return focusLevel; }
    public void setFocusLevel(double level) { this.focusLevel = Math.max(0.0, Math.min(1.0, level)); }

    public void saveToNBT(CompoundTag tag) {
        tag.putString("Name", name);
        tag.putString("Role", role);
        tag.putInt("Rapport", rapportLevel);
        tag.putDouble("Stress", stressLevel);
        tag.putDouble("Focus", focusLevel);

        // Save skills
        CompoundTag skillsTag = new CompoundTag();
        for (Map.Entry<String, SkillLevel> entry : skills.entrySet()) {
            skillsTag.putInt(entry.getKey(), entry.getValue().ordinal());
        }
        tag.put("Skills", skillsTag);

        // Save task history
        CompoundTag historyTag = new CompoundTag();
        for (Map.Entry<String, Integer> entry : taskHistory.entrySet()) {
            historyTag.putInt(entry.getKey(), entry.getValue());
        }
        tag.put("TaskHistory", historyTag);

        // Save repeated mistakes
        ListTag mistakesTag = new ListTag();
        for (String mistake : repeatedMistakes) {
            mistakesTag.add(net.minecraft.nbt.StringTag.valueOf(mistake));
        }
        tag.put("RepeatedMistakes", mistakesTag);
    }

    public static WorkerProfile loadFromNBT(CompoundTag tag) {
        String name = tag.getString("Name");
        String role = tag.getString("Role");
        WorkerProfile profile = new WorkerProfile(name, role);

        profile.rapportLevel = tag.getInt("Rapport");
        profile.stressLevel = tag.getDouble("Stress");
        profile.focusLevel = tag.getDouble("Focus");

        // Load skills
        if (tag.contains("Skills")) {
            CompoundTag skillsTag = tag.getCompound("Skills");
            for (String key : skillsTag.getAllKeys()) {
                int level = skillsTag.getInt(key);
                profile.skills.put(key, SkillLevel.values()[level]);
            }
        }

        // Load task history
        if (tag.contains("TaskHistory")) {
            CompoundTag historyTag = tag.getCompound("TaskHistory");
            for (String key : historyTag.getAllKeys()) {
                profile.taskHistory.put(key, historyTag.getInt(key));
            }
        }

        // Load repeated mistakes
        if (tag.contains("RepeatedMistakes")) {
            ListTag mistakesTag = tag.getList("RepeatedMistakes", 8);
            for (int i = 0; i < mistakesTag.size(); i++) {
                profile.repeatedMistakes.add(mistakesTag.getString(i));
            }
        }

        return profile;
    }
}

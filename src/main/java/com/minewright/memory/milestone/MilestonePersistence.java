package com.minewright.memory.milestone;

import com.minewright.memory.MilestoneTracker;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;

/**
 * Handles NBT persistence for milestone data.
 *
 * <p>This class manages saving and loading milestones to/from NBT format.</p>
 */
public class MilestonePersistence {

    private static final Logger LOGGER = LoggerFactory.getLogger(MilestonePersistence.class);

    private final MilestoneStore store;

    public MilestonePersistence(MilestoneStore store) {
        this.store = store;
    }

    /**
     * Saves milestone data to NBT.
     */
    public void saveToNBT(CompoundTag tag) {
        // Save achieved milestones
        ListTag milestonesList = new ListTag();
        for (MilestoneTracker.Milestone milestone : store.getAchievedMilestonesMap().values()) {
            CompoundTag milestoneTag = new CompoundTag();
            milestoneTag.putString("Id", milestone.id);
            milestoneTag.putString("Type", milestone.type.name());
            milestoneTag.putString("Title", milestone.title);
            milestoneTag.putString("Description", milestone.description);
            milestoneTag.putInt("Importance", milestone.importance);
            milestoneTag.putLong("AchievedAt", milestone.achievedAt.toEpochMilli());
            milestonesList.add(milestoneTag);
        }
        tag.put("Milestones", milestonesList);

        // Save first occurrences
        CompoundTag firstOccurrencesTag = new CompoundTag();
        for (Map.Entry<String, Instant> entry : store.getFirstOccurrencesMap().entrySet()) {
            firstOccurrencesTag.putLong(entry.getKey(), entry.getValue().toEpochMilli());
        }
        tag.put("FirstOccurrences", firstOccurrencesTag);

        // Save counters
        CompoundTag countersTag = new CompoundTag();
        for (Map.Entry<String, Integer> entry : store.getCountersMap().entrySet()) {
            countersTag.putInt(entry.getKey(), entry.getValue());
        }
        tag.put("Counters", countersTag);

        LOGGER.debug("MilestoneTracker saved to NBT ({} milestones)", store.getMilestoneCount());
    }

    /**
     * Loads milestone data from NBT.
     */
    public void loadFromNBT(CompoundTag tag) {
        // Load achieved milestones
        ListTag milestonesList = tag.getList("Milestones", 10);
        if (!milestonesList.isEmpty()) {
            store.getAchievedMilestonesMap().clear();
            for (int i = 0; i < milestonesList.size(); i++) {
                CompoundTag milestoneTag = milestonesList.getCompound(i);
                MilestoneTracker.Milestone milestone = new MilestoneTracker.Milestone(
                    milestoneTag.getString("Id"),
                    MilestoneTracker.MilestoneType.valueOf(milestoneTag.getString("Type")),
                    milestoneTag.getString("Title"),
                    milestoneTag.getString("Description"),
                    milestoneTag.getInt("Importance"),
                    Instant.ofEpochMilli(milestoneTag.getLong("AchievedAt"))
                );
                store.getAchievedMilestonesMap().put(milestone.id, milestone);
            }
        }

        // Load first occurrences
        CompoundTag firstOccurrencesTag = tag.getCompound("FirstOccurrences");
        if (!firstOccurrencesTag.isEmpty()) {
            store.getFirstOccurrencesMap().clear();
            for (String key : firstOccurrencesTag.getAllKeys()) {
                store.getFirstOccurrencesMap().put(key, Instant.ofEpochMilli(firstOccurrencesTag.getLong(key)));
            }
        }

        // Load counters
        CompoundTag countersTag = tag.getCompound("Counters");
        if (!countersTag.isEmpty()) {
            store.getCountersMap().clear();
            for (String key : countersTag.getAllKeys()) {
                store.getCountersMap().put(key, countersTag.getInt(key));
            }
        }

        LOGGER.info("MilestoneTracker loaded from NBT ({} milestones)", store.getMilestoneCount());
    }
}

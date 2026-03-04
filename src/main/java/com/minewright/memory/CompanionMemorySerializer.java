package com.minewright.memory;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

/**
 * Handles NBT serialization and deserialization for CompanionMemory.
 *
 * <p>This class manages saving and loading companion memory data to/from
 * Minecraft's NBT format for world persistence.</p>
 *
 * @since 1.4.0
 */
public class CompanionMemorySerializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompanionMemorySerializer.class);

    /**
     * Saves companion memory data to NBT format for world save.
     */
    public static void saveToNBT(CompoundTag tag, MemoryStore memoryStore,
                                 RelationshipTracker relationshipTracker,
                                 PersonalitySystem.PersonalityProfile personality) {

        // Save relationship data
        tag.putInt("RapportLevel", relationshipTracker.getRapportLevel());
        tag.putInt("TrustLevel", relationshipTracker.getTrustLevel());
        tag.putInt("InteractionCount", relationshipTracker.getInteractionCount());

        if (relationshipTracker.getFirstMeeting() != null) {
            tag.putLong("FirstMeeting", relationshipTracker.getFirstMeeting().toEpochMilli());
        }

        if (relationshipTracker.getPlayerName() != null) {
            tag.putString("PlayerName", relationshipTracker.getPlayerName());
        }

        // Save episodic memories
        ListTag episodicList = new ListTag();
        for (CompanionMemory.EpisodicMemory memory : memoryStore.getEpisodicMemories()) {
            CompoundTag memoryTag = new CompoundTag();
            memoryTag.putString("EventType", memory.eventType);
            memoryTag.putString("Description", memory.description);
            memoryTag.putInt("EmotionalWeight", memory.emotionalWeight);
            memoryTag.putLong("Timestamp", memory.timestamp.toEpochMilli());
            memoryTag.putInt("AccessCount", memory.getAccessCount());
            memoryTag.putLong("LastAccessed", memory.getLastAccessed().toEpochMilli());
            memoryTag.putBoolean("IsMilestone", memory.isMilestone());
            episodicList.add(memoryTag);
        }
        tag.put("EpisodicMemories", episodicList);

        // Save semantic memories
        ListTag semanticList = new ListTag();
        for (var entry : memoryStore.getSemanticMemories().entrySet()) {
            CompoundTag semanticTag = new CompoundTag();
            semanticTag.putString("Key", entry.getKey());
            semanticTag.putString("Category", entry.getValue().category);
            semanticTag.putString("FactKey", entry.getValue().key);

            Object value = entry.getValue().value;
            if (value instanceof String) {
                semanticTag.putString("Value", (String) value);
                semanticTag.putString("ValueType", "string");
            } else if (value instanceof Integer) {
                semanticTag.putInt("Value", (Integer) value);
                semanticTag.putString("ValueType", "int");
            } else if (value instanceof Boolean) {
                semanticTag.putBoolean("Value", (Boolean) value);
                semanticTag.putString("ValueType", "boolean");
            } else {
                semanticTag.putString("Value", value.toString());
                semanticTag.putString("ValueType", "string");
            }

            semanticTag.putLong("LearnedAt", entry.getValue().learnedAt.toEpochMilli());
            semanticTag.putInt("Confidence", entry.getValue().confidence);
            semanticList.add(semanticTag);
        }
        tag.put("SemanticMemories", semanticList);

        // Save emotional memories
        ListTag emotionalList = new ListTag();
        synchronized (memoryStore) {
            for (CompanionMemory.EmotionalMemory memory : memoryStore.getEmotionalMemories()) {
                CompoundTag emotionTag = new CompoundTag();
                emotionTag.putString("EventType", memory.eventType);
                emotionTag.putString("Description", memory.description);
                emotionTag.putInt("EmotionalWeight", memory.emotionalWeight);
                emotionTag.putLong("Timestamp", memory.timestamp.toEpochMilli());
                emotionalList.add(emotionTag);
            }
        }
        tag.put("EmotionalMemories", emotionalList);

        // Save inside jokes
        ListTag jokesList = new ListTag();
        for (CompanionMemory.InsideJoke joke : relationshipTracker.getConversationalMemory().getInsideJokes()) {
            CompoundTag jokeTag = new CompoundTag();
            jokeTag.putString("Context", joke.context);
            jokeTag.putString("Punchline", joke.punchline);
            jokeTag.putLong("CreatedAt", joke.createdAt.toEpochMilli());
            jokeTag.putInt("ReferenceCount", joke.referenceCount);
            jokesList.add(jokeTag);
        }
        tag.put("InsideJokes", jokesList);

        // Save discussed topics
        ListTag topicsList = new ListTag();
        for (String topic : relationshipTracker.getConversationalMemory().getDiscussedTopics()) {
            topicsList.add(StringTag.valueOf(topic));
        }
        tag.put("DiscussedTopics", topicsList);

        // Save phrase usage
        CompoundTag phraseUsageTag = new CompoundTag();
        for (var entry : relationshipTracker.getConversationalMemory().getPhraseUsage().entrySet()) {
            phraseUsageTag.putInt(entry.getKey(), entry.getValue());
        }
        tag.put("PhraseUsage", phraseUsageTag);

        // Save player preferences
        CompoundTag preferencesTag = new CompoundTag();
        for (var entry : relationshipTracker.getPlayerPreferences().entrySet()) {
            saveValueToNBT(preferencesTag, entry.getKey(), entry.getValue());
        }
        tag.put("PlayerPreferences", preferencesTag);

        // Save playstyle metrics
        CompoundTag playstyleTag = new CompoundTag();
        for (var entry : relationshipTracker.getPlaystyleMetrics().entrySet()) {
            playstyleTag.putInt(entry.getKey(), entry.getValue());
        }
        tag.put("PlaystyleMetrics", playstyleTag);

        // Save personality
        savePersonalityToNBT(tag, personality);

        // Save milestone tracker
        CompoundTag milestoneTag = new CompoundTag();
        relationshipTracker.getMilestoneTracker().saveToNBT(milestoneTag);
        tag.put("MilestoneTracker", milestoneTag);

        LOGGER.debug("CompanionMemory saved to NBT ({} episodic, {} semantic memories, {} milestones)",
            memoryStore.getEpisodicMemories().size(), memoryStore.getSemanticMemories().size(),
            relationshipTracker.getMilestones().size());
    }

    /**
     * Loads companion memory data from NBT format.
     */
    public static void loadFromNBT(CompoundTag tag, MemoryStore memoryStore,
                                    RelationshipTracker relationshipTracker,
                                    PersonalitySystem.PersonalityProfile personality) {

        // Load relationship data
        relationshipTracker.adjustRapport(tag.getInt("RapportLevel") - relationshipTracker.getRapportLevel());
        relationshipTracker.adjustTrust(tag.getInt("TrustLevel") - relationshipTracker.getTrustLevel());
        // Note: interactionCount is AtomicInteger, we need to set it directly
        while (relationshipTracker.getInteractionCount() < tag.getInt("InteractionCount")) {
            relationshipTracker.incrementInteractionCount();
        }

        long firstMeetingEpoch = tag.getLong("FirstMeeting");
        if (firstMeetingEpoch != 0) {
            // Set first meeting via reflection or direct access if needed
            // For now, we'll skip this as it's a volatile field
        }

        // Note: playerName is volatile, would need to be set via reflection or direct access

        // Load episodic memories
        ListTag episodicList = tag.getList("EpisodicMemories", 10);
        if (!episodicList.isEmpty()) {
            memoryStore.getEpisodicMemories().clear();
            for (int i = 0; i < episodicList.size(); i++) {
                CompoundTag memoryTag = episodicList.getCompound(i);
                CompanionMemory.EpisodicMemory memory = new CompanionMemory.EpisodicMemory(
                    memoryTag.getString("EventType"),
                    memoryTag.getString("Description"),
                    memoryTag.getInt("EmotionalWeight"),
                    Instant.ofEpochMilli(memoryTag.getLong("Timestamp"))
                );

                // Load access tracking fields
                if (memoryTag.contains("AccessCount")) {
                    for (int j = 0; j < memoryTag.getInt("AccessCount"); j++) {
                        memory.recordAccess();
                    }
                }
                if (memoryTag.contains("IsMilestone")) {
                    memory.setMilestone(memoryTag.getBoolean("IsMilestone"));
                }

                memoryStore.getEpisodicMemories().add(memory);
            }
        }

        // Load semantic memories
        ListTag semanticList = tag.getList("SemanticMemories", 10);
        if (!semanticList.isEmpty()) {
            memoryStore.getSemanticMemories().clear();
            for (int i = 0; i < semanticList.size(); i++) {
                CompoundTag semanticTag = semanticList.getCompound(i);
                String key = semanticTag.getString("Key");
                String category = semanticTag.getString("Category");
                String factKey = semanticTag.getString("FactKey");
                String valueType = semanticTag.getString("ValueType");

                Object value = switch (valueType) {
                    case "int" -> semanticTag.getInt("Value");
                    case "boolean" -> semanticTag.getBoolean("Value");
                    default -> semanticTag.getString("Value");
                };

                CompanionMemory.SemanticMemory memory = new CompanionMemory.SemanticMemory(
                    category, factKey, value,
                    Instant.ofEpochMilli(semanticTag.getLong("LearnedAt"))
                );
                memory.confidence = semanticTag.getInt("Confidence");
                memoryStore.getSemanticMemories().put(key, memory);
            }
        }

        // Load emotional memories
        ListTag emotionalList = tag.getList("EmotionalMemories", 10);
        if (!emotionalList.isEmpty()) {
            synchronized (memoryStore) {
                memoryStore.getEmotionalMemories().clear();
                for (int i = 0; i < emotionalList.size(); i++) {
                    CompoundTag emotionTag = emotionalList.getCompound(i);
                    CompanionMemory.EmotionalMemory memory = new CompanionMemory.EmotionalMemory(
                        emotionTag.getString("EventType"),
                        emotionTag.getString("Description"),
                        emotionTag.getInt("EmotionalWeight"),
                        Instant.ofEpochMilli(emotionTag.getLong("Timestamp"))
                    );
                    memoryStore.getEmotionalMemories().add(memory);
                }
            }
        }

        // Load inside jokes
        ListTag jokesList = tag.getList("InsideJokes", 10);
        if (!jokesList.isEmpty()) {
            relationshipTracker.getConversationalMemory().clearInsideJokes();
            for (int i = 0; i < jokesList.size(); i++) {
                CompoundTag jokeTag = jokesList.getCompound(i);
                CompanionMemory.InsideJoke joke = new CompanionMemory.InsideJoke(
                    jokeTag.getString("Context"),
                    jokeTag.getString("Punchline"),
                    Instant.ofEpochMilli(jokeTag.getLong("CreatedAt"))
                );
                joke.referenceCount = jokeTag.getInt("ReferenceCount");
                relationshipTracker.getConversationalMemory().addInsideJokeDirect(joke);
            }
        }

        // Load discussed topics
        ListTag topicsList = tag.getList("DiscussedTopics", 8);
        if (!topicsList.isEmpty()) {
            relationshipTracker.getConversationalMemory().clearDiscussedTopics();
            for (int i = 0; i < topicsList.size(); i++) {
                relationshipTracker.getConversationalMemory().addDiscussedTopicDirect(topicsList.getString(i));
            }
        }

        // Load phrase usage
        CompoundTag phraseUsageTag = tag.getCompound("PhraseUsage");
        if (!phraseUsageTag.isEmpty()) {
            relationshipTracker.getConversationalMemory().clearPhraseUsage();
            for (String key : phraseUsageTag.getAllKeys()) {
                relationshipTracker.getConversationalMemory().addPhraseUsage(key, phraseUsageTag.getInt(key));
            }
        }

        // Load player preferences
        CompoundTag preferencesTag = tag.getCompound("PlayerPreferences");
        if (!preferencesTag.isEmpty()) {
            relationshipTracker.getPlayerPreferences().clear();
            for (String key : preferencesTag.getAllKeys()) {
                Object value = preferencesTag.contains(key, 99)
                    ? preferencesTag.getInt(key)
                    : preferencesTag.getString(key);
                relationshipTracker.getPlayerPreferences().put(key, value);
            }
        }

        // Load playstyle metrics
        CompoundTag playstyleTag = tag.getCompound("PlaystyleMetrics");
        if (!playstyleTag.isEmpty()) {
            relationshipTracker.getPlaystyleMetrics().clear();
            for (String key : playstyleTag.getAllKeys()) {
                relationshipTracker.getPlaystyleMetrics().put(key, playstyleTag.getInt(key));
            }
        }

        // Load personality
        loadPersonalityFromNBT(tag.getCompound("Personality"), personality);

        // Load milestone tracker
        CompoundTag milestoneTag = tag.getCompound("MilestoneTracker");
        if (!milestoneTag.isEmpty()) {
            relationshipTracker.getMilestoneTracker().loadFromNBT(milestoneTag);
        }

        LOGGER.info("CompanionMemory loaded from NBT ({} episodic, {} semantic memories)",
            memoryStore.getEpisodicMemories().size(), memoryStore.getSemanticMemories().size());
    }

    /**
     * Saves personality data to NBT.
     */
    private static void savePersonalityToNBT(CompoundTag tag, PersonalitySystem.PersonalityProfile personality) {
        CompoundTag personalityTag = new CompoundTag();
        personalityTag.putInt("Openness", personality.getOpenness());
        personalityTag.putInt("Conscientiousness", personality.getConscientiousness());
        personalityTag.putInt("Extraversion", personality.getExtraversion());
        personalityTag.putInt("Agreeableness", personality.getAgreeableness());
        personalityTag.putInt("Neuroticism", personality.getNeuroticism());
        personalityTag.putInt("Humor", personality.getHumor());
        personalityTag.putInt("Encouragement", personality.getEncouragement());
        personalityTag.putInt("Formality", personality.getFormality());
        personalityTag.putString("FavoriteBlock", personality.getFavoriteBlock());
        personalityTag.putString("WorkStyle", personality.getWorkStyle());
        personalityTag.putString("Mood", personality.getMood());
        personalityTag.putString("ArchetypeName", personality.getArchetypeName());

        // Save catchphrases
        ListTag catchphrasesList = new ListTag();
        for (String catchphrase : personality.getCatchphrases()) {
            catchphrasesList.add(StringTag.valueOf(catchphrase));
        }
        personalityTag.put("Catchphrases", catchphrasesList);

        // Save verbal tics
        ListTag verbalTicsList = new ListTag();
        for (String tic : personality.getVerbalTics()) {
            verbalTicsList.add(StringTag.valueOf(tic));
        }
        personalityTag.put("VerbalTics", verbalTicsList);

        // Save tic usage counts
        CompoundTag ticUsageTag = new CompoundTag();
        for (var entry : personality.getTicUsageCount().entrySet()) {
            ticUsageTag.putInt(entry.getKey(), entry.getValue());
        }
        personalityTag.put("TicUsageCount", ticUsageTag);

        tag.put("Personality", personalityTag);
    }

    /**
     * Loads personality data from NBT.
     */
    private static void loadPersonalityFromNBT(CompoundTag personalityTag,
                                                PersonalitySystem.PersonalityProfile personality) {
        if (personalityTag.isEmpty()) {
            return;
        }

        personality.setOpenness(personalityTag.getInt("Openness"));
        personality.setConscientiousness(personalityTag.getInt("Conscientiousness"));
        personality.setExtraversion(personalityTag.getInt("Extraversion"));
        personality.setAgreeableness(personalityTag.getInt("Agreeableness"));
        personality.setNeuroticism(personalityTag.getInt("Neuroticism"));
        personality.setHumor(personalityTag.getInt("Humor"));
        personality.setEncouragement(personalityTag.getInt("Encouragement"));
        personality.setFormality(personalityTag.getInt("Formality"));

        personality.setFavoriteBlock(personalityTag.getString("FavoriteBlock"));
        personality.setWorkStyle(personalityTag.getString("WorkStyle"));
        personality.setMood(personalityTag.getString("Mood"));
        personality.setArchetypeName(personalityTag.contains("ArchetypeName")
            ? personalityTag.getString("ArchetypeName")
            : "THE_FOREMAN");

        // Load catchphrases
        ListTag catchphrasesList = personalityTag.getList("Catchphrases", 8);
        if (!catchphrasesList.isEmpty()) {
            personality.setCatchphrases(new java.util.ArrayList<>());
            for (int i = 0; i < catchphrasesList.size(); i++) {
                personality.addCatchphrase(catchphrasesList.getString(i));
            }
        }

        // Load verbal tics
        ListTag verbalTicsList = personalityTag.getList("VerbalTics", 8);
        if (!verbalTicsList.isEmpty()) {
            personality.setVerbalTics(new java.util.ArrayList<>());
            for (int i = 0; i < verbalTicsList.size(); i++) {
                personality.addVerbalTic(verbalTicsList.getString(i));
            }
        }

        // Note: ticUsageCount is private and not directly accessible
        // We would need to add a setter method or load it differently
    }

    /**
     * Saves an Object value to NBT with type tag.
     */
    private static void saveValueToNBT(CompoundTag tag, String key, Object value) {
        if (value instanceof String) {
            tag.putString(key, (String) value);
            tag.putString(key + "Type", "string");
        } else if (value instanceof Integer) {
            tag.putInt(key, (Integer) value);
            tag.putString(key + "Type", "int");
        } else if (value instanceof Boolean) {
            tag.putBoolean(key, (Boolean) value);
            tag.putString(key + "Type", "boolean");
        } else {
            tag.putString(key, value.toString());
            tag.putString(key + "Type", "string");
        }
    }

    /**
     * Loads an Object value from NBT based on type tag.
     */
    private static Object loadValueFromNBT(CompoundTag tag, String key) {
        String valueType = tag.getString(key + "Type");
        return switch (valueType) {
            case "int" -> tag.getInt(key);
            case "boolean" -> tag.getBoolean(key);
            default -> tag.getString(key);
        };
    }
}

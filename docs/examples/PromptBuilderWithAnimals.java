package com.minewright.llm;

import com.minewright.entity.ForemanEntity;
import com.minewright.memory.WorldKnowledge;
import com.minewright.animal.AnimalClassification;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

/**
 * Extended PromptBuilder with animal handling capabilities.
 *
 * <p>This extends the base PromptBuilder to include animal-related actions
 * and context in the LLM prompts. Update your existing PromptBuilder to include
 * these additions.</p>
 *
 * <p>To integrate, add the ANIMAL_ACTIONS section to your system prompt and
 * include animal information in the user prompt context.</p>
 */
public class PromptBuilderWithAnimals {

    private static final String ANIMAL_ACTIONS = """
ACTIONS FOR ANIMAL HANDLING:
- tame: {"target": "wolf"} (options: wolf, cat, parrot, horse, llama, camel)
- breed: {"animal": "cow", "count": 2} (options: cow, sheep, pig, chicken, goat, rabbit)
- shear: {"target": "sheep"} (collect wool from sheep)
- milk: {"target": "cow"} (milk cows with bucket)
- collect_eggs: {} (collect chicken eggs)
- build_pen: {"type": "sheep", "size": [16, 16, 3]} (type: cow, sheep, chicken, pig; size: [width, depth, height])
- pet_follow: {"mode": "follow"} (modes: follow, stay, protect)
- assign_pen: {"pen": "pen_1", "animals": "sheep"} (move animals to pen)

ANIMAL BREEDING ITEMS:
- Cows/Sheep/Goats: wheat
- Pigs/Rabbits: carrots
- Chickens: seeds (any)
- Wolves: bones
- Cats: cod or salmon
- Horses: golden carrots or golden apples

ANIMAL PRODUCTS:
- Sheep: wool (shear), mutton (kill)
- Cows: leather, beef, milk (bucket)
- Chickens: feathers, chicken, eggs
- Pigs: pork
- Goats: milk
- Rabbits: rabbit hide, rabbit meat, rabbit foot

RULES FOR ANIMAL HANDLING:
1. Always build a pen before breeding large numbers
2. Tame wolves for protection against hostile mobs
3. Shear sheep before breeding them (wool regenerates)
4. Collect eggs daily from chickens
5. Use pet_follow "protect" mode to have pets defend you
6. Animals need space - minimum 4x4 per adult animal

EXAMPLES:

Input: "tame a wolf"
Output: {"action": "tame", "parameters": {"target": "wolf"}}

Input: "build a chicken coop"
Output: {"action": "build_pen", "parameters": {"type": "chicken", "size": [12, 12, 3]}}

Input: "start a cow farm"
Output: {"action": "build_pen", "parameters": {"type": "cow", "size": [20, 20, 3]}},
        {"action": "breed", "parameters": {"animal": "cow", "count": 2}}

Input: "get some wool"
Output: {"action": "shear", "parameters": {"target": "sheep"}}

Input: "collect eggs and milk"
Output: {"action": "collect_eggs", "parameters": {}},
        {"action": "milk", "parameters": {"target": "cow"}}

Input: "make my pets follow me"
Output: {"action": "pet_follow", "parameters": {"mode": "follow"}}

Input: "breed 5 cows"
Output: {"action": "breed", "parameters": {"animal": "cow", "count": 5}}
""";

    /**
     * Build system prompt with animal handling
     *
     * Add this to your existing buildSystemPrompt() method
     */
    public static String buildSystemPromptWithAnimals() {
        return """
            You are a Minecraft AI agent. Respond ONLY with valid JSON, no extra text.

            FORMAT (strict JSON):
            {"reasoning": "brief thought", "plan": "action description", "tasks": [{"action": "type", "parameters": {...}}]}

            """ + ANIMAL_ACTIONS + """

            RULES:
            1. ALWAYS use "hostile" for attack target (mobs, monsters, creatures)
            2. Keep reasoning under 15 words
            3. Animals should be in pens before breeding
            4. Tame animals before commanding them

            CRITICAL: Output ONLY valid JSON. No markdown, no explanations, no line breaks in JSON.
            """;
    }

    /**
     * Build user prompt with animal context
     *
     * Enhance your existing buildUserPrompt() to include animal information
     */
    public static String buildUserPromptWithAnimals(
        ForemanEntity foreman,
        String command,
        WorldKnowledge worldKnowledge,
        String animalSummary
    ) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("=== YOUR SITUATION ===\n");
        prompt.append("Position: ").append(formatPosition(foreman.blockPosition())).append("\n");
        prompt.append("Nearby Players: ").append(worldKnowledge.getNearbyPlayerNames()).append("\n");
        prompt.append("Nearby Entities: ").append(worldKnowledge.getNearbyEntitiesSummary()).append("\n");
        prompt.append("Nearby Blocks: ").append(worldKnowledge.getNearbyBlocksSummary()).append("\n");
        prompt.append("Nearby Animals: ").append(animalSummary).append("\n");
        prompt.append("Biome: ").append(worldKnowledge.getBiomeName()).append("\n");

        prompt.append("\n=== PLAYER COMMAND ===\n");
        prompt.append("\"").append(command).append("\"\n");

        prompt.append("\n=== YOUR RESPONSE (with reasoning) ===\n");

        return prompt.toString();
    }

    /**
     * Generate animal summary for context
     *
     * Call this from WorldKnowledge or AnimalDetector
     */
    public static String generateAnimalSummary(ForemanEntity foreman) {
        var detector = new com.minewright.animal.AnimalDetector(foreman);
        var classified = detector.scan(32);

        if (classified.isEmpty()) {
            return "none";
        }

        StringBuilder summary = new StringBuilder();

        // Count tamable pets
        var pets = classified.get(AnimalClassification.TAMABLE_PET);
        if (pets != null && !pets.isEmpty()) {
            summary.append(pets.size()).append(" tamable animals");
        }

        // Count livestock
        var livestock = classified.get(AnimalClassification.BREEDABLE_LIVESTOCK);
        if (livestock != null && !livestock.isEmpty()) {
            if (summary.length() > 0) summary.append(", ");
            summary.append(livestock.size()).append(" livestock");
        }

        // Count mounts
        var mounts = classified.get(AnimalClassification.MOUNT);
        if (mounts != null && !mounts.isEmpty()) {
            if (summary.length() > 0) summary.append(", ");
            summary.append(mounts.size()).append(" mounts");
        }

        // Count hostiles
        var hostiles = classified.get(AnimalClassification.HOSTILE);
        if (hostiles != null && !hostiles.isEmpty()) {
            if (summary.length() > 0) summary.append(", ");
            summary.append(hostiles.size()).append(" hostile mobs");
        }

        return summary.toString();
    }

    private static String formatPosition(BlockPos pos) {
        return String.format("[%d, %d, %d]", pos.getX(), pos.getY(), pos.getZ());
    }
}

package com.minewright.skill;
import com.minewright.testutil.TestLogger;
import org.slf4j.Logger;

import com.minewright.action.Task;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Central registry for managing learned and built-in skills.
 *
 * <p><b>Purpose:</b></p>
 * <p>The SkillLibrary is the heart of the Voyager pattern implementation.
 * It stores all known skills, enables semantic search for applicable skills,
 * tracks success rates, and prevents duplicate skill creation.</p>
 *
 * <p><b>Key Features:</b></p>
 * <ul>
 *   <li><b>Thread-Safe Storage:</b> ConcurrentHashMap for concurrent access</li>
 *   <li><b>Semantic Search:</b> Find skills by description similarity</li>
 *   <li><b>Applicability Check:</b> Match skills to tasks automatically</li>
 *   <li><b>Success Tracking:</b> Record outcomes for learning</li>
 *   <li><b>Duplicate Prevention:</b> Avoid redundant skills</li>
 *   <li><b>Built-in Skills:</b> Common patterns pre-loaded</li>
 * </ul>
 *
 * <p><b>Semantic Search:</b></p>
 * <p>Uses word overlap and keyword matching to find relevant skills:</p>
 * <ul>
 *   <li>Exact match in description or name</li>
 *   <li>Partial word matching (e.g., "mine" matches "mining")</li>
 *   <li>Category-based filtering</li>
 *   <li>Success rate prioritization</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b></p>
 * <p>All operations are thread-safe for concurrent access from multiple agents.</p>
 *
 * @see Skill
 * @see ExecutableSkill
 * @see SkillGenerator
 * @since 1.0.0
 */
public class SkillLibrary {
    /**
     * Logger for this class.
     * Uses TestLogger to avoid triggering MineWrightMod initialization during tests.
     */
    private static final Logger LOGGER = TestLogger.getLogger(SkillLibrary.class);

    private final Map<String, Skill> skills;
    private final Map<String, String> skillCategories; // name -> category
    private final Set<String> skillSignatures; // For duplicate detection

    /**
     * Singleton instance for global access across all agents.
     */
    private static volatile SkillLibrary instance;

    private SkillLibrary() {
        this.skills = new ConcurrentHashMap<>();
        this.skillCategories = new ConcurrentHashMap<>();
        this.skillSignatures = ConcurrentHashMap.newKeySet();

        // Initialize built-in skills
        initializeBuiltInSkills();

        LOGGER.info("SkillLibrary initialized with {} built-in skills", skills.size());
    }

    /**
     * Gets the singleton SkillLibrary instance.
     * Uses double-checked locking for thread-safe lazy initialization.
     *
     * @return Global SkillLibrary instance
     */
    public static SkillLibrary getInstance() {
        if (instance == null) {
            synchronized (SkillLibrary.class) {
                if (instance == null) {
                    instance = new SkillLibrary();
                }
            }
        }
        return instance;
    }

    /**
     * Adds a new skill to the library.
     *
     * <p><b>Duplicate Detection:</b></p>
     * <p>Skills with the same name or code signature are rejected.</p>
     *
     * @param skill The skill to add
     * @return true if added, false if duplicate exists
     */
    public boolean addSkill(Skill skill) {
        if (skill == null) {
            LOGGER.warn("Attempted to add null skill to library");
            return false;
        }

        String name = skill.getName();

        // Check for duplicate name
        if (skills.containsKey(name)) {
            LOGGER.debug("Skill '{}' already exists in library", name);
            return false;
        }

        // Check for duplicate signature (based on description + required actions)
        String signature = generateSignature(skill);
        if (skillSignatures.contains(signature)) {
            LOGGER.debug("Skill with signature '{}' already exists", signature);
            return false;
        }

        // Add to library
        skills.put(name, skill);
        skillCategories.put(name, skill.getCategory());
        skillSignatures.add(signature);

        LOGGER.info("Added skill '{}' to library (category: {}, total: {})",
            name, skill.getCategory(), skills.size());

        return true;
    }

    /**
     * Gets a skill by name.
     *
     * @param name Skill name
     * @return Skill instance, or null if not found
     */
    public Skill getSkill(String name) {
        return skills.get(name);
    }

    /**
     * Finds all skills applicable to the given task.
     *
     * <p><b>Matching Strategy:</b></p>
     * <ol>
     *   <li>Check each skill's isApplicable() method</li>
     *   <li>Sort by success rate (highest first)</li>
     *   <li>Return top matches</li>
 * </ol>
     *
     * @param task The task to find skills for
     * @return List of applicable skills, sorted by success rate
     */
    public List<Skill> findApplicableSkills(Task task) {
        return skills.values().stream()
            .filter(skill -> skill.isApplicable(task))
            .sorted((a, b) -> Double.compare(b.getSuccessRate(), a.getSuccessRate()))
            .collect(Collectors.toList());
    }

    /**
     * Performs semantic search for skills matching a query.
     *
     * <p><b>Search Algorithm:</b></p>
     * <ol>
     *   <li>Tokenize query into lowercase words</li>
     *   <li>Score each skill by word overlap in name/description</li>
     *   <li>Apply category boost if specified</li>
     *   <li>Apply success rate multiplier</li>
     *   <li>Return top N results</li>
     * </ol>
     *
     * @param query Search query (e.g., "dig staircase for mining")
     * @return List of matching skills, sorted by relevance
     */
    public List<Skill> semanticSearch(String query) {
        if (query == null || query.isEmpty()) {
            return List.of();
        }

        String[] queryWords = query.toLowerCase().split("\\s+");
        Map<Skill, Double> scores = new HashMap<>();

        for (Skill skill : skills.values()) {
            double score = calculateRelevance(skill, queryWords);
            if (score > 0) {
                scores.put(skill, score);
            }
        }

        return scores.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(10) // Top 10 results
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }

    /**
     * Records the outcome of a skill execution.
     *
     * @param skillName Name of the skill that was executed
     * @param success true if execution was successful
     */
    public void recordOutcome(String skillName, boolean success) {
        Skill skill = skills.get(skillName);
        if (skill != null) {
            skill.recordSuccess(success);
            LOGGER.debug("Recorded outcome for skill '{}': {} (success rate: {:.2f}%)",
                skillName, success, skill.getSuccessRate() * 100);
        }
    }

    /**
     * Gets all skills in a specific category.
     *
     * @param category Category name (e.g., "mining", "building")
     * @return List of skills in the category
     */
    public List<Skill> getSkillsByCategory(String category) {
        return skills.entrySet().stream()
            .filter(e -> category.equals(e.getValue().getCategory()))
            .map(Map.Entry::getValue)
            .collect(Collectors.toList());
    }

    /**
     * Gets all skills sorted by success rate.
     *
     * @return List of all skills, sorted by success rate (highest first)
     */
    public List<Skill> getSkillsBySuccessRate() {
        return skills.values().stream()
            .sorted((a, b) -> Double.compare(b.getSuccessRate(), a.getSuccessRate()))
            .collect(Collectors.toList());
    }

    /**
     * Gets the total number of skills in the library.
     *
     * @return Skill count
     */
    public int getSkillCount() {
        return skills.size();
    }

    /**
     * Checks if a skill exists in the library.
     *
     * @param name Skill name
     * @return true if skill exists
     */
    public boolean hasSkill(String name) {
        return skills.containsKey(name);
    }

    /**
     * Removes a skill from the library.
     *
     * @param name Skill name to remove
     * @return true if skill was removed, false if not found
     */
    public boolean removeSkill(String name) {
        Skill removed = skills.remove(name);
        if (removed != null) {
            skillCategories.remove(name);
            skillSignatures.remove(generateSignature(removed));
            LOGGER.info("Removed skill '{}' from library", name);
            return true;
        }
        return false;
    }

    /**
     * Gets statistics about the skill library.
     *
     * @return Statistics map containing counts by category
     */
    public Map<String, Integer> getStatistics() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("total", skills.size());

        // Count by category
        for (String category : skillCategories.values()) {
            stats.put(category, stats.getOrDefault(category, 0) + 1);
        }

        // Count executions
        int totalExecutions = skills.values().stream()
            .mapToInt(Skill::getExecutionCount)
            .sum();
        stats.put("totalExecutions", totalExecutions);

        return stats;
    }

    /**
     * Initializes the library with built-in skills for common patterns.
     */
    private void initializeBuiltInSkills() {
        // Mining skills
        addSkill(createDigStaircaseSkill());
        addSkill(createStripMineSkill());
        addSkill(createBranchMineSkill());

        // Building skills
        addSkill(createBuildShelterSkill());
        addSkill(createBuildPlatformSkill());

        // Farming skills
        addSkill(createFarmWheatSkill());
        addSkill(createFarmTreeSkill());

        // Utility skills
        addSkill(createOrganizeInventorySkill());
        addSkill(createCollectDropsSkill());
    }

    /**
     * Calculates relevance score for a skill against query words.
     */
    private double calculateRelevance(Skill skill, String[] queryWords) {
        String name = skill.getName().toLowerCase();
        String description = skill.getDescription().toLowerCase();

        int matches = 0;
        int exactMatches = 0;

        for (String word : queryWords) {
            // Exact phrase match
            if (name.contains(word) || description.contains(word)) {
                matches++;
                if (name.equals(word) || description.startsWith(word)) {
                    exactMatches++;
                }
            }
        }

        if (matches == 0) {
            return 0;
        }

        // Base score from word matches
        double score = (double) matches / queryWords.length;

        // Boost for exact matches
        score += exactMatches * 0.2;

        // Apply success rate multiplier (successful skills are more relevant)
        score *= (0.5 + skill.getSuccessRate());

        return Math.min(score, 1.0);
    }

    /**
     * Generates a unique signature for duplicate detection.
     */
    private String generateSignature(Skill skill) {
        return skill.getCategory() + ":" +
               skill.getDescription().hashCode() + ":" +
               skill.getRequiredActions().hashCode();
    }

    // ==================== BUILT-IN SKILLS ====================

    private Skill createDigStaircaseSkill() {
        return ExecutableSkill.builder("digStaircase")
            .description("Dig a staircase downwards for safe mining")
            .category("mining")
            .codeTemplate("""
                // Dig staircase of specified depth
                var depth = {{depth}};
                var direction = "{{direction:quote}}"; // 'north', 'south', 'east', 'west'
                var dx = 0, dz = 0;
                if (direction === 'north') dz = -1;
                else if (direction === 'south') dz = 1;
                else if (direction === 'east') dx = 1;
                else if (direction === 'west') dx = -1;

                for (var i = 0; i < depth; i++) {
                    // Mine block at current level
                    var posX = startX + (dx * i);
                    var posZ = startZ + (dz * i);
                    var posY = startY - i;

                    steve.mineBlock(posX, posY, posZ);

                    // Place torch every 5 blocks
                    if (i % 5 === 0 && i > 0) {
                        steve.placeBlock('torch', posX, posY, posZ);
                    }
                }
                """)
            .requiredActions("mine", "place")
            .requiredItems("torch")
            .estimatedTicks(200)
            .applicabilityPattern("dig.*staircase|stair.*down|mining.*stair")
            .build();
    }

    private Skill createStripMineSkill() {
        return ExecutableSkill.builder("stripMine")
            .description("Execute a strip mining pattern at Y level -58")
            .category("mining")
            .codeTemplate("""
                // Strip mine in a straight line
                var length = {{length}};
                var direction = "{{direction:quote}}";

                var dx = 0, dz = 0;
                if (direction === 'north') dz = -1;
                else if (direction === 'south') dz = 1;
                else if (direction === 'east') dx = 1;
                else if (direction === 'west') dx = -1;

                for (var i = 0; i < length; i++) {
                    var x = startX + (dx * i);
                    var z = startZ + (dz * i);

                    // Mine at Y=-58 (optimal diamond layer)
                    steve.mineBlock(x, -58, z);

                    // Place torch for light
                    if (i % 7 === 0) {
                        steve.placeBlock('torch', x, -57, z);
                    }
                }
                """)
            .requiredActions("mine", "place")
            .requiredItems("torch", "pickaxe")
            .estimatedTicks(400)
            .applicabilityPattern("strip.*mine|mining.*line|mine.*tunnel")
            .build();
    }

    private Skill createBranchMineSkill() {
        return ExecutableSkill.builder("branchMine")
            .description("Create branching tunnels from a main mining shaft")
            .category("mining")
            .codeTemplate("""
                // Branch mining pattern
                var branches = {{branches}};
                var branchLength = {{branchLength}};
                var spacing = 3; // Blocks between branches

                for (var b = 0; b < branches; b++) {
                    var offset = b * spacing;

                    // Dig branch in one direction
                    for (var i = 0; i < branchLength; i++) {
                        steve.mineBlock(startX + i, startY, startZ + offset);
                        steve.mineBlock(startX + i, startY, startZ + offset + 1);
                    }

                    // Torch every 10 blocks
                    if (branchLength > 10) {
                        steve.placeBlock('torch', startX + 10, startY, startZ + offset);
                    }
                }
                """)
            .requiredActions("mine", "place")
            .requiredItems("torch", "pickaxe")
            .estimatedTicks(600)
            .applicabilityPattern("branch.*mine|tunnel.*branch")
            .build();
    }

    private Skill createBuildShelterSkill() {
        return ExecutableSkill.builder("buildShelter")
            .description("Build a basic shelter for protection")
            .category("building")
            .codeTemplate("""
                // Build simple 5x5x3 shelter
                var width = {{width}};
                var height = {{height}};
                var depth = {{depth}};
                var blockType = "{{block:quote}}";

                // Build floor
                for (var x = 0; x < width; x++) {
                    for (var z = 0; z < depth; z++) {
                        steve.placeBlock(blockType, startX + x, startY, startZ + z);
                    }
                }

                // Build walls
                for (var y = 1; y < height; y++) {
                    for (var x = 0; x < width; x++) {
                        steve.placeBlock(blockType, startX + x, startY + y, startZ);
                        steve.placeBlock(blockType, startX + x, startY + y, startZ + depth - 1);
                    }
                    for (var z = 0; z < depth; z++) {
                        steve.placeBlock(blockType, startX, startY + y, startZ + z);
                        steve.placeBlock(blockType, startX + width - 1, startY + y, startZ + z);
                    }
                }

                // Build roof
                for (var x = 0; x < width; x++) {
                    for (var z = 0; z < depth; z++) {
                        steve.placeBlock(blockType, startX + x, startY + height, startZ + z);
                    }
                }

                // Add door opening
                steve.mineBlock(startX + Math.floor(width/2), startY + 1, startZ);
                """)
            .requiredActions("place", "mine")
            .estimatedTicks(500)
            .applicabilityPattern("build.*shelter|simple.*house|protection")
            .build();
    }

    private Skill createBuildPlatformSkill() {
        return ExecutableSkill.builder("buildPlatform")
            .description("Build a flat platform for building")
            .category("building")
            .codeTemplate("""
                var size = {{size}};
                var blockType = "{{block:quote}}";

                for (var x = 0; x < size; x++) {
                    for (var z = 0; z < size; z++) {
                        steve.placeBlock(blockType, startX + x, startY, startZ + z);
                    }
                }
                """)
            .requiredActions("place")
            .estimatedTicks(300)
            .applicabilityPattern("build.*platform|flat.*area|clearing")
            .build();
    }

    private Skill createFarmWheatSkill() {
        return ExecutableSkill.builder("farmWheat")
            .description("Automated wheat farming with tilling and planting")
            .category("farming")
            .codeTemplate("""
                var size = {{size}};
                var rows = {{rows}};

                // Till soil and plant seeds
                for (var row = 0; row < rows; row++) {
                    for (var i = 0; i < size; i++) {
                        var x = startX + i;
                        var z = startZ + (row * 2);

                        // Till soil
                        steve.useItemOnBlock('hoe', x, startY, z);

                        // Plant seeds
                        steve.placeBlock('wheat_seeds', x, startY + 1, z);
                    }
                }

                // Add water channel between rows
                for (var i = 0; i < size; i++) {
                    steve.placeBlock('water', startX + i, startY, startZ + 1);
                }
                """)
            .requiredActions("place")
            .requiredItems("hoe", "wheat_seeds", "water_bucket")
            .estimatedTicks(400)
            .applicabilityPattern("farm.*wheat|plant.*wheat|wheat.*farm")
            .build();
    }

    private Skill createFarmTreeSkill() {
        return ExecutableSkill.builder("farmTree")
            .description("Plant saplings in a grid pattern")
            .category("farming")
            .codeTemplate("""
                var spacing = {{spacing}}; // Space between trees
                var count = {{count}};
                var saplingType = "{{sapling:quote}}";

                for (var i = 0; i < count; i++) {
                    var x = startX + (i % spacing) * spacing;
                    var z = startZ + Math.floor(i / spacing) * spacing;

                    // Clear grass
                    steve.mineBlock(x, startY, z);

                    // Plant sapling
                    steve.placeBlock(saplingType, x, startY + 1, z);
                }
                """)
            .requiredActions("mine", "place")
            .requiredItems("oak_sapling", "birch_sapling", "spruce_sapling")
            .estimatedTicks(200)
            .applicabilityPattern("farm.*tree|plant.*tree|tree.*farm")
            .build();
    }

    private Skill createOrganizeInventorySkill() {
        return ExecutableSkill.builder("organizeInventory")
            .description("Organize inventory items by type")
            .category("utility")
            .codeTemplate("""
                // Move items to organized slots
                var categories = {
                    'tools': ['pickaxe', 'axe', 'shovel', 'hoe', 'sword'],
                    'blocks': ['dirt', 'stone', 'wood', 'cobblestone'],
                    'food': ['bread', 'meat', 'apple', 'carrot'],
                    'resources': ['coal', 'iron', 'gold', 'diamond']
                };

                steve.organizeInventory(categories);
                """)
            .requiredActions("organize")
            .estimatedTicks(50)
            .applicabilityPattern("organize.*inventory|sort.*items")
            .build();
    }

    private Skill createCollectDropsSkill() {
        return ExecutableSkill.builder("collectDrops")
            .description("Collect all dropped items in an area")
            .category("utility")
            .codeTemplate("""
                var radius = {{radius}};

                // Spiral search pattern
                for (var r = 0; r < radius; r++) {
                    for (var angle = 0; angle < 360; angle += 45) {
                        var x = startX + Math.floor(r * Math.cos(angle));
                        var z = startZ + Math.floor(r * Math.sin(angle));

                        steve.pathfindTo(x, startY, z);
                        steve.collectNearbyItems(5);
                    }
                }
                """)
            .requiredActions("pathfind", "collect")
            .estimatedTicks(300)
            .applicabilityPattern("collect.*drop|pick.*item|gather.*drop")
            .build();
    }
}

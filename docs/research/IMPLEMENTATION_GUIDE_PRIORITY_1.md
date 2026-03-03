# Priority 1 Implementation Guide: Skill Composition System

**Date:** March 2, 2026
**Priority:** 1 (HIGH IMPACT)
**Effort:** 2-3 days
**Dependencies:** None

---

## Overview

This guide implements the **Skill Composition System**, identified as Priority 1 in the research analysis. This feature enables automatic building of complex skills from simpler components, matching the Voyager pattern.

**Research Backing:** Voyager demonstrates 15x faster task completion through skill composition.

---

## Implementation Plan

### Phase 1: Core Classes (Day 1)

**Files to Create:**

1. `src/main/java/com/minewright/skill/SkillComposer.java`
2. `src/main/java/com/minewright/skill/CompositeSkill.java`
3. `src/main/java/com/minewright/skill/SkillComposition.java`
4. `src/main/java/com/minewright/skill/CompositionType.java` (enum)

**Files to Modify:**

1. `src/main/java/com/minewright/skill/Skill.java` (add dependency methods)

---

## Phase 1: Core Classes

### 1.1 CompositionType Enum

**File:** `src/main/java/com/minewright/skill/CompositionType.java`

```java
package com.minewright.skill;

/**
 * Types of skill composition.
 */
public enum CompositionType {
    /**
     * Execute component skills in sequence (one after another).
     * Example: chopWood -> craftPlanks -> craftSticks
     */
    SEQUENCE,

    /**
     * Execute component skills in parallel (simultaneously).
     * Example: agentA mines coal while agentB mines iron
     */
    PARALLEL,

    /**
     * Execute component skills conditionally.
     * Example: if hasAxe then chopWood else craftAxe
     */
    CONDITIONAL,

    /**
     * Execute component skills iteratively.
     * Example: repeat mineBlock 10 times
     */
    LOOP
}
```

### 1.2 SkillComposition Record

**File:** `src/main/java/com/minewright/skill/SkillComposition.java`

```java
package com.minewright.skill;

import java.util.List;

/**
 * Represents a composition of skills discovered from execution sequences.
 *
 * @param name The name of the composite skill
 * @param componentSkills The skills that make up this composition
 * @param type The type of composition (sequence, parallel, etc.)
 * @param frequency How often this pattern occurs (for prioritization)
 * @param successRate The success rate of this composition
 */
public record SkillComposition(
    String name,
    List<Skill> componentSkills,
    CompositionType type,
    int frequency,
    double successRate
) {
    /**
     * Returns a display name for this composition.
     */
    public String getDisplayName() {
        return String.format("%s (%s, freq=%d, success=%.2f)",
            name, type, frequency, successRate);
    }

    /**
     * Checks if this composition is high-quality.
     * High-quality compositions are frequent and successful.
     */
    public boolean isHighQuality() {
        return frequency >= 3 && successRate >= 0.7;
    }
}
```

### 1.3 SkillComposer Class

**File:** `src/main/java/com/minewright/skill/SkillComposer.java`

```java
package com.minewright.skill;

import com.minewright.skill.pattern.PatternExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Composes complex skills from simpler component skills.
 *
 * <p><b>Voyager Pattern:</b></p>
 * <p>Voyager demonstrates that skill composition is key to scaling agent capabilities.
 * Complex behaviors are built by combining proven primitives.</p>
 *
 * <p><b>Composition Types:</b></p>
 * <ul>
 *   <li><b>Sequence:</b> Execute skills one after another (e.g., mine -> smelt -> craft)</li>
 *   <li><b>Parallel:</b> Execute skills simultaneously (e.g., multi-agent mining)</li>
 *   <li><b>Conditional:</b> Execute based on conditions (e.g., if has tool)</li>
 *   <li><b>Loop:</b> Repeat execution (e.g., mine 10 blocks)</li>
 * </ul>
 *
 * <p><b>Auto-Discovery:</b></p>
 * <p>The composer analyzes execution sequences to find recurring patterns
 * that suggest composable skills.</p>
 *
 * @since 2.3.0
 */
public class SkillComposer {
    private static final Logger LOGGER = LoggerFactory.getLogger(SkillComposer.class);

    private final SkillLibrary skillLibrary;
    private final PatternExtractor patternExtractor;

    // Composition thresholds
    private static final int MIN_PATTERN_FREQUENCY = 3;
    private static final double MIN_SUCCESS_RATE = 0.6;

    public SkillComposer(SkillLibrary skillLibrary, PatternExtractor patternExtractor) {
        this.skillLibrary = skillLibrary;
        this.patternExtractor = patternExtractor;
        LOGGER.info("SkillComposer initialized");
    }

    /**
     * Creates a composite skill from component skills.
     *
     * @param name The name for the composite skill
     * @param components The component skills
     * @param type The composition type
     * @return A new composite skill
     */
    public CompositeSkill compose(String name, List<Skill> components, CompositionType type) {
        if (components.isEmpty()) {
            throw new IllegalArgumentException("Cannot compose empty skill list");
        }

        // Validate that all components are in the library
        for (Skill component : components) {
            if (!skillLibrary.hasSkill(component.getName())) {
                throw new IllegalArgumentException(
                    "Component skill not in library: " + component.getName());
            }
        }

        // Generate description from components
        String description = generateDescription(name, components, type);

        LOGGER.info("Composed skill '{}' from {} components (type: {})",
            name, components.size(), type);

        return new CompositeSkill(name, description, components, type);
    }

    /**
     * Discovers composable skill patterns from execution sequences.
     *
     * @param sequences The execution sequences to analyze
     * @return List of discovered compositions, sorted by quality
     */
    public List<SkillComposition> discoverCompositions(List<ExecutionSequence> sequences) {
        LOGGER.info("Discovering compositions from {} sequences", sequences.size());

        // Use pattern extractor to find recurring patterns
        List<TaskPattern> patterns = patternExtractor.extractPatterns(sequences);

        // Convert patterns to skill compositions
        List<SkillComposition> compositions = new ArrayList<>();

        for (TaskPattern pattern : patterns) {
            if (pattern.getFrequency() < MIN_PATTERN_FREQUENCY) {
                continue; // Skip rare patterns
            }

            if (pattern.getSuccessRate() < MIN_SUCCESS_RATE) {
                continue; // Skip unsuccessful patterns
            }

            // Find component skills for this pattern
            List<Skill> componentSkills = findComponentSkills(pattern);

            if (componentSkills.size() < 2) {
                continue; // Need at least 2 components to compose
            }

            // Determine composition type
            CompositionType type = inferCompositionType(pattern);

            // Generate name
            String name = generateCompositionName(pattern);

            SkillComposition composition = new SkillComposition(
                name,
                componentSkills,
                type,
                pattern.getFrequency(),
                pattern.getSuccessRate()
            );

            compositions.add(composition);
        }

        // Sort by quality (frequency * success rate)
        compositions.sort((a, b) -> {
            double scoreA = a.frequency() * a.successRate();
            double scoreB = b.frequency() * b.successRate();
            return Double.compare(scoreB, scoreA);
        });

        LOGGER.info("Discovered {} high-quality compositions", compositions.size());
        return compositions;
    }

    /**
     * Automatically registers discovered compositions with the skill library.
     *
     * @param compositions The compositions to register
     * @return Number of compositions registered
     */
    public int registerCompositions(List<SkillComposition> compositions) {
        int registered = 0;

        for (SkillComposition composition : compositions) {
            if (!composition.isHighQuality()) {
                continue;
            }

            CompositeSkill skill = compose(
                composition.name(),
                composition.componentSkills(),
                composition.type()
            );

            skillLibrary.registerSkill(skill);
            registered++;

            LOGGER.info("Registered composite skill: {}", composition.getDisplayName());
        }

        return registered;
    }

    // ========== Helper Methods ==========

    /**
     * Generates a description for a composite skill.
     */
    private String generateDescription(
        String name,
        List<Skill> components,
        CompositionType type
    ) {
        String componentNames = components.stream()
            .map(Skill::getName)
            .collect(Collectors.joining(", "));

        return String.format("Composite skill (%s): %s", type, componentNames);
    }

    /**
     * Finds component skills that match a pattern.
     */
    private List<Skill> findComponentSkills(TaskPattern pattern) {
        List<Skill> components = new ArrayList<>();

        for (String actionName : pattern.getActionSequence()) {
            // Find skill that handles this action
            Optional<Skill> skill = skillLibrary.findBestSkill(actionName);
            if (skill.isPresent()) {
                components.add(skill.get());
            }
        }

        return components;
    }

    /**
     * Infers the composition type from a pattern.
     */
    private CompositionType inferCompositionType(TaskPattern pattern) {
        // Check for loop pattern (repeated actions)
        List<String> actions = pattern.getActionSequence();
        if (hasRepeatedActions(actions)) {
            return CompositionType.LOOP;
        }

        // Check for parallel pattern (independent actions)
        if (hasIndependentActions(actions)) {
            return CompositionType.PARALLEL;
        }

        // Default to sequence
        return CompositionType.SEQUENCE;
    }

    /**
     * Generates a name for a composition from its pattern.
     */
    private String generateCompositionName(TaskPattern pattern) {
        List<String> actions = pattern.getActionSequence();

        if (actions.size() == 2) {
            return String.format("%sThen%s",
                capitalize(actions.get(0)),
                capitalize(actions.get(1))
            );
        }

        // Use first and last action for longer sequences
        return String.format("%s...%s",
            capitalize(actions.get(0)),
            capitalize(actions.get(actions.size() - 1))
        );
    }

    /**
     * Checks if actions repeat (suggests loop composition).
     */
    private boolean hasRepeatedActions(List<String> actions) {
        Set<String> unique = new HashSet<>(actions);
        return unique.size() < actions.size() / 2;
    }

    /**
     * Checks if actions are independent (suggests parallel composition).
     */
    private boolean hasIndependentActions(List<String> actions) {
        // Simple heuristic: actions don't share prerequisites
        // This could be enhanced with more sophisticated analysis
        return false;
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }
}
```

### 1.4 CompositeSkill Class

**File:** `src/main/java/com/minewright/skill/CompositeSkill.java`

```java
package com.minewright.skill;

import com.minewright.action.Task;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A skill composed of multiple component skills.
 *
 * <p><b>Voyager Pattern:</b></p>
 * <p>Voyager demonstrates that complex skills should be built from
 * simpler, proven primitives. This class implements that pattern.</p>
 *
 * <p><b>Composition:</b></p>
 * <ul>
 *   <li><b>Sequence:</b> Execute components one after another</li>
 *   <li><b>Parallel:</b> Execute components simultaneously</li>
 *   <li><b>Conditional:</b> Execute based on world state</li>
 *   <li><b>Loop:</b> Repeat execution</li>
 * </ul>
 *
 * @since 2.3.0
 */
public class CompositeSkill implements Skill {

    private final String name;
    private final String description;
    private final List<Skill> components;
    private final CompositionType type;

    // Execution tracking
    private int executionCount = 0;
    private int successCount = 0;

    public CompositeSkill(
        String name,
        String description,
        List<Skill> components,
        CompositionType type
    ) {
        this.name = name;
        this.description = description;
        this.components = List.copyOf(components);
        this.type = type;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public List<String> getRequiredActions() {
        return components.stream()
            .flatMap(skill -> skill.getRequiredActions().stream())
            .distinct()
            .collect(Collectors.toList());
    }

    @Override
    public String generateCode(Map<String, Object> context) {
        StringBuilder code = new StringBuilder();
        code.append("// Composite skill: ").append(name).append("\n");
        code.append("// Type: ").append(type).append("\n");
        code.append("// Components: ").append(getComponentNames()).append("\n\n");

        switch (type) {
            case SEQUENCE:
                code.append(generateSequenceCode(context));
                break;

            case PARALLEL:
                code.append(generateParallelCode(context));
                break;

            case CONDITIONAL:
                code.append(generateConditionalCode(context));
                break;

            case LOOP:
                code.append(generateLoopCode(context));
                break;
        }

        return code.toString();
    }

    @Override
    public boolean isApplicable(Task task) {
        // Composite skill is applicable if any component is applicable
        return components.stream().anyMatch(skill -> skill.isApplicable(task));
    }

    @Override
    public double getSuccessRate() {
        if (executionCount == 0) return 1.0; // No data = assume success
        return (double) successCount / executionCount;
    }

    @Override
    public void recordSuccess(boolean success) {
        executionCount++;

        if (success) {
            successCount++;

            // Also record success for all components
            components.forEach(skill -> skill.recordSuccess(true));
        } else {
            // Record failure for components that might have caused it
            components.forEach(skill -> skill.recordSuccess(false));
        }
    }

    @Override
    public int getExecutionCount() {
        return executionCount;
    }

    @Override
    public List<String> getDependencies() {
        return components.stream()
            .flatMap(skill -> skill.getDependencies().stream())
            .distinct()
            .collect(Collectors.toList());
    }

    @Override
    public String getCategory() {
        // Derive category from first component
        return components.isEmpty() ? "composite" : components.get(0).getCategory();
    }

    @Override
    public int getEstimatedTicks() {
        // Sum of component estimates (for sequence) or max (for parallel)
        return switch (type) {
            case SEQUENCE, LOOP -> components.stream()
                .mapToInt(Skill::getEstimatedTicks)
                .sum();
            case PARALLEL, CONDITIONAL -> components.stream()
                .mapToInt(Skill::getEstimatedTicks)
                .max()
                .orElse(100);
        };
    }

    @Override
    public List<String> getRequiredItems() {
        return components.stream()
            .flatMap(skill -> skill.getRequiredItems().stream())
            .distinct()
            .collect(Collectors.toList());
    }

    // ========== Helper Methods ==========

    /**
     * Generates code for sequential execution.
     */
    private String generateSequenceCode(Map<String, Object> context) {
        StringBuilder code = new StringBuilder();

        for (int i = 0; i < components.size(); i++) {
            Skill component = components.get(i);

            code.append("// Step ").append(i + 1).append(": ")
                .append(component.getName()).append("\n");

            code.append(component.generateCode(context));

            if (i < components.size() - 1) {
                code.append("\n// Wait for completion before next step\n");
                code.append("await waitForCompletion();\n\n");
            }
        }

        return code.toString();
    }

    /**
     * Generates code for parallel execution.
     */
    private String generateParallelCode(Map<String, Object> context) {
        StringBuilder code = new StringBuilder();

        code.append("// Execute components in parallel\n");
        code.append("const results = await Promise.all([\n");

        for (Skill component : components) {
            code.append("  (async () => {\n");
            code.append("    ").append(component.generateCode(context).indent(4));
            code.append("  })(),\n");
        }

        code.append("]);\n");
        code.append("// Check all results for success\n");

        return code.toString();
    }

    /**
     * Generates code for conditional execution.
     */
    private String generateConditionalCode(Map<String, Object> context) {
        StringBuilder code = new StringBuilder();

        if (components.size() < 2) {
            return generateSequenceCode(context);
        }

        code.append("// Conditional execution\n");
        code.append("if (checkCondition(context)) {\n");
        code.append("  // If branch: ").append(components.get(0).getName()).append("\n");
        code.append(components.get(0).generateCode(context).indent(2));

        if (components.size() > 1) {
            code.append("} else {\n");
            code.append("  // Else branch: ").append(components.get(1).getName()).append("\n");
            code.append(components.get(1).generateCode(context).indent(2));
        }

        code.append("}\n");

        return code.toString();
    }

    /**
     * Generates code for loop execution.
     */
    private String generateLoopCode(Map<String, Object> context) {
        StringBuilder code = new StringBuilder();

        // Get iteration count from context, default to 1
        int iterations = (Integer) context.getOrDefault("iterations", 1);

        code.append("// Loop execution: ").append(iterations).append(" iterations\n");
        code.append("for (let i = 0; i < ").append(iterations).append("; i++) {\n");

        for (Skill component : components) {
            code.append("  // ").append(component.getName()).append("\n");
            code.append(component.generateCode(context).indent(2));
        }

        code.append("}\n");

        return code.toString();
    }

    private String getComponentNames() {
        return components.stream()
            .map(Skill::getName)
            .collect(Collectors.joining(", "));
    }

    @Override
    public String toString() {
        return String.format("CompositeSkill{name='%s', type=%s, components=%d}",
            name, type, components.size());
    }
}
```

### 1.5 Modify Skill Interface

**File:** `src/main/java/com/minewright/skill/Skill.java`

**Add these methods to the existing interface:**

```java
/**
 * Returns the skills this skill depends on.
 * Used for automatic skill composition and validation.
 *
 * @return List of dependency skill names
 * @since 2.3.0
 */
default List<String> getDependencies() {
    return List.of();
}

/**
 * Checks if this skill has unmet dependencies in the library.
 *
 * @param library The skill library to check
 * @return true if there are unmet dependencies
 * @since 2.3.0
 */
default boolean hasUnmetDependencies(SkillLibrary library) {
    return getDependencies().stream()
        .anyMatch(dep -> !library.hasSkill(dep));
}
```

---

## Phase 2: Integration (Day 2)

### 2.1 Update SkillLearningLoop

**File:** `src/main/java/com/minewright/skill/SkillLearningLoop.java`

**Add skill composition to the learning cycle:**

```java
// In performLearningCycle() method, add after skill generation:

// Discover and register composite skills
SkillComposer composer = new SkillComposer(
    SkillLibrary.getInstance(),
    PatternExtractor.getInstance()
);

List<SkillComposition> compositions = composer.discoverCompositions(sequences);
int compositesRegistered = composer.registerCompositions(compositions);

if (compositesRegistered > 0) {
    LOGGER.info("Registered {} composite skills from patterns",
        compositesRegistered);
}
```

### 2.2 Update SkillLibrary

**File:** `src/main/java/com/minewright/skill/SkillLibrary.java`

**Add methods for dependency checking:**

```java
/**
 * Checks if a skill with the given name exists in the library.
 *
 * @param name The skill name to check
 * @return true if the skill exists
 * @since 2.3.0
 */
public boolean hasSkill(String name) {
    return skills.containsKey(name);
}

/**
 * Validates that all dependencies for a skill are met.
 *
 * @param skill The skill to validate
 * @return true if all dependencies are met
 * @since 2.3.0
 */
public boolean validateDependencies(Skill skill) {
    return !skill.hasUnmetDependencies(this);
}

/**
 * Returns all skills that have no unmet dependencies.
 * Useful for finding which skills can be executed.
 *
 * @return List of executable skills
 * @since 2.3.0
 */
public List<Skill> getExecutableSkills() {
    return skills.values().stream()
        .filter(skill -> !skill.hasUnmetDependencies(this))
        .collect(Collectors.toList());
}
```

---

## Phase 3: Testing (Day 2-3)

### 3.1 Create Unit Tests

**File:** `src/test/java/com/minewright/skill/SkillComposerTest.java`

```java
package com.minewright.skill;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for SkillComposer.
 */
class SkillComposerTest {

    @Test
    void testComposeSequenceSkill() {
        SkillLibrary library = mock(SkillLibrary.class);
        PatternExtractor extractor = mock(PatternExtractor.class);
        SkillComposer composer = new SkillComposer(library, extractor);

        Skill skill1 = mock(Skill.class);
        when(skill1.getName()).thenReturn("mineBlock");

        Skill skill2 = mock(Skill.class);
        when(skill2.getName()).thenReturn("craftItem");

        when(library.hasSkill(anyString())).thenReturn(true);

        CompositeSkill composite = composer.compose(
            "mineThenCraft",
            List.of(skill1, skill2),
            CompositionType.SEQUENCE
        );

        assertEquals("mineThenCraft", composite.getName());
        assertEquals(CompositionType.SEQUENCE, composite.type);
        assertEquals(2, composite.components.size());
    }

    @Test
    void testComposeWithMissingDependency() {
        SkillLibrary library = mock(SkillLibrary.class);
        PatternExtractor extractor = mock(PatternExtractor.class);
        SkillComposer composer = new SkillComposer(library, extractor);

        Skill skill1 = mock(Skill.class);
        when(skill1.getName()).thenReturn("mineBlock");

        when(library.hasSkill("mineBlock")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () ->
            composer.compose(
                "test",
                List.of(skill1),
                CompositionType.SEQUENCE
            )
        );
    }

    @Test
    void testDiscoverCompositions() {
        // Test pattern discovery from execution sequences
        // Implementation depends on your test data
    }
}
```

**File:** `src/test/java/com/minewright/skill/CompositeSkillTest.java`

```java
package com.minewright.skill;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Map;

/**
 * Tests for CompositeSkill.
 */
class CompositeSkillTest {

    @Test
    void testSequenceComposition() {
        Skill skill1 = mock(Skill.class);
        when(skill1.generateCode(any())).thenReturn("// Skill 1 code");
        when(skill1.getRequiredActions()).thenReturn(List.of("action1"));
        when(skill1.getEstimatedTicks()).thenReturn(50);

        Skill skill2 = mock(Skill.class);
        when(skill2.generateCode(any())).thenReturn("// Skill 2 code");
        when(skill2.getRequiredActions()).thenReturn(List.of("action2"));
        when(skill2.getEstimatedTicks()).thenReturn(100);

        CompositeSkill composite = new CompositeSkill(
            "testComposite",
            "Test composite skill",
            List.of(skill1, skill2),
            CompositionType.SEQUENCE
        );

        // Test properties
        assertEquals("testComposite", composite.getName());
        assertEquals(CompositionType.SEQUENCE, composite.type);
        assertEquals(2, composite.components.size());

        // Test code generation
        String code = composite.generateCode(Map.of());
        assertTrue(code.contains("// Skill 1 code"));
        assertTrue(code.contains("// Skill 2 code"));

        // Test execution count (sum for sequence)
        assertEquals(150, composite.getEstimatedTicks());
    }

    @Test
    void testSuccessRateTracking() {
        Skill skill1 = mock(Skill.class);
        Skill skill2 = mock(Skill.class);

        CompositeSkill composite = new CompositeSkill(
            "test",
            "description",
            List.of(skill1, skill2),
            CompositionType.SEQUENCE
        );

        // Initially 100% (no data)
        assertEquals(1.0, composite.getSuccessRate());

        // Record success
        composite.recordSuccess(true);
        assertEquals(1.0, composite.getSuccessRate());
        assertEquals(1, composite.getExecutionCount());

        // Record failure
        composite.recordSuccess(false);
        assertEquals(0.5, composite.getSuccessRate());
        assertEquals(2, composite.getExecutionCount());

        // Verify components were also updated
        verify(skill1, times(2)).recordSuccess(anyBoolean());
        verify(skill2, times(2)).recordSuccess(anyBoolean());
    }

    @Test
    void testDependencyAggregation() {
        Skill skill1 = mock(Skill.class);
        when(skill1.getDependencies()).thenReturn(List.of("dep1", "dep2"));

        Skill skill2 = mock(Skill.class);
        when(skill2.getDependencies()).thenReturn(List.of("dep2", "dep3"));

        CompositeSkill composite = new CompositeSkill(
            "test",
            "description",
            List.of(skill1, skill2),
            CompositionType.SEQUENCE
        );

        // Dependencies should be unique and combined
        var deps = composite.getDependencies();
        assertTrue(deps.contains("dep1"));
        assertTrue(deps.contains("dep2"));
        assertTrue(deps.contains("dep3"));
        assertEquals(3, deps.size());
    }
}
```

---

## Phase 4: Documentation (Day 3)

### 4.1 Update Documentation

**File:** `docs/implementation/SKILL_COMPOSITION.md`

```markdown
# Skill Composition System

## Overview

The skill composition system enables automatic building of complex skills from simpler components, following the Voyager pattern.

## Usage

### Manual Composition

```java
// Get component skills
Skill mineSkill = skillLibrary.getSkill("mineOre");
Skill smeltSkill = skillLibrary.getSkill("smeltOre");

// Compose them
SkillComposer composer = new SkillComposer(skillLibrary, patternExtractor);
CompositeSkill produceIngot = composer.compose(
    "produceIronIngot",
    List.of(mineSkill, smeltSkill),
    CompositionType.SEQUENCE
);

// Register the composite skill
skillLibrary.registerSkill(produceIngot);
```

### Auto-Discovery

```java
// Discover compositions from execution sequences
List<SkillComposition> compositions = composer.discoverCompositions(sequences);

// Register high-quality compositions automatically
int registered = composer.registerCompositions(compositions);

System.out.println("Registered " + registered + " composite skills");
```

## Composition Types

- **SEQUENCE:** Execute skills one after another
- **PARALLEL:** Execute skills simultaneously (multi-agent)
- **CONDITIONAL:** Execute based on world state
- **LOOP:** Repeat execution N times

## Dependencies

Composite skills aggregate dependencies from all components:

```java
// Check if skill can be executed
if (!skill.hasUnmetDependencies(skillLibrary)) {
    // All dependencies met, safe to execute
}
```

## Best Practices

1. **Start Small:** Compose 2-3 skills before attempting complex chains
2. **Test Individually:** Verify components work before composing
3. **Monitor Success Rates:** Low success rates indicate poor composition
4. **Use Auto-Discovery:** Let the system find patterns automatically
```

---

## Summary

**Files Created:** 5
**Files Modified:** 3
**Tests Added:** 2
**Effort:** 2-3 days
**Impact:** HIGH - enables complex skill building from primitives

**Next Steps:**
1. Implement core classes (Day 1)
2. Integrate with existing systems (Day 2)
3. Write tests (Day 2)
4. Update documentation (Day 3)
5. Move to Priority 2: Skill Validation

---

**Document Version:** 1.0
**Last Updated:** March 2, 2026
**Generated By:** Claude Code Orchestrator

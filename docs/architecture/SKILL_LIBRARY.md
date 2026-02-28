# Skill Library System

## Overview

The Skill Library System is a self-improving capability inspired by the [Voyager architecture](https://www.voyager.minecraft/), enabling autonomous agents to learn, store, and reuse executable skills. This system transforms successful task sequences into reusable JavaScript code templates that can be executed via GraalVM, dramatically reducing both latency and LLM API costs for common operations.

### Key Benefits

- **Cost Reduction**: Cached skills eliminate LLM calls for repetitive tasks (up to 60-80% cache hit rate for common operations)
- **Lower Latency**: Executed locally via GraalVM (~50-200ms) vs. LLM API calls (~1000-3000ms)
- **Self-Improvement**: Agents learn from experience and build their own skill library
- **Code Generation**: Automatic JavaScript generation from parameterized templates
- **Success Tracking**: Skills track their own success rates for quality assessment

## Architecture

```
User Command
       |
       v
TaskPlanner (LLM)
       |
       v
SkillLibrary.check()
       |
       +---> Skill Found? ----> ExecutableSkill.execute() ----> GraalVM
       |                                                        |
       |                                                        v
       |                                                   Results
       |
       +---> No Skill Found? ----> LLM Planning ----> Task Execution
                                                    |
                                                    v
                                              Pattern Detection
                                                    |
                                                    v
                                              SkillGenerator
                                                    |
                                                    v
                                              New Skill Added
```

## Components

### Skill Interface

The core abstraction defining a reusable skill.

```java
public interface Skill {
    String getName();
    String getDescription();
    List<String> getRequiredActions();
    String generateCode(Map<String, Object> context);
    boolean isApplicable(Task task);
    double getSuccessRate();
    void recordSuccess(boolean success);
    int getExecutionCount();
    String getCategory();
    int getEstimatedTicks();
    List<String> getRequiredItems();
}
```

**Key Methods:**

- `generateCode()`: Produces JavaScript code by substituting context variables into templates
- `isApplicable()`: Checks if a skill can handle a given task (via regex pattern matching)
- `getSuccessRate()`: Returns historical success rate (0.0 to 1.0) for prioritization

### ExecutableSkill

Concrete implementation storing skill metadata and JavaScript template code.

**Features:**

- Thread-safe success tracking using `AtomicInteger` and `AtomicLong`
- Template variable substitution with `{{variable}}` syntax
- Quoted substitution with `{{variable:quote}}` for string values
- Builder pattern for construction

**Example Template:**

```javascript
// digStaircase skill template
var depth = {{depth}};
var direction = "{{direction:quote}}"; // 'north', 'south', 'east', 'west'
for (var i = 0; i < depth; i++) {
    steve.mineBlockAt(x, y - i, z);
    if (i % 3 == 0) steve.placeBlock("torch", x, y - i, z);
}
```

### SkillLibrary

Central registry managing all skills with thread-safe operations.

**Capabilities:**

- **Semantic Search**: Find skills by description similarity
- **Applicability Matching**: Automatic skill-to-task matching
- **Duplicate Prevention**: Signature-based deduplication
- **Category Organization**: Skills grouped by type (mining, building, farming, etc.)
- **Success Tracking**: Per-skill execution statistics

**API Usage:**

```java
SkillLibrary library = SkillLibrary.getInstance();

// Find applicable skills for a task
List<Skill> skills = library.findApplicableSkills(task);

// Semantic search
List<Skill> results = library.semanticSearch("dig staircase for mining");

// Record outcome
library.recordOutcome("digStaircase", true);

// Get skills by category
List<Skill> miningSkills = library.getSkillsByCategory("mining");
```

### TaskPattern

Analyzes successful task sequences to discover reusable patterns.

**Pattern Types:**

- **LOOP**: Repeating action sequences (e.g., mine, place, mine, place)
- **SEQUENCE**: Ordered action patterns (e.g., pathfind → mine → place)
- **PARAMETERIZED**: Same action with incrementing values (e.g., x+1, x+2)
- **CONDITIONAL**: Branching based on state
- **COMPLEX**: Multi-action patterns with variables

**Detection Example:**

```
Original Sequence:
Task 1: mine at (0, 60, 0)
Task 2: place torch at (0, 60, 0)
Task 3: pathfind to (0, 59, 1)
Task 4: mine at (0, 59, 1)
Task 5: place torch at (0, 59, 1)

Detected Pattern:
Type: LOOP
Variable: iterations (5)
Variable: yIncrement (-1)
Variable: zIncrement (+1)
```

### SkillGenerator (Future Component)

Planned component for automatic skill generation from detected patterns.

**Planned Features:**

- Pattern-to-template conversion
- Parameter extraction
- Validation testing
- Automatic skill registration

## Built-in Skills

The SkillLibrary comes pre-loaded with common patterns:

### Mining Skills

| Skill | Description | Est. Time |
|-------|-------------|-----------|
| `digStaircase` | Dig downward staircase with torches | 10s |
| `stripMine` | Strip mining at Y=-58 (diamond layer) | 20s |
| `branchMine` | Create branching tunnels from main shaft | 30s |

### Building Skills

| Skill | Description | Est. Time |
|-------|-------------|-----------|
| `buildShelter` | Build 5x5x3 basic shelter with door | 25s |
| `buildPlatform` | Create flat building platform | 15s |

### Farming Skills

| Skill | Description | Est. Time |
|-------|-------------|-----------|
| `farmWheat` | Automated wheat farming with tilling | 20s |
| `farmTree` | Plant saplings in grid pattern | 10s |

### Utility Skills

| Skill | Description | Est. Time |
|-------|-------------|-----------|
| `organizeInventory` | Sort items by category | 2.5s |
| `collectDrops` | Spiral pattern to collect items | 15s |

## Usage in Commands

### Using Skills from User Commands

When a user issues a command, the system automatically checks for applicable skills:

```
User: "build a shelter"

System Flow:
1. TaskPlanner receives command
2. SkillLibrary.semanticSearch("build shelter")
3. Returns: buildShelter (95% match)
4. Check applicability: pattern "build.*shelter" matches
5. Execute skill with context: {width: 5, height: 3, depth: 5, block: "oak_planks"}
6. GraalVM executes generated JavaScript
7. Results returned to user
```

### Skill Selection Priority

1. **Exact Match**: Skill name or exact description match
2. **Pattern Match**: Regex applicability pattern matches task
3. **Semantic Match**: Word overlap in description
4. **Category Match**: Same category with high success rate
5. **LLM Fallback**: No skill found, use LLM planning

## Integration Points

### With TaskPlanner

```java
public class TaskPlanner {
    public List<Task> planTasks(String command, ForemanEntity foreman) {
        // 1. Check skill library first
        SkillLibrary library = SkillLibrary.getInstance();
        List<Skill> applicableSkills = library.semanticSearch(command);

        if (!applicableSkills.isEmpty()) {
            Skill bestSkill = applicableSkills.get(0);
            if (bestSkill.isApplicable(createTaskFromCommand(command))) {
                // Use skill instead of LLM
                return executeSkill(bestSkill, command);
            }
        }

        // 2. Fall back to LLM planning
        return llmClient.planTasks(command);
    }
}
```

### With ActionExecutor

```java
public class ActionExecutor {
    public ActionResult execute(Task task) {
        // Check if task is from a skill
        if (task.getMetadata().containsKey("skillName")) {
            String skillName = task.getMetadata().getString("skillName");
            Skill skill = SkillLibrary.getInstance().getSkill(skillName);

            // Record outcome
            boolean success = executeAction(task);
            SkillLibrary.getInstance().recordOutcome(skillName, success);

            return new ActionResult(success, ...);
        }

        // Regular action execution
        return executeRegularAction(task);
    }
}
```

### With CodeExecutionEngine

```java
public class CodeExecutionEngine {
    public ExecutionResult execute(String code) {
        // GraalVM JavaScript execution
        Context context = Context.newBuilder("js")
            .allowAllAccess(true)
            .build();

        // Bind steve API
        Value bindings = context.getBindings("js");
        bindings.putMember("steve", steveAPI);

        // Execute and return result
        Value result = context.eval("js", code);
        return new ExecutionResult(result, ...);
    }
}
```

## Adding New Skills

### Method 1: Programmatically

```java
Skill newSkill = ExecutableSkill.builder("mySkill")
    .description("Description of what this skill does")
    .category("mining")
    .codeTemplate("""
        var count = {{count}};
        for (var i = 0; i < count; i++) {
            steve.mineBlock(startX + i, startY, startZ);
        }
        """)
    .requiredActions("mine")
    .requiredItems("pickaxe")
    .estimatedTicks(200)
    .applicabilityPattern("mine.*line|mine.*row")
    .build();

SkillLibrary.getInstance().addSkill(newSkill);
```

### Method 2: Via Pattern Detection (Future)

```java
// Successful task sequence detected
List<Task> completedTasks = getCompletedTasks();

// Detect patterns
List<TaskPattern> patterns = TaskPattern.detectPatterns(completedTasks);

// Generate and register skills
for (TaskPattern pattern : patterns) {
    if (pattern.getFrequency() >= 3) { // Seen 3+ times
        Skill skill = SkillGenerator.fromPattern(pattern);
        SkillLibrary.getInstance().addSkill(skill);
    }
}
```

## Skill Lifecycle

```
Discovery (Pattern Detection)
        |
        v
Validation (Testing Phase)
        |
        v
Execution (With Success Tracking)
        |
        v
Refinement (Success Rate Analysis)
        |
        v
Optimization (Template Improvement) OR Deprecation (Low Success Rate)
```

## Configuration

Config file: `config/steve-common.toml`

```toml
[skills]
# Enable skill learning from patterns
enableLearning = true

# Minimum frequency before pattern becomes skill
minPatternFrequency = 3

# Minimum success rate to keep skill
minSuccessRate = 0.3

# Maximum age for skill cache entries
maxCacheAge = "24h"

# Enable semantic search
enableSemanticSearch = true
```

## Performance Considerations

### Cache Hit Rates by Task Type

| Task Type | Expected Cache Hit | Latency with Cache | Latency without Cache |
|-----------|-------------------|--------------------|-----------------------|
| TRIVIAL | 60-80% | ~50ms | ~1000ms |
| SIMPLE | 30-50% | ~100ms | ~2000ms |
| MODERATE | 10-20% | ~150ms | ~3000ms |
| COMPLEX | 5-10% | ~200ms | ~4000ms |
| NOVEL | 0% | N/A | ~5000ms |

### Memory Usage

- Each skill: ~2-5KB (template + metadata)
- 100 built-in skills: ~500KB
- 1000 learned skills: ~5MB

### Thread Safety

All operations are thread-safe using `ConcurrentHashMap` and atomic counters.

## Best Practices

1. **Descriptive Names**: Use clear, action-oriented names (e.g., `digStaircase`, not `skill1`)
2. **Specific Patterns**: Tight applicability patterns reduce false positives
3. **Category Organization**: Proper categorization improves search performance
4. **Success Tracking**: Always record outcomes for learning
5. **Template Safety**: Validate generated code before execution
6. **Fallback Handling**: Always have LLM fallback for unrecognized tasks

## Troubleshooting

### Skill Not Found

**Symptom**: Common tasks still hitting LLM

**Solutions**:
1. Check skill applicability patterns: `SkillLibrary.getInstance().semanticSearch("your query")`
2. Verify skill was registered: `SkillLibrary.getInstance().hasSkill("skillName")`
3. Review pattern specificity - too specific won't match variations

### Poor Success Rate

**Symptom**: Skill has low success rate (< 0.5)

**Solutions**:
1. Review template logic for bugs
2. Check required items/conditions
3. Increase validation testing before registration
4. Consider deprecation and re-learning

### Template Errors

**Symptom**: JavaScript execution failures

**Solutions**:
1. Validate template syntax before registration
2. Escape special characters in substitution
3. Test with various parameter values
4. Add error handling in generated code

## References

- **Inspired By**: [Voyager: An Open-Ended Embodied Agent with Large Language Models](https://www.voyager.minecraft/)
- **Related**: Skill generation from task patterns
- **See Also**: `TaskPattern`, `CodeExecutionEngine`, `GraalVM` documentation

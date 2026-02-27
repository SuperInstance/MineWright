# Command UX Improvements - Quick Reference

**Related Documents:**
- `IMPROVE_COMMAND_UX.md` - Full analysis and recommendations
- `IMPROVE_COMMAND_UX_IMPLEMENTATION.md` - Detailed implementation guide

---

## Current State Summary

### What Works Well
- Async LLM processing (no game freezing)
- GUI with color-coded message bubbles
- Command history (up/down arrows)
- Scrollable message history (500 messages)
- Basic error feedback

### What's Missing
- Intelligent command suggestions
- Detailed error recovery guidance
- Progress indicators for long actions
- Fuzzy matching for typos
- Persistent command history
- Autocomplete system

---

## Priority Improvements

### Priority 1 (Quick Wins - 1-2 weeks)

#### 1. Enhanced Error Messages
**File:** Create `CommandErrorHandler.java`
**Impact:** High | **Effort:** Medium

```java
// Example usage
CommandErrorHandler.CommandError error =
    new CommandErrorHandler.CommandError(
        CommandErrorHandler.ErrorType.CREW_NOT_FOUND,
        "Builder"
    );
Component message = CommandErrorHandler.getErrorMessage(error);
source.sendSuccess(() -> message, false);
```

**Key Features:**
- Specific error messages for each error type
- Recovery steps and suggestions
- Lists available alternatives
- Examples of valid commands

#### 2. Progress Bars
**File:** Modify `ForemanOfficeGUI.java`
**Impact:** Medium | **Effort:** Low

```java
// Render progress bar
renderProgressBar(graphics, x, y, width, height,
                 progressRatio, actionName);
```

**Key Features:**
- Visual progress for building/mining
- Color-coded by completion (blue -> orange -> green)
- Shows current/total counts
- Percentage display

#### 3. Typo Correction
**File:** Create `CommandPreProcessor.java`
**Impact:** Medium | **Effort:** Low

```java
// Before sending to LLM
String corrected = CommandPreProcessor.preProcess(rawCommand);
// "buld a house" -> "build a house"
```

**Key Features:**
- Common typo corrections (buld -> build)
- Whitespace normalization
- Filler word removal
- Contraction expansion

---

### Priority 2 (Core Features - 2-4 weeks)

#### 1. Command Suggestion Engine
**File:** Create `CommandSuggestionEngine.java`
**Impact:** High | **Effort:** Medium

```java
// Get context-aware suggestions
List<Suggestion> suggestions =
    CommandSuggestionEngine.getSuggestions(crew, partialInput);
```

**Key Features:**
- Context-aware (nearby ores, mobs, blocks)
- Fuzzy matching against templates
- Crew name completion
- History-based suggestions

#### 2. Intent Recognition
**File:** Create `IntentRecognizer.java`
**Impact:** High | **Effort:** Medium

```java
// Recognize user intent
RecognizedIntent intent = IntentRecognizer.recognize(command);
// intent.getIntent() -> BUILD, MINE, ATTACK, etc.
// intent.getConfidence() -> 0.0 to 1.0
```

**Key Features:**
- Pre-LLM intent detection
- Confidence scoring
- Target extraction (player, crew, all)
- Parameter extraction (quantity, resource)

#### 3. Persistent History
**File:** Create `CommandHistoryManager.java`
**Impact:** Medium | **Effort:** Low

```java
// Save/load commands
historyManager.addCommand(command, success, executionTime);
historyManager.saveHistory();
historyManager.loadHistory();
```

**Key Features:**
- Persist to JSON file
- Search functionality
- Most-used commands
- Success rate tracking

---

### Priority 3 (Advanced - 4-8 weeks)

#### 1. Real-Time Autocomplete
**File:** Modify `ForemanOfficeGUI.java`
**Impact:** Medium-High | **Effort:** High

```java
// Tab completion
if (keyCode == 258) { // TAB
    String completed = getAutocompleteCompletion(partialInput);
    inputBox.setValue(completed);
}
```

**Key Features:**
- Tab completion
- Dropdown suggestions
- Arrow key navigation
- Context-sensitive completions

---

## Error Message Examples

### Before (Current)
```
"I couldn't understand that command."
```

### After (Improved)
```
I couldn't understand that command.

Try rephrasing:
  - "build a house" instead of "construct house"
  - "get me iron" instead of "obtain iron ore"
  - "kill mobs" instead of "eliminate hostiles"

Or try these examples:
  - build a wooden cabin
  - mine 32 iron ore
  - follow me
```

---

## Progress Bar Examples

### Building Progress
```
Building house [=========-] 90%
Placed 45/50 blocks
```

### Mining Progress
```
Mining iron [=======--] 70%
Collected 14/20 ores
```

---

## Suggestion Examples

### Context-Aware Suggestions
```
Nearby ores detected:
  - mine nearby ores
  - gather resources I can see

Hostile mobs nearby:
  - attack mobs
  - defend against monsters

Player nearby:
  - follow me
  - come with me
```

### Fuzzy Matching
```
Input: "buld a house"
Did you mean: "build a house"?
```

---

## Code Snippets

### Update ActionExecutor with Better Errors

```java
// In processNaturalLanguageCommand method
try {
    planningFuture = getTaskPlanner().planTasksAsync(foreman, command);
    sendToGUI(foreman.getSteveName(), "Thinking...");
} catch (Exception e) {
    CommandErrorHandler.CommandError error =
        new CommandErrorHandler.CommandError(
            CommandErrorHandler.ErrorType.PLANNING_FAILED,
            null
        );
    Component message = CommandErrorHandler.getErrorMessage(error);
    sendToGUI(foreman.getSteveName(), message.getString());
    isPlanning = false;
}
```

### Add Progress Tracking to Actions

```java
// In BaseAction or specific action classes
public class BuildStructureAction extends BaseAction {
    private int totalBlocks;
    private int placedBlocks;

    @Override
    public void tick() {
        // ... existing code

        // Update progress
        placedBlocks++;
        ForemanOfficeGUI.updateActionProgress(
            foreman.getSteveName(),
            "Building " + structureType,
            placedBlocks,
            totalBlocks
        );
    }
}
```

### Integrate Intent Recognition

```java
// In processNaturalLanguageCommand method
IntentRecognizer.RecognizedIntent intent =
    IntentRecognizer.recognize(command);

if (intent.getConfidence() < 0.5) {
    // Low confidence - provide suggestions
    List<Suggestion> suggestions =
        CommandSuggestionEngine.getSuggestions(foreman, command);

    if (!suggestions.isEmpty()) {
        sendToGUI(foreman.getSteveName(),
            "Did you mean: " + suggestions.get(0).text() + "?");
        return;
    }
}

// Proceed with high confidence intent
```

---

## Testing Checklist

### Unit Tests
- [ ] Levenshtein distance calculation
- [ ] Intent recognition accuracy (target: 85%+)
- [ ] Error message generation
- [ ] Command template matching
- [ ] History persistence

### Integration Tests
- [ ] End-to-end command flow
- [ ] Progress bar updates
- [ ] Suggestion relevance
- [ ] Error recovery success rate (target: 70%+)

### User Tests
- [ ] Command success rate (target: 85%+)
- [ ] Average planning time (target: <5s)
- [ ] Suggestion acceptance rate (target: 40%+)
- [ ] User satisfaction (target: 4.0/5)

---

## File Structure

```
src/main/java/com/minewright/
├── command/
│   ├── CommandErrorHandler.java          (NEW - Priority 1)
│   ├── CommandPreProcessor.java           (NEW - Priority 1)
│   ├── CommandSuggestionEngine.java       (NEW - Priority 2)
│   └── IntentRecognizer.java              (NEW - Priority 2)
├── client/
│   ├── ForemanOfficeGUI.java              (MODIFY - Priority 1)
│   └── CommandHistoryManager.java         (NEW - Priority 2)
└── action/
    └── ActionExecutor.java                 (MODIFY - Priority 1)
```

---

## Quick Wins (Copy-Paste Ready)

### Add Better Error to ActionExecutor

```java
// Replace line 328-332 in ActionExecutor.java
if (currentAction == null) {
    CommandErrorHandler.CommandError error =
        new CommandErrorHandler.CommandError(
            CommandErrorHandler.ErrorType.UNKNOWN_ACTION,
            task.getAction()
        );
    Component message = CommandErrorHandler.getErrorMessage(error);
    foreman.sendChatMessage(message);
    return;
}
```

### Add Progress Update to BuildAction

```java
// Add to BuildStructureAction.java tick() method
if (ticks % 10 == 0) {  // Update every 10 ticks
    ForemanOfficeGUI.updateActionProgress(
        foreman.getSteveName(),
        "Building " + structureType,
        blocksPlaced.size(),
        targetBlocks.size()
    );
}
```

---

## Metrics Dashboard

Track these metrics after implementation:

| Metric | Current | Target | How to Measure |
|--------|---------|--------|----------------|
| Command Success Rate | ~60% | 85%+ | Log successful/failed commands |
| Error Recovery Rate | ~30% | 70%+ | Track error -> retry -> success |
| Avg Planning Time | ~8s | <5s | Timestamp planning start/end |
| Suggestion Acceptance | 0% | 40%+ | Count suggestion clicks/uses |
| User Satisfaction | N/A | 4.0/5 | User survey after 1 week |

---

## Next Steps

1. **Week 1-2:** Implement CommandErrorHandler and progress bars
2. **Week 3-4:** Add CommandSuggestionEngine and IntentRecognizer
3. **Week 5-6:** Implement CommandHistoryManager and persistence
4. **Week 7-8:** Add real-time autocomplete and advanced features
5. **Week 9-10:** User testing and refinement

---

## Resources

- **Main Analysis:** `docs/IMPROVE_COMMAND_UX.md`
- **Implementation Guide:** `docs/IMPROVE_COMMAND_UX_IMPLEMENTATION.md`
- **Related:** `docs/COMMAND_ANALYTICS.md`
- **Related:** `docs/GUI_UX_ENHANCEMENTS.md`

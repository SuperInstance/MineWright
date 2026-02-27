# MineWright API Documentation Template

This document defines the format and generation approach for MineWright API documentation.

## Documentation Format

### Action API Documentation

Each action must be documented with the following sections:

```markdown
# Action: `<action_name>`

## Overview
Brief description of what this action does.

## Parameters

### Required Parameters
| Parameter | Type | Description | Example |
|-----------|------|-------------|---------|
| `param_name` | string/int/boolean/list | Description | `"value"` / `42` / `true` / `[...]` |

### Optional Parameters
| Parameter | Type | Default | Description | Example |
|-----------|------|---------|-------------|---------|
| `param_name` | string/int/boolean/list | `"default"` | Description | `"value"` |

## Usage Examples

### Basic Example
```json
{
  "action": "action_name",
  "parameters": {
    "param1": "value1",
    "param2": 42
  }
}
```

### Advanced Example with All Parameters
```json
{
  "action": "action_name",
  "parameters": {
    "param1": "value1",
    "param2": 42,
    "param3": ["item1", "item2"],
    "param4": {
      "nested": "value"
    }
  }
}
```

## Behavior

### Pre-conditions
- Condition 1 that must be true
- Condition 2 that must be true

### Execution Process
1. Step one description
2. Step two description
3. Step three description

### Post-conditions
- Result condition 1
- Result condition 2

## Error Cases

| Error Scenario | Error Message | Resolution |
|----------------|---------------|------------|
| Invalid parameter | `"Invalid block type: xyz"` | Use valid block name |
| Missing required param | `"Missing required parameter: x"` | Add parameter to task |
| Timeout | `"Action timeout after N ticks"` | Reduce scope or check environment |

## State Transitions
- **Starts From**: `IDLE`, `COMPLETED`, `FAILED`
- **Transitions To**: `EXECUTING` -> `COMPLETED` on success, `FAILED` on error

## Tick Behavior
- **Tick Interval**: Every N game ticks (20 ticks = 1 second)
- **Blocking**: Whether this action blocks other actions
- **Interruptible**: Can be cancelled mid-execution

## Configuration Dependencies
- `config.behavior.actionTickDelay` - Affects execution speed
- `config.ai.provider` - May affect planning if AI-driven

## Related Actions
- [`related_action`](#related-action) - Description of relationship
- [`parent_action`](#parent-action) - This action extends/uses parent

## Version
- **Since**: 1.0.0
- **Modified**: 1.2.0 - Added new parameter X

## Implementation Notes
- Internal implementation details
- Performance considerations
- Known limitations
```

---

### Event API Documentation

Each event must be documented with the following sections:

```markdown
# Event: `<EventClassName>`

## Overview
Description of when this event is published and its purpose.

## Event Payload

### Properties
| Property | Type | Description | Example |
|----------|------|-------------|---------|
| `propertyName` | String/int/boolean/Map | Description | Example value |

### Constructor Signature
```java
public EventClassName(
    String agentId,
    String actionName,
    // ... other parameters
)
```

## Publication Triggers
This event is published when:
- Trigger condition 1
- Trigger condition 2
- Trigger condition 3

## Subscribers

### Built-in Subscribers
| Subscriber | Purpose | Priority |
|------------|---------|----------|
| `LoggingInterceptor` | Logs event details | 0 |
| `MetricsInterceptor` | Tracks execution metrics | 10 |

### Example Custom Subscriber
```java
eventBus.subscribe(ActionCompletedEvent.class, event -> {
    if (event.isSuccess()) {
        System.out.println("Action succeeded: " + event.getActionName());
    }
}, 100); // priority
```

## Event Flow
1. Publisher creates event instance
2. Event published to EventBus
3. Subscribers notified in priority order
4. Each subscriber processes event synchronously

## Thread Safety
- **Publication**: Thread-safe
- **Subscription**: Thread-safe
- **Notification**: Synchronous by default, use `publishAsync()` for async

## Related Events
- [`RelatedEvent`](#related-event) - Description of relationship
- [`ParentEvent`](#parent-event) - This event extends/uses parent

## Version
- **Since**: 1.1.0
- **Modified**: 1.3.0 - Added new field X
```

---

### Configuration Documentation

Each configuration section must be documented with:

```markdown
# Configuration: `<config_section>`

## Overview
Description of what this configuration section controls.

## Configuration Options

### `option_name`
- **Type**: `String` / `Int` / `Double` / `Boolean`
- **Default Value**: `"default"` / `42` / `0.7` / `true`
- **Valid Range**: `"value1" | "value2" | ...` / `min - max`
- **Description**: Detailed description of what this option does
- **Impact**: How changing this affects behavior
- **Restart Required**: `true` / `false`

### Example Config
```toml
[section]
option_name = "value"  # Comment
```

## Configuration File Location
- **Client**: `config/minewright-client.toml`
- **Server**: `config/minewright-server.toml`
- **Common**: `config/minewright-common.toml`

## Environment Overrides
Environment variables can override config values:
- `MINWRIGHT_<SECTION>_<OPTION>` - Format for env vars

## Validation Rules
- Rule 1: Description
- Rule 2: Description
- Rule 3: Description

## Related Options
- [`other_option`](#other_option) - This option interacts with other_option

## Migration Guide
### Version X to Y
- `old_option` renamed to `new_option`
- `deprecated_option` removed, use `replacement_option` instead
```

---

## Generation Approach

### Automated Documentation Generation

1. **Source Code Annotation**
   - Use JavaDoc annotations on all public APIs
   - Include `@param`, `@return`, `@throws` tags
   - Add `@since` and `@deprecated` tags

2. **Documentation Scanner**
   - Scan `action/actions/` for action classes
   - Scan `event/` for event classes
   - Scan `config/` for config classes

3. **Template Processing**
   - Extract metadata from JavaDoc
   - Parse parameter validation code
   - Identify error conditions from try-catch blocks
   - Extract default values from constructors

4. **Output Generation**
   - Generate markdown files for each API category
   - Create index with cross-references
   - Validate all links and examples

### Manual Documentation Requirements

Even with automation, certain elements require manual documentation:

1. **Usage Examples**: Real-world usage scenarios
2. **Behavioral Notes**: Non-obvious behaviors
3. **Best Practices**: Recommended usage patterns
4. **Troubleshooting**: Common issues and solutions
5. **Integration Examples**: How to combine multiple APIs

### Documentation Versioning

- Documentation version matches mod version
- Deprecated APIs marked with warning boxes
- New APIs marked with "New in X.Y.Z" badges
- Breaking changes highlighted in red

### Review Process

1. **Code Review**: Documentation reviewed alongside code
2. **API Review**: Documentation reviewed for completeness
3. **User Testing**: Documentation tested against real use cases
4. **Accessibility**: Checked for clarity and completeness

---

## Example Entry

Below is a complete example for the `mine` action:

# Action: `mine`

## Overview
Mines a specified type of block in the world. The agent will search for and mine blocks until the target quantity is reached or a timeout occurs.

## Parameters

### Required Parameters
| Parameter | Type | Description | Example |
|-----------|------|-------------|---------|
| `block` | string | The block type to mine (can use common names like "iron_ore") | `"iron_ore"` |

### Optional Parameters
| Parameter | Type | Default | Description | Example |
|-----------|------|---------|-------------|---------|
| `quantity` | int | `8` | Number of blocks to mine | `64` |

## Usage Examples

### Basic Example - Mine Iron
```json
{
  "action": "mine",
  "parameters": {
    "block": "iron_ore",
    "quantity": 32
  }
}
```

### Example - Mine Coal with Default Quantity
```json
{
  "action": "mine",
  "parameters": {
    "block": "coal_ore"
  }
}
```

## Behavior

### Pre-conditions
- Agent must be spawned and idle
- Target block type must exist in the world
- Agent must have tool access (auto-equipped)

### Execution Process
1. Agent determines mining direction based on player's view
2. Agent creates tunnel in that direction
3. Agent searches for target blocks ahead in tunnel
4. When ore found, agent mines it
5. Agent places torches periodically for lighting
6. Process repeats until quantity reached or timeout

### Post-conditions
- Target number of blocks mined (or fewer if timeout)
- Agent returns to idle state
- Mined items dropped in world

## Error Cases

| Error Scenario | Error Message | Resolution |
|----------------|---------------|------------|
| Invalid block type | `"Invalid block type: xyz"` | Use valid Minecraft block ID |
| No blocks found | `"Mining timeout - only found N blocks"` | Reduce quantity or check location |
| Missing player | `"No nearby player to determine direction"` | Ensure player is in dimension |

## State Transitions
- **Starts From**: `IDLE`, `COMPLETED`, `FAILED`
- **Transitions To**: `EXECUTING` -> `COMPLETED` on success, `FAILED` on error

## Tick Behavior
- **Tick Interval**: Every game tick
- **Blocking**: Yes - prevents other actions while mining
- **Interruptible**: Yes - can be cancelled with `/foreman cancel`
- **Max Duration**: 24000 ticks (20 minutes)

## Configuration Dependencies
- `config.behavior.actionTickDelay` - Affects check frequency (default: 20)
- Ore spawn depths (hardcoded) - Affects where agent looks for ores

## Related Actions
- [`gather`](#gather) - Higher-level resource gathering
- [`pathfind`](#pathfind) - Used for navigation within mine action

## Implementation Details

### Mining Direction
The agent mines in the direction the player is looking:
- Player look angle determines cardinal direction
- Agent mines forward in straight tunnel
- Changes direction only if cancelled and restarted

### Torch Placement
- Torch placed every 100 ticks (5 seconds)
- Placed when light level < 8
- Searches for suitable wall/floor position

### Ore Depth Knowledge
Agent has hardcoded knowledge of optimal Y levels:
- Diamond: -59 (deepslate)
- Iron: -16 (deepslate), 64 (normal)
- Gold: -16 (deepslate), 32 (normal)
- Coal: 96
- Redstone: -32 (deepslate), 16 (normal)

## Version
- **Since**: 1.0.0
- **Modified**: 1.2.0 - Added intelligent tunnel mining and torch placement

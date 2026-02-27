# Research Documents

Comprehensive research findings for improving Steve AI (MineWright).

## Recent Research

| Document | Description | Date |
|----------|-------------|------|
| **[Cloudflare Vectorize](CLOUDFLARE_VECTORIZE.md)** | Vector database at edge for agent long-term memory storage | 2026-02-27 |
| **[LLM Tool Calling](LLM_TOOL_CALLING.md)** | Best practices for function/tool calling across OpenAI, Claude, Gemini | 2026-02-27 |
| **[LLM Tool Calling Quick Ref](LLM_TOOL_CALLING_QUICKREF.md)** | Fast reference for tool calling implementation | 2026-02-27 |
| [AI Debugging](../RESEARCH_AI_DEBUGGING.md) | Debugging techniques for AI systems | 2026-02-27 |
| [Code Generation](../RESEARCH_CODE_GENERATION.md) | AI-assisted code generation patterns | 2026-02-27 |
| [Performance Patterns](../RESEARCH_PERFORMANCE_PERFORMANCE.md) | Performance optimization patterns | 2026-02-27 |

## AI Frameworks & Patterns

| Document | Description |
|----------|-------------|
| [AI Agent Frameworks](AI_AGENT_FRAMEWORKS.md) | Analysis of agent architecture frameworks |
| [AI Agent Patterns 2025](AI_AGENT_PATTERNS_2025.md) | Latest agent coordination patterns |
| [Game AI Patterns](GAME_AI_PATTERNS.md) | Game-specific AI implementation patterns |
| [Game AI Patterns Quick Ref](GAME_AI_PATTERNS_QUICKREF.md) | Quick reference for game AI |
| [Baritone Analysis](BARITONE_ANALYSIS.md) | Analysis of Minecraft bot framework |

## AI Capabilities

| Document | Description |
|----------|-------------|
| [Companion Personality](../RESEARCH_COMPANION_PERSONALITY.md) | AI personality and behavior design |
| [Conversational AI](../RESEARCH_CONVERSATIONAL_AI.md) | Natural language interaction patterns |
| [Emotional AI](../RESEARCH_EMOTIONAL_AI.md) | Emotion modeling and expression |
| [Memory Architectures](../RESEARCH_MEMORY_ARCHITECTURES.md) | Memory system design patterns |
| [Player Modeling](../RESEARCH_PLAYER_MODELING.md) | Understanding and predicting player behavior |
| [Voice AI 2024](../RESEARCH_VOICE_AI_2024.md) | Voice interaction capabilities |

## Technical Research

| Document | Description |
|----------|-------------|
| [Knowledge Graphs](../RESEARCH_KNOWLEDGE_GRAPHS.md) | Knowledge representation techniques |
| [Local LLM 2024](../RESEARCH_LOCAL_LLM_2024.md) | Running LLMs locally |
| [MCP Protocols](../RESEARCH_MCP_PROTOCOLS.md) | Model Context Protocol standards |
| [Minecraft Bots](../RESEARCH_MINECRAFT_BOTS.md) | Minecraft automation research |
| [Pathfinding Advances](../RESEARCH_PATHFINDING_ADVANCES.md) | Navigation algorithms |
| [Procedural Generation](../RESEARCH_PROCEDURAL_GENERATION.md) | Content generation techniques |
| [Realtime Inference](../RESEARCH_REALTIME_INFERENCE.md) | Real-time AI execution |
| [Workflow Orchestration](../RESEARCH_WORKFLOW_ORCHESTRATION.md) | Multi-step AI workflows |

## Integration Research

| Document | Description |
|----------|-------------|
| [Z.AI Integration](../RESEARCH_Z_AI_INTEGRATION.md) | Z.AI provider integration |

## Featured Research: LLM Tool Calling

### Key Findings

1. **Schema-Driven Validation**
   - Move from ad-hoc parsing to formal JSON Schema definitions
   - Use provider native function calling APIs (OpenAI, Gemini, Claude)
   - Implement strict mode for guaranteed schema compliance

2. **Progressive Error Handling**
   - Tier 1: Schema validation before execution
   - Tier 2: Automatic repair for common issues
   - Tier 3: LLM correction feedback loop (max 2 retries)

3. **Multi-Step Orchestration**
   - Sequential execution for dependent tasks
   - Parallel execution for independent tasks
   - Tool result feedback loops for adaptive planning

4. **Performance Improvements**
   - Reduced LLM round-trips via function calling
   - Parallel task execution for faster completion
   - Caching and optimization strategies

### Implementation Priority

1. **Phase 1** (Week 1-2): Schema Registry
   - Create `ActionSchemaRegistry` class
   - Define schemas for all core actions
   - Update `PromptBuilder` with schema descriptions

2. **Phase 2** (Week 3-4): Error Handling
   - Implement `ParseResult` with error details
   - Add automatic repair for common issues
   - Implement LLM correction feedback loop

3. **Phase 3** (Week 5-6): Function Calling
   - Migrate to OpenAI function calling API
   - Add structured output mode
   - Update all LLM clients

4. **Phase 4** (Week 7-8): Orchestration
   - Implement multi-step execution loop
   - Add parallel task execution
   - Implement tool result feedback loops

See [LLM_TOOL_CALLING.md](LLM_TOOL_CALLING.md) for complete details.

## Research Methodology

All research follows the investigation methodology:

1. **Discovery**: Find all related projects and approaches
2. **Analysis**: Document patterns, dependencies, and trade-offs
3. **Assessment**: Evaluate completeness and production readiness
4. **Identification**: Extract novel and useful implementations
5. **Documentation**: Create comprehensive markdown reports

## Contributing Research

When adding new research:

1. Create document in appropriate category
2. Use clear, descriptive filename
3. Include date and version information
4. Add quick reference if applicable
5. Update this README

## Research Sources

Research is compiled from:
- Academic papers and technical documentation
- Open source projects and frameworks
- Industry best practices and case studies
- Provider documentation (OpenAI, Anthropic, Google, etc.)
- Community resources and forums

---

**Last Updated:** 2026-02-27
**Research Coordinator:** Orchestrator Agent

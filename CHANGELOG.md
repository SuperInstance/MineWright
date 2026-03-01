# Changelog

All notable changes to MineWright will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- **Security Infrastructure**
  - InputSanitizer utility for comprehensive prompt injection prevention
  - Environment variable support for API keys (${ENV_VAR} syntax)
  - Comprehensive security test suite (40+ test cases)
  - Jailbreak attempt detection
  - Control character stripping
  - Length limits enforcement
  - Suspicious pattern detection and logging
- **Action System Enhancements**
  - RetryPolicy for configurable retry behavior
  - ErrorRecoveryStrategy for intelligent error handling
  - Enhanced ActionResult with error context
- **Performance Monitoring**
  - TickProfiler for AI operation performance tracking
  - Budget enforcement and warning thresholds
- **Memory System**
  - MemoryConsolidationService for long-term learning
  - CompositeEmbeddingModel with multiple provider support
  - LocalEmbeddingModel for offline operation
  - OpenAIEmbeddingModel integration
- **AI Systems**
  - HTN (Hierarchical Task Network) planner with methods, world state, and domain
  - Behavior tree runtime engine (composite/leaf/decorator nodes)
  - Advanced pathfinding (A*, hierarchical, path smoothing, movement validation)
  - Cascade router for tier-based model selection
  - Evaluation infrastructure (metrics collection, benchmark scenarios)
- **Multi-Agent Coordination**
  - Contract Net Protocol implementation
  - Blackboard system for shared knowledge
  - BidCollector for task bidding
  - AwardSelector for task assignment
- **Script Execution**
  - Script parsing and execution infrastructure
  - Code execution engine (GraalVM JS sandbox)
  - Security restrictions for code execution
- **Testing Infrastructure**
  - Integration test framework
  - Pathfinding test suite (AStarPathfinderTest, HierarchicalPathfinderTest, MovementValidatorTest)
  - HTN planner test suite
  - Behavior tree test suite
  - Script execution test suite
  - Security test suite (InputSanitizerTest with 40+ cases)
  - Action test suite (ActionExecutorTest, ActionResultTest, BuildStructureActionTest, etc.)
- **Documentation**
  - Comprehensive onboarding documentation suite
    - `docs/ONBOARDING.md` - Quick start for new developers/agents
    - `docs/ARCHITECTURE_OVERVIEW.md` - System design documentation
    - `docs/DEVELOPMENT_GUIDE.md` - Build and development instructions
    - `docs/RESEARCH_GUIDE.md` - Dissertation and research documentation
  - Test coverage analysis and structure guide
  - Action system gap analysis
  - Script cache implementation documentation
  - Memory enhancements documentation
  - Dissertation progress tracking documents
  - Citation standardization and progress tracking
  - Research documents for dissertation 2
  - Specialist agent templates in `docs/agents/`
- **Contributor Resources**
  - CONTRIBUTING.md for open source contributors
  - SECURITY.md with security policy
  - MIT LICENSE file

### Changed
- Updated CLAUDE.md with accurate implementation status and security improvements
- Updated README.md with new features and security information
- Improved PromptBuilder with input sanitization
- Updated TaskPlanner with command validation
- Enhanced MineWrightConfig with environment variable resolution
- Updated BaseAction with error recovery patterns
- Enhanced ActionExecutor with retry logic
- Improved CompanionMemory with memory consolidation
- Enhanced MilestoneTracker for relationship evolution
- Updated HierarchicalPathfinder with movement validation
- Improved MovementValidator with comprehensive checks

### Fixed
- **Security Vulnerabilities (CRITICAL)**
  - Fixed empty catch block in StructureTemplateLoader.java:88 - Now logs full exception with stack trace
  - Fixed API key handling to support environment variables (prevent hardcoded secrets)
  - Fixed missing input validation for LLM prompts (added InputSanitizer)
  - Fixed lack of suspicious pattern detection in commands (added validation in TaskPlanner)
  - Fixed potential security vulnerabilities in code execution engine (enhanced sandbox)
- Fixed potential resource leaks in action execution
- Improved error handling in LLM clients
- Enhanced exception logging throughout the codebase

### Security
- **Input Sanitization**
  - Prompt injection detection and prevention
  - Jailbreak attempt detection (DAN mode, developer mode, unrestricted mode)
  - Role hijacking prevention
  - Code execution attempt detection
  - System prompt extraction prevention
  - JSON termination attack prevention
  - Control character removal
  - Length limit enforcement
  - Repetition attack collapsing
- **API Key Management**
  - Environment variable support for secure API key storage
  - API key preview logging (not full key)
  - ${ENV_VAR} syntax in config files
- **Code Execution**
  - Enhanced GraalVM JS sandbox with security restrictions
  - Timeout enforcement (30s max)
  - No native/process creation allowed
  - Controlled API bridge only
- **Audit Trail**
  - Full exception logging (no empty catch blocks)
  - Security event logging
  - Suspicious pattern detection logging
  - Stack traces on errors

### Tests
- Added ActionExecutorTest for core execution engine
- Added ActionResultTest for result handling
- Added BaseActionTest for action lifecycle
- Added BuildStructureActionTest for building operations
- Added CraftItemActionTest for crafting operations
- Added GatherResourceActionTest for resource gathering
- Added MineBlockActionTest for mining operations
- Added PlaceBlockActionTest for block placement
- Added InputSanitizerTest with 40+ security test cases
- Added pathfinding test suite (AStarPathfinderTest, HierarchicalPathfinderTest, MovementValidatorTest)
- Added HTN planner test suite
- Added behavior tree test suite
- Added script execution test suite
- Added integration test framework
- **Test Coverage**: Improved from 13% to 23% (54 test files, 32,298 lines)

### Documentation
- **Project Documentation**
  - Added CHANGELOG.md
  - Added SECURITY.md with security policies
  - Added SECURITY_IMPROVEMENTS_SUMMARY.md detailing all security enhancements
  - Updated CONTRIBUTING.md with comprehensive development setup
  - Updated CLAUDE.md with latest security improvements and project status
  - Updated README.md with new features and security information
- **Architecture Documentation**
  - Added docs/ARCHITECTURE_OVERVIEW.md with system diagrams
  - Added docs/DEVELOPMENT_GUIDE.md with build and testing instructions
  - Added docs/ONBOARDING.md for new developers
- **Testing Documentation**
  - Added docs/TEST_COVERAGE.md with coverage analysis
  - Added docs/TEST_STRUCTURE_GUIDE.md with testing guidelines
  - Added docs/TEST_COVERAGE_ANALYSIS.md with detailed analysis
- **Feature Documentation**
  - Added docs/ACTION_SYSTEM_GAP_ANALYSIS.md identifying areas for improvement
  - Added docs/MEMORY_ENHANCEMENTS.md describing memory system improvements
  - Added docs/SCRIPT_CACHE_IMPLEMENTATION.md for script caching
  - Added docs/SCRIPT_CACHE_QUICK_REFERENCE.md for quick reference
  - Added docs/SCRIPT_CACHE_SUMMARY.md summarizing script cache features
  - Added docs/SCRIPT_LAYER_LEARNING_SYSTEM.md for learning system
- **Research Documentation**
  - Added docs/research/EVALUATION_FRAMEWORK.md for benchmarking
  - Added docs/research/EVALUATION_QUICK_REFERENCE.md for evaluation quick reference
  - Added docs/research/CHAPTER_3_COMPLETION_SUMMARY.md for dissertation progress
  - Added docs/research/CHAPTER_3_INTEGRATION_PLAN.md for integration planning
  - Added docs/research/CHAPTER_6_CITATIONS_STATUS_SUMMARY.md for citation tracking
  - Added docs/research/CHAPTER_8_2024_2025_TECHNIQUES.md for LLM technique coverage
  - Added docs/research/CITATION_EDITS_QUICK_REFERENCE.md for citation standardization
  - Added docs/research/CITATION_PROGRESS_TRACKER.md for tracking citation progress
  - Added docs/research/CITATION_STANDARDIZATION_REPORT.md for citation standards
  - Added docs/research/CITATION_STANDARDIZATION_SUMMARY.md summarizing citation work
  - Added docs/research/COGNITIVE_LAYER_ARCHITECTURE.md for cognitive architecture
  - Added docs/research/COMPREHENSIVE_BIBLIOGRAPHY.md for academic references
  - Added docs/research/CYCLE_3_READINESS.md for development cycle planning
  - Added docs/research/CYCLE_3_READINESS_SUMMARY.md summarizing readiness
  - Added docs/research/DISSERTATION_2_BRAIN_ARCHITECTURE_PARALLELS.md for dissertation 2 research
  - Added docs/research/DISSERTATION_2_FOREMAN_MENTAL_MODEL.md for mental model research
  - Added docs/research/DISSERTATION_2_GAME_THEORY_WATCHERS.md for game theory analysis
  - Added docs/research/DISSERTATION_2_HIGH_LEVEL_WORKER_MODELS.md for worker model research
  - Added docs/research/DISSERTATION_2_MUD_AUTOMATION_EVOLUTION.md for MUD automation history
  - Added docs/research/DISSERTATION_2_MULTI_LAYER_ARCHITECTURE.md for multi-layer architecture
  - Added docs/research/DISSERTATION_2_ROADMAP.md for dissertation 2 roadmap
  - Added docs/research/DISSERTATION_2_SMALL_MODEL_SPECIALIZATION.md for model specialization
  - Added docs/research/DISSERTATION_CHAPTER_3_COMPLETE.md for complete chapter 3
  - Added docs/research/DISSERTATION_CHAPTER_3_RPG_INTEGRATED.md for RPG integration
  - Added docs/research/DISSERTATION_CHAPTER_8_2024_2025_COVERAGE.md for technique coverage
  - Added docs/research/DISSERTATION_INTEGRATION_SUMMARY.md for integration status
  - Added docs/research/DISSERTATION_LIMITATIONS_SECTIONS.md for limitations analysis
  - Added docs/research/INTEGRATION_SUMMARY.md for integration summary
  - Added docs/research/LLM_PATTERNS_2024_2025.md for LLM pattern analysis
  - Added docs/research/MUD_AUTOMATION_LLM_PRINCIPLES.md for MUD automation principles
  - Added docs/research/MULTI_AGENT_COORDINATION_DESIGN.md for coordination design
  - Added docs/research/NEW_ARCHITECTURE_SYSTEMS.md for new architecture documentation
  - Added docs/research/SMALL_MODEL_SPECIALIZATION.md for small model research
  - Added docs/research/VIVA_VOCE_CYCLE2_SYNTHESIS.md for viva voce preparation
- **Configuration Examples**
  - Added config/minewright-common.toml.example with all configuration options

### Build
- Updated build.gradle with Checkstyle and SpotBugs configuration (currently disabled, needs re-enabling)
- Added JaCoCo for test coverage reporting
- Added shadowJar plugin for fat JAR creation
- Improved error messages in build configuration
- **Dependencies**:
  - Updated Caffeine to 3.1.8 for high-performance caching
  - Updated Resilience4j to 2.3.0 for resilience patterns
  - Updated GraalVM JS to 24.1.2 for code execution
  - Added embedding model dependencies (OpenAI, local)

### Performance
- Improved input sanitization performance with optimized regex patterns
- Enhanced cache performance with Caffeine 3.1.8
- Optimized LLM client batching
- Improved memory consolidation performance
- Enhanced embedding model performance with composite pattern
- Added tick budget enforcement to prevent server lag

## [1.3.0] - 2026-02-27

### Added
- Behavior Tree runtime engine with composite/leaf/decorator nodes
- HTN (Hierarchical Task Network) planner implementation
- Advanced pathfinding system (A*, hierarchical, path smoothing)
- Cascade Router for intelligent LLM tier selection
- Evaluation infrastructure (metrics, benchmarks)
- Milestone tracker for relationship evolution
- Code execution engine (GraalVM JS sandbox)

### Changed
- Rebranded from "Steve" to "MineWright"
- Updated package structure and naming

### Fixed
- Various bug fixes and performance improvements

## [1.2.0] - 2026-02-15

### Added
- Multi-agent orchestration framework
- Foreman archetype system (8 personalities)
- Vector search for semantic memory
- Voice system framework (STT/TTS)
- Resilience patterns (Resilience4j integration)

### Changed
- Improved async LLM client architecture
- Enhanced memory system

## [1.1.0] - 2026-01-20

### Added
- Async LLM clients (OpenAI, Groq, Gemini, z.ai)
- Batching LLM client for API efficiency
- Plugin system with ActionRegistry
- State machine with AgentStateMachine
- Interceptor chain architecture
- Event bus for agent coordination

### Changed
- Refactored action execution to tick-based system

## [1.0.0] - 2025-12-01

### Added
- Initial release
- Basic action system (mine, build, gather, craft)
- LLM integration for natural language commands
- Foreman entity implementation
- Basic memory system
- Configuration management

---

## Version History Summary

| Version | Date | Key Features |
|---------|------|--------------|
| Unreleased | 2026-03-01 | Security Improvements, Test Coverage, Documentation |
| 1.3.0 | 2026-02-27 | Behavior Trees, HTN, Advanced Pathfinding |
| 1.2.0 | 2026-02-15 | Multi-agent, Archetypes, Vector Search |
| 1.1.0 | 2026-01-20 | Async LLM, Plugin System, State Machine |
| 1.0.0 | 2025-12-01 | Initial release |

## Project Status (2026-03-01)

**Current Version:** 1.4.0-dev (Unreleased)
**Code Completion:** 85% (234 files, 85,752 lines)
**Test Coverage:** 23% (54 test files, 32,298 lines)
**Documentation:** 425 files, 521,003 lines
**Security:** All critical issues addressed (2026-03-01)
**Build Health:** 8/10 (quality tools disabled but stable)

## Upcoming Releases

### [1.4.0] - Planned Q2 2026
- Re-enable Checkstyle and SpotBugs
- Improve test coverage to 40%+
- Script DSL for automation patterns
- LLM→Script generation pipeline
- Skill auto-generation learning loop
- Multi-agent coordination protocol
- Comprehensive evaluation pipeline

### [1.5.0] - Planned Q3 2026
- Small model fine-tuning
- MUD automation research integration
- Advanced behavior tree library
- Enhanced HTN planner
- Production-ready evaluation framework
- Multiplayer synchronization

### [2.0.0] - Planned Q4 2026
- Feature complete for production use
- 80%+ test coverage
- Full documentation
- Performance optimization
- Security audit complete
- Dissertation integration complete

---

For more details on each release, see the [GitHub Releases](https://github.com/SuperInstance/MineWright/releases) page.

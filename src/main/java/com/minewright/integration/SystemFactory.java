package com.minewright.integration;

import com.minewright.testutil.TestLogger;
import org.slf4j.Logger;
import com.minewright.blackboard.Blackboard;
import com.minewright.communication.CommunicationBus;
import com.minewright.config.MineWrightConfig;
import com.minewright.coordination.ContractNetManager;
import com.minewright.decision.TaskPrioritizer;
import com.minewright.decision.UtilityFactors;
import com.minewright.llm.TaskPlanner;
import com.minewright.llm.async.AsyncLLMClient;
import com.minewright.llm.async.AsyncGroqClient;
import com.minewright.llm.async.AsyncOpenAIClient;
import com.minewright.llm.cascade.CascadeRouter;
import com.minewright.llm.cascade.ComplexityAnalyzer;
import com.minewright.llm.cascade.LLMTier;
import com.minewright.skill.SkillLibrary;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Factory for creating and wiring all MineWright systems together.
 *
 * <p><b>Purpose:</b></p>
 * <p>SystemFactory provides a centralized location for creating and configuring
 * all subsystems. It handles dependency injection, ensures singletons are
 * properly initialized, and wires components together correctly.</p>
 *
 * <p><b>Key Responsibilities:</b></p>
 * <ul>
 *   <li>Create all subsystem instances with proper configuration</li>
 *   <li>Handle dependency injection between components</li>
 *   <li>Ensure singleton instances are reused</li>
 *   <li>Provide access to individual systems for testing</li>
 * </ul>
 *
 * <p><b>Usage:</b></p>
 * <pre>
 * // Create the full orchestrator with all subsystems
 * SteveOrchestrator orchestrator = SystemFactory.createOrchestrator();
 *
 * // Or create individual systems for testing
 * SkillLibrary skills = SystemFactory.createSkillLibrary();
 * CascadeRouter router = SystemFactory.createCascadeRouter();
 * </pre>
 *
 * <p><b>Thread Safety:</b></p>
 * <p>All created systems are thread-safe. Factory methods can be called
 * from any thread.</p>
 *
 * @since 1.6.0
 */
public class SystemFactory {
    private static final Logger LOGGER = TestLogger.getLogger(SystemFactory.class);

    // ------------------------------------------------------------------------
    // Singleton Instances
    // ------------------------------------------------------------------------

    private static volatile SkillLibrary sharedSkillLibrary;
    private static volatile CascadeRouter sharedCascadeRouter;
    private static volatile TaskPrioritizer sharedTaskPrioritizer;
    private static volatile ContractNetManager sharedContractNetManager;
    private static volatile Blackboard sharedBlackboard;
    private static volatile CommunicationBus sharedCommunicationBus;
    private static volatile SteveOrchestrator sharedOrchestrator;

    // ------------------------------------------------------------------------
    // Main Factory Methods
    // ------------------------------------------------------------------------

    /**
     * Creates a fully configured SteveOrchestrator with all subsystems.
     *
     * <p>This is the main entry point for creating the complete integration
     * layer. All subsystems are created, configured, and wired together.</p>
     *
     * <p><b>Created Systems:</b></p>
     * <ul>
     *   <li>SkillLibrary - Pattern learning and reuse</li>
     *   <li>CascadeRouter - LLM tier selection and cost optimization</li>
     *   <li>TaskPlanner - LLM-based task planning</li>
     *   <li>TaskPrioritizer - Utility-based task scoring</li>
     *   <li>ContractNetManager - Multi-agent coordination</li>
     *   <li>Blackboard - Shared knowledge system</li>
     *   <li>CommunicationBus - Inter-agent messaging</li>
     * </ul>
     *
     * @return Fully configured SteveOrchestrator
     */
    public static SteveOrchestrator createOrchestrator() {
        if (sharedOrchestrator != null) {
            LOGGER.debug("Returning existing orchestrator instance");
            return sharedOrchestrator;
        }

        synchronized (SystemFactory.class) {
            if (sharedOrchestrator != null) {
                return sharedOrchestrator;
            }

            LOGGER.info("Creating SteveOrchestrator with all subsystems...");

            // Create all subsystems
            SkillLibrary skillLibrary = createSkillLibrary();
            CascadeRouter cascadeRouter = createCascadeRouter();
            TaskPlanner taskPlanner = createTaskPlanner();
            TaskPrioritizer prioritizer = createTaskPrioritizer();
            ContractNetManager contractNet = createContractNetManager();
            Blackboard blackboard = createBlackboard();
            CommunicationBus commBus = createCommunicationBus();

            // Wire them together
            SteveOrchestrator orchestrator = new SteveOrchestrator(
                skillLibrary,
                cascadeRouter,
                taskPlanner,
                prioritizer,
                contractNet,
                blackboard,
                commBus
            );

            sharedOrchestrator = orchestrator;

            LOGGER.info("SteveOrchestrator created successfully");

            return orchestrator;
        }
    }

    /**
     * Creates a minimal orchestrator for testing.
     *
     * <p>This creates an orchestrator with only the essential systems.
     * Useful for unit testing or when full integration is not needed.</p>
     *
     * @return Minimal SteveOrchestrator
     */
    public static SteveOrchestrator createMinimalOrchestrator() {
        LOGGER.debug("Creating minimal orchestrator");

        return new SteveOrchestrator(
            createSkillLibrary(),
            null, // No cascade router
            createTaskPlanner(),
            TaskPrioritizer.withDefaults(),
            createContractNetManager(),
            null, // No blackboard
            null  // No communication bus
        );
    }

    // ------------------------------------------------------------------------
    // Individual System Creation
    // ------------------------------------------------------------------------

    /**
     * Creates or retrieves the singleton SkillLibrary.
     *
     * <p>The SkillLibrary manages learned patterns and built-in skills.
     * It is thread-safe and can be shared across all agents.</p>
     *
     * @return SkillLibrary instance
     */
    public static SkillLibrary createSkillLibrary() {
        if (sharedSkillLibrary == null) {
            synchronized (SystemFactory.class) {
                if (sharedSkillLibrary == null) {
                    sharedSkillLibrary = SkillLibrary.getInstance();
                    LOGGER.info("SkillLibrary created: {} skills",
                        sharedSkillLibrary.getSkillCount());
                }
            }
        }
        return sharedSkillLibrary;
    }

    /**
     * Creates or retrieves the singleton CascadeRouter.
     *
     * <p>The CascadeRouter analyzes task complexity and routes to the
     * appropriate LLM tier for cost optimization.</p>
     *
     * @return CascadeRouter instance
     */
    public static CascadeRouter createCascadeRouter() {
        if (sharedCascadeRouter == null) {
            synchronized (SystemFactory.class) {
                if (sharedCascadeRouter == null) {
                    sharedCascadeRouter = buildCascadeRouter();
                    LOGGER.info("CascadeRouter created with complexity analysis");
                }
            }
        }
        return sharedCascadeRouter;
    }

    /**
     * Creates or retrieves the singleton TaskPlanner.
     *
     * <p>The TaskPlanner handles LLM-based task generation with
     * async support, caching, and resilience patterns.</p>
     *
     * @return TaskPlanner instance
     */
    public static TaskPlanner createTaskPlanner() {
        return new TaskPlanner();
    }

    /**
     * Creates or retrieves the singleton TaskPrioritizer.
     *
     * <p>The TaskPrioritizer implements utility-based AI for scoring
     * and prioritizing tasks based on multiple weighted factors.</p>
     *
     * @return TaskPrioritizer with default factors
     */
    public static TaskPrioritizer createTaskPrioritizer() {
        if (sharedTaskPrioritizer == null) {
            synchronized (SystemFactory.class) {
                if (sharedTaskPrioritizer == null) {
                    sharedTaskPrioritizer = TaskPrioritizer.withDefaults();
                    LOGGER.info("TaskPrioritizer created with {} factors",
                        sharedTaskPrioritizer.getFactorCount());
                }
            }
        }
        return sharedTaskPrioritizer;
    }

    /**
     * Creates or retrieves the singleton ContractNetManager.
     *
     * <p>The ContractNetManager implements the Contract Net Protocol
     * for multi-agent task allocation via bidding.</p>
     *
     * @return ContractNetManager instance
     */
    public static ContractNetManager createContractNetManager() {
        if (sharedContractNetManager == null) {
            synchronized (SystemFactory.class) {
                if (sharedContractNetManager == null) {
                    sharedContractNetManager = new ContractNetManager();
                    LOGGER.info("ContractNetManager created");
                }
            }
        }
        return sharedContractNetManager;
    }

    /**
     * Creates or retrieves the singleton Blackboard.
     *
     * <p>The Blackboard provides a shared knowledge system for
     * inter-agent communication and information sharing.</p>
     *
     * @return Blackboard instance
     */
    public static Blackboard createBlackboard() {
        if (sharedBlackboard == null) {
            synchronized (SystemFactory.class) {
                if (sharedBlackboard == null) {
                    sharedBlackboard = Blackboard.getInstance();
                    LOGGER.info("Blackboard created");
                }
            }
        }
        return sharedBlackboard;
    }

    /**
     * Creates or retrieves the singleton CommunicationBus.
     *
     * <p>The CommunicationBus handles message routing between agents
     * with support for direct messaging and broadcasts.</p>
     *
     * @return CommunicationBus instance
     */
    public static CommunicationBus createCommunicationBus() {
        if (sharedCommunicationBus == null) {
            synchronized (SystemFactory.class) {
                if (sharedCommunicationBus == null) {
                    sharedCommunicationBus = new CommunicationBus();
                    LOGGER.info("CommunicationBus created");
                }
            }
        }
        return sharedCommunicationBus;
    }

    // ------------------------------------------------------------------------
    // System Accessors
    // ------------------------------------------------------------------------

    /**
     * Gets the shared orchestrator instance if created.
     *
     * @return Optional containing the orchestrator, or empty
     */
    public static Optional<SteveOrchestrator> getOrchestrator() {
        return Optional.ofNullable(sharedOrchestrator);
    }

    /**
     * Gets the shared SkillLibrary instance.
     *
     * @return SkillLibrary singleton
     */
    public static SkillLibrary getSkillLibrary() {
        return createSkillLibrary();
    }

    /**
     * Gets the shared CascadeRouter instance.
     *
     * @return CascadeRouter singleton
     */
    public static CascadeRouter getCascadeRouter() {
        return createCascadeRouter();
    }

    /**
     * Gets the shared TaskPrioritizer instance.
     *
     * @return TaskPrioritizer singleton
     */
    public static TaskPrioritizer getTaskPrioritizer() {
        return createTaskPrioritizer();
    }

    /**
     * Gets the shared ContractNetManager instance.
     *
     * @return ContractNetManager singleton
     */
    public static ContractNetManager getContractNetManager() {
        return createContractNetManager();
    }

    /**
     * Gets the shared Blackboard instance.
     *
     * @return Blackboard singleton
     */
    public static Blackboard getBlackboard() {
        return createBlackboard();
    }

    /**
     * Gets the shared CommunicationBus instance.
     *
     * @return CommunicationBus singleton
     */
    public static CommunicationBus getCommunicationBus() {
        return createCommunicationBus();
    }

    // ------------------------------------------------------------------------
    // System Reset
    // ------------------------------------------------------------------------

    /**
     * Resets all singleton instances.
     *
     * <p><b>Warning:</b> This clears all shared state and should only be used
     * for testing or system reset.</p>
     */
    public static void resetAll() {
        LOGGER.warn("Resetting all system singletons");

        synchronized (SystemFactory.class) {
            sharedSkillLibrary = null;
            sharedCascadeRouter = null;
            sharedTaskPrioritizer = null;
            sharedContractNetManager = null;
            sharedBlackboard = null;
            sharedCommunicationBus = null;
            sharedOrchestrator = null;
        }

        LOGGER.info("All system singletons reset");
    }

    /**
     * Resets a specific singleton instance.
     *
     * @param systemClass The class of the system to reset
     */
    public static void reset(Class<?> systemClass) {
        synchronized (SystemFactory.class) {
            if (systemClass == SkillLibrary.class) {
                sharedSkillLibrary = null;
            } else if (systemClass == CascadeRouter.class) {
                sharedCascadeRouter = null;
            } else if (systemClass == TaskPrioritizer.class) {
                sharedTaskPrioritizer = null;
            } else if (systemClass == ContractNetManager.class) {
                sharedContractNetManager = null;
            } else if (systemClass == Blackboard.class) {
                sharedBlackboard = null;
            } else if (systemClass == CommunicationBus.class) {
                sharedCommunicationBus = null;
            } else if (systemClass == SteveOrchestrator.class) {
                sharedOrchestrator = null;
            } else {
                LOGGER.warn("Unknown system class: {}", systemClass.getName());
                return;
            }

            LOGGER.info("Reset singleton: {}", systemClass.getSimpleName());
        }
    }

    // ------------------------------------------------------------------------
    // Private Helpers
    // ------------------------------------------------------------------------

    /**
     * Builds a CascadeRouter with all dependencies configured.
     */
    private static CascadeRouter buildCascadeRouter() {
        // Get LLM cache from TaskPlanner
        TaskPlanner planner = createTaskPlanner();
        var cache = planner.getLLMCache();

        // Create complexity analyzer
        ComplexityAnalyzer analyzer = new ComplexityAnalyzer();

        // Create LLM clients for each tier
        Map<LLMTier, AsyncLLMClient> clients = new HashMap<>();

        String apiKey = MineWrightConfig.OPENAI_API_KEY.get();
        String model = MineWrightConfig.OPENAI_MODEL.get();
        int maxTokens = MineWrightConfig.MAX_TOKENS.get();
        double temperature = MineWrightConfig.TEMPERATURE.get();

        // Local tier (no client - for trivial tasks)
        // Cache tier (handled by cache checks)
        // Fast tier (Groq)
        if (apiKey != null && !apiKey.isEmpty()) {
            clients.put(LLMTier.FAST, new AsyncGroqClient(apiKey, "llama-3.1-8b-instant",
                500, temperature));
        }

        // Balanced tier (OpenAI GPT-3.5)
        if (apiKey != null && !apiKey.isEmpty()) {
            clients.put(LLMTier.BALANCED, new AsyncOpenAIClient(apiKey, "gpt-3.5-turbo",
                maxTokens, temperature));
        }

        // Smart tier (OpenAI GPT-4)
        if (apiKey != null && !apiKey.isEmpty()) {
            clients.put(LLMTier.SMART, new AsyncOpenAIClient(apiKey, "gpt-4",
                maxTokens, temperature));
        }

        return new CascadeRouter(cache, analyzer, clients);
    }
}

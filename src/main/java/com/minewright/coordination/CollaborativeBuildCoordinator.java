package com.minewright.coordination;

import com.minewright.action.CollaborativeBuildManager;
import com.minewright.action.Task;
import com.minewright.entity.CrewManager;
import com.minewright.entity.ForemanEntity;
import com.minewright.structure.BlockPlacement;
import com.minewright.testutil.TestLogger;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import org.slf4j.Logger;
import java.util.*;

/**
 * Integrates Contract Net Protocol with CollaborativeBuildManager for intelligent
 * task allocation in building projects.
 *
 * <p><b>Purpose:</b></p>
 * <p>This coordinator extends the existing CollaborativeBuildManager by using
 * Contract Net Protocol to intelligently allocate building sections to agents
 * based on their capabilities, position, and current load.</p>
 *
 * <p><b>Integration:</b></p>
 * <ul>
 *   <li>Wraps existing CollaborativeBuildManager functionality</li>
 *   <li>Uses CapabilityRegistry to find best agents for each section</li>
 *   <li>Allocates building sections via Contract Net bidding</li>
 *   <li>Monitors progress and handles rebalancing</li>
 * </ul>
 *
 * @see CollaborativeBuildManager
 * @see ContractNetManager
 * @see CapabilityRegistry
 * @since 1.3.0
 */
public class CollaborativeBuildCoordinator {
    /**
     * Logger for this class.
     * Uses TestLogger to avoid triggering MineWrightMod initialization during tests.
     */
    private static final Logger LOGGER = TestLogger.getLogger(CollaborativeBuildCoordinator.class);

    private final ContractNetManager contractNet;
    private final CapabilityRegistry capabilityRegistry;
    private final CrewManager crewManager;
    private final ServerLevel level;

    // Track active building projects
    private final Map<String, BuildProject> activeProjects;

    /**
     * Represents a building project being coordinated.
     */
    public static class BuildProject {
        private final String projectId;
        private final CollaborativeBuildManager.CollaborativeBuild collaborativeBuild;
        private final Map<String, UUID> sectionAssignments; // sectionId -> agentId
        private final Map<UUID, String> agentSections; // agentId -> sectionId
        private final long startTime;
        private final Map<String, BuildSectionTask> pendingTasks;

        public BuildProject(
            String projectId,
            CollaborativeBuildManager.CollaborativeBuild collaborativeBuild
        ) {
            this.projectId = projectId;
            this.collaborativeBuild = collaborativeBuild;
            this.sectionAssignments = new HashMap<>();
            this.agentSections = new HashMap<>();
            this.startTime = System.currentTimeMillis();
            this.pendingTasks = new HashMap<>();
        }

        public String getProjectId() {
            return projectId;
        }

        public CollaborativeBuildManager.CollaborativeBuild getCollaborativeBuild() {
            return collaborativeBuild;
        }

        public boolean isAgentAssigned(UUID agentId) {
            return agentSections.containsKey(agentId);
        }

        public String getAgentSection(UUID agentId) {
            return agentSections.get(agentId);
        }

        public void assignAgent(String sectionId, UUID agentId) {
            // Unassign previous section if any
            String oldSection = agentSections.remove(agentId);
            if (oldSection != null) {
                sectionAssignments.remove(oldSection);
            }

            sectionAssignments.put(sectionId, agentId);
            agentSections.put(agentId, sectionId);
        }

        public void unassignAgent(UUID agentId) {
            String sectionId = agentSections.remove(agentId);
            if (sectionId != null) {
                sectionAssignments.remove(sectionId);
            }
        }

        public List<UUID> getAssignedAgents() {
            return new ArrayList<>(agentSections.keySet());
        }

        public int getAssignedAgentCount() {
            return agentSections.size();
        }

        public long getAgeMillis() {
            return System.currentTimeMillis() - startTime;
        }
    }

    /**
     * Represents a building section task for Contract Net.
     */
    public static class BuildSectionTask extends Task {
        private final String structureId;
        private final String sectionId;
        private final BlockPos sectionCenter;
        private final int blockCount;

        public BuildSectionTask(
            String structureId,
            String sectionId,
            BlockPos sectionCenter,
            int blockCount
        ) {
            super("build_section", Map.of(
                "structureId", structureId,
                "sectionId", sectionId,
                "sectionCenter", sectionCenter,
                "blockCount", blockCount
            ));
            this.structureId = structureId;
            this.sectionId = sectionId;
            this.sectionCenter = sectionCenter;
            this.blockCount = blockCount;
        }

        public String getStructureId() {
            return structureId;
        }

        public String getSectionId() {
            return sectionId;
        }

        public BlockPos getSectionCenter() {
            return sectionCenter;
        }

        public int getBlockCount() {
            return blockCount;
        }
    }

    /**
     * Creates a new collaborative build coordinator.
     *
     * @param contractNet Contract Net manager
     * @param capabilityRegistry Capability registry
     * @param crewManager Crew manager for agent access
     * @param level Server level
     */
    public CollaborativeBuildCoordinator(
        ContractNetManager contractNet,
        CapabilityRegistry capabilityRegistry,
        CrewManager crewManager,
        ServerLevel level
    ) {
        this.contractNet = contractNet;
        this.capabilityRegistry = capabilityRegistry;
        this.crewManager = crewManager;
        this.level = level;
        this.activeProjects = new HashMap<>();
    }

    /**
     * Starts a new collaborative building project using Contract Net allocation.
     *
     * <p>This method:</p>
     * <ol>
     *   <li>Creates a new CollaborativeBuild</li>
     *   <li>Creates tasks for each build section</li>
     *   <li>Announces tasks via Contract Net</li>
     *   <li>Awards sections to best bidders</li>
     * </ol>
     *
     * @param structureType Type of structure
     * @param buildPlan List of block placements
     * @param startPos Starting position
     * @return The project ID
     */
    public String startBuildingProject(
        String structureType,
        List<BlockPlacement> buildPlan,
        BlockPos startPos
    ) {
        // Create the collaborative build
        CollaborativeBuildManager.CollaborativeBuild build =
            CollaborativeBuildManager.registerBuild(structureType, buildPlan, startPos);

        String projectId = structureType + "_" + System.currentTimeMillis();
        BuildProject project = new BuildProject(projectId, build);
        activeProjects.put(projectId, project);

        LOGGER.info("Starting building project {} with {} blocks, {} sections",
            projectId, build.getTotalBlocks(), build.sections.size());

        // Announce each section for bidding
        for (CollaborativeBuildManager.BuildSection section : build.sections) {
            announceBuildSection(projectId, section);
        }

        // Award contracts after bidding period
        scheduleContractAwards(projectId, 35); // 35 seconds

        return projectId;
    }

    /**
     * Announces a build section for bidding.
     */
    private void announceBuildSection(String projectId,
                                     CollaborativeBuildManager.BuildSection section) {
        // Calculate section center for distance requirements
        BlockPos sectionCenter = calculateSectionCenter(section);

        TaskAnnouncement announcement = TaskAnnouncement.builder()
            .task(new BuildSectionTask(
                projectId,
                String.valueOf(section.yLevel),
                sectionCenter,
                section.getTotalBlocks()
            ))
            .requesterId(UUID.randomUUID()) // Coordinator UUID
            .deadlineAfter(30000) // 30 second bidding
            .requireSkill(AgentCapability.Skills.BUILDING)
            .requireSkill(AgentCapability.Skills.PATHFINDING)
            .maxDistance(200.0) // 200 blocks
            .minProficiency(0.3)
            .priority(7) // High priority for building
            .build();

        String announcementId = contractNet.announceTask(announcement.task(),
            announcement.requesterId(), 30000);

        BuildProject project = activeProjects.get(projectId);
        if (project != null) {
            project.pendingTasks.put(announcementId, new BuildSectionTask(
                projectId,
                String.valueOf(section.yLevel),
                sectionCenter,
                section.getTotalBlocks()
            ));
        }

        LOGGER.info("Announced build section {} of project {}",
            section.sectionName, projectId);
    }

    /**
     * Schedules contract awards for a project.
     */
    private void scheduleContractAwards(String projectId, int delaySeconds) {
        java.util.Timer timer = new java.util.Timer();
        timer.schedule(new java.util.TimerTask() {
            @Override
            public void run() {
                awardBuildContracts(projectId);
            }
        }, delaySeconds * 1000L);
    }

    /**
     * Awards contracts for all sections of a project.
     */
    public void awardBuildContracts(String projectId) {
        BuildProject project = activeProjects.get(projectId);

        if (project == null) {
            LOGGER.warn("Project not found for contract award: {}", projectId);
            return;
        }

        LOGGER.info("Awarding contracts for project {} ({} pending sections)",
            projectId, project.pendingTasks.size());

        int awarded = 0;

        for (Map.Entry<String, BuildSectionTask> entry : project.pendingTasks.entrySet()) {
            String announcementId = entry.getKey();
            BuildSectionTask task = entry.getValue();

            Optional<com.minewright.coordination.TaskBid> winner =
                contractNet.awardToBestBidder(announcementId);

            if (winner.isPresent()) {
                com.minewright.coordination.TaskBid bid = winner.get();
                UUID agentId = bid.bidderId();

                // Assign the section to the winner
                project.assignAgent(task.getSectionId(), agentId);

                // Send the build task to the agent
                sendBuildTaskToAgent(agentId, task, projectId);

                awarded++;

                LOGGER.info("Awarded section {} of project {} to agent {} (score: {:.2f})",
                    task.getSectionId(), projectId, agentId.toString().substring(0, 8),
                    bid.score());
            } else {
                LOGGER.warn("No bids for section {} of project {}",
                    task.getSectionId(), projectId);
            }
        }

        project.pendingTasks.clear();

        LOGGER.info("Awarded {} sections for project {}", awarded, projectId);
    }

    /**
     * Sends a build task to an agent.
     */
    private void sendBuildTaskToAgent(UUID agentId, BuildSectionTask task, String projectId) {
        ForemanEntity agent = crewManager.getCrewMember(agentId);

        if (agent == null) {
            LOGGER.warn("Agent not found for build task: {}",
                agentId.toString().substring(0, 8));
            return;
        }

        // Create and queue the build task
        Task buildTask = new Task("build", Map.of(
            "structureId", task.getStructureId(),
            "sectionId", task.getSectionId(),
            "coordinated", true
        ));

        agent.getActionExecutor().queueTask(buildTask);

        LOGGER.info("Sent build task to agent {} for section {} of project {}",
            agent.getEntityName(), task.getSectionId(), projectId);
    }

    /**
     * Handles a build section completion.
     *
     * @param agentId Agent that completed the section
     * @param projectId Project ID
     * @param sectionId Section ID
     * @param success Whether the section was built successfully
     */
    public void handleSectionComplete(UUID agentId, String projectId,
                                      String sectionId, boolean success) {
        BuildProject project = activeProjects.get(projectId);

        if (project == null) {
            return;
        }

        if (success) {
            // Check if project is complete
            CollaborativeBuildManager.CollaborativeBuild build = project.getCollaborativeBuild();

            if (build.isComplete()) {
                LOGGER.info("Building project {} completed!", projectId);
                CollaborativeBuildManager.completeBuild(build.structureId);
                activeProjects.remove(projectId);
            } else {
                // Agent may be available for more work
                // Could rebalance or assign another section
                LOGGER.info("Section {} completed by agent {} in project {}",
                    sectionId, agentId.toString().substring(0, 8), projectId);
            }
        } else {
            // Section failed - could retry with different agent
            LOGGER.warn("Section {} failed for agent {} in project {}",
                sectionId, agentId.toString().substring(0, 8), projectId);

            // Unassign and potentially reannounce
            project.unassignAgent(agentId);
        }
    }

    /**
     * Rebalances a project by reassigning sections when agents become available.
     *
     * @param projectId Project to rebalance
     */
    public void rebalanceProject(String projectId) {
        BuildProject project = activeProjects.get(projectId);

        if (project == null) {
            return;
        }

        CollaborativeBuildManager.CollaborativeBuild build = project.getCollaborativeBuild();

        // Find incomplete sections
        List<CollaborativeBuildManager.BuildSection> incompleteSections = new ArrayList<>();
        for (CollaborativeBuildManager.BuildSection section : build.sections) {
            if (!section.isComplete()) {
                incompleteSections.add(section);
            }
        }

        if (incompleteSections.isEmpty()) {
            return;
        }

        LOGGER.info("Rebalancing project {} - {} incomplete sections",
            projectId, incompleteSections.size());

        // Find available agents and reannounce sections
        for (CollaborativeBuildManager.BuildSection section : incompleteSections) {
            String sectionId = String.valueOf(section.yLevel);

            // Skip if already assigned
            if (project.sectionAssignments.containsKey(sectionId)) {
                continue;
            }

            // Reannounce for bidding
            announceBuildSection(projectId, section);
        }

        // Schedule new contract awards
        scheduleContractAwards(projectId, 10); // 10 seconds
    }

    /**
     * Gets an active project by ID.
     */
    public BuildProject getProject(String projectId) {
        return activeProjects.get(projectId);
    }

    /**
     * Gets all active projects.
     */
    public Collection<BuildProject> getActiveProjects() {
        return new ArrayList<>(activeProjects.values());
    }

    /**
     * Calculates the center position of a build section.
     */
    private BlockPos calculateSectionCenter(CollaborativeBuildManager.BuildSection section) {
        if (section.blocks.isEmpty()) {
            return BlockPos.ZERO;
        }

        // Average position of all blocks in section
        long sumX = 0, sumY = 0, sumZ = 0;
        int count = 0;

        for (BlockPlacement placement : section.blocks) {
            sumX += placement.pos.getX();
            sumY += placement.pos.getY();
            sumZ += placement.pos.getZ();
            count++;
        }

        return new BlockPos(
            (int) (sumX / count),
            (int) (sumY / count),
            (int) (sumZ / count)
        );
    }

    /**
     * Cleans up completed/stale projects.
     *
     * @return Number of projects cleaned up
     */
    public int cleanup() {
        long now = System.currentTimeMillis();
        int cleaned = 0;

        Iterator<Map.Entry<String, BuildProject>> it = activeProjects.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, BuildProject> entry = it.next();
            BuildProject project = entry.getValue();

            boolean shouldRemove = false;

            // Remove if build is complete and older than 5 minutes
            if (project.getCollaborativeBuild().isComplete()) {
                long age = now - project.startTime;
                shouldRemove = age > 300000;
            }
            // Remove if stale (no activity for 10 minutes)
            else if (project.getAgeMillis() > 600000) {
                shouldRemove = true;
                LOGGER.warn("Cleaning up stale project: {}", project.projectId);
            }

            if (shouldRemove) {
                it.remove();
                cleaned++;
            }
        }

        if (cleaned > 0) {
            LOGGER.debug("Cleaned up {} building projects", cleaned);
        }

        return cleaned;
    }
}

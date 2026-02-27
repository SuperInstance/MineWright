package com.minewright.entity;

import com.minewright.MineWrightMod;
import com.minewright.config.MineWrightConfig;
import com.minewright.orchestration.OrchestratorService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CrewManager {
    private final Map<String, ForemanEntity> activeCrewMembers;
    private final Map<UUID, ForemanEntity> crewMembersByUUID;

    public CrewManager() {
        this.activeCrewMembers = new ConcurrentHashMap<>();
        this.crewMembersByUUID = new ConcurrentHashMap<>();
    }

    public ForemanEntity spawnCrewMember(ServerLevel level, Vec3 position, String name) {
        MineWrightMod.LOGGER.info("Current active crew members: {}", activeCrewMembers.size());

        if (activeCrewMembers.containsKey(name)) {
            MineWrightMod.LOGGER.warn("Crew member name '{}' already exists", name);
            return null;
        }
        int maxCrewMembers = MineWrightConfig.MAX_ACTIVE_CREW_MEMBERS.get();
        if (activeCrewMembers.size() >= maxCrewMembers) {
            MineWrightMod.LOGGER.warn("Max crew member limit reached: {}", maxCrewMembers);
            return null;
        }
        ForemanEntity crewMember;
        try {
            MineWrightMod.LOGGER.info("EntityType: {}", MineWrightMod.FOREMAN_ENTITY.get());
            crewMember = new ForemanEntity(MineWrightMod.FOREMAN_ENTITY.get(), level);
        } catch (Throwable e) {
            MineWrightMod.LOGGER.error("Failed to create crew member entity", e);
            MineWrightMod.LOGGER.error("Exception class: {}", e.getClass().getName());
            MineWrightMod.LOGGER.error("Exception message: {}", e.getMessage());
            e.printStackTrace();
            return null;
        }

        try {
            crewMember.setEntityName(name);
            crewMember.setPos(position.x, position.y, position.z);
            boolean added = level.addFreshEntity(crewMember);
            if (added) {
                activeCrewMembers.put(name, crewMember);
                crewMembersByUUID.put(crewMember.getUUID(), crewMember);
                MineWrightMod.LOGGER.info("Successfully spawned crew member: {} with UUID {} at {}", name, crewMember.getUUID(), position);
                return crewMember;
            } else {
                MineWrightMod.LOGGER.error("Failed to add crew member entity to world (addFreshEntity returned false)");
                MineWrightMod.LOGGER.error("=== SPAWN ATTEMPT FAILED ===");
            }
        } catch (Throwable e) {
            MineWrightMod.LOGGER.error("Exception during spawn setup", e);
            MineWrightMod.LOGGER.error("=== SPAWN ATTEMPT FAILED WITH EXCEPTION ===");
            e.printStackTrace();
        }

        return null;
    }

    public ForemanEntity getCrewMember(String name) {
        return activeCrewMembers.get(name);
    }

    public ForemanEntity getCrewMember(UUID uuid) {
        return crewMembersByUUID.get(uuid);
    }

    // Deprecated: Use getCrewMember() instead
    public ForemanEntity getSteve(String name) {
        return activeCrewMembers.get(name);
    }

    // Deprecated: Use getCrewMember() instead
    public ForemanEntity getSteve(UUID uuid) {
        return crewMembersByUUID.get(uuid);
    }

    // Deprecated: Use spawnCrewMember() instead
    public ForemanEntity spawnSteve(ServerLevel level, Vec3 position, String name) {
        return spawnCrewMember(level, position, name);
    }

    public boolean removeCrewMember(String name) {
        ForemanEntity crewMember = activeCrewMembers.remove(name);
        if (crewMember != null) {
            crewMembersByUUID.remove(crewMember.getUUID());

            // Unregister from orchestrator before discarding
            OrchestratorService orchestrator = MineWrightMod.getOrchestratorService();
            if (orchestrator != null) {
                orchestrator.unregisterAgent(name);
            }

            crewMember.discard();
            return true;
        }
        return false;
    }

    // Deprecated: Use removeCrewMember() instead
    public boolean removeSteve(String name) {
        return removeCrewMember(name);
    }

    public void clearAllCrewMembers() {
        MineWrightMod.LOGGER.info("Clearing {} crew member entities", activeCrewMembers.size());

        // Unregister all from orchestrator
        OrchestratorService orchestrator = MineWrightMod.getOrchestratorService();
        if (orchestrator != null) {
            for (String name : activeCrewMembers.keySet()) {
                orchestrator.unregisterAgent(name);
            }
        }

        for (ForemanEntity crewMember : activeCrewMembers.values()) {
            crewMember.discard();
        }
        activeCrewMembers.clear();
        crewMembersByUUID.clear();
    }

    // Deprecated: Use clearAllCrewMembers() instead
    public void clearAllSteves() {
        clearAllCrewMembers();
    }

    public Collection<ForemanEntity> getAllCrewMembers() {
        return Collections.unmodifiableCollection(activeCrewMembers.values());
    }

    // Deprecated: Use getAllCrewMembers() instead
    public Collection<ForemanEntity> getAllSteves() {
        return Collections.unmodifiableCollection(activeCrewMembers.values());
    }

    public List<String> getCrewMemberNames() {
        return new ArrayList<>(activeCrewMembers.keySet());
    }

    // Deprecated: Use getCrewMemberNames() instead
    public List<String> getSteveNames() {
        return getCrewMemberNames();
    }

    public int getActiveCount() {
        return activeCrewMembers.size();
    }

    public void tick(ServerLevel level) {
        // Clean up dead or removed crew members
        Iterator<Map.Entry<String, ForemanEntity>> iterator = activeCrewMembers.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, ForemanEntity> entry = iterator.next();
            ForemanEntity crewMember = entry.getValue();

            if (!crewMember.isAlive() || crewMember.isRemoved()) {
                iterator.remove();
                crewMembersByUUID.remove(crewMember.getUUID());

                // Unregister from orchestrator
                OrchestratorService orchestrator = MineWrightMod.getOrchestratorService();
                if (orchestrator != null) {
                    orchestrator.unregisterAgent(entry.getKey());
                }

                MineWrightMod.LOGGER.info("Cleaned up crew member: {}", entry.getKey());
            }
        }
    }
}


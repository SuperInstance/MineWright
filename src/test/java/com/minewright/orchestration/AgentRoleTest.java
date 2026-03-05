package com.minewright.orchestration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for {@link AgentRole}.
 *
 * <p>Tests cover the agent role enumeration including:</p>
 * <ul>
 *   <li>Role display names</li>
 *   <li>Orchestration capabilities</li>
 *   <li>Task execution capabilities</li>
 *   <li>Team participation</li>
 *   <li>Role properties and methods</li>
 *   <li>Enum values and codes</li>
 * </ul>
 *
 * @see AgentRole
 */
@DisplayName("Agent Role Tests")
class AgentRoleTest {

    // ==================== Enum Values Tests ====================

    @Nested
    @DisplayName("Enum Values Tests")
    class EnumValuesTests {

        @Test
        @DisplayName("All role values are accessible")
        void allRoleValues() {
            AgentRole[] roles = AgentRole.values();

            assertEquals(4, roles.length, "Should have 4 roles");
        }

        @Test
        @DisplayName("Roles include FOREMAN")
        void hasForemanRole() {
            assertNotNull(AgentRole.valueOf("FOREMAN"));
        }

        @Test
        @DisplayName("Roles include WORKER")
        void hasWorkerRole() {
            assertNotNull(AgentRole.valueOf("WORKER"));
        }

        @Test
        @DisplayName("Roles include SPECIALIST")
        void hasSpecialistRole() {
            assertNotNull(AgentRole.valueOf("SPECIALIST"));
        }

        @Test
        @DisplayName("Roles include SOLO")
        void hasSoloRole() {
            assertNotNull(AgentRole.valueOf("SOLO"));
        }

        @Test
        @DisplayName("ValueOf throws exception for invalid role")
        void valueOfThrowsForInvalid() {
            assertThrows(IllegalArgumentException.class, () -> {
                AgentRole.valueOf("INVALID_ROLE");
            }, "valueOf should throw for invalid role name");
        }
    }

    // ==================== Display Name Tests ====================

    @Nested
    @DisplayName("Display Name Tests")
    class DisplayNameTests {

        @Test
        @DisplayName("FOREMAN display name")
        void foremanDisplayName() {
            assertEquals("Foreman", AgentRole.FOREMAN.getDisplayName());
        }

        @Test
        @DisplayName("WORKER display name")
        void workerDisplayName() {
            assertEquals("Worker", AgentRole.WORKER.getDisplayName());
        }

        @Test
        @DisplayName("SPECIALIST display name")
        void specialistDisplayName() {
            assertEquals("Specialist", AgentRole.SPECIALIST.getDisplayName());
        }

        @Test
        @DisplayName("SOLO display name")
        void soloDisplayName() {
            assertEquals("Solo", AgentRole.SOLO.getDisplayName());
        }

        @Test
        @DisplayName("All display names are non-null")
        void allDisplayNamesNonNull() {
            for (AgentRole role : AgentRole.values()) {
                assertNotNull(role.getDisplayName(),
                    role.name() + " should have display name");
                assertFalse(role.getDisplayName().isEmpty(),
                    role.name() + " display name should not be empty");
            }
        }

        @Test
        @DisplayName("All display names are unique")
        void allDisplayNamesUnique() {
            Set<String> displayNames = new java.util.HashSet<>();

            for (AgentRole role : AgentRole.values()) {
                assertTrue(displayNames.add(role.getDisplayName()),
                    role.name() + " display name should be unique");
            }
        }
    }

    // ==================== Orchestration Capability Tests ====================

    @Nested
    @DisplayName("Orchestration Capability Tests")
    class OrchestrationCapabilityTests {

        @Test
        @DisplayName("FOREMAN can orchestrate")
        void foremanCanOrchestrate() {
            assertTrue(AgentRole.FOREMAN.canOrchestrate(),
                "FOREMAN should be able to orchestrate");
        }

        @Test
        @DisplayName("WORKER cannot orchestrate")
        void workerCannotOrchestrate() {
            assertFalse(AgentRole.WORKER.canOrchestrate(),
                "WORKER should not be able to orchestrate");
        }

        @Test
        @DisplayName("SPECIALIST cannot orchestrate")
        void specialistCannotOrchestrate() {
            assertFalse(AgentRole.SPECIALIST.canOrchestrate(),
                "SPECIALIST should not be able to orchestrate");
        }

        @Test
        @DisplayName("SOLO cannot orchestrate")
        void soloCannotOrchestrate() {
            assertFalse(AgentRole.SOLO.canOrchestrate(),
                "SOLO should not be able to orchestrate");
        }

        @Test
        @DisplayName("Only FOREMAN can orchestrate")
        void onlyForemanCanOrchestrate() {
            int orchestratorCount = 0;

            for (AgentRole role : AgentRole.values()) {
                if (role.canOrchestrate()) {
                    orchestratorCount++;
                }
            }

            assertEquals(1, orchestratorCount,
                "Only one role should be able to orchestrate");
        }
    }

    // ==================== Task Execution Capability Tests ====================

    @Nested
    @DisplayName("Task Execution Capability Tests")
    class TaskExecutionCapabilityTests {

        @Test
        @DisplayName("FOREMAN can execute tasks")
        void foremanCanExecuteTasks() {
            assertTrue(AgentRole.FOREMAN.canExecuteTasks(),
                "FOREMAN should be able to execute tasks");
        }

        @Test
        @DisplayName("WORKER can execute tasks")
        void workerCanExecuteTasks() {
            assertTrue(AgentRole.WORKER.canExecuteTasks(),
                "WORKER should be able to execute tasks");
        }

        @Test
        @DisplayName("SPECIALIST can execute tasks")
        void specialistCanExecuteTasks() {
            assertTrue(AgentRole.SPECIALIST.canExecuteTasks(),
                "SPECIALIST should be able to execute tasks");
        }

        @Test
        @DisplayName("SOLO cannot execute tasks")
        void soloCannotExecuteTasks() {
            assertFalse(AgentRole.SOLO.canExecuteTasks(),
                "SOLO should not be able to execute tasks");
        }

        @Test
        @DisplayName("Three roles can execute tasks")
        void threeRolesCanExecuteTasks() {
            int executorCount = 0;

            for (AgentRole role : AgentRole.values()) {
                if (role.canExecuteTasks()) {
                    executorCount++;
                }
            }

            assertEquals(3, executorCount,
                "Three roles should be able to execute tasks");
        }
    }

    // ==================== Team Participation Tests ====================

    @Nested
    @DisplayName("Team Participation Tests")
    class TeamParticipationTests {

        @Test
        @DisplayName("FOREMAN is part of team")
        void foremanIsPartOfTeam() {
            assertTrue(AgentRole.FOREMAN.isPartOfTeam(),
                "FOREMAN should be part of team");
        }

        @Test
        @DisplayName("WORKER is part of team")
        void workerIsPartOfTeam() {
            assertTrue(AgentRole.WORKER.isPartOfTeam(),
                "WORKER should be part of team");
        }

        @Test
        @DisplayName("SPECIALIST is part of team")
        void specialistIsPartOfTeam() {
            assertTrue(AgentRole.SPECIALIST.isPartOfTeam(),
                "SPECIALIST should be part of team");
        }

        @Test
        @DisplayName("SOLO is not part of team")
        void soloIsNotPartOfTeam() {
            assertFalse(AgentRole.SOLO.isPartOfTeam(),
                "SOLO should not be part of team");
        }

        @Test
        @DisplayName("Three roles are part of team")
        void threeRolesPartOfTeam() {
            int teamMemberCount = 0;

            for (AgentRole role : AgentRole.values()) {
                if (role.isPartOfTeam()) {
                    teamMemberCount++;
                }
            }

            assertEquals(3, teamMemberCount,
                "Three roles should be part of team");
        }
    }

    // ==================== Role Property Combinations ====================

    @Nested
    @DisplayName("Role Property Combinations")
    class RolePropertyCombinationsTests {

        @Test
        @DisplayName("FOREMAN has all capabilities")
        void foremanCapabilities() {
            assertTrue(AgentRole.FOREMAN.canOrchestrate());
            assertTrue(AgentRole.FOREMAN.canExecuteTasks());
            assertTrue(AgentRole.FOREMAN.isPartOfTeam());
        }

        @Test
        @DisplayName("WORKER can execute but not orchestrate")
        void workerCapabilities() {
            assertFalse(AgentRole.WORKER.canOrchestrate());
            assertTrue(AgentRole.WORKER.canExecuteTasks());
            assertTrue(AgentRole.WORKER.isPartOfTeam());
        }

        @Test
        @DisplayName("SPECIALIST can execute but not orchestrate")
        void specialistCapabilities() {
            assertFalse(AgentRole.SPECIALIST.canOrchestrate());
            assertTrue(AgentRole.SPECIALIST.canExecuteTasks());
            assertTrue(AgentRole.SPECIALIST.isPartOfTeam());
        }

        @Test
        @DisplayName("SOLO has no team capabilities")
        void soloCapabilities() {
            assertFalse(AgentRole.SOLO.canOrchestrate());
            assertFalse(AgentRole.SOLO.canExecuteTasks());
            assertFalse(AgentRole.SOLO.isPartOfTeam());
        }
    }

    // ==================== ToString Tests ====================

    @Nested
    @DisplayName("ToString Tests")
    class ToStringTests {

        @Test
        @DisplayName("ToString returns enum name")
        void toStringReturnsEnumName() {
            assertEquals("FOREMAN", AgentRole.FOREMAN.toString());
            assertEquals("WORKER", AgentRole.WORKER.toString());
            assertEquals("SPECIALIST", AgentRole.SPECIALIST.toString());
            assertEquals("SOLO", AgentRole.SOLO.toString());
        }

        @Test
        @DisplayName("ToString is consistent with valueOf")
        void toStringConsistentWithValueOf() {
            for (AgentRole role : AgentRole.values()) {
                assertEquals(role, AgentRole.valueOf(role.toString()),
                    "toString should be consistent with valueOf for " + role.name());
            }
        }
    }

    // ==================== Ordinal Tests ====================

    @Nested
    @DisplayName("Ordinal Tests")
    class OrdinalTests {

        @Test
        @DisplayName("Role ordinals are sequential")
        void roleOrdinalsSequential() {
            assertEquals(0, AgentRole.FOREMAN.ordinal());
            assertEquals(1, AgentRole.WORKER.ordinal());
            assertEquals(2, AgentRole.SPECIALIST.ordinal());
            assertEquals(3, AgentRole.SOLO.ordinal());
        }

        @Test
        @DisplayName("Roles can be compared by ordinal")
        void rolesComparableByOrdinal() {
            assertTrue(AgentRole.FOREMAN.ordinal() < AgentRole.WORKER.ordinal());
            assertTrue(AgentRole.WORKER.ordinal() < AgentRole.SPECIALIST.ordinal());
            assertTrue(AgentRole.SPECIALIST.ordinal() < AgentRole.SOLO.ordinal());
        }
    }

    // ==================== Switch Statement Tests ====================

    @Nested
    @DisplayName("Switch Statement Tests")
    class SwitchStatementTests {

        @Test
        @DisplayName("Switch on FOREMAN")
        void switchOnForeman() {
            String result = switchOnRole(AgentRole.FOREMAN);
            assertEquals("FOREMAN", result);
        }

        @Test
        @DisplayName("Switch on WORKER")
        void switchOnWorker() {
            String result = switchOnRole(AgentRole.WORKER);
            assertEquals("WORKER", result);
        }

        @Test
        @DisplayName("Switch on SPECIALIST")
        void switchOnSpecialist() {
            String result = switchOnRole(AgentRole.SPECIALIST);
            assertEquals("SPECIALIST", result);
        }

        @Test
        @DisplayName("Switch on SOLO")
        void switchOnSolo() {
            String result = switchOnRole(AgentRole.SOLO);
            assertEquals("SOLO", result);
        }

        @Test
        @DisplayName("Switch with default case")
        void switchWithDefault() {
            // All roles should be covered without default
            for (AgentRole role : AgentRole.values()) {
                String result = switchOnRole(role);
                assertNotNull(result, "All roles should be handled");
            }
        }

        private String switchOnRole(AgentRole role) {
            return switch (role) {
                case FOREMAN -> "FOREMAN";
                case WORKER -> "WORKER";
                case SPECIALIST -> "SPECIALIST";
                case SOLO -> "SOLO";
            };
        }
    }

    // ==================== Use Case Tests ====================

    @Nested
    @DisplayName("Use Case Tests")
    class UseCaseTests {

        @Test
        @DisplayName("Filter orchestration capable roles")
        void filterOrchestrationCapable() {
            java.util.List<AgentRole> orchestrators = new java.util.ArrayList<>();

            for (AgentRole role : AgentRole.values()) {
                if (role.canOrchestrate()) {
                    orchestrators.add(role);
                }
            }

            assertEquals(1, orchestrators.size());
            assertEquals(AgentRole.FOREMAN, orchestrators.get(0));
        }

        @Test
        @DisplayName("Filter task execution capable roles")
        void filterTaskExecutionCapable() {
            java.util.List<AgentRole> executors = new java.util.ArrayList<>();

            for (AgentRole role : AgentRole.values()) {
                if (role.canExecuteTasks()) {
                    executors.add(role);
                }
            }

            assertEquals(3, executors.size());
            assertTrue(executors.contains(AgentRole.FOREMAN));
            assertTrue(executors.contains(AgentRole.WORKER));
            assertTrue(executors.contains(AgentRole.SPECIALIST));
        }

        @Test
        @DisplayName("Filter team member roles")
        void filterTeamMembers() {
            java.util.List<AgentRole> teamMembers = new java.util.ArrayList<>();

            for (AgentRole role : AgentRole.values()) {
                if (role.isPartOfTeam()) {
                    teamMembers.add(role);
                }
            }

            assertEquals(3, teamMembers.size());
            assertTrue(teamMembers.contains(AgentRole.FOREMAN));
            assertTrue(teamMembers.contains(AgentRole.WORKER));
            assertTrue(teamMembers.contains(AgentRole.SPECIALIST));
        }

        @Test
        @DisplayName("Find role by display name")
        void findRoleByDisplayName() {
            AgentRole found = null;

            for (AgentRole role : AgentRole.values()) {
                if ("Worker".equals(role.getDisplayName())) {
                    found = role;
                    break;
                }
            }

            assertEquals(AgentRole.WORKER, found);
        }

        @Test
        @DisplayName("Get all display names")
        void getAllDisplayNames() {
            java.util.List<String> displayNames = new java.util.ArrayList<>();

            for (AgentRole role : AgentRole.values()) {
                displayNames.add(role.getDisplayName());
            }

            assertEquals(4, displayNames.size());
            assertTrue(displayNames.contains("Foreman"));
            assertTrue(displayNames.contains("Worker"));
            assertTrue(displayNames.contains("Specialist"));
            assertTrue(displayNames.contains("Solo"));
        }
    }

    // ==================== Edge Cases Tests ====================

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Enum constants are immutable")
        void enumConstantsImmutable() {
            AgentRole foreman1 = AgentRole.FOREMAN;
            AgentRole foreman2 = AgentRole.FOREMAN;

            assertSame(foreman1, foreman2,
                "Enum constants should be same instance");
        }

        @Test
        @DisplayName("Enum constants are same instance")
        void enumConstantsSameInstance() {
            AgentRole role1 = AgentRole.WORKER;
            AgentRole role2 = AgentRole.WORKER;
            assertSame(role1, role2,
                "Enum constants should be same instance");
        }

        @Test
        @DisplayName("Equals is consistent")
        void equalsConsistent() {
            AgentRole role1 = AgentRole.SPECIALIST;
            AgentRole role2 = AgentRole.SPECIALIST;
            AgentRole role3 = AgentRole.SOLO;

            assertEquals(role1, role2);
            assertNotEquals(role1, role3);
        }

        @Test
        @DisplayName("HashCode is consistent")
        void hashCodeConsistent() {
            AgentRole role1 = AgentRole.FOREMAN;
            AgentRole role2 = AgentRole.FOREMAN;

            assertEquals(role1.hashCode(), role2.hashCode());
        }

        @Test
        @DisplayName("CompareTo works correctly")
        void compareToWorksCorrectly() {
            assertTrue(AgentRole.FOREMAN.compareTo(AgentRole.WORKER) < 0);
            assertTrue(AgentRole.WORKER.compareTo(AgentRole.SPECIALIST) < 0);
            assertTrue(AgentRole.SPECIALIST.compareTo(AgentRole.SOLO) < 0);

            assertEquals(0, AgentRole.FOREMAN.compareTo(AgentRole.FOREMAN));
        }

        @Test
        @DisplayName("GetClass returns AgentRole class")
        void getClassReturnsAgentRoleClass() {
            assertEquals(AgentRole.class, AgentRole.WORKER.getClass());
        }

        @Test
        @DisplayName("Name method returns enum name")
        void nameMethodReturnsEnumName() {
            assertEquals("FOREMAN", AgentRole.FOREMAN.name());
            assertEquals("WORKER", AgentRole.WORKER.name());
            assertEquals("SPECIALIST", AgentRole.SPECIALIST.name());
            assertEquals("SOLO", AgentRole.SOLO.name());
        }
    }

    // ==================== Integration Pattern Tests ====================

    @Nested
    @DisplayName("Integration Pattern Tests")
    class IntegrationPatternTests {

        @Test
        @DisplayName("Role hierarchy matches code comments")
        void roleHierarchyMatchesComments() {
            // Based on the documented hierarchy:
            // HUMAN -> FOREMAN -> WORKER/SPECIALIST
            // SOLO is independent

            assertTrue(AgentRole.FOREMAN.canOrchestrate(),
                "FOREMAN should be at top of hierarchy");
            assertTrue(AgentRole.FOREMAN.canExecuteTasks(),
                "FOREMAN can also execute tasks");
            assertTrue(AgentRole.FOREMAN.isPartOfTeam(),
                "FOREMAN is part of team");

            assertTrue(AgentRole.WORKER.canExecuteTasks() &&
                       !AgentRole.WORKER.canOrchestrate(),
                "WORKER executes but doesn't orchestrate");

            assertTrue(AgentRole.SPECIALIST.canExecuteTasks() &&
                       !AgentRole.SPECIALIST.canOrchestrate(),
                "SPECIALIST executes but doesn't orchestrate");

            assertFalse(AgentRole.SOLO.isPartOfTeam(),
                "SOLO operates independently");
        }

        @Test
        @DisplayName("Only one foreman per team pattern")
        void onlyOneForemanPerTeam() {
            // Count orchestration-capable roles
            long foremanCount = java.util.Arrays.stream(AgentRole.values())
                .filter(AgentRole::canOrchestrate)
                .count();

            assertEquals(1, foremanCount,
                "Only one role should be able to orchestrate (single foreman pattern)");
        }

        @Test
        @DisplayName("Worker and specialist are both executors")
        void workerAndSpecialistBothExecutors() {
            assertTrue(AgentRole.WORKER.canExecuteTasks(),
                "WORKER should execute tasks");
            assertTrue(AgentRole.SPECIALIST.canExecuteTasks(),
                "SPECIALIST should execute tasks");

            // Both are team members
            assertTrue(AgentRole.WORKER.isPartOfTeam());
            assertTrue(AgentRole.SPECIALIST.isPartOfTeam());
        }

        @Test
        @DisplayName("Solo is for backward compatibility")
        void soloForBackwardCompatibility() {
            // SOLO represents backward compatibility mode
            assertFalse(AgentRole.SOLO.canOrchestrate(),
                "SOLO doesn't need orchestration (no multi-agent)");
            assertFalse(AgentRole.SOLO.canExecuteTasks(),
                "SOLO doesn't execute in team context");
            assertFalse(AgentRole.SOLO.isPartOfTeam(),
                "SOLO operates independently of team");
        }

        @Test
        @DisplayName("Role capabilities are mutually exclusive where appropriate")
        void roleCapabilitiesMutuallyExclusive() {
            // No role should both orchestrate and not be part of team
            for (AgentRole role : AgentRole.values()) {
                if (role.canOrchestrate()) {
                    assertTrue(role.isPartOfTeam(),
                        role.name() + " orchestrates but is not part of team");
                }
            }

            // SOLO should not orchestrate or execute (it's independent)
            AgentRole solo = AgentRole.SOLO;
            assertFalse(solo.canOrchestrate() || solo.canExecuteTasks() || solo.isPartOfTeam(),
                "SOLO should have no team capabilities");
        }
    }

    // ==================== Documentation Consistency Tests ====================

    @Nested
    @DisplayName("Documentation Consistency Tests")
    class DocumentationConsistencyTests {

        @Test
        @DisplayName("FOREMAN matches documented responsibilities")
        void foremanMatchesDocumentation() {
            // From javadoc:
            // - Receives high-level commands
            // - Decomposes goals into distributable tasks
            // - Assigns tasks to workers
            // - Monitors progress and handles failures
            // - Reports status back to human
            // - Can perform tasks if no workers available

            assertTrue(AgentRole.FOREMAN.canOrchestrate(),
                "Should orchestrate (assign tasks, monitor progress)");
            assertTrue(AgentRole.FOREMAN.canExecuteTasks(),
                "Can perform tasks if no workers available");
            assertTrue(AgentRole.FOREMAN.isPartOfTeam(),
                "Part of the team structure");
        }

        @Test
        @DisplayName("WORKER matches documented responsibilities")
        void workerMatchesDocumentation() {
            // From javadoc:
            // - Executes tasks assigned by foreman
            // - Reports progress and completion
            // - Requests help when stuck
            // - Can coordinate with other workers

            assertFalse(AgentRole.WORKER.canOrchestrate(),
                "Should not orchestrate (follows foreman)");
            assertTrue(AgentRole.WORKER.canExecuteTasks(),
                "Executes assigned tasks");
            assertTrue(AgentRole.WORKER.isPartOfTeam(),
                "Part of the team structure");
        }

        @Test
        @DisplayName("SPECIALIST matches documented responsibilities")
        void specialistMatchesDocumentation() {
            // From javadoc:
            // - Handles specific types of tasks
            // - Can provide guidance to other agents
            // - May have enhanced capabilities in domain

            assertFalse(AgentRole.SPECIALIST.canOrchestrate(),
                "Should not orchestrate");
            assertTrue(AgentRole.SPECIALIST.canExecuteTasks(),
                "Handles specific types of tasks");
            assertTrue(AgentRole.SPECIALIST.isPartOfTeam(),
                "Part of the team structure");
        }

        @Test
        @DisplayName("SOLO matches documented responsibilities")
        void soloMatchesDocumentation() {
            // From javadoc:
            // - Agent operating independently without orchestration
            // - Used for backward compatibility

            assertFalse(AgentRole.SOLO.canOrchestrate(),
                "Independent - no orchestration");
            assertFalse(AgentRole.SOLO.canExecuteTasks(),
                "Independent - no team task execution");
            assertFalse(AgentRole.SOLO.isPartOfTeam(),
                "Independent - not part of team");
        }

        @Test
        @DisplayName("Display names match documentation")
        void displayNamesMatchDocumentation() {
            assertEquals("Foreman", AgentRole.FOREMAN.getDisplayName());
            assertEquals("Worker", AgentRole.WORKER.getDisplayName());
            assertEquals("Specialist", AgentRole.SPECIALIST.getDisplayName());
            assertEquals("Solo", AgentRole.SOLO.getDisplayName());
        }
    }

    // ==================== Serialization Tests ====================

    @Nested
    @DisplayName("Serialization Tests")
    class SerializationTests {

        @Test
        @DisplayName("Enum is serializable")
        void enumIsSerializable() throws java.io.IOException {
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(baos);

            oos.writeObject(AgentRole.FOREMAN);
            oos.close();

            assertTrue(baos.size() > 0, "Enum should be serializable");
        }

        @Test
        @DisplayName("Enum deserializes to same instance")
        void enumDeserializesToSameInstance() throws Exception {
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(baos);

            oos.writeObject(AgentRole.WORKER);
            oos.close();

            java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(baos.toByteArray());
            java.io.ObjectInputStream ois = new java.io.ObjectInputStream(bais);

            AgentRole deserialized = (AgentRole) ois.readObject();

            assertSame(AgentRole.WORKER, deserialized,
                "Deserialized enum should be same instance");
        }

        @Test
        @DisplayName("All enum values are serializable")
        void allEnumValuesSerializable() throws Exception {
            for (AgentRole role : AgentRole.values()) {
                java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(baos);

                oos.writeObject(role);
                oos.close();

                assertTrue(baos.size() > 0,
                    role.name() + " should be serializable");
            }
        }
    }
}

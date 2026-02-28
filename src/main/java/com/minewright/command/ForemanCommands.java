package com.minewright.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.minewright.testutil.TestLogger;
import com.minewright.MineWrightMod;
import com.minewright.entity.ForemanEntity;
import com.minewright.entity.CrewManager;
import com.minewright.memory.CompanionMemory;
import com.minewright.orchestration.AgentRole;
import com.minewright.orchestration.OrchestratorService;
import com.minewright.voice.VoiceManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ForemanCommands {
    private static final Logger LOGGER = TestLogger.getLogger(ForemanCommands.class);

    /** Shared executor for async command processing - prevents uncontrolled thread creation */
    private static final ExecutorService COMMAND_EXECUTOR = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "minewright-command-handler");
        t.setDaemon(true);
        return t;
    });

    /**
     * Shuts down the command executor. Should be called during mod unload.
     */
    public static void shutdown() {
        COMMAND_EXECUTOR.shutdown();
    }

    /**
     * Permission level required for administrative commands (spawn, remove, etc.)
     * Level 2 = Game Masters (OPs)
     */
    private static final int PERMISSION_ADMIN = 2;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("minewright")
            // Administrative commands require OP permission
            .then(Commands.literal("spawn")
                .requires(source -> source.hasPermission(PERMISSION_ADMIN))
                .then(Commands.argument("name", StringArgumentType.string())
                    .executes(ForemanCommands::spawnSteve)))
            .then(Commands.literal("remove")
                .requires(source -> source.hasPermission(PERMISSION_ADMIN))
                .then(Commands.argument("name", StringArgumentType.string())
                    .executes(ForemanCommands::removeSteve)))
            // Read-only commands available to all players
            .then(Commands.literal("list")
                .executes(ForemanCommands::listSteves))
            .then(Commands.literal("status")
                .executes(ForemanCommands::statusSteves))
            .then(Commands.literal("relationship")
                .then(Commands.argument("name", StringArgumentType.string())
                    .executes(ForemanCommands::showRelationship)))
            // Control commands require OP permission
            .then(Commands.literal("stop")
                .requires(source -> source.hasPermission(PERMISSION_ADMIN))
                .then(Commands.argument("name", StringArgumentType.string())
                    .executes(ForemanCommands::stopSteve)))
            .then(Commands.literal("tell")
                .requires(source -> source.hasPermission(PERMISSION_ADMIN))
                .then(Commands.argument("name", StringArgumentType.string())
                    .then(Commands.argument("command", StringArgumentType.greedyString())
                        .executes(ForemanCommands::tellSteve))))
            // Coordination commands require OP permission
            .then(Commands.literal("promote")
                .requires(source -> source.hasPermission(PERMISSION_ADMIN))
                .then(Commands.argument("name", StringArgumentType.string())
                    .executes(ForemanCommands::promoteSteve)))
            .then(Commands.literal("demote")
                .requires(source -> source.hasPermission(PERMISSION_ADMIN))
                .then(Commands.argument("name", StringArgumentType.string())
                    .executes(ForemanCommands::demoteSteve)))
            // Voice commands available to all players (client-side feature)
            .then(Commands.literal("voice")
                .then(Commands.literal("on")
                    .executes(ForemanCommands::voiceOn))
                .then(Commands.literal("off")
                    .executes(ForemanCommands::voiceOff))
                .then(Commands.literal("status")
                    .executes(ForemanCommands::voiceStatus))
                .then(Commands.literal("test")
                    .executes(ForemanCommands::voiceTest)))
        );
    }

    private static int spawnSteve(CommandContext<CommandSourceStack> context) {
        String name = StringArgumentType.getString(context, "name");
        CommandSourceStack source = context.getSource();

        LOGGER.info("Command /minewright spawn {} executed by {}", name, source.getTextName());

        ServerLevel serverLevel = source.getLevel();
        if (serverLevel == null) {
            LOGGER.warn("Spawn command failed for '{}': not on server", name);
            source.sendFailure(Component.literal("Hey! This command only works on the job site (server)."));
            return 0;
        }

        CrewManager manager = MineWrightMod.getCrewManager();

        Vec3 sourcePos = source.getPosition();
        if (source.getEntity() != null) {
            Vec3 lookVec = source.getEntity().getLookAngle();
            sourcePos = sourcePos.add(lookVec.x * 3, 0, lookVec.z * 3);
        } else {
            sourcePos = sourcePos.add(3, 0, 0);
        }
        Vec3 spawnPos = sourcePos;

        ForemanEntity crewMember = manager.spawnCrewMember(serverLevel, spawnPos, name);
        if (crewMember != null) {
            LOGGER.info("Successfully spawned crew member '{}' at {} by {}", name, spawnPos, source.getTextName());
            source.sendSuccess(() -> Component.literal("New crew member " + name + " reported for duty!"), true);
            return 1;
        } else {
            LOGGER.warn("Failed to spawn crew member '{}' (already exists or max crew limit reached)", name);
            source.sendFailure(Component.literal("Can't add " + name + " to the crew! Either we already have one, or the crew is at maximum capacity."));
            return 0;
        }
    }

    private static int removeSteve(CommandContext<CommandSourceStack> context) {
        String name = StringArgumentType.getString(context, "name");
        CommandSourceStack source = context.getSource();

        LOGGER.info("Command /minewright remove {} executed by {}", name, source.getTextName());

        CrewManager manager = MineWrightMod.getCrewManager();
        if (manager.removeCrewMember(name)) {
            LOGGER.info("Crew member '{}' removed by {}", name, source.getTextName());
            source.sendSuccess(() -> Component.literal(name + " has clocked out and left the job site."), true);
            return 1;
        } else {
            LOGGER.warn("Failed to remove crew member '{}' - not found", name);
            source.sendFailure(Component.literal("Can't find " + name + " on the crew. Check the roster with /minewright list"));
            return 0;
        }
    }

    private static int listSteves(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        CrewManager manager = MineWrightMod.getCrewManager();

        var names = manager.getCrewMemberNames();
        if (names.isEmpty()) {
            source.sendSuccess(() -> Component.literal("The job site is empty - no crew members on duty. Use /minewright spawn <name> to hire someone!"), false);
        } else {
            source.sendSuccess(() -> Component.literal("Crew on duty (" + names.size() + "): " + String.join(", ", names)), false);
        }
        return 1;
    }

    private static int stopSteve(CommandContext<CommandSourceStack> context) {
        String name = StringArgumentType.getString(context, "name");
        CommandSourceStack source = context.getSource();

        LOGGER.info("Command /minewright stop {} executed by {}", name, source.getTextName());

        CrewManager manager = MineWrightMod.getCrewManager();
        ForemanEntity crewMember = manager.getCrewMember(name);

        if (crewMember != null) {
            crewMember.getActionExecutor().stopCurrentAction();
            crewMember.getMemory().clearTaskQueue();
            LOGGER.info("Crew member '{}' stopped and task queue cleared by {}", name, source.getTextName());
            source.sendSuccess(() -> Component.literal(name + " has stopped working and cleared their task queue."), true);
            return 1;
        } else {
            LOGGER.warn("Stop command failed for '{}' - crew member not found", name);
            source.sendFailure(Component.literal("Can't find " + name + " on the crew. Check the roster with /minewright list"));
            return 0;
        }
    }

    private static int tellSteve(CommandContext<CommandSourceStack> context) {
        String name = StringArgumentType.getString(context, "name");
        String command = StringArgumentType.getString(context, "command");
        CommandSourceStack source = context.getSource();

        LOGGER.info("Command /minewright tell {} '{}' executed by {}", name, command, source.getTextName());

        CrewManager manager = MineWrightMod.getCrewManager();
        ForemanEntity crewMember = manager.getCrewMember(name);

        if (crewMember != null) {
            // Send immediate feedback that the order was received
            source.sendSuccess(() -> Component.literal("Order sent to " + name + ": \"" + command + "\""), true);

            // Use shared executor instead of creating new threads
            COMMAND_EXECUTOR.submit(() -> {
                try {
                    crewMember.getActionExecutor().processNaturalLanguageCommand(command);
                } catch (Exception e) {
                    LOGGER.error("Error processing command for crew member '{}': {}", name, command, e);
                }
            });

            return 1;
        } else {
            LOGGER.warn("Failed to send command to crew member '{}' - not found", name);
            source.sendFailure(Component.literal("Can't find " + name + " on the crew. Check the roster with /minewright list"));
            return 0;
        }
    }

    private static int voiceOn(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        VoiceManager voice = VoiceManager.getInstance();

        voice.setEnabled(true);
        source.sendSuccess(() -> Component.literal("Foreman radio is ON! Hold V to give voice commands."), true);

        LOGGER.info("Voice system enabled via command");
        return 1;
    }

    private static int voiceOff(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        VoiceManager voice = VoiceManager.getInstance();

        voice.setEnabled(false);
        voice.stopAll();
        source.sendSuccess(() -> Component.literal("Foreman radio is OFF. Back to manual commands."), true);

        LOGGER.info("Voice system disabled via command");
        return 1;
    }

    private static int voiceStatus(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        VoiceManager voice = VoiceManager.getInstance();

        var config = voice.getConfig();
        StringBuilder status = new StringBuilder();
        status.append("Voice System Status:\n");
        status.append("  Enabled: ").append(voice.isEnabled()).append("\n");
        status.append("  Mode: ").append(config.getMode()).append("\n");
        status.append("  STT Language: ").append(config.getSttLanguage()).append("\n");
        status.append("  TTS Voice: ").append(config.getTtsVoice()).append("\n");
        status.append("  Push-to-Talk: ").append(config.isPushToTalk() ? "Enabled (V key)" : "Disabled");

        source.sendSuccess(() -> Component.literal(status.toString()), false);
        return 1;
    }

    private static int voiceTest(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        VoiceManager voice = VoiceManager.getInstance();

        LOGGER.debug("Voice test command executed by {}", source.getTextName());

        if (!voice.isEnabled()) {
            LOGGER.warn("Voice test failed - voice system is disabled");
            source.sendFailure(Component.literal("Foreman radio is OFF! Use '/minewright voice on' first to enable it."));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("Testing the foreman radio..."), true);

        CompletableFuture<com.minewright.voice.VoiceSystem.VoiceTestResult> test = voice.test();
        test.thenAccept(result -> {
            String message = result.success()
                ? "Radio check passed: " + result.message() + " (Latency: " + result.latencyMs() + "ms)"
                : "Radio check failed: " + result.message();
            LOGGER.info("Voice test result: {} (latency: {}ms)", result.success() ? "SUCCESS" : "FAILED", result.latencyMs());
            source.sendSuccess(() -> Component.literal(message), true);
        }).exceptionally(e -> {
            LOGGER.error("Voice test error", e);
            source.sendFailure(Component.literal("Radio test error: " + e.getMessage()));
            return null;
        });

        return 1;
    }

    private static int statusSteves(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        CrewManager manager = MineWrightMod.getCrewManager();

        var names = manager.getCrewMemberNames();
        if (names.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No crew members on duty. Use /minewright spawn <name> to hire someone!"), false);
            return 1;
        }

        source.sendSuccess(() -> Component.literal("=== Job Site Status ==="), false);
        for (String name : names) {
            var crewMember = manager.getCrewMember(name);
            if (crewMember != null) {
                String goal = crewMember.getActionExecutor().getCurrentGoal();
                String status = goal != null ? "Working on: " + goal : "Idle - waiting for orders";
                source.sendSuccess(() -> Component.literal("  " + name + ": " + status), false);
            }
        }

        return 1;
    }

    private static int promoteSteve(CommandContext<CommandSourceStack> context) {
        String name = StringArgumentType.getString(context, "name");
        CommandSourceStack source = context.getSource();

        LOGGER.info("Command /minewright promote {} executed by {}", name, source.getTextName());

        CrewManager manager = MineWrightMod.getCrewManager();
        ForemanEntity crewMember = manager.getCrewMember(name);

        if (crewMember == null) {
            LOGGER.warn("Promote command failed for '{}' - crew member not found", name);
            source.sendFailure(Component.literal("Can't find " + name + " on the crew. Check the roster with /minewright list"));
            return 0;
        }

        // Promote to FOREMAN role if currently a WORKER
        if (crewMember.getRole() == AgentRole.WORKER) {
            crewMember.setRole(AgentRole.FOREMAN);
            LOGGER.info("Crew member '{}' promoted to FOREMAN by {}", name, source.getTextName());
            source.sendSuccess(() -> Component.literal("Promoted " + name + " to Foreman"), true);
        } else {
            LOGGER.warn("Promote command failed for '{}' - already a Foreman or Solo agent (current role: {})", name, crewMember.getRole());
            source.sendFailure(Component.literal(name + " is already a Foreman or Solo agent"));
            return 0;
        }

        return 1;
    }

    private static int demoteSteve(CommandContext<CommandSourceStack> context) {
        String name = StringArgumentType.getString(context, "name");
        CommandSourceStack source = context.getSource();

        LOGGER.info("Command /minewright demote {} executed by {}", name, source.getTextName());

        CrewManager manager = MineWrightMod.getCrewManager();
        ForemanEntity crewMember = manager.getCrewMember(name);

        if (crewMember == null) {
            LOGGER.warn("Demote command failed for '{}' - crew member not found", name);
            source.sendFailure(Component.literal("Can't find " + name + " on the crew. Check the roster with /minewright list"));
            return 0;
        }

        // Demote to WORKER role if currently a FOREMAN
        if (crewMember.getRole() == AgentRole.FOREMAN) {
            crewMember.setRole(AgentRole.WORKER);
            LOGGER.info("Crew member '{}' demoted to WORKER by {}", name, source.getTextName());
            source.sendSuccess(() -> Component.literal("Demoted " + name + " to Worker"), true);
        } else {
            LOGGER.warn("Demote command failed for '{}' - already a Worker or Solo agent (current role: {})", name, crewMember.getRole());
            source.sendFailure(Component.literal(name + " is already a Worker or Solo agent"));
            return 0;
        }

        return 1;
    }

    private static int showRelationship(CommandContext<CommandSourceStack> context) {
        String name = StringArgumentType.getString(context, "name");
        CommandSourceStack source = context.getSource();

        CrewManager manager = MineWrightMod.getCrewManager();
        ForemanEntity crewMember = manager.getCrewMember(name);

        if (crewMember != null) {
            var companionMemory = crewMember.getCompanionMemory();
            if (companionMemory != null) {
                var relationship = companionMemory.getRelationship();
                var personality = companionMemory.getPersonality();

                // Calculate relationship title based on rapport
                String relationshipTitle = getRelationshipTitle(relationship.getAffection());

                // Calculate days together
                final long daysTogether;
                if (companionMemory.getFirstMeeting() != null) {
                    daysTogether = ChronoUnit.DAYS.between(
                        companionMemory.getFirstMeeting(),
                        Instant.now()
                    );
                } else {
                    daysTogether = 0;
                }

                // Get stats
                int rapport = relationship.getAffection();
                int trust = relationship.getTrust();
                int interactions = companionMemory.getInteractionCount();
                int insideJokes = companionMemory.getInsideJokeCount();
                int milestones = companionMemory.getMilestones().size();

                // Build rapport bar
                String rapportBar = buildRapportBar(rapport);

                // Send formatted output
                source.sendSuccess(() ->
                    Component.literal("Work Relationship: ").withStyle(s -> s.withColor(0x00AA00))
                    .append(Component.literal(name + " - " + relationshipTitle).withStyle(s -> s.withColor(0xFFFF55)))
                , false);

                // Stats section
                source.sendSuccess(() ->
                    Component.literal("  Rapport: ").withStyle(s -> s.withColor(0xAAAAAA))
                    .append(Component.literal(rapportBar).withStyle(s -> s.withColor(0xFFFF55)))
                    .append(Component.literal(" " + rapport + "/100").withStyle(s -> s.withColor(0xFFFF55)))
                , false);

                source.sendSuccess(() ->
                    Component.literal("  Trust Level: ").withStyle(s -> s.withColor(0xAAAAAA))
                    .append(Component.literal(String.valueOf(trust)).withStyle(s -> s.withColor(0xFFFF55)))
                , false);

                source.sendSuccess(() ->
                    Component.literal("  Days on Job: ").withStyle(s -> s.withColor(0xAAAAAA))
                    .append(Component.literal(String.valueOf(daysTogether)).withStyle(s -> s.withColor(0xFFFF55)))
                , false);

                source.sendSuccess(() ->
                    Component.literal("  Jobs Completed: ").withStyle(s -> s.withColor(0xAAAAAA))
                    .append(Component.literal(String.valueOf(interactions)).withStyle(s -> s.withColor(0xFFFF55)))
                , false);

                source.sendSuccess(() ->
                    Component.literal("  Inside Jokes: ").withStyle(s -> s.withColor(0xAAAAAA))
                    .append(Component.literal(String.valueOf(insideJokes)).withStyle(s -> s.withColor(0xFFFF55)))
                , false);

                source.sendSuccess(() ->
                    Component.literal("  Milestones: ").withStyle(s -> s.withColor(0xAAAAAA))
                    .append(Component.literal(String.valueOf(milestones)).withStyle(s -> s.withColor(0xFFFF55)))
                , false);

                // Current Mood
                source.sendSuccess(() ->
                    Component.literal("  Current Mood: ").withStyle(s -> s.withColor(0xAAAAAA))
                    .append(Component.literal(relationship.getCurrentMood().getDisplayName()).withStyle(s -> s.withColor(0xFFFF55)))
                , false);

                // Personality Archetype
                String archetype = getPersonalityArchetype(personality);
                source.sendSuccess(() ->
                    Component.literal("  Work Style: ").withStyle(s -> s.withColor(0xAAAAAA))
                    .append(Component.literal(archetype).withStyle(s -> s.withColor(0xFFFF55)))
                , false);

                // Recent Memories
                var recentMemories = companionMemory.getRecentMemories(3);
                if (!recentMemories.isEmpty()) {
                    source.sendSuccess(() ->
                        Component.literal("  Recent Jobs:").withStyle(s -> s.withColor(0x00AA00))
                    , false);
                    for (var mem : recentMemories) {
                        source.sendSuccess(() ->
                            Component.literal("    - ").withStyle(s -> s.withColor(0xAAAAAA))
                            .append(Component.literal(mem.description).withStyle(s -> s.withColor(0xFFFFFF)))
                        , false);
                    }
                }

                return 1;
            }
        }

        source.sendFailure(Component.literal("Can't find " + name + " on the crew. Check the roster with /minewright list"));
        return 0;
    }

    /**
     * Gets the relationship title based on rapport level.
     */
    private static String getRelationshipTitle(int rapport) {
        if (rapport <= 20) {
            return "Stranger";
        } else if (rapport <= 40) {
            return "Acquaintance";
        } else if (rapport <= 60) {
            return "Friend";
        } else if (rapport <= 80) {
            return "Close Friend";
        } else {
            return "Best Friend";
        }
    }

    /**
     * Builds a visual bar for rapport level.
     */
    private static String buildRapportBar(int rapport) {
        int filled = (rapport + 9) / 10; // Round up to determine filled segments
        StringBuilder bar = new StringBuilder("[");
        for (int i = 0; i < 10; i++) {
            bar.append(i < filled ? "#" : "-");
        }
        bar.append("]");
        return bar.toString();
    }

    /**
     * Gets a personality archetype description based on traits.
     */
    private static String getPersonalityArchetype(CompanionMemory.PersonalityProfile personality) {
        StringBuilder archetype = new StringBuilder();

        // Primary trait
        if (personality.extraversion > 70) {
            archetype.append("Outgoing ");
        } else if (personality.extraversion < 40) {
            archetype.append("Reserved ");
        }

        if (personality.humor > 70) {
            archetype.append("Wit");
        } else if (personality.encouragement > 70) {
            archetype.append("Motivator");
        } else if (personality.conscientiousness > 80) {
            archetype.append("Perfectionist");
        } else if (personality.agreeableness > 80) {
            archetype.append("Peacemaker");
        } else {
            archetype.append("Companion");
        }

        return archetype.toString();
    }
}

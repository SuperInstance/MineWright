package com.minewright.event;

import com.minewright.MineWrightMod;
import com.minewright.entity.ForemanEntity;
import com.minewright.entity.CrewManager;
import com.minewright.memory.StructureRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MineWrightMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ServerEventHandler {
    private static boolean crewSpawned = false;

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ServerLevel level = (ServerLevel) player.level();
            CrewManager manager = MineWrightMod.getCrewManager();
            if (!crewSpawned) {                manager.clearAllCrewMembers();

                // Clear structure registry for fresh spatial awareness
                StructureRegistry.clear();

                // Then, remove ALL ForemanEntity instances from the world (including ones loaded from NBT)
                int removedCount = 0;
                for (var entity : level.getAllEntities()) {
                    if (entity instanceof ForemanEntity) {
                        entity.discard();
                        removedCount++;
                    }
                }                Vec3 playerPos = player.position();
                Vec3 lookVec = player.getLookAngle();

                String[] names = {"Foreman", "Alex", "Bob", "Charlie"};

                for (int i = 0; i < 4; i++) {
                    double offsetX = lookVec.x * 5 + (lookVec.z * (i - 1.5) * 2);
                    double offsetZ = lookVec.z * 5 + (-lookVec.x * (i - 1.5) * 2);

                    Vec3 spawnPos = new Vec3(
                        playerPos.x + offsetX,
                        playerPos.y,
                        playerPos.z + offsetZ
                    );

                    ForemanEntity foreman = manager.spawnCrewMember(level, spawnPos, names[i]);
                    if (foreman != null) {                    }
                }

                crewSpawned = true;            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        crewSpawned = false;
    }
}


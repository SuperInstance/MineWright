package com.minewright;

import com.mojang.logging.LogUtils;
import com.minewright.command.ForemanCommands;
import com.minewright.config.MineWrightConfig;
import com.minewright.entity.ForemanEntity;
import com.minewright.entity.CrewManager;
import com.minewright.orchestration.OrchestratorService;
import com.minewright.voice.VoiceException;
import com.minewright.voice.VoiceManager;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

@Mod(MineWrightMod.MODID)
public class MineWrightMod {
    public static final String MODID = "minewright";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<EntityType<?>> ENTITIES =
        DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MODID);

    public static final RegistryObject<EntityType<ForemanEntity>> FOREMAN_ENTITY = ENTITIES.register("foreman",
        () -> EntityType.Builder.of(ForemanEntity::new, MobCategory.CREATURE)
            .sized(0.6F, 1.8F)
            .clientTrackingRange(10)
            .build("foreman"));

    private static CrewManager crewManager;
    private static OrchestratorService orchestratorService;

    public MineWrightMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ENTITIES.register(modEventBus);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, MineWrightConfig.SPEC);

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::entityAttributes);
        modEventBus.addListener(this::onConfigReload);

        MinecraftForge.EVENT_BUS.register(this);

        if (net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient()) {
            MinecraftForge.EVENT_BUS.register(com.minewright.client.ForemanOfficeGUI.class);
        }

        crewManager = new CrewManager();
        orchestratorService = new OrchestratorService();

        LOGGER.info("MineWright initialized with OrchestratorService");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Validate configuration on startup
        MineWrightConfig.validateAndLog();

        // Initialize voice system
        try {
            VoiceManager.getInstance().initialize();
            LOGGER.info("Voice system initialized");
        } catch (VoiceException e) {
            LOGGER.warn("Failed to initialize voice system: {}", e.getMessage());
        }
    }

    /**
     * Handles config reload events.
     *
     * <p>This is called when the config file is reloaded via /reload command
     * or when the config file changes on disk.</p>
     *
     * @param event The config reload event
     */
    private void onConfigReload(final ModConfigEvent event) {
        if (event.getConfig().getModId().equals(MODID)) {
            LOGGER.info("MineWright configuration reloaded");
            MineWrightConfig.validateAndLog();

            // Reload voice configuration
            VoiceManager.getInstance().reloadConfiguration();
        }
    }

    private void entityAttributes(EntityAttributeCreationEvent event) {
        event.put(FOREMAN_ENTITY.get(), ForemanEntity.createAttributes().build());
    }

    @SubscribeEvent
    public void onCommandRegister(RegisterCommandsEvent event) {        ForemanCommands.register(event.getDispatcher());    }

    public static CrewManager getCrewManager() {
        return crewManager;
    }

    public static OrchestratorService getOrchestratorService() {
        return orchestratorService;
    }
}


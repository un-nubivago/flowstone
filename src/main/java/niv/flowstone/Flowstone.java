package niv.flowstone;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import niv.flowstone.config.Configuration;
import niv.flowstone.impl.CustomGenerator;
import niv.flowstone.impl.DeepslateGenerator;
import niv.flowstone.impl.WorldlyGenerator;

public class Flowstone implements ModInitializer {
    // This logger is used to write text to the console and the log file.
    // It is considered best practice to use your mod id as the logger's name.
    // That way, it's clear which mod wrote info, warnings, and errors.
    public static final Logger LOGGER = LoggerFactory.getLogger("Flowstone");

    public static final String MOD_ID = "flowstone";

    @Override
    public void onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.
        LOGGER.info("Initialize");

        DynamicRegistries.register(CustomGenerator.REGISTRY, CustomGenerator.CODEC);

        ServerWorldEvents.LOAD.register(DeepslateGenerator.getCacheInvalidator());
        ServerWorldEvents.LOAD.register(WorldlyGenerator.getCacheInvalidator());

        Configuration.LOADED.register(() -> LOGGER.info("Configuration loaded"));
        Configuration.LOADED.register(Replacers.getInvalidator());

        Configuration.init();
    }

    public static final BlockState replace(LevelAccessor level, BlockPos pos, BlockState state) {
        return Optional.ofNullable(Replacers.configuredReplacer().apply(level, pos, state)).orElse(state);
    }
}

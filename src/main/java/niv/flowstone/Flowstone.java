package niv.flowstone;

import java.util.Optional;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import niv.flowstone.config.Configuration;
import niv.flowstone.impl.CustomGenerator;
import niv.flowstone.impl.DeepslateGenerator;
import niv.flowstone.impl.WorldlyGenerator;

@NullMarked
public class Flowstone implements ModInitializer {

    public static final String MOD_ID = "flowstone";

    public static final String MOD_NAME = "Flowstone";

    @SuppressWarnings("null")
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    @Override
    public void onInitialize() {
        DynamicRegistries.register(CustomGenerator.REGISTRY, CustomGenerator.CODEC);

        ServerLevelEvents.LOAD.register(DeepslateGenerator.getCacheInvalidator());
        ServerLevelEvents.LOAD.register(WorldlyGenerator.getCacheInvalidator());

        Configuration.LOADED.register(() -> LOGGER.info("({}) Configuration loaded", MOD_NAME));
        Configuration.LOADED.register(Replacers.getInvalidator());

        Configuration.init();

        LOGGER.info("({}) Initialized", MOD_NAME);
    }

    @SuppressWarnings("null")
    public static final BlockState replace(LevelAccessor level, BlockPos pos, BlockState state) {
        return Optional.ofNullable(Replacers.configuredReplacer().apply(level, pos, state)).orElse(state);
    }
}

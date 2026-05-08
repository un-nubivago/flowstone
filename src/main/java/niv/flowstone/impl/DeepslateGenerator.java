package niv.flowstone.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents.Load;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.SurfaceRules.BlockRuleSource;
import net.minecraft.world.level.levelgen.SurfaceRules.SequenceRuleSource;
import net.minecraft.world.level.levelgen.SurfaceRules.TestRuleSource;
import net.minecraft.world.level.levelgen.SurfaceRules.VerticalGradientConditionSource;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import niv.flowstone.Replacers;
import niv.flowstone.api.Generator;
import niv.flowstone.api.Replacer;

@NullMarked
public class DeepslateGenerator implements Generator {

    @SuppressWarnings("null")
    private static final Map<@NonNull ServerLevel, @Nullable Map<@NonNull Block, @NonNull Generator>> cache = HashMap
            .newHashMap(
                    3);

    private final BlockState state;

    private final int maxY;

    private final int minY;

    private DeepslateGenerator(BlockState state, int maxY, int minY) {
        this.state = state;
        this.maxY = maxY;
        this.minY = minY;
    }

    @SuppressWarnings("null")
    @Override
    public Optional<@Nullable BlockState> apply(LevelAccessor level, BlockPos pos) {
        return Optional.of(this.state).filter(value -> test(level.getRandom(), pos.getY()));
    }

    private boolean test(RandomSource random, int y) {
        return y <= minY || y < maxY && random.nextDouble() < Mth.map(y, minY, maxY, 1d, 0d);
    }

    @SuppressWarnings({ "null", "java:S2637" })
    private static final BlockState applyAny(LevelAccessor level, BlockPos pos, BlockState state) {
        var result = cache.get(level);
        if (result == null) {
            result = Map.of();
            if (level instanceof ServerLevel serverLevel) {
                result = loadGenerators(serverLevel);
                cache.put(serverLevel, result);
            }
        }
        return Optional.ofNullable(result.get(state.getBlock()))
                .flatMap(value -> value.apply(level, pos))
                .orElse(state);
    }

    @SuppressWarnings("null")
    private static Map<@NonNull Block, @NonNull Generator> loadGenerators(ServerLevel level) {
        var gradient = Optional.of(level)
                .map(ServerLevel::getChunkSource)
                .map(ServerChunkCache::getGenerator)
                .filter(NoiseBasedChunkGenerator.class::isInstance)
                .map(NoiseBasedChunkGenerator.class::cast)
                .map(NoiseBasedChunkGenerator::generatorSettings)
                .map(Holder::value)
                .map(NoiseGeneratorSettings::surfaceRule)
                .filter(SequenceRuleSource.class::isInstance)
                .map(SequenceRuleSource.class::cast)
                .map(SequenceRuleSource::sequence)
                .stream().flatMap(List::stream)
                .filter(TestRuleSource.class::isInstance)
                .map(TestRuleSource.class::cast)
                .filter(DeepslateGenerator::byThenRunResultState).findFirst()
                .map(TestRuleSource::ifTrue)
                .filter(VerticalGradientConditionSource.class::isInstance)
                .map(VerticalGradientConditionSource.class::cast);
        var result = HashMap.<@NonNull Block, @NonNull Generator>newHashMap(2);
        if (gradient.isPresent()) {
            var context = new WorldGenerationContext(level.getChunkSource().getGenerator(), level);
            var maxY = gradient.get().falseAtAndAbove().resolveY(context);
            var minY = gradient.get().trueAtAndBelow().resolveY(context);
            result.put(Blocks.STONE,
                    new DeepslateGenerator(Blocks.DEEPSLATE.defaultBlockState(), maxY, minY));
            result.put(Blocks.COBBLESTONE,
                    new DeepslateGenerator(Blocks.COBBLED_DEEPSLATE.defaultBlockState(), maxY, minY));
        }
        return result;
    }

    private static boolean byThenRunResultState(TestRuleSource condition) {
        return Optional.of(condition)
                .map(TestRuleSource::thenRun)
                .filter(BlockRuleSource.class::isInstance)
                .map(BlockRuleSource.class::cast)
                .map(BlockRuleSource::resultState)
                .filter(value -> value.is(Blocks.DEEPSLATE))
                .isPresent();
    }

    public static final Load getCacheInvalidator() {
        return (server, level) -> DeepslateGenerator.cache.clear();
    }

    public static final Replacer getReplacer() {
        return Replacers.defaultedMultiReplacer(
                Replacers.allowedBlocksNullableReplacer(Blocks.STONE, Blocks.COBBLESTONE),
                DeepslateGenerator::applyAny);
    }
}

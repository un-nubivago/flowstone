package niv.flowstone.impl;

import static java.util.stream.Collectors.toSet;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import com.google.common.base.MoreObjects;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import it.unimi.dsi.fastutil.ints.Int2DoubleFunction;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents.Load;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.heightproviders.TrapezoidHeight;
import net.minecraft.world.level.levelgen.heightproviders.UniformHeight;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import niv.flowstone.Replacers;
import niv.flowstone.api.Generator;
import niv.flowstone.api.Replacer;

import static niv.flowstone.config.Configuration.debugMode;

@NullMarked
public class WorldlyGenerator implements Generator {

    private static final Table<Block, Biome, Set<Generator>> biomeCache = HashBasedTable.create();
    private static final Table<Block, PlacedFeature, Set<Generator>> featureCache = HashBasedTable.create();

    private static record BaseGenerator(int blockCount, int maxBlockCount, Int2DoubleFunction function) {
    }

    private static final Map<PlacedFeature, Optional<BaseGenerator>> baseGeneratorCache = new HashMap<>();

    private final BlockState state;

    private final int blockCount;

    private final int maxBlockCount;

    private final Int2DoubleFunction function;

    private WorldlyGenerator(BlockState state, int blockCount, int maxBlockCount, Int2DoubleFunction function) {
        this.state = state;
        this.blockCount = blockCount;
        this.maxBlockCount = maxBlockCount;
        this.function = function;
    }

    @SuppressWarnings("null")
    @Override
    public Optional<@Nullable BlockState> apply(LevelAccessor level, BlockPos pos) {
        return Optional.of(this.state).filter(x -> test(level.getRandom(), pos.getY()));
    }

    private boolean test(RandomSource random, int y) {
        return debugMode() || random.nextInt(this.maxBlockCount) < (this.blockCount * this.function.applyAsDouble(y));
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("state", this.state)
                .add("blockCount", this.blockCount)
                .add("maxBlockCount", this.maxBlockCount)
                .add("function", this.function)
                .toString();
    }

    @SuppressWarnings("null")
    private static final BlockState applyAll(LevelAccessor accessor, BlockPos pos, BlockState state) {
        var biome = accessor.getBiome(pos).value();
        var result = biomeCache.get(state.getBlock(), biome);
        if (result == null) {
            if (accessor instanceof ServerLevel level) {
                result = biome.getGenerationSettings().features().stream()
                        .flatMap(HolderSet::stream)
                        .map(Holder::value)
                        .flatMap(value -> {
                            var generators = featureCache.get(state.getBlock(), value);
                            if (generators == null) {
                                generators = loadGenerators(level, value, state);
                                featureCache.put(state.getBlock(), value, generators);
                            }
                            return generators.stream();
                        })
                        .collect(toSet());
            } else {
                result = Set.of();
            }
            biomeCache.put(state.getBlock(), biome, result);
        }
        return Generator.applyAll(result, accessor, pos).orElse(state);
    }

    @SuppressWarnings("null")
    private static final Set<@NonNull Generator> loadGenerators(ServerLevel level, PlacedFeature feature, BlockState state) {
        var base = baseGeneratorCache.computeIfAbsent(feature, key -> loadBaseGenerator(level, feature));
        if (base.isEmpty()) {
            return Set.of();
        }

        var config = feature.getFeatures()
                .map(Holder::value)
                .map(ConfiguredFeature::config)
                .filter(OreConfiguration.class::isInstance)
                .findFirst()
                .map(OreConfiguration.class::cast);
        if (config.isEmpty()) {
            return Set.of();
        }

        return config.stream()
                .flatMap(value -> value.targetStates.stream())
                .filter(value -> value.target.test(state, RandomSource.create(0)))
                .map(value -> value.state).distinct()
                .filter(value -> value.is(ConventionalBlockTags.ORES))
                .map(value -> new WorldlyGenerator(value,
                        base.get().blockCount() * Math.max(1, config.get().size),
                        base.get().maxBlockCount(),
                        base.get().function()))
                .collect(toSet());
    }

    private static final class BaseGeneratorBuilder {
        private @Nullable Integer blockCount = 1;
        private @Nullable Integer maxBlockCount = null;
        private @Nullable Int2DoubleFunction function = null;

        public BaseGeneratorBuilder blockCountMultiply(int value) {
            if (this.blockCount == null) {
                this.blockCount = value;
            } else {
                this.blockCount *= value;
            }
            return this;
        }

        public BaseGeneratorBuilder maxBlockCount(int value) {
            this.maxBlockCount = value;
            return this;
        }

        public BaseGeneratorBuilder function(Int2DoubleFunction value) {
            this.function = value;
            return this;
        }

        @SuppressWarnings("null")
        public Optional<@Nullable BaseGenerator> tryBuild() {
            if (blockCount == null || maxBlockCount == null || function == null) {
                return Optional.empty();
            } else {
                return Optional.of(new BaseGenerator(blockCount, maxBlockCount, function));
            }
        }
    }

    private static final Optional<BaseGenerator> loadBaseGenerator(ServerLevel level, PlacedFeature feature) {
        var builder = new BaseGeneratorBuilder();
        var context = new WorldGenerationContext(level.getChunkSource().getGenerator(), level);
        for (var element : feature.placement()) {
            if (element instanceof CountPlacement modifier) {
                processCount(builder, modifier);
            } else if (element instanceof HeightRangePlacement modifier) {
                processHeightRange(builder, context, modifier);
            }
        }
        return builder.tryBuild();
    }

    private static final void processCount(
            BaseGeneratorBuilder builder, CountPlacement modifier) {
        builder.blockCountMultiply(modifier.count.maxInclusive());
    }

    private static record UniformFunction(int minY, int maxY)
            implements Int2DoubleFunction {
        @Override
        public double get(int y) {
            return minY <= y && y <= maxY ? 1d : 0d;
        }
    }

    private static record TrapezoidFunction(int minY, int minL, int l, int maxL, int maxY)
            implements Int2DoubleFunction {
        @Override
        public double get(int y) {
            if (y >= minY) {
                if (y < minL) {
                    return (.0 + y - minY) / l;
                } else if (y <= maxL) {
                    return 1d;
                } else if (y <= maxY) {
                    return (.0 + l - y + minY) / l;
                }
            }
            return 0d;
        }
    }

    private static final void processHeightRange(
            BaseGeneratorBuilder builder, WorldGenerationContext context, HeightRangePlacement modifier) {
        if (modifier.height instanceof UniformHeight uniform) {
            int maxY = uniform.maxInclusive.resolveY(context);
            int minY = uniform.minInclusive.resolveY(context);
            builder
                    .maxBlockCount(Math.max(0, maxY - minY) * 256)
                    .function(new UniformFunction(minY, maxY));
        } else if (modifier.height instanceof TrapezoidHeight trapezoid) {
            int maxY = trapezoid.maxInclusive.resolveY(context);
            int minY = trapezoid.minInclusive.resolveY(context);
            int l = Math.max(0, maxY - minY - trapezoid.plateau) / 2;
            if (l == 0) {
                builder
                        .maxBlockCount(Math.max(0, maxY - minY) * 256)
                        .function(new UniformFunction(minY, maxY));
            } else {
                int maxL = maxY - l;
                int minL = minY + l;
                builder
                        .maxBlockCount(Math.max(0, maxY - minY + trapezoid.plateau) * 128)
                        .function(new TrapezoidFunction(minY, minL, l, maxL, maxY));
            }
        }
    }

    public static final Load getCacheInvalidator() {
        return (server, level) -> {
            WorldlyGenerator.biomeCache.clear();
            WorldlyGenerator.featureCache.clear();
            WorldlyGenerator.baseGeneratorCache.clear();
        };
    }

    public static final Replacer getReplacer() {
        return Replacers.defaultedMultiReplacer(
                Replacers.allowedBlocksNullableReplacer(Blocks.STONE, Blocks.DEEPSLATE, Blocks.NETHERRACK),
                WorldlyGenerator::applyAll);
    }
}

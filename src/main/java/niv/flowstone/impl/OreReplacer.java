package niv.flowstone.impl;

import static com.google.common.base.Suppliers.memoize;
import static java.util.stream.Collectors.toSet;
import static niv.flowstone.config.Configuration.debugMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

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
import net.minecraft.world.level.levelgen.feature.AbstractOreFeature;
import net.minecraft.world.level.levelgen.feature.BlockReplacement;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.heightproviders.TrapezoidHeight;
import net.minecraft.world.level.levelgen.heightproviders.UniformHeight;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import niv.flowstone.Replacers;
import niv.flowstone.api.Generator;
import niv.flowstone.api.Replacer;

@NullMarked
public class OreReplacer implements Replacer {

    private static final Supplier<@NonNull OreReplacer> INSTANCE = memoize(OreReplacer::new);

    private final Table<Block, Biome, Set<@NonNull GeneratorBuilder>> cache = HashBasedTable.create();

    @SuppressWarnings("null")
    @Override
    public @Nullable BlockState apply(LevelAccessor accessor, BlockPos pos, BlockState state) {
        var biome = accessor.getBiome(pos).value();
        var builders = cache.get(state.getBlock(), biome);
        if (builders == null && accessor instanceof ServerLevel level) {
            builders = biome.getGenerationSettings().features().stream()
                    .flatMap(HolderSet::stream)
                    .map(Holder::value)
                    .map(placedFeature -> GeneratorBuilder.from(level, placedFeature))
                    .filter(GeneratorBuilder::isValid)
                    .collect(toSet());
            cache.put(state.getBlock(), biome, builders);
        }
        return Generator.applyAll(builders.stream()
                .flatMap(builder -> builder.buildGenerators(state, pos, accessor.getRandom())).toList(), accessor,
                pos)
                .orElse(state);
    }

    @SuppressWarnings("null")
    private static Stream<@NonNull AbstractOreFeature> filterMapOreFeature(Feature feature) {
        if (feature instanceof AbstractOreFeature abstractOreFeature) {
            return Stream.of(abstractOreFeature);
        } else {
            return Stream.empty();
        }
    }

    private static final class GeneratorBuilder {

        private List<BlockReplacement> targetStates = new ArrayList<>();
        private int blockCount = 1;
        private int maxBlockCount = 0;
        private Int2DoubleFunction function = _ -> 1d;

        public void applyOreFeature(AbstractOreFeature feature) {
            this.targetStates = feature.targetStates();
            this.blockCount *= feature.size();
        }

        public void applyPlacementModifiers(ServerLevel level, List<PlacementModifier> modifiers) {
            var context = new WorldGenerationContext(level.getChunkSource().getGenerator(), level);
            for (var any : modifiers) {
                if (any instanceof CountPlacement modifier) {
                    this.blockCount *= modifier.count.maxInclusive();
                } else if (any instanceof HeightRangePlacement modifier) {
                    if (modifier.height instanceof UniformHeight uniform) {
                        int maxY = uniform.maxInclusive.resolveY(context);
                        int minY1 = uniform.minInclusive.resolveY(context);
                        this.maxBlockCount = Math.max(0, maxY - minY1) * 256;
                        this.function = new UniformFunction(minY1, maxY);
                    } else if (modifier.height instanceof TrapezoidHeight trapezoid) {
                        int maxY1 = trapezoid.maxInclusive.resolveY(context);
                        int minY = trapezoid.minInclusive.resolveY(context);
                        int l = Math.max(0, maxY1 - minY - trapezoid.plateau) / 2;
                        if (l == 0) {
                            this.maxBlockCount = Math.max(0, maxY1 - minY) * 256;
                            this.function = new UniformFunction(minY, maxY1);
                        } else {
                            int maxL = maxY1 - l;
                            int minL = minY + l;
                            this.maxBlockCount = Math.max(0, maxY1 - minY + trapezoid.plateau) * 128;
                            this.function = new TrapezoidFunction(minY, minL, l, maxL, maxY1);
                        }
                    }
                }
            }
        }

        public boolean isValid() {
            return !targetStates.isEmpty() && blockCount > 0 && maxBlockCount > 0;
        }

        @SuppressWarnings("null")
        public Stream<@NonNull Generator> buildGenerators(BlockState state, BlockPos pos, RandomSource random) {
            return this.targetStates.stream().filter(targetState -> targetState.target().test(state, pos, random))
                    .map(BlockReplacement::state)
                    .filter(targetState -> targetState.is(ConventionalBlockTags.ORES))
                    .map(thisState -> new Generator() {
                        public Optional<@NonNull BlockState> apply(LevelAccessor accessor, BlockPos pos) {
                            if (GeneratorBuilder.this.test(pos, accessor.getRandom())) {
                                return Optional.of(thisState);
                            } else {
                                return Optional.empty();
                            }
                        }
                    });
        }

        private boolean test(BlockPos pos, RandomSource random) {
            return debugMode()
                    || random.nextInt(this.maxBlockCount) < (this.blockCount * this.function.applyAsDouble(pos.getY()));
        }

        @SuppressWarnings("null")
        public static final GeneratorBuilder from(ServerLevel level, PlacedFeature feature) {
            var result = new GeneratorBuilder();
            result.applyPlacementModifiers(level, feature.placement());
            feature.getFeatures().map(Holder::value)
                    .flatMap(OreReplacer::filterMapOreFeature)
                    .findFirst().ifPresent(result::applyOreFeature);
            return result;
        }
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

    @SuppressWarnings("null")
    public static final Load getCacheInvalidator() {
        return (server, level) -> INSTANCE.get().cache.clear();
    }

    public static final Replacer getReplacer() {
        return Replacers.defaultedMultiReplacer(
                Replacers.allowedBlocksNullableReplacer(Blocks.STONE, Blocks.DEEPSLATE, Blocks.NETHERRACK),
                INSTANCE.get());
    }
}

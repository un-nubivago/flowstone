package niv.flowstone.impl;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents.Load;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.material.condition.MaterialCondition;
import net.minecraft.world.level.levelgen.material.condition.VerticalGradientCondition;
import net.minecraft.world.level.levelgen.material.rule.BlockRule;
import net.minecraft.world.level.levelgen.material.rule.ConditionRule;
import net.minecraft.world.level.levelgen.material.rule.MaterialRule;
import net.minecraft.world.level.levelgen.material.rule.MaterialRule.HolderHolder;
import net.minecraft.world.level.levelgen.material.rule.SequenceRule;
import niv.flowstone.Replacers;
import niv.flowstone.api.Generator;
import niv.flowstone.api.Replacer;

@NullMarked
public class DeepslateGenerator implements Generator {

    private static final Table<@NonNull ServerLevel, @NonNull Block, @NonNull Generator> CACHE = HashBasedTable
            .create(3, 2);

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
        return test(level.getRandom(), pos.getY()) ? Optional.of(this.state) : Optional.empty();
    }

    private boolean test(RandomSource random, int y) {
        return y <= minY || y < maxY && random.nextDouble() < Mth.map(y, minY, maxY, 1d, 0d);
    }

    @SuppressWarnings({ "null", "java:S2637" })
    private static final BlockState applyAny(LevelAccessor level, BlockPos pos, BlockState state) {
        var result = CACHE.get(level, state.getBlock());
        if (result == null && level instanceof ServerLevel serverLevel) {
            var map = loadGenerators(serverLevel);
            map.forEach((key, value) -> CACHE.put(serverLevel, key, value));
            result = map.getOrDefault(state.getBlock(), null);
        }
        return result == null ? state : result.apply(level, pos).orElse(state);
    }

    @SuppressWarnings("null")
    private static Map<@NonNull Block, @NonNull Generator> loadGenerators(ServerLevel level) {
        var rules = new LinkedList<MaterialRule>();

        if (level.getChunkSource().getGenerator() instanceof NoiseBasedChunkGenerator generator)
            rules.addLast(generator.generatorSettings().value().materialRule().value());
        else
            return Collections.emptyMap();

        VerticalGradientCondition gradient = null;
        while (gradient == null && !rules.isEmpty()) {
            var rule = rules.removeFirst();
            switch (rule) {
                case HolderHolder (Holder<MaterialRule> holder):
                    rules.addLast(holder.value());
                    break;
                case SequenceRule (List<MaterialRule> sequence):
                    rules.addAll(sequence);
                    break;
                case ConditionRule (MaterialCondition ifTrue, MaterialRule thenRun):
                    if (thenRun instanceof BlockRule block
                            && block.resultState().is(Blocks.DEEPSLATE)
                            && ifTrue instanceof VerticalGradientCondition candidate) {
                        gradient = candidate;
                    }
                    break;
                default:
                    break;
            }
        }

        if (gradient == null)
            return Collections.emptyMap();

        var context = new WorldGenerationContext(level.getChunkSource().getGenerator(), level);
        var maxY = gradient.falseAtAndAbove().resolveY(context);
        var minY = gradient.trueAtAndBelow().resolveY(context);

        return Map.of(
                Blocks.STONE, new DeepslateGenerator(Blocks.DEEPSLATE.defaultBlockState(), maxY, minY),
                Blocks.COBBLESTONE, new DeepslateGenerator(Blocks.COBBLED_DEEPSLATE.defaultBlockState(), maxY, minY));
    }

    public static final Load getCacheInvalidator() {
        return (server, level) -> DeepslateGenerator.CACHE.clear();
    }

    public static final Replacer getReplacer() {
        return Replacers.defaultedMultiReplacer(
                Replacers.allowedBlocksNullableReplacer(Blocks.STONE, Blocks.COBBLESTONE),
                DeepslateGenerator::applyAny);
    }
}

package niv.flowstone;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import com.google.common.collect.ImmutableList;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import niv.flowstone.api.Replacer;
import niv.flowstone.config.Configuration;
import niv.flowstone.impl.CustomGenerator;
import niv.flowstone.impl.DeepslateGenerator;
import niv.flowstone.impl.EndReplacer;
import niv.flowstone.impl.WorldlyGenerator;

@NullMarked
public class Replacers {

    private static final Replacer NO_OP = (level, pos, state) -> state;

    private static final AtomicReference<@Nullable Replacer> CONFIGURED_REPLACER = new AtomicReference<>();

    private Replacers() {
    }

    private static record AllowedBlocksNullableReplacer(ImmutableList<Block> allowed) implements Replacer {
        @Override
        public @Nullable BlockState apply(LevelAccessor level, BlockPos pos, BlockState state) {
            return allowed().stream().anyMatch(state::is) ? state : null;
        }
    }

    public static final Replacer allowedBlocksNullableReplacer(Collection<Block> blocks) {
        return new AllowedBlocksNullableReplacer(ImmutableList.copyOf(blocks));
    }

    public static final Replacer allowedBlocksNullableReplacer(Block... blocks) {
        return new AllowedBlocksNullableReplacer(ImmutableList.copyOf(blocks));
    }

    private static record DefaultedMultiReplacer(ImmutableList<Replacer> replacers) implements Replacer {
        @SuppressWarnings("null")
        @Override
        public @Nullable BlockState apply(LevelAccessor level, BlockPos pos, BlockState state) {
            var result = Optional.of(state);
            for (var replacer : replacers) {
                result = result.map(value -> replacer.apply(level, pos, value));
            }
            return result.orElse(state);
        }
    }

    public static final Replacer defaultedMultiReplacer(List<Replacer> replacers) {
        return new DefaultedMultiReplacer(ImmutableList.copyOf(replacers));
    }

    public static final Replacer defaultedMultiReplacer(Replacer... replacers) {
        return new DefaultedMultiReplacer(ImmutableList.copyOf(replacers));
    }

    public static final Runnable getInvalidator() {
        return () -> CONFIGURED_REPLACER.lazySet(null);
    }

    @SuppressWarnings({"null", "java:S2637"})
    public static final Replacer configuredReplacer() {
        return CONFIGURED_REPLACER.updateAndGet(Replacers::update);
    }

    @SuppressWarnings("null")
    private static final Replacer update(@Nullable Replacer value) {
        if (value == null) {
            var replacers = new ArrayList<@NonNull Replacer>(3);

            if (Configuration.allowDeepslateGenerators())
                replacers.add(DeepslateGenerator.getReplacer());

            if (Configuration.allowWorldlyGenerators())
                replacers.add(WorldlyGenerator.getReplacer());

            if (Configuration.allowCustomGenerators())
                replacers.add(CustomGenerator.getReplacer());

            if (Configuration.enableEndStoneGeneration()) {
                replacers.add(EndReplacer.getReplacer());
            }

            if (replacers.isEmpty()) {
                return NO_OP;
            } else if (replacers.size() == 1) {
                return replacers.get(0);
            } else {
                return defaultedMultiReplacer(replacers);
            }
        } else {
            return value;
        }
    }
}

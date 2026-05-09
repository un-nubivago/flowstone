package niv.flowstone.api;

import org.apache.commons.lang3.function.TriFunction;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

@NullMarked
public interface Replacer extends TriFunction<@NonNull LevelAccessor, @NonNull BlockPos, @NonNull BlockState, @Nullable BlockState> {
}

package niv.flowstone.impl;

import java.util.function.Supplier;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import niv.flowstone.Replacers;
import niv.flowstone.api.Replacer;

import static com.google.common.base.Suppliers.memoize;

@NullMarked
public class EndReplacer implements Replacer {

    private static final Supplier<@NonNull EndReplacer> INSTANCE = memoize(EndReplacer::new);

    private EndReplacer() {
    }

    @Override
    public BlockState apply(LevelAccessor accessor, BlockPos pos, BlockState state) {
        if (accessor instanceof Level level && level.dimension() == Level.END) {
            return Blocks.END_STONE.defaultBlockState();
        } else {
            return state;
        }
    }

    public static final Replacer getReplacer() {
        return Replacers.defaultedMultiReplacer(
                Replacers.allowedBlocksNullableReplacer(Blocks.STONE, Blocks.COBBLESTONE),
                INSTANCE.get());
    }
}

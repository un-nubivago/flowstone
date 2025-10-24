package niv.flowstone.impl;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import niv.flowstone.Replacers;
import niv.flowstone.api.Replacer;

public class EndReplacer implements Replacer {

    private static final EndReplacer INSTANCE = new EndReplacer();

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
                INSTANCE);
    }
}

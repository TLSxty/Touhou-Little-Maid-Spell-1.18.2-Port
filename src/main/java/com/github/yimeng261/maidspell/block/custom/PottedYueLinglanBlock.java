package com.github.yimeng261.maidspell.block.custom;

import com.github.yimeng261.maidspell.block.entity.YueLinglanBlockEntity;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 盆栽月铃兰：把月铃兰种进花盆，光环照常生效。
 * <p>
 * 移植自 1.9.2.1 反编译源 block/custom/PottedYueLinglanBlock.java（49 行）。
 * 1.18.2 的 {@link FlowerPotBlock} 已有三参构造，无需额外适配。
 */
public class PottedYueLinglanBlock extends FlowerPotBlock implements EntityBlock {

    public PottedYueLinglanBlock(Supplier<FlowerPotBlock> emptyPot, Supplier<? extends Block> plant) {
        super(emptyPot, plant, YueLinglanBlock.createPottedProperties());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new YueLinglanBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : YueLinglanBlockEntity.createTicker(type);
    }
}

package com.github.yimeng261.maidspell.block.custom;

import com.github.yimeng261.maidspell.block.entity.JingxuYoulanBlockEntity;
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
 * 盆栽静虚幽兰：把静虚幽兰种进花盆，净化光环照常生效。
 * <p>
 * 移植自 1.9.2.1 反编译源 block/custom/PottedJingxuYoulanBlock.java（49 行）。
 */
public class PottedJingxuYoulanBlock extends FlowerPotBlock implements EntityBlock {

    public PottedJingxuYoulanBlock(Supplier<FlowerPotBlock> emptyPot, Supplier<? extends Block> plant) {
        super(emptyPot, plant, JingxuYoulanBlock.createPottedProperties());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new JingxuYoulanBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : JingxuYoulanBlockEntity.createTicker(type);
    }
}

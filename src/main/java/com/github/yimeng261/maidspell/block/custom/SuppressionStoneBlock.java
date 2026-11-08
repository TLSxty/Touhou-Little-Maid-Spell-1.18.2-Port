package com.github.yimeng261.maidspell.block.custom;

import com.github.yimeng261.maidspell.block.entity.SuppressionStoneBlockEntity;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.MaterialColor;

/**
 * 压制之石：标记自身区块，使附近的花丛光尘尾迹停止（由 BlockEntity 的静态集合实现）。
 * <p>
 * 移植自 1.9.2.1 反编译源 block/custom/SuppressionStoneBlock.java（40 行）。
 * <strong>与原文的差异</strong>：1.18.2 没有 {@code Properties.of()}（无参）/ {@code MapColor}，
 * 改用 {@code Properties.of(Material.STONE, MaterialColor.STONE)}；其余属性（硬度 3、抗爆 6、
 * 需正确工具、石头音效、亮度 4）逐项对应。
 */
public class SuppressionStoneBlock extends Block implements EntityBlock {

    /** 发光等级。 */
    private static final int LIGHT_LEVEL = 4;

    public SuppressionStoneBlock() {
        super(BlockBehaviour.Properties.of(Material.STONE, MaterialColor.STONE)
                .strength(3.0f, 6.0f)
                .requiresCorrectToolForDrops()
                .sound(SoundType.STONE)
                .lightLevel(state -> LIGHT_LEVEL));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SuppressionStoneBlockEntity(pos, state);
    }
}

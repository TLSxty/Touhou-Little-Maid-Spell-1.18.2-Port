package com.github.yimeng261.maidspell.block.entity;

import com.github.yimeng261.maidspell.block.custom.JingxuYoulanBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 静虚幽兰的方块实体：每 20 tick 跑一次净化光环。
 * <p>
 * 移植自 1.9.2.1 反编译源 block/entity/JingxuYoulanBlockEntity.java（50 行）。
 * 相位偏移用 {@code pos.asLong()} 取模，让同一片花丛的光环分散到不同 tick，避免同一 tick 集中计算。
 */
public class JingxuYoulanBlockEntity extends BlockEntity {

    /** 光环间隔（tick）。 */
    private static final int TICK_INTERVAL = 20;

    public JingxuYoulanBlockEntity(BlockPos pos, BlockState state) {
        super(MaidSpellBlockEntities.JINGXU_YOULAN.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, JingxuYoulanBlockEntity blockEntity) {
        if (!(level instanceof ServerLevel)) {
            return;
        }
        ServerLevel serverLevel = (ServerLevel) level;
        long gameTime = serverLevel.getGameTime();
        int offset = Math.floorMod(pos.asLong(), TICK_INTERVAL);
        if ((gameTime + offset) % TICK_INTERVAL != 0L) {
            return;
        }
        JingxuYoulanBlock.applyAura(serverLevel, pos);
    }

    public static <T extends BlockEntity> BlockEntityTicker<T> createTicker(BlockEntityType<T> type) {
        return type == MaidSpellBlockEntities.JINGXU_YOULAN.get()
                ? (level, pos, state, blockEntity) -> serverTick(level, pos, state, (JingxuYoulanBlockEntity) blockEntity)
                : null;
    }
}

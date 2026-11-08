package com.github.yimeng261.maidspell.block.entity;

import com.github.yimeng261.maidspell.block.custom.ScarletZhuhuaBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 绯红朱华的方块实体：每 40 tick 跑一次力量 / 凋零光环。
 * <p>
 * 移植自 1.9.2.1 反编译源 block/entity/ScarletZhuhuaBlockEntity.java（50 行）。
 */
public class ScarletZhuhuaBlockEntity extends BlockEntity {

    /** 光环间隔（tick）。 */
    private static final int TICK_INTERVAL = 40;

    public ScarletZhuhuaBlockEntity(BlockPos pos, BlockState state) {
        super(MaidSpellBlockEntities.SCARLET_ZHUHUA.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ScarletZhuhuaBlockEntity blockEntity) {
        if (!(level instanceof ServerLevel)) {
            return;
        }
        ServerLevel serverLevel = (ServerLevel) level;
        long gameTime = serverLevel.getGameTime();
        int offset = Math.floorMod(pos.asLong(), TICK_INTERVAL);
        if ((gameTime + offset) % TICK_INTERVAL != 0L) {
            return;
        }
        ScarletZhuhuaBlock.applyAura(serverLevel, pos);
    }

    public static <T extends BlockEntity> BlockEntityTicker<T> createTicker(BlockEntityType<T> type) {
        return type == MaidSpellBlockEntities.SCARLET_ZHUHUA.get()
                ? (level, pos, state, blockEntity) -> serverTick(level, pos, state, (ScarletZhuhuaBlockEntity) blockEntity)
                : null;
    }
}

package com.github.yimeng261.maidspell.block.entity;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 压制之石的方块实体：用一张静态表记录每个维度里已放置的压制之石坐标，
 * 供月铃兰的光尘尾迹判断「是否该停手」。
 * <p>
 * 移植自 1.9.2.1 反编译源 block/entity/SuppressionStoneBlockEntity.java（92 行）。
 * <strong>与原文的差异</strong>：
 * <ul>
 *   <li>1.18.2 的 {@code BlockEntity} 没有 {@code onLoad()}（1.19+ 才有），
 *       改用 {@link #setLevel(Level)} 这份在方块实体装载时一定会被调用的钩子做登记。</li>
 *   <li>维度键取 {@code level.dimension().location()}。</li>
 * </ul>
 */
public class SuppressionStoneBlockEntity extends BlockEntity {

    /** 维度 → 该维度内所有压制之石的坐标。 */
    private static final Map<ResourceLocation, Set<BlockPos>> ACTIVE_STONES = new ConcurrentHashMap<>();
    /** 压制范围为区块坐标相差不超过该值（即 7×7 区块）。 */
    private static final int CHUNK_RANGE = 3;

    /** 标记是否已登记，避免 {@code setLevel} 被重复调用时重复操作。 */
    private boolean registered;

    public SuppressionStoneBlockEntity(BlockPos pos, BlockState state) {
        super(MaidSpellBlockEntities.SUPPRESSION_STONE.get(), pos, state);
    }

    /**
     * 方块实体装载时由 {@code Level.setBlockEntity} 调用。
     * <p>1.18.2 用这个钩子替代 1.19+ 的 {@code onLoad()}。
     */
    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        if (this.registered || level == null || level.isClientSide) {
            return;
        }
        this.registered = true;
        ResourceLocation dimKey = level.dimension().location();
        ACTIVE_STONES.computeIfAbsent(dimKey, key -> ConcurrentHashMap.newKeySet()).add(this.worldPosition);
    }

    @Override
    public void setRemoved() {
        // 世界可能已经卸载，此时 level 仍可用但不再需要维护集合，做空值防御
        if (this.level != null) {
            ResourceLocation dimKey = this.level.dimension().location();
            Set<BlockPos> positions = ACTIVE_STONES.get(dimKey);
            if (positions != null) {
                positions.remove(this.worldPosition);
                if (positions.isEmpty()) {
                    ACTIVE_STONES.remove(dimKey);
                }
            }
        }
        this.registered = false;
        super.setRemoved();
    }

    /** 给定位置是否处在任意一颗压制之石的压制范围内。 */
    public static boolean isWithinSuppressionRange(LevelAccessor levelAccessor, BlockPos pos) {
        Level level;
        if (levelAccessor instanceof Level) {
            level = (Level) levelAccessor;
        } else if (levelAccessor instanceof ServerLevelAccessor) {
            level = ((ServerLevelAccessor) levelAccessor).getLevel();
        } else {
            return false;
        }
        Set<BlockPos> positions = ACTIVE_STONES.get(level.dimension().location());
        if (positions == null || positions.isEmpty()) {
            return false;
        }
        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;
        for (BlockPos stonePos : positions) {
            if (Math.abs((stonePos.getX() >> 4) - chunkX) <= CHUNK_RANGE
                    && Math.abs((stonePos.getZ() >> 4) - chunkZ) <= CHUNK_RANGE) {
                return true;
            }
        }
        return false;
    }

    /** 该维度内已登记的压制之石数量（调试 / 状态查询用）。 */
    public static int getActiveStoneCount(ResourceLocation dimKey) {
        Set<BlockPos> positions = ACTIVE_STONES.get(dimKey);
        return positions == null ? 0 : positions.size();
    }

    /** 只读视图，便于外部（例如调试命令）遍历。 */
    public static Map<ResourceLocation, Set<BlockPos>> getActiveStonesView() {
        return Collections.unmodifiableMap(ACTIVE_STONES);
    }

    /** 卸服时清空（由 {@code MaidSpellBlockEntities} 侧的订阅者调用）。 */
    public static void clearAll() {
        ACTIVE_STONES.clear();
    }
}

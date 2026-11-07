package com.github.yimeng261.maidspell.utils;

import com.github.yimeng261.maidspell.block.custom.TransientFoxLeafTrailBlock;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

/**
 * 狐叶踏水/踏岩浆的几何辅助（浮波狐叶与熔岩狐叶共用）。
 * <p>
 * 移植自 1.9.2.1 反编译源 utils/FoxLeafSurfaceHelper.java（133 行），
 * 只把 SRG 方法名换成 1.18.2 官方名，逻辑逐行保持一致。
 */
public final class FoxLeafSurfaceHelper {

    private FoxLeafSurfaceHelper() {
    }

    /**
     * 把实体按在液面上：液体表面下一格以内时吸附到 surfaceY，否则抑制下沉速度。
     */
    public static void keepEntityAtFluidSurface(LivingEntity entity, BlockPos blockPos,
                                                FluidState feetFluid, FluidState belowFluid,
                                                Vec3 deltaMovement, TagKey<Fluid> fluidTag,
                                                double maxSinkSpeed, double surfaceFloatSpeed,
                                                double surfaceYOffset) {
        BlockPos fluidPos = resolveSurfaceFluidPos(blockPos, feetFluid, belowFluid, fluidTag);
        if (fluidPos == null) {
            if (deltaMovement.y < maxSinkSpeed) {
                entity.setDeltaMovement(deltaMovement.x, maxSinkSpeed, deltaMovement.z);
            }
            return;
        }
        Level level = entity.getLevel();
        BlockPos trueSurface = fluidPos;
        while (level.getFluidState(trueSurface.above()).is(fluidTag)) {
            trueSurface = trueSurface.above();
        }
        CollisionContext collisionContext = CollisionContext.of(entity);
        if (collisionContext.isAbove(LiquidBlock.STABLE_SHAPE, trueSurface, true)) {
            entity.setOnGround(true);
        }
        double surfaceY = trueSurface.getY() + surfaceYOffset;
        if (Math.abs(entity.getY() - surfaceY) > 0.02) {
            entity.setPos(entity.getX(), surfaceY, entity.getZ());
            entity.setOnGround(true);
            entity.setDeltaMovement(deltaMovement.x, Math.max(deltaMovement.y, 0.0), deltaMovement.z);
            return;
        }
        if (deltaMovement.y < 0.0) {
            entity.setDeltaMovement(deltaMovement.x, Math.max(deltaMovement.y, surfaceFloatSpeed), deltaMovement.z);
        }
    }

    /** 返回「液体表面格」的位置：脚所在格是目标液体、且其上一格不是该液体时就取脚格；否则取脚下一格。 */
    @Nullable
    public static BlockPos findSurfaceFluidPos(BlockGetter level, BlockPos pos, TagKey<Fluid> fluidTag) {
        FluidState fluidAtPos = level.getFluidState(pos);
        if (fluidAtPos.is(fluidTag) && !level.getFluidState(pos.above()).is(fluidTag)) {
            return pos;
        }
        BlockPos below = pos.below();
        FluidState fluidBelow = level.getFluidState(below);
        if (fluidBelow.is(fluidTag) && !fluidAtPos.is(fluidTag)) {
            return below;
        }
        return null;
    }

    public static boolean isFluidSurface(BlockGetter level, BlockPos pos, TagKey<Fluid> fluidTag) {
        return findSurfaceFluidPos(level, pos, fluidTag) != null;
    }

    /**
     * 尝试在液面上放一片踏痕；已有踏痕则续命。
     *
     * @param allowCenterFallback 允许退回「脚下那一格」再试一次
     */
    public static boolean tryPlaceTrail(Level level, BlockPos pos, boolean allowCenterFallback,
                                        TagKey<Fluid> fluidTag, Predicate<BlockState> isTrailState,
                                        Supplier<BlockState> trailStateSupplier) {
        BlockState current = level.getBlockState(pos);
        if (current.getBlock() instanceof TransientFoxLeafTrailBlock && isTrailState.test(current)) {
            ((TransientFoxLeafTrailBlock) current.getBlock()).refreshLifetime(level, pos);
            return true;
        }
        if (canPlaceTrailAt(level, pos, fluidTag, isTrailState)) {
            level.setBlock(pos, trailStateSupplier.get(), 3);
            return true;
        }
        if (allowCenterFallback) {
            BlockPos below = pos.below();
            if (canPlaceTrailAt(level, below, fluidTag, isTrailState)) {
                level.setBlock(below, trailStateSupplier.get(), 3);
                return true;
            }
        }
        return false;
    }

    private static boolean canPlaceTrailAt(Level level, BlockPos pos, TagKey<Fluid> fluidTag,
                                           Predicate<BlockState> isTrailState) {
        BlockState state = level.getBlockState(pos);
        if (!state.isAir() && !isTrailState.test(state)) {
            return false;
        }
        FluidState below = level.getFluidState(pos.below());
        return below.is(fluidTag) && below.isSource();
    }

    @Nullable
    private static BlockPos resolveSurfaceFluidPos(BlockPos blockPos, FluidState feetFluid,
                                                   FluidState belowFluid, TagKey<Fluid> fluidTag) {
        if (feetFluid.is(fluidTag)) {
            return blockPos;
        }
        if (belowFluid.is(fluidTag)) {
            return blockPos.below();
        }
        return null;
    }
}

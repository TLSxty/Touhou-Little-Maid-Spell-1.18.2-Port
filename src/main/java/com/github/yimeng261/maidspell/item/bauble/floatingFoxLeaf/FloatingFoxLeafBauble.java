package com.github.yimeng261.maidspell.item.bauble.floatingFoxLeaf;

import com.github.tartaricacid.touhoulittlemaid.api.bauble.IMaidBauble;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.yimeng261.maidspell.Config;
import com.github.yimeng261.maidspell.block.MaidSpellBlocks;
import com.github.yimeng261.maidspell.utils.FoxLeafOwnerEffectHelper;
import com.github.yimeng261.maidspell.utils.FoxLeafSurfaceHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

/**
 * 浮波狐叶：让女仆（以及范围内的主人）踏水而行，并在水面留下荷叶踏痕。
 * <p>
 * 移植自 1.9.2.1 反编译源 item/bauble/floatingFoxLeaf/FloatingFoxLeafBauble.java（136 行）。
 * SRG 方法名逐个换成 1.18.2 官方名，逻辑与数值保持一致。
 */
public class FloatingFoxLeafBauble implements IMaidBauble {

    private static final double MAX_SINK_SPEED = -0.05;
    private static final double SURFACE_FLOAT_SPEED = 0.08;
    private static final double MIN_HORIZONTAL_SPEED_SQR = 0.0025;
    private static final double SURFACE_Y_OFFSET = 0.9;

    @Override
    public void onTick(EntityMaid maid, ItemStack baubleItem) {
        if (maid.getLevel().isClientSide() || maid.isPassenger() || maid.isUnderWater()) {
            return;
        }
        BlockPos blockPos = maid.blockPosition();
        FluidState feetFluid = maid.getLevel().getFluidState(blockPos);
        FluidState belowFluid = maid.getLevel().getFluidState(blockPos.below());
        if (!feetFluid.is(FluidTags.WATER) && !belowFluid.is(FluidTags.WATER)) {
            applyToOwnerIfNearby(maid);
            return;
        }
        maid.fallDistance = 0.0f;
        keepEntityAtWaterSurface(maid, blockPos, feetFluid, belowFluid, maid.getDeltaMovement());
        Vec3 deltaMovement = maid.getDeltaMovement();
        ensureTrailUnderMaid(maid, maid.getLevel(), blockPos);
        if (deltaMovement.horizontalDistanceSqr() >= MIN_HORIZONTAL_SPEED_SQR
                && maid.tickCount % 2 == 0
                && maid.getRandom().nextFloat() < 0.55f) {
            spawnFloatingLeafTrail(maid);
        }
        applyToOwnerIfNearby(maid);
    }

    private static void keepEntityAtWaterSurface(LivingEntity entity, BlockPos blockPos,
                                                 FluidState feetFluid, FluidState belowFluid, Vec3 deltaMovement) {
        FoxLeafSurfaceHelper.keepEntityAtFluidSurface(entity, blockPos, feetFluid, belowFluid, deltaMovement,
                FluidTags.WATER, MAX_SINK_SPEED, SURFACE_FLOAT_SPEED, SURFACE_Y_OFFSET);
    }

    private static void applyToOwnerIfNearby(EntityMaid maid) {
        LivingEntity ownerEntity = maid.getOwner();
        if (!(ownerEntity instanceof Player owner)) {
            return;
        }
        if (owner.getLevel() != maid.getLevel() || owner.isSpectator() || owner.isPassenger()) {
            return;
        }
        double range = Config.floatingFoxLeafOwnerRange;
        if (!FoxLeafOwnerEffectHelper.isValidSupportingMaid(owner, maid, FluidTags.WATER, range)) {
            return;
        }
        if (!FoxLeafOwnerEffectHelper.canApplyOwnerSurfaceMotion(owner, FluidTags.WATER)) {
            return;
        }
        BlockPos ownerPos = owner.blockPosition();
        FluidState feetFluid = owner.getLevel().getFluidState(ownerPos);
        FluidState belowFluid = owner.getLevel().getFluidState(ownerPos.below());
        if (!feetFluid.is(FluidTags.WATER) && !belowFluid.is(FluidTags.WATER)) {
            return;
        }
        owner.fallDistance = 0.0f;
        keepEntityAtWaterSurface(owner, ownerPos, feetFluid, belowFluid, owner.getDeltaMovement());
        ensureTrailUnderMaid(owner, owner.getLevel(), ownerPos);
    }

    /** 在指定实体脚下的液面放一片踏痕（幂等：已有同类型踏痕只续命）。 */
    private static void ensureTrailUnderMaid(LivingEntity entity, Level level, BlockPos blockPos) {
        BlockPos fluidPos = FoxLeafSurfaceHelper.findSurfaceFluidPos(level, blockPos, FluidTags.WATER);
        if (fluidPos == null) {
            return;
        }
        FoxLeafSurfaceHelper.tryPlaceTrail(level, fluidPos.above(), false, FluidTags.WATER,
                MaidSpellBlocks::isFloatingFoxLeafTrail,
                () -> MaidSpellBlocks.getRandomFloatingFoxLeafTrail(level.random));
    }

    private static void spawnFloatingLeafTrail(EntityMaid maid) {
        Level level = maid.getLevel();
        BlockPos centerFluidPos = FoxLeafSurfaceHelper.findSurfaceFluidPos(level, maid.blockPosition(), FluidTags.WATER);
        if (centerFluidPos != null && FoxLeafSurfaceHelper.tryPlaceTrail(level, centerFluidPos.above(), true,
                FluidTags.WATER, MaidSpellBlocks::isFloatingFoxLeafTrail,
                () -> MaidSpellBlocks.getRandomFloatingFoxLeafTrail(level.random))) {
            return;
        }
        for (int i = 0; i < 2; ++i) {
            double offsetX = (maid.getRandom().nextDouble() - 0.5) * maid.getBbWidth() * 1.2;
            double offsetZ = (maid.getRandom().nextDouble() - 0.5) * maid.getBbWidth() * 1.2;
            BlockPos sample = new BlockPos(maid.getX() + offsetX, maid.getY(), maid.getZ() + offsetZ);
            BlockPos fluidPos = FoxLeafSurfaceHelper.findSurfaceFluidPos(level, sample, FluidTags.WATER);
            if (fluidPos == null) {
                continue;
            }
            if (!FoxLeafSurfaceHelper.tryPlaceTrail(level, fluidPos.above(), maid.getRandom().nextFloat() < 0.35f,
                    FluidTags.WATER, MaidSpellBlocks::isFloatingFoxLeafTrail,
                    () -> MaidSpellBlocks.getRandomFloatingFoxLeafTrail(level.random))) {
                continue;
            }
            return;
        }
    }
}

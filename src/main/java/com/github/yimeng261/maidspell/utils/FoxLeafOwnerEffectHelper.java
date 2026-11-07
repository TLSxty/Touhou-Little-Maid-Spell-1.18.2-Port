package com.github.yimeng261.maidspell.utils;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.yimeng261.maidspell.Config;
import com.github.yimeng261.maidspell.item.MaidSpellItems;
import com.github.yimeng261.maidspell.spell.manager.BaubleStateManager;
import javax.annotation.Nullable;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;

/**
 * 狐叶的主人共享效果判定（浮波狐叶的「踏水」与熔岩狐叶的「踏熔岩 + 火焰保护」共用）。
 * <p>
 * 移植自 1.9.2.1 反编译源 utils/FoxLeafOwnerEffectHelper.java（115 行）。
 * <strong>改动点</strong>：原实现调用 {@code ItemsUtil.hasBaubleItemInMaid(maid, item)}，
 * 但 TLM 1.18.2 1.2.2 的 {@code ItemsUtil} 没有这个方法（见 docs/已验证结论.md），
 * 这里改用本工程的 {@link BaubleStateManager#hasBauble(EntityMaid, net.minecraft.world.item.Item)}。
 */
public final class FoxLeafOwnerEffectHelper {

    private FoxLeafOwnerEffectHelper() {
    }

    public static boolean canOwnerStandOnFluid(Player owner, FluidState fluidState) {
        TagKey<Fluid> fluidTag = fluidTagForState(fluidState);
        if (fluidTag == null || owner.isShiftKeyDown() || owner.isEyeInFluid(fluidTag)) {
            return false;
        }
        return findSupportingMaid(owner, fluidTag) != null;
    }

    /**
     * 客户端专用：用服务端同步过来的状态判断主人能否站在液体表面。
     *
     * <p>{@link #canOwnerStandOnFluid} 依赖 {@code getMaidBauble()}，而 TLM 1.2.2 不会把女仆饰品栏
     * 同步到客户端（只写在 NBT 里），所以客户端调用它必然返回 false —— 表现就是主人踩水直接穿下去。
     * 这条路径改读 {@link FoxLeafSyncState}（服务端每 10 tick 算一次并推送）。</p>
     */
    public static boolean canOwnerStandOnFluidSynced(Player owner, FluidState fluidState) {
        TagKey<Fluid> fluidTag = fluidTagForState(fluidState);
        if (fluidTag == null || owner.isShiftKeyDown() || owner.isEyeInFluid(fluidTag)) {
            return false;
        }
        if (fluidTag == FluidTags.WATER) {
            return FoxLeafSyncState.hasFloating();
        }
        if (fluidTag == FluidTags.LAVA) {
            return FoxLeafSyncState.hasMolten();
        }
        return false;
    }

    public static boolean canApplyOwnerSurfaceMotion(Player owner, TagKey<Fluid> fluidTag) {
        return !owner.isShiftKeyDown() && !owner.isEyeInFluid(fluidTag);
    }

    public static boolean hasMoltenProtection(Player owner) {
        return findSupportingMaid(owner, FluidTags.LAVA) != null;
    }

    @Nullable
    public static EntityMaid findSupportingMaid(Player owner, TagKey<Fluid> fluidTag) {
        double range = rangeForFluid(fluidTag);
        for (EntityMaid maid : owner.getLevel().getEntitiesOfClass(EntityMaid.class,
                owner.getBoundingBox().inflate(range))) {
            if (!isValidSupportingMaid(owner, maid, fluidTag, range)) {
                continue;
            }
            return maid;
        }
        return null;
    }

    public static boolean isValidSupportingMaid(Player owner, EntityMaid maid, TagKey<Fluid> fluidTag, double range) {
        if (!maid.isAlive() || owner.getLevel() != maid.getLevel()) {
            return false;
        }
        if (!isOwner(owner, maid)) {
            return false;
        }
        if (owner.distanceToSqr(maid) > range * range) {
            return false;
        }
        return hasMatchingFoxLeaf(maid, fluidTag);
    }

    public static boolean isOwner(Player owner, EntityMaid maid) {
        LivingEntity maidOwner = maid.getOwner();
        if (maidOwner == owner) {
            return true;
        }
        return maid.getOwnerUUID() != null && maid.getOwnerUUID().equals(owner.getUUID());
    }

    public static boolean hasMatchingFoxLeaf(EntityMaid maid, TagKey<Fluid> fluidTag) {
        if (fluidTag == FluidTags.WATER) {
            return BaubleStateManager.hasBauble(maid, MaidSpellItems.FLOATING_FOX_LEAF);
        }
        if (fluidTag == FluidTags.LAVA) {
            return BaubleStateManager.hasBauble(maid, MaidSpellItems.MOLTEN_FOX_LEAF);
        }
        return false;
    }

    public static double rangeForFluid(TagKey<Fluid> fluidTag) {
        if (fluidTag == FluidTags.WATER) {
            return Config.floatingFoxLeafOwnerRange;
        }
        if (fluidTag == FluidTags.LAVA) {
            return Config.moltenFoxLeafOwnerRange;
        }
        return 0.0;
    }

    @Nullable
    private static TagKey<Fluid> fluidTagForState(FluidState fluidState) {
        if (fluidState.is(FluidTags.WATER)) {
            return FluidTags.WATER;
        }
        if (fluidState.is(FluidTags.LAVA)) {
            return FluidTags.LAVA;
        }
        return null;
    }
}

package com.github.yimeng261.maidspell.event;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.yimeng261.maidspell.Config;
import com.github.yimeng261.maidspell.utils.FoxLeafOwnerEffectHelper;
import com.github.yimeng261.maidspell.utils.FoxLeafSurfaceHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 主人共享狐叶踏水/踏岩浆。
 * <p>
 * 移植自 1.9.2.1 反编译源 event/FoxLeafOwnerWaterWalking.java（74 行）。
 * 原实现由主类 {@code forgeBus.register(...)} 手动登记，本工程改用
 * {@code @Mod.EventBusSubscriber} 自动登记（同 {@code StaranchorStunEffect} 的做法）。
 * <p>
 * 与饰品自身的 {@code applyToOwnerIfNearby} 是两条互补路径：饰品侧 9 槽 tick 触发，
 * 这里是玩家 tick 兜底（女仆饰品 tick 可能因区块卸载/距离而不跑）。
 */
@Mod.EventBusSubscriber(modid = Config.MOD_ID)
public final class FoxLeafOwnerWaterWalking {

    private static final double MAX_SINK_SPEED = -0.05;
    private static final double SURFACE_FLOAT_SPEED = 0.1;
    private static final double WATER_SURFACE_Y_OFFSET = 0.9;
    private static final double LAVA_SURFACE_Y_OFFSET = 1.0;

    private FoxLeafOwnerWaterWalking() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Player player = event.player;
        if (player.getLevel().isClientSide() || player.isSpectator() || player.isPassenger()) {
            return;
        }
        double range = Math.max(Config.floatingFoxLeafOwnerRange, Config.moltenFoxLeafOwnerRange);
        for (EntityMaid maid : player.getLevel().getEntitiesOfClass(EntityMaid.class,
                player.getBoundingBox().inflate(range))) {
            if (!maid.isAlive()) {
                continue;
            }
            if (FoxLeafOwnerEffectHelper.isValidSupportingMaid(player, maid, FluidTags.WATER,
                    Config.floatingFoxLeafOwnerRange)) {
                if (!FoxLeafOwnerEffectHelper.canApplyOwnerSurfaceMotion(player, FluidTags.WATER)) {
                    break;
                }
                BlockPos pos = player.blockPosition();
                FluidState feet = player.getLevel().getFluidState(pos);
                FluidState below = player.getLevel().getFluidState(pos.below());
                if (!feet.is(FluidTags.WATER) && !below.is(FluidTags.WATER)) {
                    break;
                }
                player.fallDistance = 0.0f;
                FoxLeafSurfaceHelper.keepEntityAtFluidSurface(player, pos, feet, below, player.getDeltaMovement(),
                        FluidTags.WATER, MAX_SINK_SPEED, SURFACE_FLOAT_SPEED, WATER_SURFACE_Y_OFFSET);
                break;
            }
            if (!FoxLeafOwnerEffectHelper.isValidSupportingMaid(player, maid, FluidTags.LAVA,
                    Config.moltenFoxLeafOwnerRange)) {
                continue;
            }
            if (!FoxLeafOwnerEffectHelper.canApplyOwnerSurfaceMotion(player, FluidTags.LAVA)) {
                break;
            }
            BlockPos pos = player.blockPosition();
            FluidState feet = player.getLevel().getFluidState(pos);
            FluidState below = player.getLevel().getFluidState(pos.below());
            if (!feet.is(FluidTags.LAVA) && !below.is(FluidTags.LAVA)) {
                break;
            }
            if (player.isOnFire()) {
                player.clearFire();
            }
            player.fallDistance = 0.0f;
            FoxLeafSurfaceHelper.keepEntityAtFluidSurface(player, pos, feet, below, player.getDeltaMovement(),
                    FluidTags.LAVA, MAX_SINK_SPEED, SURFACE_FLOAT_SPEED, LAVA_SURFACE_Y_OFFSET);
            break;
        }
    }
}

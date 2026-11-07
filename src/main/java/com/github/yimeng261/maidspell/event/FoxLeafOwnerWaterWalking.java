package com.github.yimeng261.maidspell.event;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.yimeng261.maidspell.Config;
import com.github.yimeng261.maidspell.network.NetworkHandler;
import com.github.yimeng261.maidspell.network.message.FoxLeafStateMessage;
import com.github.yimeng261.maidspell.utils.FoxLeafOwnerEffectHelper;
import com.github.yimeng261.maidspell.utils.FoxLeafSurfaceHelper;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

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

    /** 同步间隔：10 tick（0.5 秒）。踩水判定发生在客户端，状态变化必须及时送到。 */
    private static final int SYNC_INTERVAL_TICKS = 10;

    /** 上一次推送给该玩家的状态位（bit0=浮波狐叶，bit1=熔岩狐叶），避免每 10 tick 都发包。 */
    private static final Map<UUID, Integer> LAST_SENT = new ConcurrentHashMap<>();

    private FoxLeafOwnerWaterWalking() {
    }

    /**
     * 把「主人是否共享狐叶效果」推给该玩家本人的客户端。
     * <p>
     * 为什么必须同步：玩家实际踩水的判定发生在客户端 {@code LivingEntity.travel} 里的
     * {@code canStandOnFluid}，而 TLM 1.2.2 不会把女仆饰品栏同步到客户端
     * （{@code MaidBaubleInventory} 只写在 NBT，无任何同步包），
     * 因此客户端自己查饰品永远查不到，主人会直接穿过水面掉下去。
     * 上游 1.20.1 靠 {@code IMaidBauble#syncClient} 解决，本工程自建这条同步。
     */
    private static void syncOwnerState(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (serverPlayer.tickCount % SYNC_INTERVAL_TICKS != 0) {
            return;
        }
        boolean floating = FoxLeafOwnerEffectHelper.findSupportingMaid(player, FluidTags.WATER) != null;
        boolean molten = FoxLeafOwnerEffectHelper.findSupportingMaid(player, FluidTags.LAVA) != null;
        int flags = (floating ? 1 : 0) | (molten ? 2 : 0);
        Integer previous = LAST_SENT.get(serverPlayer.getUUID());
        if (previous != null && previous == flags) {
            return;
        }
        LAST_SENT.put(serverPlayer.getUUID(), flags);
        NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                new FoxLeafStateMessage(floating, molten));
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_SENT.remove(event.getPlayer().getUUID());
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
        syncOwnerState(player);
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

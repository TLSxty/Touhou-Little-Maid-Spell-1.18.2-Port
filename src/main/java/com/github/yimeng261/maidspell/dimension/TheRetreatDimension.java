package com.github.yimeng261.maidspell.dimension;

import com.github.yimeng261.maidspell.Config;
import com.github.yimeng261.maidspell.Global;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.ITeleporter;

/**
 * 退隐维度（the_retreat）—— 真实实现。
 *
 * <p>移植自 1.9.2.1 反编译源 {@code dimension/TheRetreatDimension.java}（220 行）。
 * 维度本身由数据包 json 注册（{@code data/touhou_little_maid_spell/dimension/the_retreat.json}
 * + {@code dimension_type/the_retreat.json}），1.18.2 的 {@code MinecraftServer#createLevels}
 * 会在开服时自动创建所有数据包维度，因此这里只做「判定 + 传送」两件事，
 * 不需要像上游 1.20.1 那样用 mixin 手工造 ServerLevel。</p>
 *
 * <p><b>个人维度（{@code the_retreat_<uuid>}）尚未启用</b>：上游靠
 * {@code PlayerRetreatManager} + {@code MinecraftServerMixin} 动态造世界，
 * 那部分随 P3 建筑移植一起评估；本版只提供共享维度
 * {@code touhou_little_maid_spell:the_retreat}，以及按 id 前缀识别
 * （这样以后加了个人维度也不用改判定逻辑）。</p>
 */
public final class TheRetreatDimension {

    /** 共享退隐维度 id。 */
    private static final ResourceLocation SHARED_RETREAT_LOCATION =
            new ResourceLocation(Config.MOD_ID, "the_retreat");

    private TheRetreatDimension() {
    }

    /** 玩家个人退隐维度的 key（本版尚未创建，仅保留 id 计算逻辑）。 */
    public static ResourceKey<Level> getPlayerRetreatDimension(java.util.UUID playerUUID) {
        return ResourceKey.create(Registry.DIMENSION_REGISTRY,
                new ResourceLocation(Config.MOD_ID, "the_retreat_" + playerUUID.toString().replace("-", "_")));
    }

    /** 共享退隐维度的 key。 */
    public static ResourceKey<Level> getSharedRetreatDimension() {
        return ResourceKey.create(Registry.DIMENSION_REGISTRY, SHARED_RETREAT_LOCATION);
    }

    /** 按 id 判定：同命名空间且 path 以 {@code the_retreat} 开头即视为退隐维度。 */
    public static boolean isRetreatDimension(ResourceLocation dimensionLocation) {
        return dimensionLocation.getNamespace().equals(Config.MOD_ID)
                && dimensionLocation.getPath().startsWith("the_retreat");
    }

    /** 实体是否处在任意退隐维度内。 */
    public static boolean isInRetreat(Entity entity) {
        return isRetreatDimension(entity.getLevel().dimension().location());
    }

    /** 玩家是否处在自己的个人退隐维度内。 */
    public static boolean isInOwnRetreat(ServerPlayer player) {
        return player.getLevel().dimension().equals(getPlayerRetreatDimension(player.getUUID()));
    }

    public static void teleportToRetreat(ServerPlayer player, ServerLevel retreatLevel) {
        teleportToRetreat(player, retreatLevel, player.blockPosition());
    }

    public static void teleportToRetreat(ServerPlayer player, ServerLevel retreatLevel, BlockPos targetPos) {
        try {
            BlockPos safePos = findSafePosition(retreatLevel, targetPos);
            player.changeDimension(retreatLevel, new RetreatTeleporter(safePos));
            Global.LOGGER.info("已将玩家 {} 传送到退隐维度 {}", player.getScoreboardName(), safePos);
        } catch (Exception e) {
            Global.LOGGER.error("传送玩家 {} 到退隐维度时出错: {}", player.getScoreboardName(), e.getMessage(), e);
        }
    }

    /** 把玩家送回主世界。 */
    public static void teleportFromRetreat(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) {
            return;
        }
        try {
            BlockPos safePos = findSafePosition(overworld, player.blockPosition());
            player.changeDimension(overworld, new RetreatTeleporter(safePos));
            Global.LOGGER.info("已将玩家 {} 从退隐维度送回主世界 {}", player.getScoreboardName(), safePos);
        } catch (Exception e) {
            Global.LOGGER.error("把玩家 {} 从退隐维度送回主世界时出错: {}", player.getScoreboardName(), e.getMessage(), e);
        }
    }

    // ------------------------------------------------------------------ 安全落点

    /**
     * 找一个能站人的落点。
     *
     * <p>上游用 {@code DensityFunction} 二分推算地表高度；1.18.2 直接有
     * {@link Level#getHeight(Heightmap.Types, int, int)}，语义更直接，改用后者。</p>
     */
    private static BlockPos findSafePosition(ServerLevel level, BlockPos targetPos) {
        int fallbackY = Math.max(level.getMinBuildHeight() + 5, 100);
        try {
            // 让目标区块先加载，否则高度图读到的是空区块。
            level.getChunk(targetPos.getX() >> 4, targetPos.getZ() >> 4);
            if (isSafeStandingPosition(level, targetPos)) {
                Global.LOGGER.debug("退隐维度：目标点本身可站立 {}", targetPos);
                return targetPos.immutable();
            }
            int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    targetPos.getX(), targetPos.getZ());
            BlockPos surfacePos = new BlockPos(targetPos.getX(), surfaceY, targetPos.getZ());
            if (isSafeStandingPosition(level, surfacePos)) {
                return surfacePos;
            }
            Global.LOGGER.info("退隐维度：地表点不可站立，改为搭建平台 {}", surfacePos);
            return createSafePlatform(level, surfacePos);
        } catch (Exception e) {
            Global.LOGGER.error("退隐维度：在 {} 附近寻找安全落点失败: {}", targetPos, e.getMessage(), e);
            BlockPos platformPos = new BlockPos(targetPos.getX(), fallbackY, targetPos.getZ());
            Global.LOGGER.info("退隐维度：搭建保底平台 {}", platformPos);
            return createSafePlatform(level, platformPos);
        }
    }

    /** 脚下一格是实心地面、非危险方块，且脚部与头部两格可站。 */
    private static boolean isSafeStandingPosition(ServerLevel level, BlockPos pos) {
        if (pos.getY() <= level.getMinBuildHeight() || pos.getY() >= level.getMaxBuildHeight() - 1) {
            return false;
        }
        BlockPos floorPos = pos.below();
        BlockState floorState = level.getBlockState(floorPos);
        if (!floorState.isFaceSturdy(level, floorPos, Direction.UP) || isHazardousFloor(floorState)) {
            return false;
        }
        return isOpenForPlayer(level, pos) && isOpenForPlayer(level, pos.above());
    }

    /** 无流体、且碰撞箱为空（能站进去）。 */
    private static boolean isOpenForPlayer(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        FluidState fluidState = level.getFluidState(pos);
        return fluidState.isEmpty() && state.getCollisionShape((BlockGetter) level, pos).isEmpty();
    }

    private static boolean isHazardousFloor(BlockState state) {
        Block block = state.getBlock();
        return block == Blocks.MAGMA_BLOCK
                || block == Blocks.CAMPFIRE
                || block == Blocks.SOUL_CAMPFIRE
                || block == Blocks.CACTUS;
    }

    /** 在目标点下方铺 3×3 圆石，并把目标点与其上方换成空气。 */
    private static BlockPos createSafePlatform(ServerLevel level, BlockPos pos) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                level.setBlock(pos.offset(x, -1, z), Blocks.COBBLESTONE.defaultBlockState(), 3);
            }
        }
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 3);
        return pos;
    }

    /** 落到指定方块坐标的传送器。 */
    public record RetreatTeleporter(BlockPos targetPos) implements ITeleporter {

        @Override
        public Entity placeEntity(Entity entity, ServerLevel currentWorld, ServerLevel destWorld,
                                  float yaw, Function<Boolean, Entity> repositionEntity) {
            Entity repositioned = repositionEntity.apply(false);
            if (repositioned != null) {
                repositioned.moveTo(targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5,
                        yaw, entity.getXRot());
            }
            return repositioned;
        }

        @Override
        public PortalInfo getPortalInfo(Entity entity, ServerLevel destWorld,
                                        Function<ServerLevel, PortalInfo> defaultPortalInfo) {
            return new PortalInfo(
                    new Vec3(targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5),
                    Vec3.ZERO, entity.getYRot(), entity.getXRot());
        }
    }
}

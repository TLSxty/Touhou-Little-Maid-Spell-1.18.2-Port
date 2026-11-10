package com.github.yimeng261.maidspell.event;

import com.github.yimeng261.maidspell.Config;
import com.github.yimeng261.maidspell.Global;
import com.github.yimeng261.maidspell.dimension.RetreatChunkTickets;
import com.github.yimeng261.maidspell.dimension.TheRetreatDimension;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * 退隐维度的刷怪与区块票据管控。
 *
 * <p><b>为什么需要这个类</b>：实机验收时出现过「从退隐维度返回主世界后服务器单 tick 永久挂死」。
 * ModernFix 的 watchdog 线程转储显示卡点在第三方模组 Champions 的
 * {@code ChampionHelper.nearActiveBeacon → Level.getBiome → ServerChunkCache.getChunk}
 * 同步区块加载递归上，本模组不在任何栈帧中；但触发条件是本模组造成的 ——
 * 退隐维度当时把<b>全量结构集</b>都纳入分布（见 {@code RetreatChunkGenerator} 的修复），
 * 玩家一传送过去就触发大面积区块生成与实体涌入，把那条递归放大到不收敛。</p>
 *
 * <p>结构集的问题已由 {@code RetreatChunkGenerator} 从根上解决。本类补另一半：
 * 退隐维度不刷敌意生物（与上游 {@code allowHostileMobSpawnsInRetreat} 的默认值 false 对齐），
 * 把「实体涌入」这个放大因子也压掉。</p>
 *
 * <p><b>注意</b>：1.18.2 的 {@code DimensionType} 没有 1.20 的
 * {@code monster_spawn_light_level} / {@code monster_spawn_block_light_limit} 字段
 * （javap 已核），因此无法靠 {@code dimension_type} 数据包关闭刷怪，只能拦刷怪事件。</p>
 */
@Mod.EventBusSubscriber(modid = Config.MOD_ID)
public final class RetreatSpawnControl {

    private RetreatSpawnControl() {
    }

    /**
     * 退隐维度不刷敌意生物。
     * <p>
     * 只清 {@link MobCategory#MONSTER}；动物、环境生物、水生生物保持原样。
     */
    @SubscribeEvent
    public static void onPotentialSpawns(WorldEvent.PotentialSpawns event) {
        if (event.getMobCategory() != MobCategory.MONSTER) {
            return;
        }
        LevelAccessor world = event.getWorld();
        if (!(world instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!TheRetreatDimension.isRetreatDimension(serverLevel.dimension().location())) {
            return;
        }
        // 事件暴露的是可变视图，逐条移除即可让本次刷怪无候选。
        List<MobSpawnSettings.SpawnerData> list = event.getSpawnerDataList();
        for (MobSpawnSettings.SpawnerData data : List.copyOf(list)) {
            event.removeSpawnerData(data);
        }
    }

    /**
     * 开服时清理退隐维度里的历史强制加载残留。
     * <p>
     * 旧版本的寻风铃只加票不收票，实测旧存档 {@code chunks.dat} 里留了 25 条
     * {@code data.Forced}（一整片 5×5）。这些会让存档每次加载都去强制生成那一片区块。
     */
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        try {
            int cleared = RetreatChunkTickets.clearStaleOnServerStarted(event.getServer());
            if (cleared > 0) {
                Global.LOGGER.info("[maidspell] 开服清理完成：退隐维度历史强制加载 {} 个区块", cleared);
            }
        } catch (Exception e) {
            // 清理失败不能影响开服。
            Global.LOGGER.warn("[maidspell] 清理退隐维度历史强制加载区块时出错", e);
        }
    }
}

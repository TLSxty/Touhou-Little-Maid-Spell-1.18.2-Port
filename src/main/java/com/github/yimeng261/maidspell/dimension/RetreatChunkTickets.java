package com.github.yimeng261.maidspell.dimension;

import com.github.yimeng261.maidspell.Global;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 退隐维度的临时区块强制加载（force-load）票据管理。
 * <p>
 * <b>为什么需要这个类</b>：寻风铃搜到隐世之境后必须先把那一片区块强制加载起来，
 * 否则玩家 {@code /tp} 过去是一片未生成的虚空。上游在
 * {@code HiddenRetreatStructure.afterPlace} 里调
 * {@code RetreatManager.unforceLoadStructureChunks} 收票；本工程走 D1=C 路线，没有那套管理器，
 * 第一版实现只加了票、没加解除，结果旧存档 {@code chunks.dat} 里残留了 25 条
 * {@code data.Forced}（一整片 5×5），永久钉住区块加载。
 * <p>
 * 本类的语义：
 * <ul>
 *   <li>{@link #forceTemporarily}：加票并排定一次性回收任务；</li>
 *   <li>{@link #clearStaleOnServerStarted}：开服时把退隐维度里<b>不属于本进程本次加票</b>的
 *       强制区块统一收回 —— 用于清理上面那种历史残留。</li>
 * </ul>
 * <p>
 * 之所以要区分「本次加票」与「存档遗留」，是因为退隐维度是模组私有的：
 * 玩家不该用 {@code /forceload} 在那里做长期加载，而残留会实打实地拖慢存档加载。
 */
public final class RetreatChunkTickets {

    /** 票据存活时长（tick，15 秒）。足够玩家点开聊天栏的 /tp 并完成传送。 */
    public static final int DEFAULT_LIFETIME_TICKS = 300;

    /** 维度 id → 已加票区块（{@link ChunkPos#toLong()}）集合。 */
    private static final Map<ResourceKey<Level>, Set<Long>> ACTIVE = new ConcurrentHashMap<>();

    private RetreatChunkTickets() {
    }

    /**
     * 以 {@code center} 为中心强制加载 {@code radius} 半径的方形区块，并在
     * {@code lifetimeTicks} 个 tick 后自动解除。
     *
     * @return 本次实际加票的区块数（已经加过票的不重复计）
     */
    public static int forceTemporarily(ServerLevel level, BlockPos center, int radius, int lifetimeTicks) {
        MinecraftServer server = level.getServer();
        Set<Long> tracked = ACTIVE.computeIfAbsent(level.dimension(), k -> ConcurrentHashMap.newKeySet());

        int chunkX = center.getX() >> 4;
        int chunkZ = center.getZ() >> 4;
        int applied = 0;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                long key = ChunkPos.asLong(chunkX + dx, chunkZ + dz);
                // 以 setChunkForced 的返回值判断「是否真的从无票变成有票」，
                // 不能只看集合，否则并发重复加票会让自己提前把别人的票据收掉。
                if (level.setChunkForced(chunkX + dx, chunkZ + dz, true)) {
                    tracked.add(key);
                    applied++;
                }
            }
        }

        if (applied > 0) {
            // 用 TickTask 而不是自己维护计数器：它挂在服务端主线程任务队列上，
            // 世界退出/重载时随之丢弃，不会留下跨存档的悬空回收。
            server.tell(new TickTask(server.getTickCount() + lifetimeTicks,
                    () -> release(level, chunkX, chunkZ, radius)));
        }
        return applied;
    }

    /** 解除以 {@code (chunkX, chunkZ)} 为中心、半径 {@code radius} 的强制加载。 */
    public static void release(ServerLevel level, int chunkX, int chunkZ, int radius) {
        Set<Long> tracked = ACTIVE.get(level.dimension());
        int released = 0;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int cx = chunkX + dx;
                int cz = chunkZ + dz;
                long key = ChunkPos.asLong(cx, cz);
                if (level.setChunkForced(cx, cz, false)) {
                    released++;
                }
                if (tracked != null) {
                    tracked.remove(key);
                }
            }
        }
        if (released > 0) {
            Global.LOGGER.debug("[maidspell] 已解除退隐维度 {} 个临时强制加载区块（中心 {},{}）",
                    released, chunkX, chunkZ);
        }
    }

    /**
     * 开服时清理退隐维度里的历史强制加载残留（本进程本次加过的票不碰）。
     *
     * @return 清理掉的区块数
     */
    public static int clearStaleOnServerStarted(MinecraftServer server) {
        int cleared = 0;
        for (ServerLevel level : server.getAllLevels()) {
            if (!TheRetreatDimension.isRetreatDimension(level.dimension().location())) {
                continue;
            }
            Set<Long> tracked = ACTIVE.get(level.dimension());
            LongSet forced = level.getForcedChunks();
            if (forced.isEmpty()) {
                continue;
            }
            // 先快照再改：setChunkForced 会在遍历过程中改动同一个 LongSet。
            long[] snapshot = new long[forced.size()];
            int i = 0;
            for (LongIterator it = forced.iterator(); it.hasNext(); ) {
                snapshot[i++] = it.nextLong();
            }
            for (long key : snapshot) {
                if (tracked != null && tracked.contains(key)) {
                    continue;
                }
                ChunkPos pos = new ChunkPos(key);
                if (level.setChunkForced(pos.x, pos.z, false)) {
                    cleared++;
                }
            }
        }
        if (cleared > 0) {
            Global.LOGGER.info("[maidspell] 已清理退隐维度 {} 个历史强制加载区块（旧版本寻风铃遗留）", cleared);
        }
        return cleared;
    }
}

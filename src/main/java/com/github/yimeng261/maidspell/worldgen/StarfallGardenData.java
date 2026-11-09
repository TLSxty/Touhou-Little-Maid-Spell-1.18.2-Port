package com.github.yimeng261.maidspell.worldgen;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 星落之庭的「一存档一座」门控。
 *
 * <p>移植自 1.9.2.1 反编译源 {@code worldgen/StarfallGardenData.java}（203 行），
 * 但<b>只保留门控语义，不带旧存档回填</b>（用户 m03661 决策 D2 = 「只搬门控，不回填」）：
 * 上游那些 {@code unlockForRetrofit} / {@code enterRetrofitGeneration} /
 * {@code RETROFIT_RESERVED} 全部服务于 {@code StarfallGardenRetrofit}，本工程不搬回购，
 * 因此一并删除，只留下四态里真正被使用到的 {@link Gate#OPEN} / {@link Gate#CLOSED} /
 * {@link Gate#GENERATING}。</p>
 *
 * <p>与原文的差异：</p>
 * <ul>
 *   <li>1.18.2 的花粉是 {@code net.minecraftforge.event.world.WorldEvent$Load}
 *       （1.20 是 {@code event.level.LevelEvent$Load}）；</li>
 *   <li>1.18.2 的 {@code SavedData#save(CompoundTag)} 是抽象方法，1.20 亦然；</li>
 *   <li>不保存 {@code RetrofitAttempted} 字段（无回填流程）；</li>
 *   <li>状态机保留原子性（跨线程的区块生成会并发进入）。</li>
 * </ul>
 */
public final class StarfallGardenData extends SavedData {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** 存档数据名。 */
    private static final String DATA_NAME = "touhou_little_maid_spell_starfall_garden";

    private static final String TAG_GENERATED = "Generated";
    private static final String TAG_POSITION = "Position";

    /** 门控状态：OPEN 可生成；GENERATING 正在生成；CLOSED 已生成过。 */
    private enum Gate {
        OPEN,
        GENERATING,
        CLOSED
    }

    private static final AtomicReference<Gate> GATE = new AtomicReference<>(Gate.OPEN);
    private static final AtomicBoolean RESTORED = new AtomicBoolean(false);

    private boolean generated;
    private long position = Long.MIN_VALUE;

    public StarfallGardenData() {
    }

    // ---------------------------------------------------------------- 存档读写

    public static StarfallGardenData load(CompoundTag tag) {
        StarfallGardenData data = new StarfallGardenData();
        data.generated = tag.getBoolean(TAG_GENERATED);
        data.position = tag.contains(TAG_POSITION) ? tag.getLong(TAG_POSITION) : Long.MIN_VALUE;
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putBoolean(TAG_GENERATED, this.generated);
        if (this.position != Long.MIN_VALUE) {
            tag.putLong(TAG_POSITION, this.position);
        }
        return tag;
    }

    // ---------------------------------------------------------------- 门控

    /** 是否已经（或正在）生成过。{@code OPEN} 之外都算「已占用」。 */
    public static boolean isPlaced() {
        return GATE.get() != Gate.OPEN;
    }

    /** 尝试从 {@code OPEN} 抢占生成权；成功返回 {@code true}。 */
    public static boolean tryBeginGeneration() {
        return GATE.compareAndSet(Gate.OPEN, Gate.GENERATING);
    }

    /** 生成失败时回滚，让后续区块还能再试。 */
    public static void abortGeneration() {
        GATE.compareAndSet(Gate.GENERATING, Gate.OPEN);
    }

    /** 标记已生成并异步落盘。 */
    public static void markPlaced(BlockPos pos) {
        GATE.set(Gate.CLOSED);
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        server.execute(() -> {
            try {
                StarfallGardenData data = overworldData(server);
                if (data != null) {
                    data.generated = true;
                    data.position = pos.asLong();
                    data.setDirty();
                }
            } catch (RuntimeException e) {
                LOGGER.error("写入星落之庭生成标记失败，重启后可能重复生成", e);
            }
        });
    }

    /** 供调试/提示用：已生成坐标（可能为 null）。 */
    @Nullable
    public static BlockPos placedPosition() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return null;
        }
        StarfallGardenData data = overworldData(server);
        if (data == null || data.position == Long.MIN_VALUE) {
            return null;
        }
        return BlockPos.of(data.position);
    }

    @Nullable
    private static StarfallGardenData overworldData(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        if (overworld == null) {
            return null;
        }
        return overworld.getDataStorage().computeIfAbsent(
                StarfallGardenData::load, StarfallGardenData::new, DATA_NAME);
    }

    /** 服务器启动时从存档恢复门控状态（只执行一次）。 */
    public static void restore(MinecraftServer server) {
        if (!RESTORED.compareAndSet(false, true)) {
            return;
        }
        StarfallGardenData data = overworldData(server);
        if (data != null && data.generated) {
            GATE.set(Gate.CLOSED);
        }
    }

    /** 新会话开始时把内存态打回 OPEN，等 {@link #restore} 再定。 */
    public static void resetSession() {
        RESTORED.set(false);
        GATE.set(Gate.OPEN);
    }

    // ---------------------------------------------------------------- 事件

    /** 事件订阅：与 {@code StellarEndshoreFixedSpawn} 同款风格，用嵌套类挂总线。 */
    @Mod.EventBusSubscriber(modid = com.github.yimeng261.maidspell.MaidSpellMod.MOD_ID)
    public static final class Events {

        private Events() {
        }

        @SubscribeEvent
        public static void onServerAboutToStart(ServerAboutToStartEvent event) {
            resetSession();
        }

        @SubscribeEvent
        public static void onWorldLoad(WorldEvent.Load event) {
            if (!(event.getWorld() instanceof ServerLevel level)) {
                return;
            }
            MinecraftServer server = level.getServer();
            if (server != null && level == server.overworld()) {
                restore(server);
            }
        }
    }
}

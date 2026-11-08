package com.github.yimeng261.maidspell.worldgen;

import com.github.yimeng261.maidspell.MaidSpellMod;
import com.github.yimeng261.maidspell.worldgen.structure.StellarEndshoreStructure;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.feature.ConfiguredStructureFeature;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.Random;

/**
 * 星途终岸「固定刷新」：每个存档在末地四个固定锚点之一强放一座星途终岸。
 *
 * <p>移植自 1.9.2.1 反编译源 {@code worldgen/StellarEndshoreFixedSpawn.java}（160 行）。
 * 与原文的差异：</p>
 * <ul>
 *   <li><b>查的是 {@link ConfiguredStructureFeature} 而不是 {@code Structure}</b>：1.18.2 没有
 *       {@code Structure} 类，配置对象才是能调 {@code generate} 的载体，注册表键位是
 *       {@link Registry#CONFIGURED_STRUCTURE_FEATURE_REGISTRY}；</li>
 *   <li>1.20 的 {@code structure.generate(registryAccess, generator, biomeSource, randomState,
 *       templateManager, seed, chunkPos, 0, level, biomePredicate)} 在 1.18.2 的签名是
 *       {@code generate(RegistryAccess, ChunkGenerator, BiomeSource, StructureManager, long,
 *       ChunkPos, int, LevelHeightAccessor, Predicate)} —— <b>没有 RandomState 参数</b>，
 *       且 {@code ServerChunkCache} 也没有 {@code randomState()} 方法；</li>
 *   <li>1.20 的 {@code level.structureManager()} 在 1.18.2 叫
 *       {@code level.structureFeatureManager()}；</li>
 *   <li>1.20 的 {@code structureManager.structureStartForFeature(...)} 在 1.18.2 叫
 *       {@link StructureFeatureManager#addReferenceForFeature};</li>
 *   <li>1.20 的 {@code structureManager.structureTemplateManager()} 在 1.18.2 是
 *       {@code level.getStructureManager()}；</li>
 *   <li>上游开头对 {@code GameRules} 的判空在本工程无实际作用（那行反编译产物看不出语义），
 *       这里改为「只在主线程的 SERVER_STARTED 阶段跑」，语义等价且更稳妥。</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = MaidSpellMod.MOD_ID)
public final class StellarEndshoreFixedSpawn {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String DATA_NAME = "touhou_little_maid_spell_stellar_endshore_fixed";

    /** 四个锚点：从出生点向四个方向各 800 格的空中平台高度。 */
    private static final BlockPos[] ANCHORS = {
            new BlockPos(800, 150, 0),
            new BlockPos(-800, 200, 0),
            new BlockPos(0, 200, 800),
            new BlockPos(0, 200, -800)
    };

    private StellarEndshoreFixedSpawn() {
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        // 交给服务端主线程执行：此时世界已完全就绪，区块读写安全。
        server.execute(() -> placeIfNeeded(server));
    }

    /** 按存档种子挑一个锚点（与上游同算法，保证同一存档每次都是同一个点）。 */
    private static BlockPos selectAnchor(long seed) {
        return ANCHORS[new Random(seed ^ 0x5EED0B5EEDL).nextInt(ANCHORS.length)];
    }

    private static void placeIfNeeded(MinecraftServer server) {
        ServerLevel end = server.getLevel(Level.END);
        if (end == null) {
            return;
        }
        DimensionDataStorage storage = end.getDataStorage();
        FixedSpawnData data = storage.computeIfAbsent(FixedSpawnData::load, FixedSpawnData::new, DATA_NAME);
        if (data.attempted) {
            return;
        }

        ResourceKey<ConfiguredStructureFeature<?, ?>> structureKey = ResourceKey.create(
                Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY,
                new ResourceLocation(MaidSpellMod.MOD_ID, "stellar_endshore"));
        ConfiguredStructureFeature<?, ?> configured = server.registryAccess()
                .registryOrThrow(Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY)
                .get(structureKey);
        if (configured == null) {
            LOGGER.warn("星途终岸固定刷新：找不到已配置的结构 {}，跳过。", structureKey.location());
            return;
        }

        BlockPos anchor = selectAnchor(end.getSeed());
        ChunkPos anchorChunkPos = new ChunkPos(anchor);
        StructureFeatureManager featureManager = end.structureFeatureManager();
        if (featureManager.getStructureAt(anchor, configured).isValid()) {
            data.attempted = true;
            data.position = anchor.asLong();
            storage.set(DATA_NAME, data);
            return;
        }

        ChunkGenerator generator = end.getChunkSource().getGenerator();
        StructureStart start;
        StellarEndshoreStructure.setFixedAnchor(anchor);
        try {
            start = configured.generate(
                    server.registryAccess(),
                    generator,
                    generator.getBiomeSource(),
                    end.getStructureManager(),
                    end.getSeed(),
                    anchorChunkPos,
                    0,
                    end,
                    holder -> true);
        } finally {
            StellarEndshoreStructure.clearFixedAnchor();
        }

        if (start == null || !start.isValid()) {
            LOGGER.warn("星途终岸固定刷新：结构生成失败，保留状态以便下次开服重试。锚点 {}", anchor);
            return;
        }

        BoundingBox box = start.getBoundingBox();
        ChunkPos min = new ChunkPos(SectionPos.blockToSectionCoord(box.minX()),
                SectionPos.blockToSectionCoord(box.minZ()));
        ChunkPos max = new ChunkPos(SectionPos.blockToSectionCoord(box.maxX()),
                SectionPos.blockToSectionCoord(box.maxZ()));
        try {
            for (int cx = min.x; cx <= max.x; cx++) {
                for (int cz = min.z; cz <= max.z; cz++) {
                    end.getChunk(cx, cz);
                }
            }
            for (int cx = min.x; cx <= max.x; cx++) {
                for (int cz = min.z; cz <= max.z; cz++) {
                    ChunkPos pos = new ChunkPos(cx, cz);
                    BoundingBox clip = new BoundingBox(
                            pos.getMinBlockX(), end.getMinBuildHeight(), pos.getMinBlockZ(),
                            pos.getMaxBlockX(), end.getMaxBuildHeight(), pos.getMaxBlockZ());
                    start.placeInChunk(end, featureManager, generator, end.getRandom(), clip, pos);
                }
            }
            // 把结构起点登记到锚点所在的 STRUCTURE_STARTS 区块，之后原版就能正常找到它。
            var anchorChunk = end.getChunk(anchorChunkPos.x, anchorChunkPos.z,
                    ChunkStatus.STRUCTURE_STARTS, true);
            featureManager.setStartForFeature(SectionPos.bottomOf(anchorChunk), configured, start, anchorChunk);
            for (int cx = min.x; cx <= max.x; cx++) {
                for (int cz = min.z; cz <= max.z; cz++) {
                    ChunkPos pos = new ChunkPos(cx, cz);
                    var chunk = end.getChunk(pos.x, pos.z, ChunkStatus.STRUCTURE_STARTS, true);
                    featureManager.addReferenceForFeature(SectionPos.bottomOf(chunk), configured,
                            start.getChunkPos().toLong(), chunk);
                }
            }
        } catch (RuntimeException e) {
            LOGGER.error("星途终岸固定刷新失败，保留状态以便下次开服重试。锚点 {}", anchor, e);
            return;
        }

        data.attempted = true;
        data.position = anchor.asLong();
        storage.set(DATA_NAME, data);
        storage.save();
        LOGGER.info("星途终岸固定刷新：已在末地 {} 生成（覆盖区块 {}~{}）", anchor, min, max);
    }

    /** 存档级标记：只尝试一次，失败也记录状态避免每次开服重复。 */
    private static final class FixedSpawnData extends SavedData {

        private static final String TAG_ATTEMPTED = "Attempted";
        private static final String TAG_POSITION = "Position";

        private boolean attempted;
        private long position;

        private FixedSpawnData() {
        }

        private static FixedSpawnData load(@Nullable CompoundTag tag) {
            FixedSpawnData data = new FixedSpawnData();
            if (tag != null) {
                data.attempted = tag.getBoolean(TAG_ATTEMPTED);
                data.position = tag.getLong(TAG_POSITION);
            }
            return data;
        }

        @Override
        public CompoundTag save(CompoundTag tag) {
            tag.putBoolean(TAG_ATTEMPTED, this.attempted);
            tag.putLong(TAG_POSITION, this.position);
            return tag;
        }
    }
}

package com.github.yimeng261.maidspell.dimension;

import com.github.yimeng261.maidspell.Config;
import com.github.yimeng261.maidspell.Global;
import com.google.common.collect.ImmutableList;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraft.world.level.storage.DerivedLevelData;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;

import java.util.Optional;

/**
 * 运行时补建 the_retreat 维度。
 * <p>
 * 为什么需要这个类：1.18.2 的 {@code MinecraftServer.createLevels} 只遍历
 * {@code level.dat} 里保存的 {@code WorldGenSettings.dimensions()} 快照。存档建立时若还没有本维度，
 * 之后无论数据包里补多少 dimension json，开服都不会创建它 —— 表现为
 * {@code /execute in touhou_little_maid_spell:the_retreat ...} 报「未知的维度」。
 * <p>
 * 因此这里在 {@code MinecraftServer.loadLevel} 尾部（原版维度已建完、玩家尚未登录）检查一次，
 * 缺了就手工 new 一个 {@link ServerLevel} 放进服务器的世界表。
 */
public final class RetreatDimensionFactory {

    /** 空进度监听器：运行期补建维度不需要向界面汇报加载进度。 */
    private static final ChunkProgressListener NO_PROGRESS = new ChunkProgressListener() {
        @Override
        public void updateSpawnPos(ChunkPos pos) {
        }

        @Override
        public void onStatusChange(ChunkPos pos, ChunkStatus status) {
        }

        @Override
        public void start() {
        }

        @Override
        public void stop() {
        }
    };

    private RetreatDimensionFactory() {
    }

    /**
     * 若共享退隐维度尚未创建则补建。
     *
     * @return 本次是否真的新建了维度
     */
    public static boolean createSharedIfMissing(MinecraftServer server,
                                                LevelStorageSource.LevelStorageAccess storageSource) {
        ResourceKey<Level> levelKey = TheRetreatDimension.getSharedRetreatDimension();
        if (server.forgeGetWorldMap().containsKey(levelKey)) {
            return false;
        }
        try {
            ServerLevel level = buildLevel(server, storageSource, levelKey);
            server.forgeGetWorldMap().put(levelKey, level);
            server.getPlayerList().addWorldborderListener(level);
            MinecraftForge.EVENT_BUS.post(new WorldEvent.Load(level));
            server.markWorldsDirty();
            Global.LOGGER.info("[maidspell] 已补建退隐维度 {}（存档建立时该维度尚不存在）", levelKey.location());
            return true;
        } catch (Exception e) {
            Global.LOGGER.error("[maidspell] 补建退隐维度 {} 失败", levelKey.location(), e);
            return false;
        }
    }

    private static ServerLevel buildLevel(MinecraftServer server,
                                          LevelStorageSource.LevelStorageAccess storageSource,
                                          ResourceKey<Level> levelKey) {
        ResourceLocation id = levelKey.location();
        RegistryAccess.Frozen access = server.registryAccess();

        // 维度类型必须有值：数据包里的 dimension_type/the_retreat.json 在前面已加载完毕。
        ResourceKey<DimensionType> typeKey =
                ResourceKey.create(Registry.DIMENSION_TYPE_REGISTRY, id);
        Optional<Holder<DimensionType>> typeHolder =
                access.registryOrThrow(Registry.DIMENSION_TYPE_REGISTRY).getHolder(typeKey);
        if (typeHolder.isEmpty()) {
            throw new IllegalStateException("找不到维度类型 " + id + "，检查数据包 dimension_type 配置");
        }

        ServerLevel overworld = server.overworld();
        long seed = overworld.getSeed();

        // 生成器：与原版 dimension json 里 noise + fixed biome 的描述保持等价。
        Registry<StructureSet> structureSets =
                access.registryOrThrow(Registry.STRUCTURE_SET_REGISTRY);
        Registry<NormalNoise.NoiseParameters> noises =
                access.registryOrThrow(Registry.NOISE_REGISTRY);
        Registry<net.minecraft.world.level.biome.Biome> biomes =
                access.registryOrThrow(Registry.BIOME_REGISTRY);
        Registry<NoiseGeneratorSettings> noiseSettings =
                access.registryOrThrow(Registry.NOISE_GENERATOR_SETTINGS_REGISTRY);

        FixedBiomeSource biomeSource =
                new FixedBiomeSource(biomes.getHolderOrThrow(Biomes.FLOWER_FOREST));
        ChunkGenerator generator = new NoiseBasedChunkGenerator(
                structureSets,
                noises,
                biomeSource,
                seed,
                noiseSettings.getHolderOrThrow(NoiseGeneratorSettings.OVERWORLD));

        ServerLevelData levelData =
                new DerivedLevelData(server.getWorldData(), server.getWorldData().overworldData());
        boolean isDebug = server.getWorldData().worldGenSettings().isDebug();

        // 第 2 参 Executor 直接传 server 本体（BlockableEventLoop 实现了 Executor）。
        return new ServerLevel(
                server,
                server,
                storageSource,
                levelData,
                levelKey,
                typeHolder.get(),
                NO_PROGRESS,
                generator,
                isDebug,
                BiomeManager.obfuscateSeed(seed),
                ImmutableList.of(),
                false);
    }

    /** 供日志/调试使用：描述当前维度是否已就绪。 */
    public static String describe(MinecraftServer server) {
        ResourceKey<Level> key = TheRetreatDimension.getSharedRetreatDimension();
        boolean created = server.forgeGetWorldMap().containsKey(key);
        return Config.MOD_ID + " 退隐维度 " + key.location() + " 已创建=" + created;
    }
}

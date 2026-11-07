package com.github.yimeng261.maidspell.dimension;

import com.github.yimeng261.maidspell.Config;
import com.github.yimeng261.maidspell.Global;
import com.mojang.serialization.Lifecycle;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.WritableRegistry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

import java.util.Optional;

/**
 * 让旧存档也能拥有 the_retreat 维度。
 * <p>
 * 为什么需要这个类：1.18.2 的 {@code MinecraftServer.createLevels} 只遍历 level.dat 里保存的
 * {@code WorldGenSettings.dimensions()} 快照。存档建立时若还没有本维度，之后无论数据包里补多少
 * dimension json，开服都不会创建它 —— 表现为 {@code /execute in
 * touhou_little_maid_spell:the_retreat ...} 报「未知的维度」。
 * <p>
 * 做法：在 {@code createLevels} 开头把缺失的 {@link LevelStem} 注册进
 * {@code WorldGenSettings.dimensions()}，随后<b>让原版循环自己去 new ServerLevel</b>。
 * <b>绝对不要自己 new ServerLevel</b>：原版建维度时还要做 readScoreboard / CommandStorage /
 * setInitialSpawn / WorldEvent.Load 等一串步骤，漏掉会出现「维度里没有任何方块」以及
 * 「传送过去卡死」——区块从未生成，实体定位时死等 {@code ServerChunkCache.getChunk}。
 * <p>
 * 注册进 {@code WorldGenSettings} 的维度会随存档写回 level.dat，所以只需补建一次。
 */
public final class RetreatDimensionFactory {

    private RetreatDimensionFactory() {
    }

    /**
     * 若共享退隐维度还没出现在 {@code WorldGenSettings.dimensions()} 里，就补注册一个 LevelStem。
     * <p>
     * 必须在 {@code createLevels} 之前调用，否则原版循环已经跑完了。
     *
     * @return 本次是否真的注册了新维度
     */
    public static boolean registerSharedStemIfMissing(MinecraftServer server) {
        ResourceKey<Level> levelKey = TheRetreatDimension.getSharedRetreatDimension();
        ResourceLocation id = levelKey.location();
        ResourceKey<LevelStem> stemKey = ResourceKey.create(Registry.LEVEL_STEM_REGISTRY, id);

        Registry<LevelStem> dimensions = server.getWorldData().worldGenSettings().dimensions();
        if (dimensions.containsKey(stemKey)) {
            return false;
        }
        if (!(dimensions instanceof WritableRegistry<LevelStem> writable)) {
            Global.LOGGER.error("[maidspell] 无法补建退隐维度 {}：WorldGenSettings.dimensions 不是可写注册表（{}）",
                    id, dimensions.getClass().getName());
            return false;
        }
        try {
            // 从 level.dat 解码出来的注册表通常是冻结状态，先解冻才能写入。
            if (dimensions instanceof MappedRegistry<LevelStem> mapped) {
                mapped.unfreeze();
            }
            LevelStem stem = buildStem(server);
            writable.register(stemKey, stem, Lifecycle.stable());
            Global.LOGGER.info("[maidspell] 已补建退隐维度 {}（存档建立时该维度尚不存在，本次已写入世界生成设置）", id);
            return true;
        } catch (Exception e) {
            Global.LOGGER.error("[maidspell] 补建退隐维度 {} 失败", id, e);
            return false;
        }
    }

    private static LevelStem buildStem(MinecraftServer server) {
        ResourceLocation id = TheRetreatDimension.getSharedRetreatDimension().location();
        RegistryAccess.Frozen access = server.registryAccess();

        // 维度类型来自数据包 dimension_type/the_retreat.json，此时已加载完毕。
        ResourceKey<DimensionType> typeKey = ResourceKey.create(Registry.DIMENSION_TYPE_REGISTRY, id);
        Optional<Holder<DimensionType>> typeHolder =
                access.registryOrThrow(Registry.DIMENSION_TYPE_REGISTRY).getHolder(typeKey);
        if (typeHolder.isEmpty()) {
            throw new IllegalStateException("找不到维度类型 " + id + "，检查数据包 dimension_type 配置");
        }

        long seed = server.overworld().getSeed();

        // 生成器与数据包 dimension json 里的 noise + fixed biome 描述保持等价。
        Registry<StructureSet> structureSets = access.registryOrThrow(Registry.STRUCTURE_SET_REGISTRY);
        Registry<NormalNoise.NoiseParameters> noises = access.registryOrThrow(Registry.NOISE_REGISTRY);
        Registry<net.minecraft.world.level.biome.Biome> biomes = access.registryOrThrow(Registry.BIOME_REGISTRY);
        Registry<NoiseGeneratorSettings> noiseSettings =
                access.registryOrThrow(Registry.NOISE_GENERATOR_SETTINGS_REGISTRY);

        FixedBiomeSource biomeSource = new FixedBiomeSource(biomes.getHolderOrThrow(Biomes.FLOWER_FOREST));
        ChunkGenerator generator = new NoiseBasedChunkGenerator(
                structureSets,
                noises,
                biomeSource,
                seed,
                noiseSettings.getHolderOrThrow(NoiseGeneratorSettings.OVERWORLD));

        return new LevelStem(typeHolder.get(), generator);
    }

    /** 供日志/调试使用：描述当前维度是否已就绪。 */
    public static String describe(MinecraftServer server) {
        ResourceKey<Level> key = TheRetreatDimension.getSharedRetreatDimension();
        ResourceKey<LevelStem> stemKey = ResourceKey.create(Registry.LEVEL_STEM_REGISTRY, key.location());
        boolean inStem = server.getWorldData().worldGenSettings().dimensions().containsKey(stemKey);
        boolean created = server.forgeGetWorldMap().containsKey(key);
        return Config.MOD_ID + " 退隐维度 " + key.location() + " 已注册=" + inStem + " 已创建=" + created;
    }
}

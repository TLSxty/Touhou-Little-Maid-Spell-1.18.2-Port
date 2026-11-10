package com.github.yimeng261.maidspell.worldgen.structure;

import com.github.yimeng261.maidspell.Global;
import com.github.yimeng261.maidspell.dimension.TheRetreatDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.RandomSource;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.feature.configurations.JigsawConfiguration;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGenerator;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGeneratorSupplier;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.Random;

/**
 * 隐世之境（hidden_retreat）—— 只在退隐维度（the_retreat / the_retreat_*）内生成。
 *
 * <p>移植自 1.9.2.1 反编译源 {@code worldgen/structure/HiddenRetreatStructure.java}（19708 字节）。</p>
 *
 * <p><b>架构差异（1.20.1 → 1.18.2）</b></p>
 * <ul>
 *   <li>上游 {@code extends Structure} 并覆写 {@code generate(...)} 做门控；1.18.2 没有 {@code Structure}
 *       类，注册的是 {@link StructureFeature} 实例本身，且 {@code ConfiguredStructureFeature.generate}
 *       会<b>绕过</b> {@code canGenerate} 直接调 {@code pieceGeneratorSupplier}。故本实现按
 *       批次 4 {@code StarfallGardenStructure} 的范式，把全部门控收进 {@link #createGenerator} 单点。</li>
 *   <li>上游用 {@code ChunkGeneratorAccessor} 取 dimensionKey 判维度（需要
 *       {@code ChunkGeneratorMixin} + {@code ServerLevelMixin}）；本工程不搬这两个 mixin，
 *       改为「拿当前 server 的所有退隐维度、比对 generator 实例」——退隐维度的 generator 一定是
 *       独立的 {@code NoiseBasedChunkGenerator} 实例，引用比较可靠且无 mixin 依赖。</li>
 *   <li>上游 {@code m_214086_} 里调 8 参
 *       {@code JigsawPlacement.m_227238_(context, startPool, Optional.empty(), size, centerPos, false, Optional.empty(), 150)}；
 *       1.18.2 只有 5 参版本，且 {@code size} 只能来自 {@link JigsawConfiguration}（json 的 {@code config.size}，
 *       上限 7）。</li>
 *   <li>上游 {@code StructureStart.EMPTY} → 1.18.2 用 {@link Optional#empty()} 表达「本区块不生成」。</li>
 * </ul>
 *
 * <p><b>地形筛选（原样移植）</b>：在中心区块周围 5×5 区块各随机取 5 个点，用 Welford 算法累积
 * 地表高度方差；方差 &gt; {@code 16.0} 或水域采样过多即放弃。数学与上游逐行一致。</p>
 */
public class HiddenRetreatStructure extends StructureFeature<JigsawConfiguration> {

    /** 允许的地表高度方差上限（上游 MAX_TERRAIN_VARIANCE）。 */
    private static final double MAX_TERRAIN_VARIANCE = 16.0;

    /** 每个候选区块的采样点数（上游 SAMPLES_PER_CHUNK）。 */
    private static final int SAMPLES_PER_CHUNK = 5;

    /** 以中心区块为中心向外检查的半径（上游 TERRAIN_CHECK_RANGE）。 */
    private static final int TERRAIN_CHECK_RANGE = 2;

    public HiddenRetreatStructure() {
        // 三参构造器：第三个参数是 PostPlacementProcessor，1.18.2 支持挂 afterPlace，无需 mixin。
        super(JigsawConfiguration.CODEC, HiddenRetreatStructure::createGenerator,
                HiddenRetreatStructure::afterPlace);
    }

    @Override
    public GenerationStep.Decoration step() {
        return GenerationStep.Decoration.SURFACE_STRUCTURES;
    }

    // ------------------------------------------------------------------ 门控 + 生成

    private static Optional<PieceGenerator<JigsawConfiguration>> createGenerator(
            PieceGeneratorSupplier.Context<JigsawConfiguration> context) {
        // D4：只在退隐维度生成。主世界 / 其他维度一律不产。
        if (!isRetreatGenerator(context.chunkGenerator())) {
            return Optional.empty();
        }

        ChunkPos chunkPos = context.chunkPos();
        OptionalInt terrainHeight = checkTerrainAndGetHeight(context, chunkPos);
        if (terrainHeight.isEmpty()) {
            return Optional.empty();
        }

        ChunkGenerator generator = context.chunkGenerator();
        if (!(generator instanceof NoiseBasedChunkGenerator noiseGenerator)) {
            return Optional.empty();
        }

        int centerX = chunkPos.getMinBlockX() + 8;
        int centerZ = chunkPos.getMinBlockZ() + 8;
        int height = estimateSurfaceHeight(noiseGenerator.router().depth(), centerX, centerZ,
                context.heightAccessor());

        // 顶点必须落在世界高度内，否则 JigsawPlacement 的包围盒会越界。
        height = Math.max(context.heightAccessor().getMinBuildHeight() + 1,
                Math.min(height, context.heightAccessor().getMaxBuildHeight() - 2));

        BlockPos centerPos = new BlockPos(centerX, height, centerZ);

        // useExpansionHack 必须为 false（否则 y 会被再叠一次地表高度，结构飞到天上）。
        return JigsawPlacement.addPieces(context,
                (structureManager, element, pos, groundLevelDelta, rotation, box) ->
                        new PoolElementStructurePiece(structureManager, element, pos, groundLevelDelta,
                                rotation, box),
                centerPos, false, false);
    }

    /**
     * 传入的 generator 是否属于某个退隐维度。
     *
     * <p>退隐维度由 {@code RetreatDimensionFactory} 在 {@code createLevels} HEAD 补 LevelStem 后由
     * 原版创建，每个维度一个独立的 {@code NoiseBasedChunkGenerator} 实例 → 引用比较即可，
     * 不会与主世界同类型生成器混淆。</p>
     */
    private static boolean isRetreatGenerator(ChunkGenerator generator) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return false;
        }
        for (ServerLevel level : server.getAllLevels()) {
            if (!TheRetreatDimension.isRetreatDimension(level.dimension().location())) {
                continue;
            }
            if (level.getChunkSource().getGenerator() == generator) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ 地形筛选

    /**
     * 检查 5×5 区块的地表是否足够平坦、且不在水下；返回平均地表高度。
     *
     * <p>Welford 在线方差 + 水域采样，与上游 {@code checkTerrainAndGetHeight} 逐行一致。</p>
     */
    private static OptionalInt checkTerrainAndGetHeight(
            PieceGeneratorSupplier.Context<JigsawConfiguration> context, ChunkPos centerChunk) {
        ChunkGenerator generator = context.chunkGenerator();
        if (!(generator instanceof NoiseBasedChunkGenerator noiseGenerator)) {
            return OptionalInt.empty();
        }
        NoiseRouter router = noiseGenerator.router();
        DensityFunction depthFn = router.depth();
        DensityFunction finalDensityFn = router.finalDensity();
        int seaLevel = generator.getSeaLevel();
        LevelHeightAccessor heightAccessor = context.heightAccessor();

        int n = 0;
        double mean = 0.0;
        double m2 = 0.0;
        int waterSamples = 0;

        for (int dx = -TERRAIN_CHECK_RANGE; dx <= TERRAIN_CHECK_RANGE; dx++) {
            for (int dz = -TERRAIN_CHECK_RANGE; dz <= TERRAIN_CHECK_RANGE; dz++) {
                ChunkPos checkChunk = new ChunkPos(centerChunk.x + dx, centerChunk.z + dz);
                RandomSource random = new LegacyRandomSource(checkChunk.toLong());
                int minX = checkChunk.getMinBlockX();
                int minZ = checkChunk.getMinBlockZ();

                for (int i = 0; i < SAMPLES_PER_CHUNK; i++) {
                    int x = minX + random.nextInt(16);
                    int z = minZ + random.nextInt(16);
                    int surfaceY = estimateSurfaceHeight(depthFn, x, z, heightAccessor);

                    double delta = surfaceY - mean;
                    mean += delta / (++n);
                    double delta2 = surfaceY - mean;
                    m2 += delta * delta2;

                    if (n >= 10 && m2 / n > MAX_TERRAIN_VARIANCE) {
                        return OptionalInt.empty();
                    }

                    if (surfaceY < seaLevel) {
                        waterSamples++;
                    } else if (surfaceY < seaLevel + 5) {
                        DensityFunction.SinglePointContext singlePoint =
                                new DensityFunction.SinglePointContext(x, seaLevel - 1, z);
                        if (finalDensityFn.compute(singlePoint) <= 0.0) {
                            waterSamples++;
                        }
                    }

                    if (waterSamples > n / 10) {
                        return OptionalInt.empty();
                    }
                }
            }
        }

        if (n < 2) {
            return OptionalInt.empty();
        }
        if (m2 / n > MAX_TERRAIN_VARIANCE) {
            return OptionalInt.empty();
        }
        int meanHeight = (int) mean;
        if (meanHeight < seaLevel || waterSamples * 10 >= n) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(meanHeight);
    }

    /**
     * 用 {@code depth} 密度函数在竖直方向二分，估出该 (x,z) 处的地表高度。
     *
     * <p>与上游 {@code estimateSurfaceHeight} 一致：{@code depth > 0} 表示在地下，取上界。</p>
     */
    private static int estimateSurfaceHeight(DensityFunction depthFn, int x, int z,
                                             LevelHeightAccessor heightAccessor) {
        int lo = heightAccessor.getMinBuildHeight();
        int hi = heightAccessor.getMaxBuildHeight();
        while (hi - lo > 2) {
            int mid = (lo + hi) >>> 1;
            double depthAtMid = depthFn.compute(new DensityFunction.SinglePointContext(x, mid, z));
            if (depthAtMid > 0.0) {
                lo = mid;
            } else {
                hi = mid;
            }
        }
        return hi;
    }

    // ------------------------------------------------------------------ 放置后

    /**
     * 结构放置后的回调。
     *
     * <p>上游在此处做三件依赖「第二层」的事：解析个人维度 owner、写
     * {@code RetreatDimensionData.markStructureGenerated}、归还强制加载的区块。本工程走 D1=C
     * 路线（不搬 {@code RetreatManager} / {@code RetreatDimensionData} / 个人维度），故只保留日志。</p>
     */
    private static void afterPlace(WorldGenLevel level, StructureFeatureManager structureManager,
                                   ChunkGenerator chunkGenerator, Random random, BoundingBox box,
                                   ChunkPos chunkPos, PiecesContainer pieces) {
        BoundingBox bounds = pieces.calculateBoundingBox();
        Global.LOGGER.info("隐世之境生成于 {} 的区块 {}，包围盒 {}",
                level.getLevel().dimension().location(), chunkPos, bounds);
    }
}

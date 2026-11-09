package com.github.yimeng261.maidspell.worldgen.structure;

import java.util.Optional;
import java.util.OptionalInt;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.feature.configurations.JigsawConfiguration;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGenerator;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGeneratorSupplier;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;

/**
 * 遗物圣所（地表拼图结构，只生成在地形起伏小的高地）。
 *
 * <p>移植自 1.9.2.1 反编译源 {@code worldgen/structure/RelicSanctumStructure.java}（1.20.1，147 行）。</p>
 *
 * <p><b>与原文的差异</b>（1.20 → 1.18.2）：
 * <ul>
 *   <li>上游从 {@code context.randomState().router()} 取 {@link NoiseRouter}；1.18.2 的
 *       {@code PieceGeneratorSupplier.Context} 没有 {@code randomState()}（{@code ServerChunkCache}
 *       也没有随机状态访问器），改用 {@code ((NoiseBasedChunkGenerator) chunkGenerator).router()}。</li>
 *   <li>{@code RandomSource.create(long)} 在 1.18.2 的 {@code net.minecraft.util} 包里不存在，
 *       用 {@link WorldgenRandom}{@code +}{@link LegacyRandomSource} 得到可复现随机。</li>
 *   <li>上游 CODEC 的 {@code size}/{@code max_distance_from_center} 在 1.18.2 无处安放
 *       （{@link JigsawConfiguration} 只认 {@code start_pool}/{@code size}），常量固化在类里。</li>
 *   <li>{@code Structure.GenerationStub} 不存在，改用 {@link PieceGenerator} 直通
 *       {@link JigsawPlacement#addPieces}。</li>
 * </ul></p>
 */
public class RelicSanctumStructure extends StructureFeature<JigsawConfiguration> {

    /** 采样点的地形高度标准差平方上限；超过即认为地形太崎岖，放弃。 */
    private static final double MAX_TERRAIN_VARIANCE = 16.0D;
    /** 每个区块的采样点数。 */
    private static final int SAMPLES_PER_CHUNK = 5;
    /** 以中心区块为中心，向四周扩展多少区块做地形检查。 */
    private static final int TERRAIN_CHECK_RANGE = 1;

    public RelicSanctumStructure() {
        super(JigsawConfiguration.CODEC, RelicSanctumStructure::createGenerator);
    }

    @Override
    public GenerationStep.Decoration step() {
        // 必须覆写：vanilla STEP 表里没有自定义 StructureFeature，返回 null 会让区块装饰阶段 NPE。
        return GenerationStep.Decoration.SURFACE_STRUCTURES;
    }

    private static Optional<PieceGenerator<JigsawConfiguration>> createGenerator(
            PieceGeneratorSupplier.Context<JigsawConfiguration> context) {
        if (!context.validBiomeOnTop(Heightmap.Types.WORLD_SURFACE_WG)) {
            return Optional.empty();
        }
        if (!(context.chunkGenerator() instanceof NoiseBasedChunkGenerator noiseGenerator)) {
            return Optional.empty();
        }

        NoiseRouter router = noiseGenerator.router();
        ChunkPos chunkPos = context.chunkPos();

        OptionalInt terrainHeight = checkTerrainAndGetHeight(context, router, chunkPos);
        if (terrainHeight.isEmpty()) {
            return Optional.empty();
        }

        int x = chunkPos.getMinBlockX() + 8;
        int z = chunkPos.getMinBlockZ() + 8;
        int surfaceY = estimateSurfaceHeight(router.depth(), x, z, context);
        BlockPos startPos = new BlockPos(x, surfaceY + 1, z);

        return JigsawPlacement.addPieces(
                context,
                (structureManager, element, pos, groundLevelDelta, rotation, box) ->
                        new PoolElementStructurePiece(structureManager, element, pos, groundLevelDelta, rotation, box),
                startPos, false, false);
    }

    /**
     * 逐区块采样地表高度，用 Welford 增量法算方差；地形太崎岖或水域过多则放弃。
     */
    private static OptionalInt checkTerrainAndGetHeight(PieceGeneratorSupplier.Context<JigsawConfiguration> context,
                                                        NoiseRouter router, ChunkPos centerChunk) {
        DensityFunction depthFn = router.depth();
        DensityFunction finalDensityFn = router.finalDensity();
        int seaLevel = context.chunkGenerator().getSeaLevel();

        int n = 0;
        double mean = 0.0D;
        double m2 = 0.0D;
        int waterSamples = 0;

        for (int dx = -TERRAIN_CHECK_RANGE; dx <= TERRAIN_CHECK_RANGE; dx++) {
            for (int dz = -TERRAIN_CHECK_RANGE; dz <= TERRAIN_CHECK_RANGE; dz++) {
                ChunkPos checkChunk = new ChunkPos(centerChunk.x + dx, centerChunk.z + dz);
                WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(checkChunk.toLong()));
                int minX = checkChunk.getMinBlockX();
                int minZ = checkChunk.getMinBlockZ();

                for (int i = 0; i < SAMPLES_PER_CHUNK; i++) {
                    int x = minX + random.nextInt(16);
                    int z = minZ + random.nextInt(16);
                    int surfaceY = estimateSurfaceHeight(depthFn, x, z, context);

                    double delta = surfaceY - mean;
                    n++;
                    mean += delta / n;
                    double delta2 = surfaceY - mean;
                    m2 += delta * delta2;

                    if (n >= 10 && m2 / n > MAX_TERRAIN_VARIANCE) {
                        return OptionalInt.empty();
                    }
                    if (surfaceY < seaLevel) {
                        waterSamples++;
                    } else if (surfaceY < seaLevel + 5) {
                        DensityFunction.SinglePointContext point =
                                new DensityFunction.SinglePointContext(x, seaLevel - 1, z);
                        if (finalDensityFn.compute(point) <= 0.0D) {
                            waterSamples++;
                        }
                    }
                    if (waterSamples > n / 10) {
                        return OptionalInt.empty();
                    }
                }
            }
        }

        if (n < 2 || m2 / n > MAX_TERRAIN_VARIANCE) {
            return OptionalInt.empty();
        }
        int meanHeight = (int) mean;
        if (meanHeight < seaLevel || waterSamples * 10 >= n) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(meanHeight);
    }

    /**
     * 用 depth 函数二分搜索地表高度（depth &gt; 0 视为地下）。
     */
    private static int estimateSurfaceHeight(DensityFunction depthFn, int x, int z,
                                             PieceGeneratorSupplier.Context<JigsawConfiguration> context) {
        int lo = context.heightAccessor().getMinBuildHeight();
        int hi = context.heightAccessor().getMaxBuildHeight();
        while (hi - lo > 2) {
            int mid = (lo + hi) >>> 1;
            double depth = depthFn.compute(new DensityFunction.SinglePointContext(x, mid, z));
            if (depth > 0.0D) {
                lo = mid;
            } else {
                hi = mid;
            }
        }
        return lo;
    }
}

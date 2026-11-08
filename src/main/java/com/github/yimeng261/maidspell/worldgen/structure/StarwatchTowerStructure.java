package com.github.yimeng261.maidspell.worldgen.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.feature.configurations.JigsawConfiguration;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGenerator;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGeneratorSupplier;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;

import java.util.Arrays;
import java.util.Optional;

/**
 * 星望塔：地表拼图结构，起始高度取「塔基覆盖范围内地表高度的中位数」。
 *
 * <p>移植自 1.9.2.1 反编译源 {@code worldgen/structure/StarwatchTowerStructure.java}（85 行）。
 * 与原文的差异：</p>
 * <ul>
 *   <li>1.20 的 {@code Structure} + {@code StructureSettings} 在 1.18.2 不存在，改为继承
 *       {@link StructureFeature}；配置类型固定用 {@link JigsawConfiguration}
 *       （json 的 {@code config} 里只有 {@code start_pool} 与 {@code size}）；</li>
 *   <li>原文 json 的 {@code min_surface_y} / {@code vertical_offset} / {@code max_distance_from_center}
 *       三个字段在 1.18.2 无处安放（{@code JigsawPlacement} 不接受 maxDistance，另两个是自定义字段），
 *       故按上游 json 的取值固化成常量（60 / 0 / 256）；</li>
 *   <li>1.20 的 {@code JigsawPlacement.addPieces} 是 8 参，1.18.2 只有 5 参
 *       {@code (context, pieceFactory, pos, useExpansionHack, ensureNoRoomCollision)}，
 *       startPool / size 从 {@code context.config()} 读；</li>
 *   <li>1.20 的 {@code getBaseHeight} 是 5 参（带 {@code RandomState}），1.18.2 只有 4 参；</li>
 *   <li><b>必须覆写 {@link #step()}</b>：{@code StructureFeature.STEP} 表只由原版静态注册填充，
 *       自定义结构的默认 {@code step()} 返回 {@code null}，会让
 *       {@code ChunkGenerator#applyBiomeDecoration} 在 {@code Collectors.groupingBy} 处抛 NPE；</li>
 *   <li>{@code useExpansionHack} 必须传 {@code false}：传 true 时 {@code JigsawPlacement} 会把传入的
 *       y 再叠一次地表高度（{@code y = pos.getY() + getFirstFreeHeight(...)}），塔会飞到天上。</li>
 * </ul>
 */
public class StarwatchTowerStructure extends StructureFeature<JigsawConfiguration> {

    /** 上界：低于此高度的地表不生成（上游 json 的 {@code min_surface_y}）。 */
    private static final int MIN_SURFACE_Y = 60;
    /** 上游 json 的 {@code vertical_offset}。 */
    private static final int VERTICAL_OFFSET = 0;
    private static final int FOOTPRINT_RADIUS = 13;
    private static final int SAMPLE_STEP = 6;

    public StarwatchTowerStructure() {
        super(JigsawConfiguration.CODEC, StarwatchTowerStructure::createGenerator);
    }

    /**
     * 1.18.2 里「自己算 y 再交给原版拼图放置」只能走
     * {@link PieceGeneratorSupplier#createGenerator} 这一层：它拿到的是带
     * {@link PieceGeneratorSupplier.Context}（可读 {@code heightAccessor} / {@code chunkGenerator} /
     * {@code validBiome}），而 {@link PieceGenerator#generatePieces} 拿到的是已经被定死的
     * {@link PieceGenerator.Context}（没有生物群系判定能力）。上游 1.20 的
     * {@code Structure.findGenerationPoint} 语义与此层等价。
     */
    private static Optional<PieceGenerator<JigsawConfiguration>> createGenerator(
            PieceGeneratorSupplier.Context<JigsawConfiguration> context) {
        if (!context.validBiomeOnTop(Heightmap.Types.WORLD_SURFACE_WG)) {
            return Optional.empty();
        }

        ChunkPos chunkPos = context.chunkPos();
        int centerX = chunkPos.getMiddleBlockX();
        int centerZ = chunkPos.getMiddleBlockZ();
        int surface = representativeSurfaceInFootprint(context, centerX, centerZ);
        if (surface - 1 < MIN_SURFACE_Y) {
            return Optional.empty();
        }

        BlockPos startPos = new BlockPos(centerX, surface + 1 - 1 + VERTICAL_OFFSET, centerZ);
        return JigsawPlacement.addPieces(
                context,
                (structureManager, element, pos, groundLevelDelta, rotation, box) ->
                        new PoolElementStructurePiece(structureManager, element, pos, groundLevelDelta, rotation, box),
                startPos,
                false,
                false);
    }

    /**
     * 在塔基覆盖范围内取 25 个采样点（±13，步长 6）的地表高度，排序后取中位数，
     * 避免单点高度受树/石块干扰。
     */
    private static int representativeSurfaceInFootprint(
            PieceGeneratorSupplier.Context<JigsawConfiguration> context, int centerX, int centerZ) {
        int span = 5;
        int[] samples = new int[span * span];
        int n = 0;
        for (int dx = -FOOTPRINT_RADIUS; dx <= FOOTPRINT_RADIUS; dx += SAMPLE_STEP) {
            for (int dz = -FOOTPRINT_RADIUS; dz <= FOOTPRINT_RADIUS; dz += SAMPLE_STEP) {
                if (n >= samples.length) {
                    break;
                }
                samples[n++] = context.chunkGenerator().getBaseHeight(
                        centerX + dx, centerZ + dz, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor());
            }
        }
        int[] sorted = Arrays.copyOf(samples, n);
        Arrays.sort(sorted);
        return sorted[n / 2];
    }

    @Override
    public GenerationStep.Decoration step() {
        return GenerationStep.Decoration.SURFACE_STRUCTURES;
    }
}

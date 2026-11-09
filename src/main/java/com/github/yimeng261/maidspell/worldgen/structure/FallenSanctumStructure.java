package com.github.yimeng261.maidspell.worldgen.structure;

import com.github.yimeng261.maidspell.worldgen.MaidSpellStructures;
import java.util.Optional;
import java.util.OptionalInt;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.feature.configurations.JigsawConfiguration;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGenerator;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGeneratorSupplier;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;

/**
 * 陨落圣殿（下界拼图结构）。
 *
 * <p>移植自 1.9.2.1 反编译源 {@code worldgen/structure/FallenSanctumStructure.java}（1.20.1，124 行）。
 * 上游是 {@code extends Structure} + {@code findGenerationPoint}；1.18.2 没有 {@code Structure} 类，
 * 改为 {@code extends StructureFeature<JigsawConfiguration>} + {@code createGenerator}，
 * 放置逻辑仍在 {@code PieceGeneratorSupplier.Context} 里完成（它才提供 heightAccessor 与 chunkGenerator）。</p>
 *
 * <p><b>与原文的差异</b>（全部由 1.20 → 1.18.2 的 API 断代造成）：
 * <ul>
 *   <li>{@code Structure.GenerationStub.map(...)} 在 1.18.2 不存在。上游的
 *       {@code sinkBelowNetherRoof} 改写成「包一层 {@link PieceGenerator}」：先让原生成器往 builder 里
 *       塞 piece，再对 builder 调 {@code offsetPiecesVertically}，语义等价。</li>
 *   <li>上游 CODEC 里的 {@code size}/{@code max_distance_from_center} 两个字段在 1.18.2 无处安放
 *       （{@code JigsawConfiguration} 是原版固定结构，只认 {@code start_pool} 与 {@code size}）。
 *       故 {@code MIN_AIR_CLEARANCE} 等常量按上游取值固化在本类里，json 只保留 {@code start_pool} 与 {@code size}。</li>
 *   <li>{@code chunkGenerator.getBaseColumn(x, z, heightAccessor)} 在 1.18.2 是 3 参
 *       （上游 1.20 是 4 参，多一个 {@code RandomState}）。</li>
 *   <li>{@code BlockState.isSolid()} 在 1.18.2 不存在，等价判定用 {@code canOcclude()}。</li>
 *   <li>上游 {@code Blocks.f_50699_} 等 5 个 SRG 常量按语义还原为
 *       {@code NETHERRACK / SOUL_SAND / SOUL_SOIL / BASALT / BLACKSTONE}（下界可站立地表）。</li>
 * </ul></p>
 */
public class FallenSanctumStructure extends StructureFeature<JigsawConfiguration> {

    /** 地表上方至少要有这么多格空气，才认为该位置可放结构。 */
    private static final int MIN_AIR_CLEARANCE = 12;
    /** 结构起点允许的最大 Y；超过则整体下移（避开下界顶部基岩层）。 */
    private static final int MAX_ALLOWED_START_Y = 32;

    public FallenSanctumStructure() {
        super(JigsawConfiguration.CODEC,
                FallenSanctumStructure::createGenerator);
    }

    @Override
    public GenerationStep.Decoration step() {
        // 自定义 StructureFeature 不在 vanilla 的 STEP 表里，step() 默认返回 null，
        // 会让 ChunkGenerator.applyBiomeDecoration 的 groupingBy 抛 NPE，必须显式覆写。
        return GenerationStep.Decoration.SURFACE_STRUCTURES;
    }

    private static Optional<PieceGenerator<JigsawConfiguration>> createGenerator(
            PieceGeneratorSupplier.Context<JigsawConfiguration> context) {
        if (!context.validBiomeOnTop(Heightmap.Types.WORLD_SURFACE_WG)) {
            return Optional.empty();
        }

        int x = context.chunkPos().getMinBlockX() + 8;
        int z = context.chunkPos().getMinBlockZ() + 8;

        OptionalInt surfaceY = findNetherSurfaceY(context, x, z);
        if (surfaceY.isEmpty()) {
            return Optional.empty();
        }

        BlockPos startPos = new BlockPos(x, surfaceY.getAsInt() + 1, z);

        Optional<PieceGenerator<JigsawConfiguration>> placed = JigsawPlacement.addPieces(
                context,
                (structureManager, element, pos, groundLevelDelta, rotation, box) ->
                        new net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece(
                                structureManager, element, pos, groundLevelDelta, rotation, box),
                startPos, false, false);

        return placed.map(generator -> (StructurePiecesBuilder builder, PieceGenerator.Context<JigsawConfiguration> ctx) -> {
            generator.generatePieces(builder, ctx);
            sinkBelowNetherRoof(builder, startPos);
        });
    }

    /**
     * 从下界顶部向下扫，找到「可站立地表 + 上方有足够净空」的 Y。
     */
    private static OptionalInt findNetherSurfaceY(PieceGeneratorSupplier.Context<JigsawConfiguration> context,
                                                  int x, int z) {
        int minY = context.heightAccessor().getMinBuildHeight() + 8;
        int maxY = Math.min(context.heightAccessor().getMaxBuildHeight() - 1,
                context.chunkGenerator().getSeaLevel() + 80);

        NoiseColumn column = context.chunkGenerator().getBaseColumn(x, z, context.heightAccessor());
        int clearAbove = 0;
        for (int y = maxY; y >= minY; y--) {
            BlockState state = column.getBlock(y);
            if (isOpenSpace(state)) {
                clearAbove++;
                continue;
            }
            if (isValidSurfaceBlock(state) && clearAbove >= MIN_AIR_CLEARANCE) {
                return OptionalInt.of(y);
            }
            clearAbove = 0;
        }
        return OptionalInt.empty();
    }

    /**
     * 结构起点若高于 {@link #MAX_ALLOWED_START_Y}，把整组 piece 一起下移。
     */
    private static void sinkBelowNetherRoof(StructurePiecesBuilder builder, BlockPos startPos) {
        int offsetY = Math.min(0, MAX_ALLOWED_START_Y - startPos.getY());
        if (offsetY == 0) {
            return;
        }
        builder.offsetPiecesVertically(offsetY);
    }

    private static boolean isOpenSpace(BlockState state) {
        return !state.canOcclude() && state.getFluidState().isEmpty();
    }

    private static boolean isValidSurfaceBlock(BlockState state) {
        return state.is(Blocks.NETHERRACK)
                || state.is(Blocks.SOUL_SAND)
                || state.is(Blocks.SOUL_SOIL)
                || state.is(Blocks.BASALT)
                || state.is(Blocks.BLACKSTONE);
    }
}

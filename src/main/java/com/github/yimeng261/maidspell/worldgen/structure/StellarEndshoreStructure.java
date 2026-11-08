package com.github.yimeng261.maidspell.worldgen.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.feature.configurations.JigsawConfiguration;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGenerator;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGeneratorSupplier;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;

import java.util.Optional;

/**
 * 星途终岸：末地拼图结构，支持「固定锚点」强制放置。
 *
 * <p>移植自 1.9.2.1 反编译源 {@code worldgen/structure/StellarEndshoreStructure.java}（83 行）。
 * 与原文的差异：</p>
 * <ul>
 *   <li>1.20 的 {@code Structure} + {@code StructureSettings} 在 1.18.2 不存在，改为继承
 *       {@link StructureFeature}，配置类型固定用 {@link JigsawConfiguration}；</li>
 *   <li>上游 json 的 {@code base_height}（150）/ {@code height_jitter}（5）/
 *       {@code max_distance_from_center}（256）在 1.18.2 的 {@code JigsawConfiguration} 里无处安放，
 *       按上游取值固化成常量；</li>
 *   <li><b>{@code size} 必须 ≤ 7</b>：1.18.2 的 {@code JigsawConfiguration.CODEC} 是
 *       {@code Codec.intRange(0, 7)}，上游 json 写的是 8，直接搬会被解析器拒绝；</li>
 *   <li>1.20 的 {@code JigsawPlacement.addPieces} 是 8 参，1.18.2 只有 5 参；</li>
 *   <li>{@link #FIXED_ANCHOR} 用 {@link ThreadLocal} 保留原文的语义 —— 固定刷新只在服务端线程里，
 *       并发世界生成不会串味。</li>
 * </ul>
 */
public class StellarEndshoreStructure extends StructureFeature<JigsawConfiguration> {

    /** 竞技场地板相对锚点高度的偏移（上游 {@code ARENA_FLOOR_OFFSET}）。 */
    private static final int ARENA_FLOOR_OFFSET = 27;
    /** 结构最大高度，用于把起点压进世界高度范围内（上游 {@code MAX_STRUCTURE_HEIGHT}）。 */
    private static final int MAX_STRUCTURE_HEIGHT = 80;
    private static final int BASE_HEIGHT = 150;
    private static final int HEIGHT_JITTER = 5;

    private static final ThreadLocal<BlockPos> FIXED_ANCHOR = new ThreadLocal<>();

    public StellarEndshoreStructure() {
        super(JigsawConfiguration.CODEC, StellarEndshoreStructure::createGenerator);
    }

    /** 固定刷新用：设置锚点后，下一次生成必定落到该锚点。 */
    public static void setFixedAnchor(BlockPos anchor) {
        FIXED_ANCHOR.set(anchor);
    }

    /** 固定刷新结束后必须清掉，否则后续自然生成也会被劫持。 */
    public static void clearFixedAnchor() {
        FIXED_ANCHOR.remove();
    }

    /** 竞技场地板高度 → 结构起点高度。 */
    public static int fixedStartY(int arenaY) {
        return arenaY - ARENA_FLOOR_OFFSET;
    }

    private static Optional<PieceGenerator<JigsawConfiguration>> createGenerator(
            PieceGeneratorSupplier.Context<JigsawConfiguration> context) {
        if (FIXED_ANCHOR.get() == null && !context.validBiomeOnTop(Heightmap.Types.WORLD_SURFACE_WG)) {
            return Optional.empty();
        }

        ChunkPos chunkPos = context.chunkPos();
        LevelHeightAccessor heightAccessor = context.heightAccessor();

        BlockPos anchor = FIXED_ANCHOR.get();
        int x;
        int z;
        int y;
        if (anchor != null) {
            x = anchor.getX();
            z = anchor.getZ();
            y = fixedStartY(anchor.getY());
        } else {
            x = chunkPos.getMinBlockX() + 8;
            z = chunkPos.getMinBlockZ() + 8;
            y = BASE_HEIGHT - ARENA_FLOOR_OFFSET;
            if (HEIGHT_JITTER > 0) {
                // 上游用 randomState 派生随机数；1.18.2 这里按原版 JigsawPlacement 的做法
                // 用同一个大特征种子初始化，保证同区块结果可复现。
                WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(0L));
                random.setLargeFeatureSeed(context.seed(), chunkPos.x, chunkPos.z);
                y += random.nextInt(HEIGHT_JITTER * 2 + 1) - HEIGHT_JITTER;
            }
        }
        y = Mth.clamp(y, heightAccessor.getMinBuildHeight(),
                heightAccessor.getMaxBuildHeight() - MAX_STRUCTURE_HEIGHT);

        BlockPos startPos = new BlockPos(x, y, z);
        return JigsawPlacement.addPieces(
                context,
                (structureManager, element, pos, groundLevelDelta, rotation, box) ->
                        new PoolElementStructurePiece(structureManager, element, pos, groundLevelDelta, rotation, box),
                startPos,
                false,
                false);
    }

    @Override
    public GenerationStep.Decoration step() {
        return GenerationStep.Decoration.SURFACE_STRUCTURES;
    }
}

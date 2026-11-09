package com.github.yimeng261.maidspell.worldgen.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.feature.configurations.JigsawConfiguration;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGenerator;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGeneratorSupplier;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;

import java.util.Optional;

/**
 * 魔法使的足迹（enchantress_footsteps）全变体通用结构类。
 *
 * <p>上游 1.20.1 的 5 个变体共用 `"type": "minecraft:jigsaw"`，但 <b>1.18.2 没有注册名为
 * {@code jigsaw} 的结构类型</b>（{@code StructureFeature} 常量池只有 18 个 vanilla 名，全 jar 中字面量
 * {@code jigsaw} 仅出现在方块 / 方块实体 / piece 类型上）。因此这里自建 {@link StructureFeature}
 * 子类，一个类注册 6 个 id（5 个上游变体 + woods_perch）。</p>
 *
 * <p>与原文的差异：</p>
 * <ul>
 *   <li>上游 json 的 {@code type: minecraft:jigsaw} → 改为本类注册的 id；</li>
 *   <li>{@code project_start_to_heightmap: WORLD_SURFACE_WG} 与 {@code start_height: absolute 0}
 *       在 1.18.2 无对应 json 字段 → 在 {@link #createGenerator} 里用
 *       {@link ChunkGenerator#getBaseHeight} 自己算 y；</li>
 *   <li>{@code max_distance_from_center}（32/16/32/48/80）在 1.18.2 无处安放
 *       （{@code JigsawPlacement} 内部硬编码搜索半径 80），丢弃；</li>
 *   <li>{@code use_expansion_hack: true}（仅 outpost）必须改成 {@code false}：开启后
 *       {@code JigsawPlacement} 会把传入的 pos.y <b>再叠一次</b>地表高度，结构会飞到天上；</li>
 *   <li>{@code exclusion_zone: {chunk_count: 10, other_set: minecraft:villages}} 在 1.18.2 的
 *       {@code RandomSpreadStructurePlacement} 里不存在字段 → 改为在 {@link #avoidVillages}
 *       为真时于 {@code createGenerator} 里用
 *       {@link ChunkGenerator#hasFeatureChunkInRange} 做同样的避让（10 区块）；</li>
 *   <li>{@code step} 由 {@link #step()} 覆写决定（不覆写父类返回 null，区块装饰阶段
 *       {@code GenerationStep.Decoration.ordinal()} 会 NPE）。</li>
 * </ul>
 */
public class EnchantressFootstepsStructure extends StructureFeature<JigsawConfiguration> {

    /** 原版村庄结构集的注册键，用于 outpost 的避让判定。 */
    private static final ResourceKey<StructureSet> VILLAGES_STRUCTURE_SET =
            ResourceKey.create(Registry.STRUCTURE_SET_REGISTRY, new ResourceLocation("minecraft", "villages"));

    /** 上游 outpost 的 {@code exclusion_zone.chunk_count}。 */
    private static final int VILLAGE_EXCLUSION_CHUNKS = 10;

    /** 上游的 {@code start_height: {"absolute": 0}}。 */
    private static final int START_HEIGHT_OFFSET = 0;

    /** 是否为需要避让原版村庄的变体（上游只有 outpost 带 exclusion_zone）。 */
    private final boolean avoidVillages;

    public EnchantressFootstepsStructure() {
        this(false);
    }

    private EnchantressFootstepsStructure(boolean avoidVillages) {
        // 注意：lambda 里只能捕获构造参数，不能引用 this（super 调用尚未完成）。
        super(JigsawConfiguration.CODEC, context -> createGenerator(context, avoidVillages));
        this.avoidVillages = avoidVillages;
    }

    @Override
    public GenerationStep.Decoration step() {
        return GenerationStep.Decoration.SURFACE_STRUCTURES;
    }

    /** 仅用于日志/调试展示。 */
    public boolean isAvoidingVillages() {
        return this.avoidVillages;
    }

    private static Optional<PieceGenerator<JigsawConfiguration>> createGenerator(
            PieceGeneratorSupplier.Context<JigsawConfiguration> context, boolean avoidVillages) {
        // 上游靠 structure_set 的 biomes tag 过滤群系，但 createGenerator 自身仍应收敛到「地表合法处」，
        // 否则同一区块可能在地表以下或水中落点。
        if (!context.validBiomeOnTop(Heightmap.Types.WORLD_SURFACE_WG)) {
            return Optional.empty();
        }

        ChunkPos chunkPos = context.chunkPos();
        ChunkGenerator generator = context.chunkGenerator();
        LevelHeightAccessor heightAccessor = context.heightAccessor();

        if (avoidVillages
                && generator.hasFeatureChunkInRange(VILLAGES_STRUCTURE_SET, context.seed(),
                        chunkPos.x, chunkPos.z, VILLAGE_EXCLUSION_CHUNKS)) {
            return Optional.empty();
        }

        int x = chunkPos.getMiddleBlockX();
        int z = chunkPos.getMiddleBlockZ();
        int y = generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, heightAccessor)
                + START_HEIGHT_OFFSET;

        // 顶点必须落在世界高度范围内，否则 JigsawPlacement 的包围盒会越界。
        y = Math.max(heightAccessor.getMinBuildHeight() + 1,
                Math.min(y, heightAccessor.getMaxBuildHeight() - 2));

        BlockPos startPos = new BlockPos(x, y, z);

        // useExpansionHack 必须为 false（见类注释）；ensureNoRoomCollision 与上游一致取 false。
        return JigsawPlacement.addPieces(context,
                (structureManager, element, pos, groundLevelDelta, rotation, box) ->
                        new PoolElementStructurePiece(structureManager, element, pos, groundLevelDelta,
                                rotation, box),
                startPos, false, false);
    }

    /** 供 outpost 变体注册时引用（DeferredRegister 的 factory 需要无参构造）。 */
    public static EnchantressFootstepsStructure outpost() {
        return new EnchantressFootstepsStructure(true);
    }
}

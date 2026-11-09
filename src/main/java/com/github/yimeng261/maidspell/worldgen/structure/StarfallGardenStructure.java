package com.github.yimeng261.maidspell.worldgen.structure;

import com.github.yimeng261.maidspell.MaidSpellMod;
import com.github.yimeng261.maidspell.worldgen.StarfallGardenData;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGenerator;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGeneratorSupplier;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;

import java.util.Optional;

/**
 * 星落之庭：浮空花园，一存档仅生成一座。
 *
 * <p>移植自 1.9.2.1 反编译源 {@code worldgen/structure/StarfallGardenStructure.java}（188 行）。
 * 本工程按用户 m03661 拍板走 <b>D1 = B（单模板整体放置）</b>：上游是 12 段拼图
 * （{@code starfall_garden_1..12}），构建期已按各段 jigsaw 的 {@code orientation} 精确
 * 合并成单一模板 {@code starfall_garden/starfall_garden_merged}（尺寸 [74,136,73]，
 * 303380 方块），所以运行期不再走 {@code JigsawPlacement}。</p>
 *
 * <p>与原文的差异：</p>
 * <ul>
 *   <li><b>合并单模板</b>：绕开 1.18.2 {@code JigsawPlacement} 的 ±80 格 free 空间盒
 *       （实测原 12 段里 {@code _8} 段 y 上探到 +93，超出盒上界 81 达 12 格，会被判定冲突而丢弃）；</li>
 *   <li>1.20 的 {@code Structure} / {@code StructureType} 在 1.18.2 不存在，改继承
 *       {@link StructureFeature}，配置用 {@link NoneFeatureConfiguration}（json 写 {@code "config": {}}）；</li>
 *   <li>上游在 {@code Structure#generate} 里做「一存档一座」门控；1.18.2 的
 *       {@code ConfiguredStructureFeature#generate} 是 final 语义、不可覆写，改为在
 *       {@code createGenerator} 里抢 {@link StarfallGardenData#tryBeginGeneration()}，
 *       并把返回的 {@link PieceGenerator} 包一层：生成有产出才 {@link StarfallGardenData#markPlaced}，
 *       失败则 {@link StarfallGardenData#abortGeneration()} 回滚；</li>
 *   <li>上游 {@code getBaseHeight} 是 5 参（带 {@code RandomState}），1.18.2 只有 4 参；</li>
 *   <li>上游的 {@code height_offset}(72) / {@code min_spawn_distance}(200) /
 *       {@code max_spawn_distance}(500) 三个配置项在 1.18.2 无处安放（{@code NoneFeatureConfiguration}
 *       没有字段），按上游默认值固化成类常量；</li>
 *   <li>必须覆写 {@link #step()}，否则 1.18.2 会在 {@code applyBiomeDecoration} 处 NPE；</li>
 *   <li>不搬 {@code StarfallGardenRetrofit}（旧存档回填），见用户决策 D2 = B。</li>
 * </ul>
 */
public class StarfallGardenStructure extends StructureFeature<NoneFeatureConfiguration> {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** 合并后的单模板位置。 */
    public static final ResourceLocation TEMPLATE_LOCATION =
            new ResourceLocation(MaidSpellMod.MOD_ID, "starfall_garden/starfall_garden_merged");

    /** 上游 {@code height_offset} 默认值：结构底面比地表高 72 格（浮空花园）。 */
    private static final int HEIGHT_OFFSET = 72;
    /** 上游 {@code min_spawn_distance} 默认值。 */
    private static final int MIN_SPAWN_DISTANCE = 200;
    /** 上游 {@code max_spawn_distance} 默认值。 */
    private static final int MAX_SPAWN_DISTANCE = 500;

    public StarfallGardenStructure() {
        super(NoneFeatureConfiguration.CODEC,
                PieceGeneratorSupplier.simple(
                        PieceGeneratorSupplier.checkForBiomeOnTop(Heightmap.Types.WORLD_SURFACE_WG),
                        StarfallGardenStructure::generatePieces));
    }

    private static void generatePieces(StructurePiecesBuilder builder,
                                       PieceGenerator.Context<NoneFeatureConfiguration> context) {
        // 已生成过（或正在生成）就放弃。
        if (StarfallGardenData.isPlaced()) {
            return;
        }

        ChunkPos chunkPos = context.chunkPos();
        int centerX = chunkPos.getMiddleBlockX();
        int centerZ = chunkPos.getMiddleBlockZ();

        // 只允许主世界（上游 isOverworldGeneration）。
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        ServerLevel overworld = server == null ? null : server.overworld();
        if (overworld != null && !isOverworldGeneration(overworld, context)) {
            return;
        }

        // 距出生点的环形带 [200, 500]。
        if (overworld != null) {
            BlockPos spawn = overworld.getSharedSpawnPos();
            long dx = centerX - spawn.getX();
            long dz = centerZ - spawn.getZ();
            long distSq = dx * dx + dz * dz;
            long minSq = (long) MIN_SPAWN_DISTANCE * MIN_SPAWN_DISTANCE;
            long maxSq = (long) MAX_SPAWN_DISTANCE * MAX_SPAWN_DISTANCE;
            if (distSq < minSq || distSq > maxSq) {
                return;
            }
        }

        StructureManager structureManager = context.structureManager();
        Optional<StructureTemplate> template = structureManager.get(TEMPLATE_LOCATION);
        if (template.isEmpty()) {
            LOGGER.error("星落之庭：找不到合并模板 {}", TEMPLATE_LOCATION);
            return;
        }

        int surface = context.chunkGenerator().getBaseHeight(
                centerX, centerZ, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor());
        int minBuildY = context.heightAccessor().getMinBuildHeight();
        int maxBuildY = Math.max(minBuildY, context.heightAccessor().getMaxBuildHeight() - 96);
        int floorY = net.minecraft.util.Mth.clamp(surface + HEIGHT_OFFSET, minBuildY, maxBuildY);

        // 抢门控：抢不到说明别的区块已经开建。
        if (!StarfallGardenData.tryBeginGeneration()) {
            return;
        }

        BlockPos pos = new BlockPos(centerX, floorY + 1, centerZ);
        LOGGER.info("星落之庭：在 {} 生成（地表 {}，抬高 {}）", pos, surface, HEIGHT_OFFSET);
        builder.addPiece(new SingleTemplatePiece(structureManager, TEMPLATE_LOCATION, pos, Rotation.NONE));

        // 有产出才算成功；否则回滚门控，让后续区块还能再试。
        if (builder.isEmpty()) {
            StarfallGardenData.abortGeneration();
            LOGGER.warn("星落之庭：生成未产出任何片段，已回滚门控");
        } else {
            StarfallGardenData.markPlaced(pos);
        }
    }

    /** 判断本次生成的区块生成器是否就是主世界的那个。 */
    private static boolean isOverworldGeneration(ServerLevel overworld, PieceGenerator.Context<?> context) {
        if (overworld == null) {
            return true;
        }
        return overworld.getChunkSource().getGenerator() == context.chunkGenerator();
    }

    @Override
    public GenerationStep.Decoration step() {
        return GenerationStep.Decoration.SURFACE_STRUCTURES;
    }
}

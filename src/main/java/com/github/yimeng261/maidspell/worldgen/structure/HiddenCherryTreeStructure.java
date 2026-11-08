package com.github.yimeng261.maidspell.worldgen.structure;

import com.github.yimeng261.maidspell.MaidSpellMod;
import com.github.yimeng261.maidspell.worldgen.MaidSpellStructurePieceTypes;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
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
import org.slf4j.Logger;

import java.util.Optional;

/**
 * 隐秘樱树：单模板结构，直接放一棵树。
 *
 * <p>移植自 1.9.2.1 反编译源 {@code worldgen/structure/HiddenCherryTreeStructure.java}（66 行）。
 * 与原文的差异：</p>
 * <ul>
 *   <li>1.20 的 {@code Structure} + {@code StructureType} 在 1.18.2 不存在，改为继承
 *       {@link StructureFeature}，配置类型用 {@link NoneFeatureConfiguration}；</li>
 *   <li>1.20 的 {@code Structure.onTopOfChunkCenter(context, heightmap, consumer)} 在 1.18.2 无对应方法，
 *       其「先判生物群系、再算高度」的语义拆成两部分：生物群系判断交给
 *       {@link PieceGeneratorSupplier#checkForBiomeOnTop(Heightmap.Types)}，
 *       高度计算在 {@link #generatePieces} 里按原文（{@code getMinBlockX()+8} /
 *       {@code getFirstFreeHeight(MOTION_BLOCKING_NO_LEAVES)}）自己算；</li>
 *   <li>1.20 的 {@code getFirstFreeHeight} 有 5 参（带 {@code RandomState}），1.18.2 只有 4 参；</li>
 *   <li><b>必须覆写 {@link #step()}</b>：1.18.2 的 {@code StructureFeature.STEP} 表只由原版静态注册填充，
 *       自定义结构的默认 {@code step()} 返回 {@code null}，会让
 *       {@code ChunkGenerator#applyBiomeDecoration} 在 {@code Collectors.groupingBy} 处抛 NPE。</li>
 * </ul>
 */
public class HiddenCherryTreeStructure extends StructureFeature<NoneFeatureConfiguration> {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final ResourceLocation TEMPLATE_LOCATION =
            new ResourceLocation(MaidSpellMod.MOD_ID, "hidden_cherry_tree/hidden_cherry_tree");

    public HiddenCherryTreeStructure() {
        super(NoneFeatureConfiguration.CODEC,
                PieceGeneratorSupplier.simple(
                        PieceGeneratorSupplier.checkForBiomeOnTop(Heightmap.Types.WORLD_SURFACE_WG),
                        HiddenCherryTreeStructure::generatePieces));
    }

    private static void generatePieces(StructurePiecesBuilder builder,
                                       PieceGenerator.Context<NoneFeatureConfiguration> context) {
        ChunkPos chunkPos = context.chunkPos();
        int x = chunkPos.getMinBlockX() + 8;
        int z = chunkPos.getMinBlockZ() + 8;
        int y = context.chunkGenerator().getFirstFreeHeight(
                x, z, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, context.heightAccessor());

        StructureManager structureManager = context.structureManager();
        Optional<StructureTemplate> template = structureManager.get(TEMPLATE_LOCATION);
        if (template.isEmpty()) {
            LOGGER.error("Found no template for HiddenCherryTree: {}", TEMPLATE_LOCATION);
            return;
        }

        Rotation rotation = Rotation.getRandom(context.random());
        BlockPos pos = new BlockPos(x, y, z);
        LOGGER.info("Generating HiddenCherryTree at position: {}, rotation: {}", pos, rotation);
        builder.addPiece(new SingleTemplatePiece(structureManager, TEMPLATE_LOCATION, pos, rotation));
    }

    @Override
    public GenerationStep.Decoration step() {
        return GenerationStep.Decoration.SURFACE_STRUCTURES;
    }

    /** 让编译器知道这个类会用到片段注册表（同包的注册顺序约束）。 */
    static void touchPieceTypes() {
        MaidSpellStructurePieceTypes.SINGLE_TEMPLATE_PIECE.get();
    }
}

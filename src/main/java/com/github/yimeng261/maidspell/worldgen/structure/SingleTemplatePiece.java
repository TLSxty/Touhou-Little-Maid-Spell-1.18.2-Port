package com.github.yimeng261.maidspell.worldgen.structure;

import com.github.yimeng261.maidspell.worldgen.MaidSpellStructurePieceTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;

import java.util.Random;
import java.util.function.Function;

/**
 * 单模板结构片段：把一个 NBT 模板原样放进世界里。
 *
 * <p>移植自 1.9.2.1 反编译源 {@code worldgen/structure/SingleTemplatePiece.java}（58 行）。
 * 与原文的差异：</p>
 * <ul>
 *   <li>1.18.2 没有 {@code net.minecraft.util.RandomSource}，{@code postProcess} 与
 *       {@code handleDataMarker} 的随机数参数类型是 {@link Random}；</li>
 *   <li>1.18.2 的 {@code StructureManager} 类名是 {@code StructureManager}（1.20 是
 *       {@code StructureTemplateManager}），方法 {@code getOrCreate} 同名；</li>
 *   <li>1.18.2 的反序列化构造只接收 {@code (StructureManager, CompoundTag)}，不是
 *       {@code StructurePieceSerializationContext}——见 {@link MaidSpellStructurePieceTypes}；</li>
 *   <li>{@code StructureManager#get} 返回 {@code Optional<StructureTemplate>}，调用方自行判空。</li>
 * </ul>
 */
public class SingleTemplatePiece extends TemplateStructurePiece {

    public SingleTemplatePiece(StructureManager structureManager, ResourceLocation templateLocation,
                               BlockPos pos, Rotation rotation) {
        super(MaidSpellStructurePieceTypes.SINGLE_TEMPLATE_PIECE.get(), 0, structureManager,
                templateLocation, templateLocation.toString(), makeSettings(rotation), pos);
    }

    public SingleTemplatePiece(StructureManager structureManager, CompoundTag tag) {
        super(MaidSpellStructurePieceTypes.SINGLE_TEMPLATE_PIECE.get(), tag, structureManager,
                (Function<ResourceLocation, StructurePlaceSettings>) location -> makeSettings(Rotation.NONE));
    }

    private static StructurePlaceSettings makeSettings(Rotation rotation) {
        return new StructurePlaceSettings()
                .setRotation(rotation)
                .setMirror(Mirror.NONE)
                .setRotationPivot(BlockPos.ZERO)
                .setIgnoreEntities(false);
    }

    /** 模板里没有 data marker，空实现即可。 */
    @Override
    protected void handleDataMarker(String name, BlockPos pos, ServerLevelAccessor level,
                                    Random random, BoundingBox box) {
        // 原文即空实现。
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureFeatureManager structureManager,
                            ChunkGenerator generator, Random random, BoundingBox box,
                            ChunkPos chunkPos, BlockPos pos) {
        super.postProcess(level, structureManager, generator, random, box, chunkPos, pos);
    }

    /** 供注册表引用，避免 IDE 提示未使用。 */
    public static StructurePieceType.StructureTemplateType pieceType() {
        return (structureManager, tag) -> new SingleTemplatePiece(structureManager, tag);
    }

    static {
        // 触发类加载顺序：piece type 必须在结构被序列化前注册。
    }
}

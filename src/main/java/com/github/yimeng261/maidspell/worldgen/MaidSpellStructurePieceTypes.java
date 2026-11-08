package com.github.yimeng261.maidspell.worldgen;

import com.github.yimeng261.maidspell.worldgen.structure.SingleTemplatePiece;
import net.minecraft.core.Registry;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * 结构片段类型注册表。
 *
 * <p>移植自 1.9.2.1 反编译源 {@code worldgen/MaidSpellStructurePieceTypes.java}（1151 字节）。
 * 与原文的差异：原文用 1.20 的 {@code Registries.STRUCTURE_PIECE}（{@code ResourceKey}），
 * 1.18.2 没有 {@code Registries} 类，改用 {@link Registry#STRUCTURE_PIECE_REGISTRY}；
 * {@code ForgeRegistries} 里也没有 STRUCTURE_PIECE，只能走 vanilla Registry 的 ResourceKey 重载。</p>
 *
 * <p>注册的类型只有 1 个：{@code single_template_piece}。拼图结构（starwatch_tower /
 * stellar_endshore）复用 vanilla 的 {@code StructurePieceType.JIGSAW}，不需要再注册。</p>
 */
public final class MaidSpellStructurePieceTypes {

    public static final DeferredRegister<StructurePieceType> STRUCTURE_PIECE_TYPES =
            DeferredRegister.create(Registry.STRUCTURE_PIECE_REGISTRY, "touhou_little_maid_spell");

    /**
     * 单模板片段。1.18.2 的反序列化入口是
     * {@link StructurePieceType.StructureTemplateType#load(net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager, net.minecraft.nbt.CompoundTag)}。
     */
    public static final RegistryObject<StructurePieceType> SINGLE_TEMPLATE_PIECE =
            STRUCTURE_PIECE_TYPES.register("single_template_piece", SingleTemplatePiece::pieceType);

    private MaidSpellStructurePieceTypes() {
    }

    public static void register(net.minecraftforge.eventbus.api.IEventBus bus) {
        STRUCTURE_PIECE_TYPES.register(bus);
    }
}

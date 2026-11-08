package com.github.yimeng261.maidspell.block.entity;

import com.github.yimeng261.maidspell.block.MaidSpellBlocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 方块实体注册表。
 * <p>
 * 移植自 1.9.2.1 反编译源 block/entity/MaidSpellBlockEntities.java（43 行）。
 * 4 个类型与原文一致：三个花丛（地栽 + 盆栽共用一个类型）与压制之石。
 * <p>
 * <strong>与原文的差异</strong>：原文用 {@code ForgeRegistries.BLOCK_ENTITY_TYPES}
 * （1.19+ 的字段名），1.18.2 是 {@code ForgeRegistries.BLOCK_ENTITIES}。
 */
public final class MaidSpellBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITIES, "touhou_little_maid_spell");

    public static final RegistryObject<BlockEntityType<ScarletZhuhuaBlockEntity>> SCARLET_ZHUHUA =
            BLOCK_ENTITIES.register("scarlet_zhuhua", () -> BlockEntityType.Builder.of(
                    ScarletZhuhuaBlockEntity::new,
                    MaidSpellBlocks.SCARLET_ZHUHUA.get(),
                    MaidSpellBlocks.POTTED_SCARLET_ZHUHUA.get()
            ).build(null));

    public static final RegistryObject<BlockEntityType<YueLinglanBlockEntity>> YUE_LINGLAN =
            BLOCK_ENTITIES.register("yue_linglan", () -> BlockEntityType.Builder.of(
                    YueLinglanBlockEntity::new,
                    MaidSpellBlocks.YUE_LINGLAN.get(),
                    MaidSpellBlocks.POTTED_YUE_LINGLAN.get()
            ).build(null));

    public static final RegistryObject<BlockEntityType<JingxuYoulanBlockEntity>> JINGXU_YOULAN =
            BLOCK_ENTITIES.register("jingxu_youlan", () -> BlockEntityType.Builder.of(
                    JingxuYoulanBlockEntity::new,
                    MaidSpellBlocks.JINGXU_YOULAN.get(),
                    MaidSpellBlocks.POTTED_JINGXU_YOULAN.get()
            ).build(null));

    public static final RegistryObject<BlockEntityType<SuppressionStoneBlockEntity>> SUPPRESSION_STONE =
            BLOCK_ENTITIES.register("suppression_stone", () -> BlockEntityType.Builder.of(
                    SuppressionStoneBlockEntity::new,
                    new Block[]{MaidSpellBlocks.SUPPRESSION_STONE.get()}
            ).build(null));

    private MaidSpellBlockEntities() {
    }

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}

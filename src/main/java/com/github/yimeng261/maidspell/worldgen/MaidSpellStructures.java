package com.github.yimeng261.maidspell.worldgen;

import com.github.yimeng261.maidspell.MaidSpellMod;
import com.github.yimeng261.maidspell.worldgen.structure.EnchantressFootstepsStructure;
import com.github.yimeng261.maidspell.worldgen.structure.FallenSanctumStructure;
import com.github.yimeng261.maidspell.worldgen.structure.HiddenCherryTreeStructure;
import com.github.yimeng261.maidspell.worldgen.structure.RelicSanctumStructure;
import com.github.yimeng261.maidspell.worldgen.structure.StarwatchTowerStructure;
import com.github.yimeng261.maidspell.worldgen.structure.StellarEndshoreStructure;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 建筑结构注册表。
 *
 * <p>移植自 1.9.2.1 反编译源 {@code worldgen/MaidSpellStructures.java}（3249 字节），但
 * <b>架构完全不同</b>：原文注册的是 1.20 的 {@code StructureType<?>}（结构类型的码器外壳），
 * 1.18.2 没有 {@code StructureType} 类，注册的是 {@link StructureFeature} 实例本身。</p>
 *
 * <p>因此 json 里 {@code "type": "touhou_little_maid_spell:xxx"} 指向的是本类的注册 id，
 * 而配置字段要写进 {@code config} 对象内（原 1.20 是扁平的）。</p>
 */
public final class MaidSpellStructures {

    public static final DeferredRegister<StructureFeature<?>> STRUCTURE_FEATURES =
            DeferredRegister.create(ForgeRegistries.STRUCTURE_FEATURES, MaidSpellMod.MOD_ID);

    /** 批次 1：隐秘樱树（单模板，直连 nbt）。 */
    public static final RegistryObject<StructureFeature<?>> HIDDEN_CHERRY_TREE =
            STRUCTURE_FEATURES.register("hidden_cherry_tree", HiddenCherryTreeStructure::new);

    /** 批次 1：星望塔（地表拼图，主世界末地高地）。 */
    public static final RegistryObject<StructureFeature<?>> STARWATCH_TOWER =
            STRUCTURE_FEATURES.register("starwatch_tower", StarwatchTowerStructure::new);

    /** 批次 1：星途终岸（末地拼图，带固定锚点）。 */
    public static final RegistryObject<StructureFeature<?>> STELLAR_ENDSHORE =
            STRUCTURE_FEATURES.register("stellar_endshore", StellarEndshoreStructure::new);

    /** 批次 2：陨落圣殿（下界拼图，自己找净空地表）。 */
    public static final RegistryObject<StructureFeature<?>> FALLEN_SANCTUM =
            STRUCTURE_FEATURES.register("fallen_sanctum", FallenSanctumStructure::new);

    /**
     * 批次 2：遗物圣所（地表拼图，只挑平坦高地）。
     *
     * <p>精灵领域（elven_realm）在上游 json 里 {@code type} 也写成本注册项（上游笔误），
     * 通过各自的 {@code start_pool} 区分，故本工程保持与上游一致，不为精灵领域另建类。</p>
     */
    public static final RegistryObject<StructureFeature<?>> RELIC_SANCTUM =
            STRUCTURE_FEATURES.register("relic_sanctum", RelicSanctumStructure::new);

    /*
     * 批次 3：魔法使的足迹（enchantress_footsteps）全变体。
     *
     * 上游 5 个变体的 json 全部是 `"type": "minecraft:jigsaw"`，而 1.18.2 没有注册这个结构类型
     * → 改为共用 EnchantressFootstepsStructure 一个类、注册 6 个 id（5 个上游变体 + woods_perch，
     * 后者上游从未注册为独立结构，本工程按用户 m03259 决策补注册）。
     * 每个 id 对应各自的 configured_structure_feature json 与 start_pool。
     */

    /** 批次 3：墓地（goety 方块已降级为深板岩/深色橡木系）。 */
    public static final RegistryObject<StructureFeature<?>> ENCHANTRESS_FOOTSTEPS_GRAVEYARD =
            STRUCTURE_FEATURES.register("enchantress_footsteps_graveyard",
                    EnchantressFootstepsStructure::new);

    /** 批次 3：雪屋。 */
    public static final RegistryObject<StructureFeature<?>> ENCHANTRESS_FOOTSTEPS_IGLOO =
            STRUCTURE_FEATURES.register("enchantress_footsteps_igloo",
                    EnchantressFootstepsStructure::new);

    /** 批次 3：蘑菇岛小屋。 */
    public static final RegistryObject<StructureFeature<?>> ENCHANTRESS_FOOTSTEPS_MUSHROOM_FIELDS =
            STRUCTURE_FEATURES.register("enchantress_footsteps_mushroom_fields",
                    EnchantressFootstepsStructure::new);

    /** 批次 3：绿洲。 */
    public static final RegistryObject<StructureFeature<?>> ENCHANTRESS_FOOTSTEPS_OASIS =
            STRUCTURE_FEATURES.register("enchantress_footsteps_oasis",
                    EnchantressFootstepsStructure::new);

    /**
     * 批次 3：掠夺者前哨站（魔法使版）。
     *
     * <p>上游带 {@code exclusion_zone: {chunk_count: 10, other_set: minecraft:villages}}，
     * 1.18.2 的 placement 没有该字段 → 用 {@code avoidVillages = true} 在 createGenerator 里
     * 通过 {@code ChunkGenerator#hasFeatureChunkInRange} 复刻同样的 10 区块避让。</p>
     */
    public static final RegistryObject<StructureFeature<?>> ENCHANTRESS_FOOTSTEPS_OUTPOST =
            STRUCTURE_FEATURES.register("enchantress_footsteps_outpost",
                    EnchantressFootstepsStructure::outpost);

    /**
     * 批次 3：林间栖所。
     *
     * <p>上游只有 nbt / template_pool / biome tag，<b>没有</b> structure json 与 structure_set
     * ——它原本只由 {@code TemplatePoolModifier} 注入原版村庄街道池，而那条路径整段被
     * {@code IronsSpellbooksCompat.isLoaded()} 门控，本工程不装铁魔法即死代码。
     * 按用户 m03259 决策 D2 补注册为独立结构，否则这批 nbt 完全不可见。</p>
     */
    public static final RegistryObject<StructureFeature<?>> ENCHANTRESS_FOOTSTEPS_WOODS_PERCH =
            STRUCTURE_FEATURES.register("enchantress_footsteps_woods_perch",
                    EnchantressFootstepsStructure::new);

    private MaidSpellStructures() {
    }

    public static void register(net.minecraftforge.eventbus.api.IEventBus bus) {
        STRUCTURE_FEATURES.register(bus);
    }
}

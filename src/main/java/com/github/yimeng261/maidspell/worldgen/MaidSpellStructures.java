package com.github.yimeng261.maidspell.worldgen;

import com.github.yimeng261.maidspell.MaidSpellMod;
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

    private MaidSpellStructures() {
    }

    public static void register(net.minecraftforge.eventbus.api.IEventBus bus) {
        STRUCTURE_FEATURES.register(bus);
    }
}

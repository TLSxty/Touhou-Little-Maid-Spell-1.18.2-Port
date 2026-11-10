package com.github.yimeng261.maidspell.dimension;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

import java.util.stream.Stream;

/**
 * 退隐维度专用区块生成器：只允许 hidden_retreat 一个结构集参与分布。
 * <p>
 * 为什么需要这个类：1.18.2 的 {@link net.minecraft.world.level.chunk.ChunkGenerator} 构造时吃进去的是
 * 全量 {@code Registry<StructureSet>}，{@code possibleStructureSets()} 默认直接返回
 * {@code structureSets.holders()}（不按群系过滤）。退隐维度的群系源是
 * {@link net.minecraft.world.level.biome.FixedBiomeSource}（固定 flower_forest），于是整合包里
 * 所有「按群系标签允许」的结构集都会在退隐维度参与随机分布 —— 实测表现为「主世界的村庄、矿洞、
 * 各模组建筑全都长在退隐维度里」，密度极高，玩家一传送过去就会触发大面积区块生成。
 * <p>
 * 该方法是唯一入口：{@code generatePositions()}（预生成结构位置表）与
 * {@code createStructures()}（区块实际生成结构）都走它，因此覆写一处即可同时掐死两条路径。
 * <p>
 * 注意：1.18.2 的 {@code structure_overrides} 没有出现在 {@code ChunkGenerator.commonCodec} 里
 * （全 jar 搜该字段零命中），所以无法用数据包 level 配置限制，只能在 Java 层过滤。
 */
public class RetreatChunkGenerator extends NoiseBasedChunkGenerator {

    /** 退隐维度唯一允许生成的结构集。 */
    public static final ResourceLocation HIDDEN_RETREAT_SET_ID =
            new ResourceLocation("touhou_little_maid_spell", "hidden_retreat_set");

    public RetreatChunkGenerator(Registry<StructureSet> structureSets,
                                 Registry<NormalNoise.NoiseParameters> noises,
                                 BiomeSource biomeSource,
                                 long seed,
                                 Holder<NoiseGeneratorSettings> settings) {
        super(structureSets, noises, biomeSource, seed, settings);
    }

    @Override
    public Stream<Holder<StructureSet>> possibleStructureSets() {
        return super.possibleStructureSets()
                .filter(holder -> holder.unwrapKey()
                        .map(key -> key.location().equals(HIDDEN_RETREAT_SET_ID))
                        .orElse(false));
    }
}

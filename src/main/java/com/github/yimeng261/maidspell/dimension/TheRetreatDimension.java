package com.github.yimeng261.maidspell.dimension;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/**
 * 退隐维度（the_retreat）占位类 —— 批次 A 降级版。
 *
 * <p><b>⚠ TODO(必须恢复)：</b>本类是占位实现，<b>不是</b>真实功能。用户已明确要求：
 * 「降级之后标注一下到时候我们搬完要恢复」。</p>
 *
 * <p>原实现见 {@code _decompile/maidspell_src/com/github/yimeng261/maidspell/dimension/TheRetreatDimension.java}
 * （220 行），依赖整条 the_retreat 维度链：
 * {@code RetreatManager}（457 行）、{@code PlayerRetreatManager}（535 行）、
 * {@code RetreatDimensionData}（272 行）、{@code RetreatLevelData}（173 行）、
 * {@code RetreatLevelStateData}（117 行）、{@code StructureSearchWorker}（297 行）、
 * {@code SharedRetreatManager}（43 行），以及主类 {@code MaidSpellMod} 里的维度注册。
 * 这条链属于「建筑结构移植」阶段的工作，不在批次 A 范围内。</p>
 *
 * <p>搬迁完成后恢复方式：把上述 8 个文件搬进本包，用真实实现替换这里的两个方法，
 * 并删除本注释里的 TODO 标记。</p>
 *
 * <p>当前唯一调用点是 {@code DreamCatCrystalBauble.applyDimensionBuffs}：
 * 原逻辑是「女仆在退隐维度内 → 给无敌时间」。降级后恒返回 {@code false}，
 * 即该分支永不命中，云梦水晶在退隐维度内不会额外获得无敌时间，其余功能不受影响。</p>
 */
public final class TheRetreatDimension {

    /** 退隐维度 id，供日志/未来恢复时使用。 */
    public static final ResourceLocation RETREAT_DIMENSION =
            new ResourceLocation("touhou_little_maid_spell", "the_retreat");

    private TheRetreatDimension() {
    }

    /** 占位：恒 false。恢复真实实现前，任何实体都不被视为处于退隐维度。 */
    public static boolean isInRetreat(Entity entity) {
        return false;
    }

    /** 占位：仅比对维度 id，不解析任何退隐维度数据。 */
    public static boolean isRetreatDimension(ResourceLocation dimensionLocation) {
        return RETREAT_DIMENSION.equals(dimensionLocation);
    }
}

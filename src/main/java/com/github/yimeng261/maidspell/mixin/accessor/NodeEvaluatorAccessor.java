package com.github.yimeng261.maidspell.mixin.accessor;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.pathfinder.NodeEvaluator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 读取 {@link NodeEvaluator} 的 {@code mob} 字段。
 *
 * <p>为什么不直接在 {@code MaidNodeEvaluatorMixin} 里用 {@code @Shadow}：{@code mob} 声明在
 * 爷爷类 {@code NodeEvaluator}，而 {@code @Shadow} 只针对目标类本身生成 refmap 条目。
 * 实测生产环境下 refmap 里没有该字段的映射，加载时抛
 * {@code @Shadow field mob was not located in the target class ... MaidNodeEvaluator}。
 * 改成针对字段真正所在的类写一个 accessor mixin 即可拿到正确映射。</p>
 */
@Mixin(NodeEvaluator.class)
public interface NodeEvaluatorAccessor {

    @Accessor("mob")
    Mob maidspell$getMob();
}

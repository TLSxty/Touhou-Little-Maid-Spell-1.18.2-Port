package com.github.yimeng261.maidspell.api;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;

/**
 * 饰品生命周期回调（本模组自建）。
 *
 * <p><b>为什么需要这个接口：</b>TLM 1.2.2 的 {@code IMaidBauble} 只有
 * {@code onTick} 与 {@code getChatBubbleId} 两个方法，不含
 * {@code onPutOn / onTakeOff / onDeath / syncClient}；
 * 且 {@code BaubleItemHandler.onContentsChanged} 只维护内部缓存、
 * 不回调任何饰品方法（见 {@code docs/已验证结论.md} §六）。
 * 因此这些回调在 1.2.2 上是死代码，必须由本模组自己分发。</p>
 *
 * <p><b>分发者：</b>{@code MaidSpellEventHandler}。
 * <ul>
 *   <li>{@link #onPutOn} / {@link #onTakeOff}：在 {@code MaidTickEvent} 里对女仆饰品栏 9 个槽做快照 diff，
 *       进入新物品调 onPutOn、物品离开调 onTakeOff。</li>
 *   <li>{@link #onDeath}：在 {@code MaidDeathEvent} 里遍历 9 槽调用，返回 true 表示「已阻止死亡」，
 *       分发器会取消事件（这正是云梦水晶概率复活的接入点）。</li>
 * </ul>
 *
 * <p>只有本模组自己的饰品类实现本接口；原版与其它模组的饰品不受影响。</p>
 */
public interface IMaidBaubleLifecycle {

    /** 饰品被放入女仆饰品栏时调用。 */
    default void onPutOn(EntityMaid maid, ItemStack baubleItem) {
    }

    /** 饰品离开女仆饰品栏时调用。 */
    default void onTakeOff(EntityMaid maid, ItemStack baubleItem) {
    }

    /**
     * 女仆死亡时调用。
     *
     * @return true 表示本次死亡已被阻止（分发器会取消 {@code MaidDeathEvent} 并停止后续饰品）；
     *         false 表示放行。
     */
    default boolean onDeath(EntityMaid maid, ItemStack baubleItem, DamageSource source) {
        return false;
    }
}

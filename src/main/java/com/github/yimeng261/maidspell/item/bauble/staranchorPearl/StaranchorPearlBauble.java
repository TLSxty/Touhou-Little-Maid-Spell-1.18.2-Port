// 移植自 1.9.2.1 反编译源 item/bauble/staranchorPearl/StaranchorPearlBauble.java；SRG 名已替换为 1.18.2 official 名。
package com.github.yimeng261.maidspell.item.bauble.staranchorPearl;

import com.github.tartaricacid.touhoulittlemaid.api.bauble.IMaidBauble;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.yimeng261.maidspell.api.IMaidBaubleLifecycle;
import com.github.yimeng261.maidspell.effect.StaranchorStunEffect;
import com.github.yimeng261.maidspell.effect.VoidWalkEffect;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.mutable.MutableFloat;

/**
 * 星锚珍珠（女仆饰品）。
 * <p>
 * 原文实现的是 1.20 的 {@code IMaidBauble#onInjured}——TLM 1.2.2 没有这个方法，
 * 因此改为实现本模组自建的 {@link IMaidBaubleLifecycle#onInjured}，
 * 由 {@code MaidSpellEventHandler#onLivingHurt} 分发。
 * <p>
 * 语义与原文一致：只对「虚空伤害」生效，替女仆挡下并把伤害清零，
 * 同时放出虚空环粒子与眩晕冲击波；返回 true 表示取消本次伤害且后续饰品不再触发。
 */
public class StaranchorPearlBauble implements IMaidBauble, IMaidBaubleLifecycle {

    @Override
    public boolean onInjured(EntityMaid maid, ItemStack baubleItem, DamageSource source, MutableFloat damage) {
        if (!VoidWalkEffect.isVoidDamage(source)) {
            return false;
        }
        baubleItem.hurtAndBreak(StaranchorPearl.MAID_BLOCK_COST, maid, holder -> maid.sendItemBreakMessage(baubleItem));
        Level level = maid.getLevel();
        if (level instanceof ServerLevel serverLevel) {
            VoidWalkEffect.spawnRing(serverLevel, maid);
            StaranchorStunEffect.shockwave(serverLevel, maid);
        }
        damage.setValue(0.0f);
        return true;
    }
}

// 移植自 1.9.2.1 反编译源 event/StaranchorPearlEvents.java；SRG 名已替换为 1.18.2 official 名。
package com.github.yimeng261.maidspell.event;

import com.github.yimeng261.maidspell.effect.MaidSpellEffects;
import com.github.yimeng261.maidspell.effect.VoidWalkEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 星锚珍珠的效果保护：处于「虚空漫步」期间的实体免疫虚空伤害。
 * <p>
 * 上游在主类里用 {@code forgeBus.register(StaranchorPearlEvents.class)} 登记，
 * 本工程改为 {@code @Mod.EventBusSubscriber} 自动登记。
 */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = com.github.yimeng261.maidspell.Config.MOD_ID)
public final class StaranchorPearlEvents {

    private StaranchorPearlEvents() {
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        if (!VoidWalkEffect.isVoidDamage(event.getSource())) {
            return;
        }
        LivingEntity entity = event.getEntityLiving();
        if (!entity.hasEffect(MaidSpellEffects.VOID_WALK.get())) {
            return;
        }
        event.setCanceled(true);
    }
}

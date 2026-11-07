// 移植自 1.9.2.1 反编译源 mixin/StaranchorStunLivingEntityMixin.java。
package com.github.yimeng261.maidspell.mixin;

import com.github.yimeng261.maidspell.effect.StaranchorStunEffect;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 星锚眩晕期间让生物被视为「无法移动」（停掉 AI 寻路）。
 * <p>
 * 1.18.2 的 {@code LivingEntity#isImmobile()} 存在且语义一致。
 */
@Mixin(LivingEntity.class)
public abstract class StaranchorStunLivingEntityMixin {

    @Inject(method = "isImmobile", at = @At("HEAD"), cancellable = true)
    private void maidspell$stopStunnedAi(CallbackInfoReturnable<Boolean> cir) {
        if (StaranchorStunEffect.isStunned((LivingEntity) (Object) this)) {
            cir.setReturnValue(true);
        }
    }
}

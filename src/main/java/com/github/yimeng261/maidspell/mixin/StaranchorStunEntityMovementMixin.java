// 移植自 1.9.2.1 反编译源 mixin/StaranchorStunEntityMovementMixin.java。
package com.github.yimeng261.maidspell.mixin;

import com.github.yimeng261.maidspell.effect.StaranchorStunEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 星锚眩晕期间禁止移动。
 * <p>
 * 1.18.2 的 {@code Entity#move(MoverType, Vec3)} 与 1.20 一致，注入点无需改写。
 */
@Mixin(Entity.class)
public abstract class StaranchorStunEntityMovementMixin {

    @Inject(method = "move", at = @At("HEAD"), cancellable = true)
    private void maidspell$holdStunnedPosition(MoverType type, Vec3 movement, CallbackInfo ci) {
        if ((Object) this instanceof LivingEntity living && StaranchorStunEffect.isStunned(living)) {
            living.setDeltaMovement(Vec3.ZERO);
            ci.cancel();
        }
    }
}

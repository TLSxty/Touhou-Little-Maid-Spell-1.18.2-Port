package com.github.yimeng261.maidspell.mixin.accessor;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * 把 {@link LivingEntity} 的两个 protected 方法暴露出来，供真伤系统直接调用。
 * <p>
 * {@code actuallyHurt} 走完整的护甲/药水/事件流程但绕过无敌帧；
 * {@code tickDeath} 用于血量被真伤改到 0 后立刻推进死亡状态。
 */
@Mixin(LivingEntity.class)
public interface LivingEntityInvoker {

    @Invoker("actuallyHurt")
    void maidspell$invokeActuallyHurt(DamageSource source, float amount);

    @Invoker("tickDeath")
    void maidspell$invokeTickDeath();
}

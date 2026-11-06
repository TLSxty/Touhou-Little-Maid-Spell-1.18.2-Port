package com.github.yimeng261.maidspell.api;

import net.minecraft.world.entity.LivingEntity;

/**
 * 让实体可以提供「权威生命值」的接口。
 * <p>
 * 原模组用它在不直接改 SynchedEntityData 的情况下读写真实血量：实现了本接口的实体
 * 由自己托管生命值，真伤系统通过 {@link #health(LivingEntity)} 等静态方法取值，
 * 从而避开原版 {@code getHealth()} 的缓存/同步问题。
 */
public interface IAuthoritativeHealth {

    float maidspell$authoritativeHealth();

    float maidspell$authoritativeMaxHealth();

    static float health(LivingEntity entity) {
        if (entity instanceof IAuthoritativeHealth authority) {
            return authority.maidspell$authoritativeHealth();
        }
        return entity.getHealth();
    }

    static boolean combatAlive(LivingEntity entity) {
        if (entity == null || entity.isRemoved()) {
            return false;
        }
        if (entity instanceof IAuthoritativeHealth authority) {
            return authority.maidspell$authoritativeHealth() > 0.0F;
        }
        return entity.isAlive();
    }

    static boolean deadOrDying(LivingEntity entity) {
        if (entity instanceof IAuthoritativeHealth authority) {
            return authority.maidspell$authoritativeHealth() <= 0.0F;
        }
        return entity.isDeadOrDying();
    }
}

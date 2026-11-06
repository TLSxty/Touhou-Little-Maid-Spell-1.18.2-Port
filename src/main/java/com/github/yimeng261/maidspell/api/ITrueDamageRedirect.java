package com.github.yimeng261.maidspell.api;

import javax.annotation.Nullable;
import net.minecraft.world.entity.LivingEntity;

/**
 * 实体拦截真伤用的接口。
 * <p>
 * 云梦水晶的复活/护盾逻辑需要先把真伤「排队」，由实体自己决定怎么处理，
 * 所以实现本接口的实体会接管 {@link com.github.yimeng261.maidspell.utils.TrueDamageUtil}
 * 派发的真伤，而不是让工具类直接改血量。
 */
public interface ITrueDamageRedirect {

    default void maidspell$onTrueDamageQueued() {
    }

    boolean maidspell$redirectTrueDamage(float amount, @Nullable LivingEntity attacker);
}

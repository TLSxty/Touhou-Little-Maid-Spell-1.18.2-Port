package com.github.yimeng261.maidspell.damage;

import javax.annotation.Nullable;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/**
 * 带来源标记的伤害源。
 * <p>
 * 原模组（1.20.1）的实现基于 {@code DamageType} 注册表 + {@code Holder<DamageType>} 构造，
 * 1.18.2 没有这套注册表，因此这里直接继承 {@link DamageSource}：用 {@link #msg_type} 标记伤害种类，
 * 供 {@link #canHurtProtectedEntity()} 与各饰品判断是否为自家派发的伤害、避免递归；
 * 「谁打的」信息通过覆盖 {@link #getEntity()} / {@link #getDirectEntity()} 提供。
 * <p>
 * 与原文的差异：不再包装原始 {@code DamageSource} 的 DamageType，只保留
 * {@link #damage_source} 字段供调用方自查。
 */
public class InfoDamageSource extends DamageSource {

    /** 伤害种类标记，例如 {@code chaos_book} / {@code double_heart_chain}。 */
    public String msg_type;

    /** 被包装的原始伤害源，调用方自查用。 */
    public DamageSource damage_source;

    /** 施加伤害的实体（可能为空）。 */
    @Nullable
    public LivingEntity sourceEntity;

    private final Entity directEntity;
    private final Entity causingEntity;

    public InfoDamageSource(String msgType, DamageSource damageSource) {
        this(msgType, damageSource, damageSource.getEntity());
    }

    public InfoDamageSource(String msgType, DamageSource damageSource, @Nullable Entity entity) {
        super("maidspell_" + msgType);
        this.msg_type = msgType;
        this.damage_source = damageSource;
        this.directEntity = damageSource.getDirectEntity();
        this.causingEntity = entity;
        this.sourceEntity = entity instanceof LivingEntity living ? living : null;
    }

    public void setSourceEntity(@Nullable LivingEntity sourceEntity) {
        this.sourceEntity = sourceEntity;
    }

    @Override
    @Nullable
    public Entity getDirectEntity() {
        return this.directEntity;
    }

    @Override
    @Nullable
    public Entity getEntity() {
        return this.causingEntity;
    }

    public static InfoDamageSource create(String msgType, DamageSource baseDamageSource) {
        if (baseDamageSource == null) {
            throw new IllegalArgumentException("baseDamageSource cannot be null");
        }
        return new InfoDamageSource(msgType, baseDamageSource);
    }

    /**
     * 兼容原模组的三参调用点。{@code Level} 在 1.18.2 的实现里不再需要（原来只为取 DamageType 注册表）。
     */
    public static InfoDamageSource create(Level level, String msgType, DamageSource baseDamageSource) {
        return create(msgType, baseDamageSource);
    }

    /** 这两种标记的伤害允许穿透「被保护实体」（主人/女仆）的减伤拦截。 */
    public boolean canHurtProtectedEntity() {
        return "hairpin_redirect".equals(this.msg_type) || "double_heart_chain".equals(this.msg_type);
    }
}

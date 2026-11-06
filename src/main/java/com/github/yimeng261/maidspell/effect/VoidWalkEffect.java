package com.github.yimeng261.maidspell.effect;

import com.github.yimeng261.maidspell.particle.MaidSpellParticles;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.Random;

/**
 * 虚空行走：在身上持续生成环形粒子。
 * <p>
 * 1.20 用 {@code DamageTypes.FELL_OUT_OF_WORLD} 判定虚空伤害，1.18.2 没有 DamageType 注册表，
 * 改为直接比对 {@link DamageSource#OUT_OF_WORLD} 的 msgId。
 */
public class VoidWalkEffect extends MobEffect {

    private static final int COLOR = 8077268;
    private static final double RING_RADIUS = 0.6;
    private static final double TURNS_PER_SECOND = 0.3;
    private static final int PARTICLES_PER_TICK = 2;
    private static final double VERTICAL_SPREAD = 0.8;

    public VoidWalkEffect() {
        super(MobEffectCategory.BENEFICIAL, COLOR);
    }

    /**
     * 判定是否为虚空（掉出世界）伤害。1.18.2 直接用 OUT_OF_WORLD 的 msgId。
     */
    public static boolean isVoidDamage(DamageSource source) {
        return source != null && "outOfWorld".equals(source.getMsgId());
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        super.applyEffectTick(entity, amplifier);
        Level level = entity.getLevel();
        if (level instanceof ServerLevel serverLevel) {
            spawnRing(serverLevel, entity);
        }
    }

    public static void spawnRing(ServerLevel level, LivingEntity entity) {
        Random random = entity.getRandom();
        double halfHeight = entity.getBbHeight() * 0.5;
        double phase = entity.tickCount * TURNS_PER_SECOND * Math.PI * 2.0 / 20.0;
        for (int i = 0; i < PARTICLES_PER_TICK; i++) {
            double angle = phase + i * Math.PI;
            double radius = RING_RADIUS * (0.6 + random.nextDouble() * 0.6);
            level.sendParticles(MaidSpellParticles.VOID_SPELL.get(),
                    entity.getX() + Math.cos(angle) * radius,
                    entity.getY() + halfHeight + (random.nextDouble() - 0.5) * VERTICAL_SPREAD,
                    entity.getZ() + Math.sin(angle) * radius,
                    1, 0.0, 0.0, 0.0, 0.0);
        }
    }
}

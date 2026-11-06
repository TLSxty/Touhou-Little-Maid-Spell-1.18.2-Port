package com.github.yimeng261.maidspell.effect;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.yimeng261.maidspell.api.IAuthoritativeHealth;
import com.github.yimeng261.maidspell.compat.MaidSpellAllyResolver;
import com.github.yimeng261.maidspell.particle.MaidSpellParticles;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 星锚眩晕：落地冲击波，命中范围内的敌人施加眩晕并打断当前动作。
 * <p>
 * 相比 1.20.1 原实现，这里去掉了铁魔法（StaranchorStunSpellInterrupt）与
 * SpellBookManager#stopAllCasting 两段——它们属于魔法系统，本移植不启用。
 */
public final class StaranchorStunEffect extends MobEffect {

    public static final double SHOCKWAVE_RADIUS = 6.0;
    public static final int STUN_TICKS = 120;

    public StaranchorStunEffect() {
        super(MobEffectCategory.HARMFUL, 10064314);
    }

    public static boolean isStunned(LivingEntity entity) {
        return MaidSpellEffects.STARANCHOR_STUN.isPresent()
                && entity.hasEffect(MaidSpellEffects.STARANCHOR_STUN.get());
    }

    public static void shockwave(ServerLevel level, LivingEntity user) {
        for (LivingEntity enemy : level.getEntitiesOfClass(LivingEntity.class,
                user.getBoundingBox().inflate(SHOCKWAVE_RADIUS),
                entity -> entity != user
                        && IAuthoritativeHealth.combatAlive(entity)
                        && !entity.isSpectator()
                        && (!(entity instanceof Player player) || !player.isCreative())
                        && user.distanceToSqr(entity) <= SHOCKWAVE_RADIUS * SHOCKWAVE_RADIUS
                        && !MaidSpellAllyResolver.areFriendly(user, entity))) {
            if (!enemy.addEffect(new MobEffectInstance(MaidSpellEffects.STARANCHOR_STUN.get(),
                    STUN_TICKS, 0, false, false, false), user)) {
                continue;
            }
            interrupt(enemy);
        }
        Vec3 center = user.position().add(0.0, 0.15, 0.0);
        for (int i = 0; i < 48; i++) {
            double angle = Math.PI * 2 * i / 48.0;
            double dx = Math.cos(angle);
            double dz = Math.sin(angle);
            level.sendParticles(MaidSpellParticles.VOID_SPELL.get(),
                    center.x + dx * SHOCKWAVE_RADIUS,
                    center.y,
                    center.z + dz * SHOCKWAVE_RADIUS,
                    0, dx, 0.0, dz, 0.15);
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        interrupt(entity);
    }

    public static void interrupt(LivingEntity entity) {
        entity.setDeltaMovement(Vec3.ZERO);
        entity.resetFallDistance();
        entity.stopUsingItem();
        entity.swinging = false;
        entity.swingTime = 0;
        if (entity.getLevel().isClientSide()) {
            return;
        }
        entity.hurtMarked = true;
        if (entity instanceof Mob mob) {
            mob.getNavigation().stop();
        }
        // TODO(魔法不启用)：原实现还会调 StaranchorStunSpellInterrupt 与 SpellBookManager#stopAllCasting。
        if (entity instanceof EntityMaid) {
            // 女仆的施法状态属于魔法系统，本移植无对应逻辑。
        }
    }

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (event.getEntity() instanceof LivingEntity living && isStunned(living)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent event) {
        if (event.isCancelable() && event.getEntity() instanceof LivingEntity living && isStunned(living)) {
            event.setCanceled(true);
        }
    }
}

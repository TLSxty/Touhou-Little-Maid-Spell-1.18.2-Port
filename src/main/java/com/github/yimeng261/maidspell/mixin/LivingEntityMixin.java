package com.github.yimeng261.maidspell.mixin;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.yimeng261.maidspell.Global;
import com.github.yimeng261.maidspell.api.IAuthoritativeHealth;
import com.github.yimeng261.maidspell.item.bauble.hairpin.HairpinBauble;
import com.github.yimeng261.maidspell.item.bauble.soulBook.SoulBookBauble;
import com.github.yimeng261.maidspell.item.bauble.woundRimeBlade.WoundRimeBladeBauble;
import com.github.yimeng261.maidspell.spell.manager.BaubleStateManager;
import com.github.yimeng261.maidspell.utils.MaidDamageProcessor;
import com.github.yimeng261.maidspell.utils.MaidHealthWriteGuard;
import java.util.Map;
import java.util.function.BiFunction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 生命值写入总闸：拦截 {@code setHealth}，把女仆/主人的扣血交给饰品分发链处理。
 * <p>
 * 与 1.20.1 原实现的差异：
 * <ul>
 *   <li>原版用 {@code @WrapOperation}（mixinextras）拦截 {@code addEffect} 内部的 Map.put，
 *       1.18.2 Forge 不自带 mixinextras，这里改为在 {@code addEffect} 头部 {@code @Inject} +
 *       {@code setReturnValue(false)}，语义等价（效果不会进入效果表），且不引入额外依赖。</li>
 *   <li>原版还混入了 {@code canStandOnFluid}（浮狐叶/熔狐叶水面行走）。这两个饰品本批次未搬，
 *       故该方法暂不注入；搬浮狐叶/熔狐叶时在此补回。</li>
 *   <li>{@code getMaxHealth} 的权威血量分支保留，但本工程没有实现
 *       {@link IAuthoritativeHealth} 的实体（原版只有酒狐 Boss），因此实际不生效。</li>
 * </ul>
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Shadow
    @Final
    private static EntityDataAccessor<Float> DATA_HEALTH_ID;

    @Inject(method = "getMaxHealth", at = @At("HEAD"), cancellable = true)
    private void maidspell$readAuthoritativeMaxHealth(CallbackInfoReturnable<Float> cir) {
        if ((Object) this instanceof IAuthoritativeHealth authority) {
            cir.setReturnValue(authority.maidspell$authoritativeMaxHealth());
        }
    }

    @Inject(method = "setHealth", at = @At("HEAD"), cancellable = true)
    private void maidspell$onSetHealth(float health, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        float oldHealth;
        try {
            oldHealth = IAuthoritativeHealth.health(entity);
        } catch (Exception e) {
            return;
        }
        if (oldHealth <= health) {
            if (WoundRimeBladeBauble.handleWoundRimeMap(entity, health)) {
                ci.cancel();
            }
            return;
        }
        maidspell$handleHpDecrease(entity, health, ci);
    }

    @Unique
    private void maidspell$handleHpDecrease(LivingEntity entity, float health, CallbackInfo ci) {
        if (entity instanceof ServerPlayer player) {
            if (HairpinBauble.captureRedirectedDamage(player, player.getHealth() - health)) {
                player.hurtTime = 0;
                ci.cancel();
                return;
            }
            if (!SoulBookBauble.hasOwnerProtection(player.getUUID())) {
                return;
            }
            if (!MaidDamageProcessor.isCommonHurt(health, entity)) {
                ci.cancel();
                return;
            }
        }
        if (!(entity instanceof EntityMaid maid)) {
            return;
        }
        MaidDamageProcessor.MaidHealthChange healthChange =
                MaidDamageProcessor.processMaidHealthDecrease(maid, health);
        if (healthChange.outcome() == MaidDamageProcessor.MaidHealthChange.Outcome.PASS) {
            // 不是战斗掉血（外部模组直接 setHealth、指令改血等），且女仆没有魂之书：
            // 放行给原版写入，不做任何拦截 —— 否则女仆会对一切非常规伤害无条件免疫。
            return;
        }
        if (healthChange.outcome() == MaidDamageProcessor.MaidHealthChange.Outcome.BLOCK) {
            // 魂之书的「非常规伤害拦截」生效：拒绝这次写血，血量保持不变。
            ci.cancel();
            return;
        }
        MaidHealthWriteGuard.runBypassing(() ->
                maid.getEntityData().set(DATA_HEALTH_ID, healthChange.health()));
        ci.cancel();
    }

    /**
     * 真伤分发入口：原模组用字节码 coremod（{@code HurtHeadTransformationService}）改写 {@code hurt} 方法头，
     * 本工程改用普通 {@code @Inject}，语义等价 —— 有处理器「接了」这次伤害就取消原版 {@code hurt}。
     */
    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void maidspell$dispatchHurtHead(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (Global.baubleHurtHeadHandlers.isEmpty() && Global.commonHurtHeadHandlers.isEmpty()) {
            return;
        }
        Global.HurtHeadContext context =
                Global.dispatchHurtHeadHandlers((LivingEntity) (Object) this, source, amount);
        if (context.isHandled()) {
            cir.setReturnValue(true);
        }
    }

    /**
     * 命中 {@code Global.baubleEffectBlockFilters} 的效果直接拒绝添加（如云梦水晶挡负面效果）。
     */
    @Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
            at = @At("HEAD"), cancellable = true)
    private void maidspell$blockEffect(MobEffectInstance effectInstance, Entity source,
                                       CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof EntityMaid maid)) {
            return;
        }
        if (effectInstance == null) {
            return;
        }
        if (maidspell$shouldBlockMaidEffect(maid, effectInstance.getEffect())) {
            cir.setReturnValue(false);
        }
    }

    @Unique
    private static boolean maidspell$shouldBlockMaidEffect(EntityMaid maid, MobEffect effect) {
        for (Map.Entry<Item, BiFunction<EntityMaid, MobEffect, Boolean>> entry
                : Global.baubleEffectBlockFilters.entrySet()) {
            if (!BaubleStateManager.hasBauble(maid, entry.getKey())) {
                continue;
            }
            if (Boolean.TRUE.equals(entry.getValue().apply(maid, effect))) {
                return true;
            }
        }
        return false;
    }
}

// 移植自 1.9.2.1 反编译源 item/bauble/hairpin/HairpinBauble.java；SRG 名已替换为 1.18.2 official 名。
package com.github.yimeng261.maidspell.item.bauble.hairpin;

import com.github.tartaricacid.touhoulittlemaid.api.bauble.IMaidBauble;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.util.ItemsUtil;
import com.github.yimeng261.maidspell.Config;
import com.github.yimeng261.maidspell.Global;
import com.github.yimeng261.maidspell.damage.InfoDamageSource;
import com.github.yimeng261.maidspell.item.MaidSpellItems;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.PotionEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;

/**
 * 发簪：好感度越高，正面效果时长越长、负面效果越容易被拒。
 * <p>
 * 与 1.20.1 原实现的差异：
 * <ul>
 *   <li>{@code MobEffectEvent.Applicable} 在 1.18.2 不存在，改用
 *       {@link PotionEvent.PotionApplicableEvent}（构造参数同样带 {@link MobEffectInstance}）。</li>
 *   <li>原文另有两个 mixin（{@code HairpinServerLevelMixin} 注入 {@code broadcastDamageEvent}、
 *       {@code HairpinServerPlayerMixin} 注入 {@code indicateDamage}）用于在「主人伤害被女仆接管」
 *       期间屏蔽主人身上的受伤动画与伤害指示。这两个方法在 1.18.2 完全不存在
 *       （只有 {@code ServerLevel#broadcastEntityEvent(Entity,byte)}），因此本次未移植这两个 mixin：
 *       功能不受影响，只是主人被接管的瞬间可能仍会闪一下受伤动画。见 TODO(发簪动画抑制)。</li>
 * </ul>
 */
public class HairpinBauble
implements IMaidBauble {
    public static final Logger LOGGER = LogUtils.getLogger();
    private static final ThreadLocal<RedirectContext> ACTIVE_REDIRECT = new ThreadLocal<>();

    public static boolean captureRedirectedDamage(Player player, float amount) {
        RedirectContext context = ACTIVE_REDIRECT.get();
        if (context == null || context.owner != player) {
            return false;
        }
        context.redirectedAmount = Math.max(0.0f, amount);
        context.captured = true;
        return true;
    }

    public static boolean isSamplingOwnerDamage(LivingEntity entity) {
        RedirectContext context = ACTIVE_REDIRECT.get();
        return context != null && context.owner == entity;
    }

    public HairpinBauble() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public void onTick(EntityMaid maid, ItemStack baubleItem) {
        if (maid.tickCount % 10 == 0) {
            List<MobEffect> toRemove = new ArrayList<>();
            for (MobEffectInstance instance : maid.getActiveEffects()) {
                MobEffect effect = instance.getEffect();
                if (!effect.isBeneficial()) {
                    toRemove.add(effect);
                }
            }
            for (MobEffect effect : toRemove) {
                maid.removeEffect(effect);
            }
        }
    }

    @SubscribeEvent
    public void beforeMaidEffectAdded(PotionEvent.PotionApplicableEvent event) {
        LivingEntity livingEntity = event.getEntityLiving();
        if (!(livingEntity instanceof EntityMaid maid)) {
            return;
        }
        if (ItemsUtil.getBaubleSlotInMaid(maid, this) < 0) {
            return;
        }
        MobEffectInstance effectInstance = event.getPotionEffect();
        int favorabilityLevel = maid.getFavorabilityManager().getLevel();
        if (favorabilityLevel >= 3 && !effectInstance.getEffect().isBeneficial()) {
            event.setResult(Event.Result.DENY);
        }
        if (favorabilityLevel >= 2 && effectInstance.getEffect().isBeneficial()) {
            int duration = effectInstance.getDuration();
            int extended = Math.max((int) (duration * Config.hairpinBeneficialEffectExtension),
                    duration + Config.hairpinMinExtensionTicks);
            effectInstance.update(new MobEffectInstance(effectInstance.getEffect(), extended,
                    effectInstance.getAmplifier(), effectInstance.isAmbient(), effectInstance.isVisible()));
        }
    }

    static {
        Global.baubleHurtEventHandlers.put(MaidSpellItems.HAIRPIN.get(), (event, maid) -> {
            LivingEntity owner = maid.getOwner();
            if (!(owner instanceof Player player)) {
                return null;
            }
            DamageSource source = event.getSource();
            if (source instanceof InfoDamageSource infoDamage
                    && "hairpin_redirect".equals(infoDamage.msg_type)) {
                return null;
            }
            RedirectContext existing = ACTIVE_REDIRECT.get();
            if (existing != null && existing.maid == maid) {
                return null;
            }
            RedirectContext context = new RedirectContext(maid, player);
            ACTIVE_REDIRECT.set(context);
            try {
                InfoDamageSource hairpinDamage =
                        InfoDamageSource.create(player.getLevel(), "hairpin_redirect", source);
                hairpinDamage.setSourceEntity(maid);
                player.setInvulnerable(false);
                player.hurtTime = 0;
                player.hurt(hairpinDamage, event.getAmount());
                event.setAmount(context.captured ? context.redirectedAmount : 0.0f);
            } finally {
                ACTIVE_REDIRECT.remove();
            }
            return null;
        });
    }

    private static final class RedirectContext {
        private final EntityMaid maid;
        private final Player owner;
        private float redirectedAmount;
        private boolean captured;

        private RedirectContext(EntityMaid maid, Player owner) {
            this.maid = maid;
            this.owner = owner;
        }
    }
}

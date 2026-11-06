// 移植自 1.9.2.1 反编译源 item/bauble/chaosBook/ChaosBookBauble.java；SRG 名已替换为 1.18.2 official 名。
package com.github.yimeng261.maidspell.item.bauble.chaosBook;

import com.github.tartaricacid.touhoulittlemaid.api.bauble.IMaidBauble;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.yimeng261.maidspell.Config;
import com.github.yimeng261.maidspell.Global;
import com.github.yimeng261.maidspell.damage.InfoDamageSource;
import com.github.yimeng261.maidspell.item.MaidSpellItems;
import com.github.yimeng261.maidspell.spell.manager.BaubleStateManager;
import com.github.yimeng261.maidspell.utils.TrueDamageUtil;
import com.mojang.logging.LogUtils;
import java.util.IdentityHashMap;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.MinecraftForge;
import org.slf4j.Logger;

public class ChaosBookBauble
implements IMaidBauble {
    public static final Logger LOGGER = LogUtils.getLogger();
    private static final ThreadLocal<IdentityHashMap<LivingEntity, Integer>> ACTIVE_CHAOS_DAMAGE = ThreadLocal.withInitial(IdentityHashMap::new);

    public ChaosBookBauble() {
        MinecraftForge.EVENT_BUS.register((Object)this);
    }

    public static void chaosBookProcess(EntityMaid maid, LivingEntity target) {
        if (TrueDamageUtil.isApplyingQueuedDamage()) {
            return;
        }
        if (BaubleStateManager.hasBauble(maid, MaidSpellItems.CHAOS_BOOK)) {
            float multiplier = BaubleStateManager.hasBauble(maid, MaidSpellItems.DREAM_CAT_CRYSTAL) ? 2.0f : 1.0f;
            float damage = (float)Math.max(Config.chaosBookTrueDamageMin, (double)target.getMaxHealth() * Config.chaosBookTrueDamagePercent) * multiplier;
            TrueDamageUtil.dealTrueDamage(target, damage, (LivingEntity)maid);
        }
    }

    private static boolean isApplyingChaosDamage(LivingEntity target) {
        return target != null && ACTIVE_CHAOS_DAMAGE.get().containsKey(target);
    }

    private static void pushChaosDamage(LivingEntity target) {
        if (target == null) {
            return;
        }
        IdentityHashMap<LivingEntity, Integer> activeCalls = ACTIVE_CHAOS_DAMAGE.get();
        activeCalls.merge(target, 1, Integer::sum);
    }

    private static void popChaosDamage(LivingEntity target) {
        if (target == null) {
            return;
        }
        IdentityHashMap<LivingEntity, Integer> activeCalls = ACTIVE_CHAOS_DAMAGE.get();
        Integer depth = activeCalls.get(target);
        if (depth == null) {
            return;
        }
        if (depth <= 1) {
            activeCalls.remove(target);
            if (activeCalls.isEmpty()) {
                ACTIVE_CHAOS_DAMAGE.remove();
            }
        } else {
            activeCalls.put(target, depth - 1);
        }
    }

    static {
        Global.registerBaubleHurtHeadHandler((Item)MaidSpellItems.CHAOS_BOOK.get(), context -> {
            EntityMaid maid = context.getSourceMaid();
            if (maid != null) {
                ChaosBookBauble.chaosBookProcess(maid, context.getTarget());
            }
        });
        Global.baubleDamageHandlers.put((Item)MaidSpellItems.CHAOS_BOOK.get(), (event, maid) -> {
            LivingEntity target = event.getEntityLiving();
            DamageSource source = event.getSource();
            Float amount = Float.valueOf(event.getAmount());
            if (ChaosBookBauble.isApplyingChaosDamage(target)) {
                return null;
            }
            if (source instanceof InfoDamageSource) {
                InfoDamageSource infoDamage = (InfoDamageSource)source;
                if ("chaos_book".equals(infoDamage.msg_type)) {
                    ChaosBookBauble.pushChaosDamage(target);
                    try {
                        InfoDamageSource newDamageSource = InfoDamageSource.create(target.getLevel(), "chaos_book2", infoDamage.damage_source);
                        target.setInvulnerable(false);
                        target.hurtTime = 0;
                        target.hurt((DamageSource)newDamageSource, amount.floatValue());
                        event.setCanceled(true);
                    }
                    finally {
                        target.hurtTime = 0;
                        target.setInvulnerable(false);
                        ChaosBookBauble.popChaosDamage(target);
                    }
                }
            } else {
                ChaosBookBauble.pushChaosDamage(target);
                try {
                    int n = Config.chaosBookDamageSplitCount;
                    InfoDamageSource newDamageSource = InfoDamageSource.create(target.getLevel(), "chaos_book", source);
                    float finalAmount = Math.max(amount.floatValue() / (float)n, (float)Config.chaosBookMinSplitDamage);
                    for (int i = 0; i < n; ++i) {
                        target.setInvulnerable(false);
                        target.hurtTime = 0;
                        target.hurt((DamageSource)newDamageSource, finalAmount);
                    }
                    event.setCanceled(true);
                }
                finally {
                    target.setInvulnerable(false);
                    target.hurtTime = 0;
                    ChaosBookBauble.popChaosDamage(target);
                }
            }
            return null;
        });
    }
}


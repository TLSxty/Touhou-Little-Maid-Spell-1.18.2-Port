// 移植自 1.9.2.1 反编译源 item/bauble/doubleHeartChain/DoubleHeartChainBauble.java；SRG 名已替换为 1.18.2 official 名。
package com.github.yimeng261.maidspell.item.bauble.doubleHeartChain;

import com.github.tartaricacid.touhoulittlemaid.api.bauble.IMaidBauble;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.yimeng261.maidspell.Config;
import com.github.yimeng261.maidspell.Global;
import com.github.yimeng261.maidspell.damage.InfoDamageSource;
import com.github.yimeng261.maidspell.item.MaidSpellItems;
import com.github.yimeng261.maidspell.spell.manager.BaubleStateManager;
import java.util.IdentityHashMap;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

public class DoubleHeartChainBauble
implements IMaidBauble {
    private static final ThreadLocal<IdentityHashMap<EntityMaid, Integer>> ACTIVE_SHARED_DAMAGE = ThreadLocal.withInitial(IdentityHashMap::new);

    private static boolean isSharingDamage(EntityMaid maid) {
        return maid != null && ACTIVE_SHARED_DAMAGE.get().containsKey(maid);
    }

    private static void pushSharedDamage(EntityMaid maid) {
        if (maid == null) {
            return;
        }
        ACTIVE_SHARED_DAMAGE.get().merge(maid, 1, Integer::sum);
    }

    private static void popSharedDamage(EntityMaid maid) {
        if (maid == null) {
            return;
        }
        IdentityHashMap<EntityMaid, Integer> activeCalls = ACTIVE_SHARED_DAMAGE.get();
        Integer depth = activeCalls.get(maid);
        if (depth == null) {
            return;
        }
        if (depth <= 1) {
            activeCalls.remove(maid);
            if (activeCalls.isEmpty()) {
                ACTIVE_SHARED_DAMAGE.remove();
            }
        } else {
            activeCalls.put(maid, depth - 1);
        }
    }

    static {
        Global.baubleSetHealthFinalHandlers.put((Item)MaidSpellItems.DOUBLE_HEART_CHAIN.get(), data -> {
            EntityMaid maid = data.getMaid();
            LivingEntity patt1913$temp = maid.getOwner();
            if (patt1913$temp instanceof Player) {
                Player owner = (Player)patt1913$temp;
                if (!owner.getLevel().isClientSide) {
                    float originalDamage = data.getAmount();
                    if (BaubleStateManager.hasBauble(maid, MaidSpellItems.DREAM_CAT_CRYSTAL)) {
                        data.setAmount(originalDamage * 0.5f);
                    } else {
                        if ((double)maid.distanceTo((Entity)owner) > Config.doubleHeartChainMaxDistance) {
                            return null;
                        }
                        if (!DoubleHeartChainBauble.isSharingDamage(maid)) {
                            float sharedDamage = originalDamage * (float)Config.doubleHeartChainShareRatio;
                            data.setAmount(sharedDamage);
                            DoubleHeartChainBauble.pushSharedDamage(maid);
                            try {
                                DamageSource defaultDamageSource = DamageSource.GENERIC;
                                InfoDamageSource infoDamageSource = InfoDamageSource.create(owner.getLevel(), "double_heart_chain", defaultDamageSource);
                                owner.hurt((DamageSource)infoDamageSource, sharedDamage);
                            }
                            finally {
                                DoubleHeartChainBauble.popSharedDamage(maid);
                            }
                        }
                    }
                }
            }
            return null;
        });
    }
}


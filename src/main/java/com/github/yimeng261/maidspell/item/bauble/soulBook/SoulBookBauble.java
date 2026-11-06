// 移植自 1.9.2.1 反编译源 item/bauble/soulBook/SoulBookBauble.java；SRG 名已替换为 1.18.2 official 名。
package com.github.yimeng261.maidspell.item.bauble.soulBook;

import com.github.tartaricacid.touhoulittlemaid.api.bauble.IMaidBauble;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.yimeng261.maidspell.Config;
import com.github.yimeng261.maidspell.Global;
import com.github.yimeng261.maidspell.item.MaidSpellItems;
import com.github.yimeng261.maidspell.spell.manager.BaubleStateManager;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.item.ItemStack;
import oshi.util.tuples.Pair;

public class SoulBookBauble
implements IMaidBauble {
    private static final Map<UUID, Integer> lastHurtTimeMap = new ConcurrentHashMap<UUID, Integer>();

    public static Pair<Boolean, Float> damageCalc(EntityMaid maid, float originalDamage) {
        UUID maidId = maid.getUUID();
        int currentTime = maid.tickCount;
        int lastHurtTime = lastHurtTimeMap.computeIfAbsent(maidId, uuid -> maid.tickCount);
        int timeDiff = currentTime - lastHurtTime;
        float damageThreshold = Math.min(originalDamage, maid.getMaxHealth() * (float)Config.soulBookDamageThresholdPercent);
        return new Pair((Object)(timeDiff > Config.soulBookDamageIntervalThreshold ? 1 : 0), (Object)Float.valueOf(damageThreshold));
    }

    public static void recordHurt(EntityMaid maid) {
        lastHurtTimeMap.put(maid.getUUID(), maid.tickCount);
    }

    public static boolean hasOwnerProtection(UUID ownerId) {
        Map<UUID, EntityMaid> maids = Global.ownerMaidRegistry.get(ownerId);
        if (maids == null) {
            return false;
        }
        for (EntityMaid maid : maids.values()) {
            if (maid == null || maid.isRemoved() || !maid.isAddedToWorld() || !maid.isAlive() || !ownerId.equals(maid.getOwnerUUID()) || !BaubleStateManager.hasBauble(maid, MaidSpellItems.SOUL_BOOK)) continue;
            return true;
        }
        return false;
    }

    public void onTakeOff(EntityMaid maid, ItemStack baubleItem) {
        if (!maid.getLevel().isClientSide()) {
            SoulBookBauble.cleanupMaid(maid.getUUID());
        }
    }

    public static void cleanupMaid(UUID maidId) {
        lastHurtTimeMap.remove(maidId);
    }

    public static void clearSession() {
        lastHurtTimeMap.clear();
    }
}


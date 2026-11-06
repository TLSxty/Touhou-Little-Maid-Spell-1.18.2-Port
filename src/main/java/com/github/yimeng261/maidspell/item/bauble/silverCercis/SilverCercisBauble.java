// 移植自 1.9.2.1 反编译源 item/bauble/silverCercis/SilverCercisBauble.java；SRG 名已替换为 1.18.2 official 名。
package com.github.yimeng261.maidspell.item.bauble.silverCercis;

import com.github.tartaricacid.touhoulittlemaid.api.bauble.IMaidBauble;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.yimeng261.maidspell.Config;
import com.github.yimeng261.maidspell.item.MaidSpellItems;
import com.github.yimeng261.maidspell.spell.manager.BaubleStateManager;
import com.github.yimeng261.maidspell.utils.DataItem;
import com.github.yimeng261.maidspell.utils.TrueDamageUtil;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import oshi.util.tuples.Pair;

public class SilverCercisBauble
implements IMaidBauble {
    private static final ConcurrentHashMap<UUID, Pair<Integer, Integer>> maidCercisMap = new ConcurrentHashMap();

    public static void handleSilverCercis(EntityMaid maid, LivingEntity target, DataItem dataItem) {
        float multiplier;
        UUID uuid = maid.getUUID();
        Pair record = maidCercisMap.computeIfAbsent(uuid, k -> new Pair((Object)0, (Object)0));
        int tick = maid.tickCount;
        float f = multiplier = BaubleStateManager.hasBauble(maid, MaidSpellItems.DREAM_CAT_CRYSTAL) ? 2.0f : 1.0f;
        if ((Integer)record.getA() >= Config.silverCercisTriggerCount || tick - (Integer)record.getB() > Config.silverCercisCooldownTicks || BaubleStateManager.hasBauble(maid, MaidSpellItems.DREAM_CAT_CRYSTAL)) {
            if (TrueDamageUtil.dealTrueDamage(target, dataItem.getAmount() * (float)Config.silverCercisTrueDamageMultiplier * multiplier, (LivingEntity)maid)) {
                maidCercisMap.put(uuid, (Pair<Integer, Integer>)new Pair((Object)0, (Object)tick));
            }
        } else {
            maidCercisMap.put(uuid, (Pair<Integer, Integer>)new Pair((Object)((Integer)record.getA() + 1), (Object)tick));
        }
    }

    public void onTakeOff(EntityMaid maid, ItemStack baubleItem) {
        if (!maid.getLevel().isClientSide()) {
            SilverCercisBauble.cleanupMaid(maid.getUUID());
        }
    }

    public static void cleanupMaid(UUID maidId) {
        maidCercisMap.remove(maidId);
    }

    public static void clearSession() {
        maidCercisMap.clear();
    }
}


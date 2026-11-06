package com.github.yimeng261.maidspell.utils;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.yimeng261.maidspell.Global;
import com.github.yimeng261.maidspell.api.IAuthoritativeHealth;
import com.github.yimeng261.maidspell.item.MaidSpellItems;
import com.github.yimeng261.maidspell.item.bauble.silverCercis.SilverCercisBauble;
import com.github.yimeng261.maidspell.item.bauble.soulBook.SoulBookBauble;
import com.github.yimeng261.maidspell.mixin.accessor.CombatTrackerMixin;
import com.github.yimeng261.maidspell.spell.manager.BaubleStateManager;
import net.minecraft.world.damagesource.CombatEntry;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import oshi.util.tuples.Pair;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 女仆生命值下降的统一处理管线：护主判定 → 紫荆银冠 → 魂之书 → 各饰品 setHealth 钩子。
 * <p>
 * 只在 1.18.2 的 {@code LivingEntity#setHealth} mixin 里被调用（原模组同）。
 */
public final class MaidDamageProcessor {

    private MaidDamageProcessor() {
    }

    public record MaidHealthChange(boolean writeHealth, float health) {
        public static MaidHealthChange block() {
            return new MaidHealthChange(false, 0.0f);
        }

        public static MaidHealthChange write(float health) {
            return new MaidHealthChange(true, health);
        }
    }

    /**
     * 判断这次 setHealth 是否由常规受伤流程触发（而不是药水/指令等直接写血）。
     */
    public static boolean isCommonHurt(float requestedHealth, LivingEntity entity) {
        List<CombatEntry> entries = ((CombatTrackerMixin) entity.getCombatTracker()).getEntries();
        if (entries == null || entries.isEmpty()) {
            return false;
        }
        CombatEntry last = entries.get(entries.size() - 1);
        float expected = entity.getHealth() - requestedHealth;
        return Math.abs(last.getHealthBeforeDamage() - expected) <= 0.1f;
    }

    public static MaidHealthChange processMaidHealthDecrease(EntityMaid maid, float requestedHealth) {
        float current = IAuthoritativeHealth.health(maid);
        DataItem dataItem = new DataItem(maid, current - requestedHealth);
        processSilverCercis(maid, dataItem);
        if (!isCommonHurt(requestedHealth, maid)) {
            return MaidHealthChange.block();
        }
        processSoulBook(dataItem);
        applyBaubleHandlers(dataItem, maid);
        float amount = dataItem.isCanceled() ? 0.0f : dataItem.getAmount();
        return MaidHealthChange.write(Math.max(0.0f, current - amount));
    }

    private static void processSilverCercis(EntityMaid maid, DataItem dataItem) {
        if (!BaubleStateManager.hasBauble(maid, MaidSpellItems.SLIVER_CERCIS)) {
            return;
        }
        LivingEntity target = maid.getLastHurtByMob();
        if (!IAuthoritativeHealth.combatAlive(target)) {
            target = maid.getLastHurtMob();
        }
        SilverCercisBauble.handleSilverCercis(maid, target, dataItem);
    }

    private static void processSoulBook(DataItem dataItem) {
        EntityMaid maid = dataItem.getMaid();
        if (!BaubleStateManager.hasBauble(maid, MaidSpellItems.SOUL_BOOK)) {
            return;
        }
        Pair<Boolean, Float> result = SoulBookBauble.damageCalc(maid, dataItem.getAmount());
        if (Boolean.FALSE.equals(result.getA())) {
            dataItem.setCanceled(true);
            return;
        }
        dataItem.setAmount(result.getB());
        SoulBookBauble.recordHurt(maid);
    }

    private static void applyBaubleHandlers(DataItem dataItem, EntityMaid maid) {
        for (Map.Entry<Item, Function<DataItem, Void>> entry : Global.baubleSetHealthHandlers.entrySet()) {
            if (!BaubleStateManager.hasBauble(maid, entry.getKey())) {
                continue;
            }
            entry.getValue().apply(dataItem);
        }
        for (Map.Entry<Item, Function<DataItem, Void>> entry : Global.baubleSetHealthFinalHandlers.entrySet()) {
            if (!BaubleStateManager.hasBauble(maid, entry.getKey())) {
                continue;
            }
            entry.getValue().apply(dataItem);
        }
    }
}

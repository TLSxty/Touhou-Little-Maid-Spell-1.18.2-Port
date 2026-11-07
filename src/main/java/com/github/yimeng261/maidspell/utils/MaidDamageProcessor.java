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

    /**
     * 女仆血量写入的结果。
     * <ul>
     *   <li>{@link Outcome#PASS}：本次写血不是战斗流程触发的（例如外部模组直接
     *       {@code setHealth}、指令改血），**放行**给原版写入，不做任何拦截。
     *       装饰性的减伤/免疫不适用于这类写血。</li>
     *   <li>{@link Outcome#BLOCK}：本次写血被**拒绝**，血量保持不变。
     *       唯一的来源是魂之书的饰品效果——「同时拦截玩家与女仆的非正常伤害结算链路」
     *       （道具描述 {@code item.maidspell.soul_book.desc4}）。</li>
     *   <li>{@link Outcome#WRITE}：本次是战斗掉血，已走过饰品管线，由调用方写入
     *       {@link MaidHealthChange#health()} 给出的血量。</li>
     * </ul>
     */
    public record MaidHealthChange(Outcome outcome, float health) {
        public enum Outcome {
            /** 放行：不拦截，原版照常写入。 */
            PASS,
            /** 拒绝：本次写血被魂之书的「非常规伤害拦截」挡下，血量不变。 */
            BLOCK,
            /** 由本模组写入指定血量。 */
            WRITE
        }

        public static MaidHealthChange pass() {
            return new MaidHealthChange(Outcome.PASS, 0.0f);
        }

        public static MaidHealthChange block() {
            return new MaidHealthChange(Outcome.BLOCK, 0.0f);
        }

        public static MaidHealthChange write(float health) {
            return new MaidHealthChange(Outcome.WRITE, health);
        }

        public boolean writeHealth() {
            return outcome == Outcome.WRITE;
        }

        /** 是否应当拒绝本次写血（血量保持不变）。 */
        public boolean blockHealth() {
            return outcome == Outcome.BLOCK;
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
        // 必须比对 CombatEntry 的 damage 字段（本次最终伤害量），不是 health 字段（受伤前血量）。
        // 原模组写的是 f_19252_，对应 1.18.2 的 getDamage()；f_19253_（health）才是 getHealthBeforeDamage()。
        return Math.abs(last.getDamage() - expected) <= 0.1f;
    }

    public static MaidHealthChange processMaidHealthDecrease(EntityMaid maid, float requestedHealth) {
        float current = IAuthoritativeHealth.health(maid);
        DataItem dataItem = new DataItem(maid, current - requestedHealth);
        processSilverCercis(maid, dataItem);
        if (!isCommonHurt(requestedHealth, maid)) {
            // 不是战斗流程触发的写血（外部模组直接 setHealth、指令改血等）：
            // 不进饰品管线（战斗减伤/吸血等不适用于这类写血）。
            // 是否**拒绝**这次写血，取决于魂之书的饰品效果——道具描述写明
            // 「同时拦截玩家与女仆的非正常伤害结算链路」（item.maidspell.soul_book.desc4）。
            // 没有魂之书 → 放行（原版照常写入）；有魂之书 → 拒绝，血量不变。
            // 上游 1.20.1 在这里无条件 block()，靠的是它自带的权威血量系统与真伤链兜底；
            // 本工程没有那套体系，照抄会让女仆对一切非常规伤害无条件免疫。
            if (BaubleStateManager.hasBauble(maid, MaidSpellItems.SOUL_BOOK)) {
                return MaidHealthChange.block();
            }
            return MaidHealthChange.pass();
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

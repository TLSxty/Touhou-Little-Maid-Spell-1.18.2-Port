package com.github.yimeng261.maidspell.event;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.yimeng261.maidspell.item.MaidSpellItems;
import com.github.yimeng261.maidspell.item.bauble.dreamCatCrystal.DreamCatCrystalBauble;
import com.github.yimeng261.maidspell.spell.manager.BaubleStateManager;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 云梦水晶的「免伤 / 免疫」事件（非魔法部分）。
 *
 * <p>移植自 1.9.2.1 反编译源 event/DreamCrystalMaidEvents.java（203 行）。</p>
 *
 * <p><b>保留</b>：
 * <ul>
 *   <li>复活后的无敌窗口：饰品 NBT {@code dream_crystal_invulnerable_time > 0} 时取消一切伤害。</li>
 *   <li>固定伤害类型免疫：燃烧 / 熔岩 / 溺水 / 爆炸 / 原版魔法。</li>
 * </ul>
 *
 * <p><b>去掉</b>：
 * <ul>
 *   <li>Boss 仇恨控制（{@code LivingChangeTargetEvent} / {@code LivingHurtEvent} 里的
 *       {@code MAID_ATTACKED_BOSSES} 记录）—— 原实现依赖 1.19+ 的
 *       {@code forge:bosses} 实体标签，1.18.2 的 Forge 没有 {@code Tags.EntityTypes}，
 *       属于表现层 AI 行为、计划文档 §2.3 也未列入保留项，故整段不搬。</li>
 * </ul>
 *
 * <p><b>1.18.2 的免疫判定方式</b>：原实现用 1.20 的 {@code DamageTypeTags} / {@code DamageTypes}
 * 判定，1.18.2 没有 {@code DamageType} 注册表，改用 {@link DamageSource} 的
 * {@code isFire()} / {@code isExplosion()} 与静态常量做等价判定。</p>
 */
@Mod.EventBusSubscriber(modid = com.github.yimeng261.maidspell.Config.MOD_ID)
public final class DreamCrystalMaidEvents {

    private DreamCrystalMaidEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMaidAttacked(LivingAttackEvent event) {
        if (!(event.getEntityLiving() instanceof EntityMaid maid)) {
            return;
        }
        if (maid.getLevel().isClientSide()) {
            return;
        }
        if (!BaubleStateManager.hasBauble(maid, MaidSpellItems.DREAM_CAT_CRYSTAL)) {
            return;
        }
        ItemStack stack = DreamCatCrystalBauble.findDreamCrystalStack(maid);
        if (stack != null && isInvulnerable(stack)) {
            event.setCanceled(true);
            return;
        }
        if (isImmuneTo(event.getSource())) {
            event.setCanceled(true);
        }
    }

    /** 复活后的无敌窗口是否还在。 */
    public static boolean isInvulnerable(ItemStack baubleItem) {
        if (baubleItem == null || baubleItem.isEmpty()) {
            return false;
        }
        var tag = baubleItem.getTag();
        return tag != null
                && tag.contains(DreamCatCrystalBauble.NBT_INVULNERABLE_TIME)
                && tag.getInt(DreamCatCrystalBauble.NBT_INVULNERABLE_TIME) > 0;
    }

    /**
     * 固定伤害类型免疫：燃烧 / 熔岩 / 溺水 / 爆炸 / 原版魔法。
     * <p>对应 1.20 的 {@code DamageTypeTags.IS_FIRE} / {@code IS_EXPLOSION} /
     * {@code DamageTypes.DROWN} / {@code DamageTypes.MAGIC}。
     */
    public static boolean isImmuneTo(DamageSource source) {
        if (source == null) {
            return false;
        }
        return source.isFire()
                || source == DamageSource.LAVA
                || source == DamageSource.HOT_FLOOR
                || source == DamageSource.DROWN
                || source.isExplosion()
                || source == DamageSource.MAGIC
                || source.isMagic();
    }
}

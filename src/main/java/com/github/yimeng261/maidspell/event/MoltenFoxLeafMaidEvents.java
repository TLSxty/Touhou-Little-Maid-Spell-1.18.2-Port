package com.github.yimeng261.maidspell.event;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.yimeng261.maidspell.Config;
import com.github.yimeng261.maidspell.item.MaidSpellItems;
import com.github.yimeng261.maidspell.spell.manager.BaubleStateManager;
import com.github.yimeng261.maidspell.utils.FoxLeafOwnerEffectHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 熔岩狐叶的火焰/岩浆伤害免疫（女仆与范围内的主人各一条）。
 * <p>
 * 移植自 1.9.2.1 反编译源 event/MoltenFoxLeafMaidEvents.java（78 行）。
 * <strong>1.18.2 改写点</strong>：原实现用 1.19+ 的 {@code DamageTypeTags.IS_FREEZING}
 * 与 {@code DamageTypes.LAVA/ON_FIRE/HOT_FLOOR/IN_FIRE} 判定，1.18.2 既没有
 * {@code DamageTypeTags} 类、也没有 {@code DamageTypes} 类。这里改用
 * {@link DamageSource#isFire()}（字节码确认 IN_FIRE / ON_FIRE / LAVA / HOT_FLOOR
 * 四个常量都是 {@code setIsFire()} 构造）与 {@code DamageSource.FREEZE} 常量。
 */
@Mod.EventBusSubscriber(modid = Config.MOD_ID)
public final class MoltenFoxLeafMaidEvents {

    private MoltenFoxLeafMaidEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onOwnerLivingAttack(LivingAttackEvent event) {
        LivingEntity entity = event.getEntityLiving();
        if (!(entity instanceof Player owner)) {
            return;
        }
        DamageSource source = event.getSource();
        if (!isFireOrLavaDamage(source) || !FoxLeafOwnerEffectHelper.hasMoltenProtection(owner)) {
            return;
        }
        owner.clearFire();
        event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMaidLivingAttack(LivingAttackEvent event) {
        LivingEntity entity = event.getEntityLiving();
        if (!(entity instanceof EntityMaid maid)) {
            return;
        }
        if (!BaubleStateManager.hasBauble(maid, MaidSpellItems.MOLTEN_FOX_LEAF)) {
            return;
        }
        if (isFireOrLavaDamage(event.getSource())) {
            event.setCanceled(true);
        }
    }

    /**
     * 是否为火焰/岩浆类伤害。
     * <p>
     * 1.18.2 的 {@code DamageSource} 没有 {@code is(DamageType)}，但四个火焰常量
     * （IN_FIRE / ON_FIRE / LAVA / HOT_FLOOR）都是 {@code setIsFire()} 构造的，
     * 所以 {@link DamageSource#isFire()} 一次即可覆盖；额外比对常量以防自定义伤害源漏判。
     * 冰冻伤害（{@code DamageSource.FREEZE}）显式排除，与原实现的 IS_FREEZING 判定等价。
     */
    private static boolean isFireOrLavaDamage(DamageSource source) {
        if (source == DamageSource.FREEZE) {
            return false;
        }
        return source.isFire()
                || source == DamageSource.IN_FIRE
                || source == DamageSource.ON_FIRE
                || source == DamageSource.LAVA
                || source == DamageSource.HOT_FLOOR;
    }
}

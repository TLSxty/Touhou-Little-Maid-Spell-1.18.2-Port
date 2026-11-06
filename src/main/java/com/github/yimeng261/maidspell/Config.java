package com.github.yimeng261.maidspell;

import com.github.yimeng261.maidspell.item.bauble.dreamCatCrystal.DreamCatCrystalBauble;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.IConfigSpec;
import net.minecraftforge.fml.event.config.ModConfigEvent;

/**
 * 配置入口（批次 A 精简版）。
 * <p>
 * 移植自 1.9.2.1 反编译源 Config.java。原文有 5 个 spec（战斗/饰品/退隐/兼容/winefox），
 * 本工程只保留饰品 spec，其余 4 个属魔法或未移植功能，删。
 * <p>
 * 与原文的差异：
 * <ul>
 *   <li>删掉 {@code spellDamageMultiplier / coolDownMultiplier}：只被 {@code Global.resetCommonDamageCalc}
 *       与 {@code resetCommonCoolDownCalc} 用来缩放「法术伤害/法术冷却」，属魔法乘区，批次 A 不搬。</li>
 *   <li>删掉 {@code applyRuntimeValues()} 里的 {@code SpellCombatMeleeTask / SpellCombatFarTask /
 *       SimplifiedSpellCaster} 调用：法术战斗任务不在本任务范围。</li>
 *   <li>删掉 {@code migrateDreamCrystalEffectBlacklist}：新移植无需迁移旧配置文件。</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = Config.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class Config {

    /** 模组 id，translatable 键里也用作前缀。 */
    public static final String MOD_ID = "touhou_little_maid_spell";

    public static final ForgeConfigSpec BAUBLES_SPEC = ConfigBaubles.SPEC;

    // ---- 云梦水晶 ----
    public static java.util.List<String> dreamCrystalEffectBlacklist = java.util.List.of();
    public static boolean dreamCrystalUseEffectWhitelist = true;
    public static java.util.List<String> dreamCrystalEffectWhitelist = java.util.List.of();
    public static boolean dreamCrystalExtraTrueDamageEnabled = true;

    // ---- 春华归返 ----
    public static int springBloomReturnMaxStacks = 3;
    public static int springBloomReturnStackDurationTicks = 400;
    public static int springBloomReturnGainCooldownTicks = 20;
    public static int springBloomReturnTriggerCooldownTicks = 200;
    public static double springBloomReturnDamageThreshold = 4.0;
    public static double springBloomReturnDamageThresholdRatio = 0.1;
    public static double springBloomReturnHealRatio = 0.05;

    private Config() {
    }

    @SubscribeEvent
    static void onLoad(ModConfigEvent event) {
        IConfigSpec spec = event.getConfig().getSpec();
        if (spec != BAUBLES_SPEC) {
            return;
        }
        ConfigBaubles.load(event);
        applyRuntimeValues();
    }

    private static void applyRuntimeValues() {
        Global.resetCommonDamageCalc();
        DreamCatCrystalBauble.invalidateBeneficialEffectsCache();
    }
}

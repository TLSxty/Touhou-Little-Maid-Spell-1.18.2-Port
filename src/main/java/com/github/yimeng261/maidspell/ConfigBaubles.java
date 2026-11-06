package com.github.yimeng261.maidspell;

import com.github.yimeng261.maidspell.item.bauble.dreamCatCrystal.DreamCatCrystalBauble;
import java.util.ArrayList;
import java.util.List;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.event.config.ModConfigEvent;

/**
 * 饰品相关的配置项。
 * <p>
 * 移植自 1.9.2.1 反编译源 ConfigBaubles.java。批次 A 只保留了云梦水晶与春华归返两组，
 * 批次 B 按原文件的分组结构补齐其余饰品的参数（伤害 / 治疗 / 防御 / 实用 / 触发 / 特殊）。
 * <p>
 * 与原文的差异：
 * <ul>
 *   <li>删掉 {@code migrateDreamCrystalEffectBlacklist}：那是给已装旧版模组的存档迁移默认值用的，
 *       本工程是全新移植，没有旧配置文件需要迁移。</li>
 *   <li>删掉 {@code springBloomReturnCooldownRefundRatio} 与 {@code quickChantRingCooldownReduction}：
 *       两者只服务于「法术冷却」这一纯魔法机制，本工程不移植。</li>
 * </ul>
 */
final class ConfigBaubles {

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final List<String> DEFAULT_WHITELIST = List.of("regex:minecraft:.*", "regex:goety:.*",
            "regex:irons_spellbooks:.*", "regex:ars_nouveau:.*", "regex:youkaishomecoming:.*",
            "regex:farmersdelight:.*", "regex:kaleidoscope_cookery:.*", "regex:alexsmobs:.*",
            "regex:alexscaves:.*", "regex:cataclysm:.*");

    private static final List<String> DEFAULT_BLACKLIST = List.of("irons_spellbooks:ascension",
            "irons_spellbooks:burning_dash", "irons_spellbooks:antigravity", "irons_spellbooks:volt_strike",
            "traveloptics:aqua_missiles_hover", "traveloptics:meteor_storm", "traveloptics:aerial_collapse",
            "traveloptics:aerial_collapse_helper", "soulsweapons:chungus_tonic_effect", "goety:fire_trail",
            "goety:charged", "goety:rampage", "goety:shadow_walk", "goety:fiery_aura", "goety:frosty_aura",
            "minecraft:invisibility", "irons_spellbooks:true_invisibility", "goety:explosive");

    // ---- 春华归返 ----
    private static final ForgeConfigSpec.IntValue SPRING_BLOOM_RETURN_MAX_STACKS;
    private static final ForgeConfigSpec.IntValue SPRING_BLOOM_RETURN_STACK_DURATION_TICKS;
    private static final ForgeConfigSpec.IntValue SPRING_BLOOM_RETURN_GAIN_COOLDOWN_TICKS;
    private static final ForgeConfigSpec.IntValue SPRING_BLOOM_RETURN_TRIGGER_COOLDOWN_TICKS;
    private static final ForgeConfigSpec.DoubleValue SPRING_BLOOM_RETURN_DAMAGE_THRESHOLD;
    private static final ForgeConfigSpec.DoubleValue SPRING_BLOOM_RETURN_DAMAGE_THRESHOLD_RATIO;
    private static final ForgeConfigSpec.DoubleValue SPRING_BLOOM_RETURN_HEAL_RATIO;

    // ---- 云梦水晶 ----
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> DREAM_CRYSTAL_EFFECT_BLACKLIST;
    private static final ForgeConfigSpec.BooleanValue DREAM_CRYSTAL_USE_EFFECT_WHITELIST;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> DREAM_CRYSTAL_EFFECT_WHITELIST;
    private static final ForgeConfigSpec.BooleanValue DREAM_CRYSTAL_EXTRA_TRUE_DAMAGE_ENABLED;

    // ---- 伤害 ----
    private static final ForgeConfigSpec.DoubleValue SILVER_CERCIS_TRUE_DAMAGE_MULTIPLIER;
    private static final ForgeConfigSpec.IntValue SILVER_CERCIS_TRIGGER_COUNT;
    private static final ForgeConfigSpec.IntValue SILVER_CERCIS_COOLDOWN_TICKS;
    private static final ForgeConfigSpec.DoubleValue CHAOS_BOOK_TRUE_DAMAGE_MIN;
    private static final ForgeConfigSpec.DoubleValue CHAOS_BOOK_TRUE_DAMAGE_PERCENT;
    private static final ForgeConfigSpec.IntValue CHAOS_BOOK_DAMAGE_SPLIT_COUNT;
    private static final ForgeConfigSpec.DoubleValue CHAOS_BOOK_MIN_SPLIT_DAMAGE;

    // ---- 治疗 ----
    private static final ForgeConfigSpec.DoubleValue BLEEDING_HEART_HEAL_RATIO;
    private static final ForgeConfigSpec.DoubleValue FLOW_CORE_HEALTH_REGEN_RATE;

    // ---- 防御 ----
    private static final ForgeConfigSpec.DoubleValue FLOW_CORE_DAMAGE_REDUCTION;
    private static final ForgeConfigSpec.DoubleValue DOUBLE_HEART_CHAIN_SHARE_RATIO;
    private static final ForgeConfigSpec.DoubleValue DOUBLE_HEART_CHAIN_MAX_DISTANCE;
    private static final ForgeConfigSpec.IntValue ROCK_CRYSTAL_KNOCKBACK_RESISTANCE;

    // ---- 实用 ----
    private static final ForgeConfigSpec.IntValue FLOW_CORE_TICK_INTERVAL;
    private static final ForgeConfigSpec.DoubleValue HAIRPIN_BENEFICIAL_EFFECT_EXTENSION;
    private static final ForgeConfigSpec.IntValue HAIRPIN_MIN_EXTENSION_TICKS;
    private static final ForgeConfigSpec.IntValue FRAGRANT_INGENUITY_FAVORABILITY_GAIN;
    private static final ForgeConfigSpec.IntValue FRAGRANT_INGENUITY_BUFF_DURATION;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> FRAGRANT_INGENUITY_EFFECT_BLACKLIST;

    // ---- 触发 ----
    private static final ForgeConfigSpec.DoubleValue SOUL_BOOK_DAMAGE_THRESHOLD_PERCENT;
    private static final ForgeConfigSpec.IntValue SOUL_BOOK_DAMAGE_INTERVAL_THRESHOLD;
    private static final ForgeConfigSpec.IntValue WOUND_RIME_BLADE_RECORD_DURATION;

    // ---- 特殊 ----
    private static final ForgeConfigSpec.DoubleValue FLOATING_FOX_LEAF_OWNER_RANGE;
    private static final ForgeConfigSpec.DoubleValue MOLTEN_FOX_LEAF_OWNER_RANGE;
    private static final ForgeConfigSpec.DoubleValue SPRING_RING_MAX_DAMAGE_BONUS;

    static final ForgeConfigSpec SPEC;

    private ConfigBaubles() {
    }

    static void load(ModConfigEvent event) {
        // 春华归返
        Config.springBloomReturnMaxStacks = SPRING_BLOOM_RETURN_MAX_STACKS.get();
        Config.springBloomReturnStackDurationTicks = SPRING_BLOOM_RETURN_STACK_DURATION_TICKS.get();
        Config.springBloomReturnGainCooldownTicks = SPRING_BLOOM_RETURN_GAIN_COOLDOWN_TICKS.get();
        Config.springBloomReturnTriggerCooldownTicks = SPRING_BLOOM_RETURN_TRIGGER_COOLDOWN_TICKS.get();
        Config.springBloomReturnDamageThreshold = SPRING_BLOOM_RETURN_DAMAGE_THRESHOLD.get();
        Config.springBloomReturnDamageThresholdRatio = SPRING_BLOOM_RETURN_DAMAGE_THRESHOLD_RATIO.get();
        Config.springBloomReturnHealRatio = SPRING_BLOOM_RETURN_HEAL_RATIO.get();
        // 云梦水晶
        Config.dreamCrystalEffectBlacklist = new ArrayList<>(DREAM_CRYSTAL_EFFECT_BLACKLIST.get());
        Config.dreamCrystalUseEffectWhitelist = DREAM_CRYSTAL_USE_EFFECT_WHITELIST.get();
        Config.dreamCrystalEffectWhitelist = new ArrayList<>(DREAM_CRYSTAL_EFFECT_WHITELIST.get());
        Config.dreamCrystalExtraTrueDamageEnabled = DREAM_CRYSTAL_EXTRA_TRUE_DAMAGE_ENABLED.get();
        // 伤害
        Config.silverCercisTrueDamageMultiplier = SILVER_CERCIS_TRUE_DAMAGE_MULTIPLIER.get();
        Config.silverCercisTriggerCount = SILVER_CERCIS_TRIGGER_COUNT.get();
        Config.silverCercisCooldownTicks = SILVER_CERCIS_COOLDOWN_TICKS.get();
        Config.chaosBookTrueDamageMin = CHAOS_BOOK_TRUE_DAMAGE_MIN.get();
        Config.chaosBookTrueDamagePercent = CHAOS_BOOK_TRUE_DAMAGE_PERCENT.get();
        Config.chaosBookDamageSplitCount = CHAOS_BOOK_DAMAGE_SPLIT_COUNT.get();
        Config.chaosBookMinSplitDamage = CHAOS_BOOK_MIN_SPLIT_DAMAGE.get();
        // 治疗
        Config.bleedingHeartHealRatio = BLEEDING_HEART_HEAL_RATIO.get();
        Config.flowCoreHealthRegenRate = FLOW_CORE_HEALTH_REGEN_RATE.get();
        // 防御
        Config.flowCoreDamageReduction = FLOW_CORE_DAMAGE_REDUCTION.get();
        Config.doubleHeartChainShareRatio = DOUBLE_HEART_CHAIN_SHARE_RATIO.get();
        Config.doubleHeartChainMaxDistance = DOUBLE_HEART_CHAIN_MAX_DISTANCE.get();
        Config.rockCrystalKnockbackResistance = ROCK_CRYSTAL_KNOCKBACK_RESISTANCE.get();
        // 实用
        Config.flowCoreTickInterval = FLOW_CORE_TICK_INTERVAL.get();
        Config.hairpinBeneficialEffectExtension = HAIRPIN_BENEFICIAL_EFFECT_EXTENSION.get();
        Config.hairpinMinExtensionTicks = HAIRPIN_MIN_EXTENSION_TICKS.get();
        Config.fragrantIngenuityFavorabilityGain = FRAGRANT_INGENUITY_FAVORABILITY_GAIN.get();
        Config.fragrantIngenuityBuffDuration = FRAGRANT_INGENUITY_BUFF_DURATION.get();
        Config.fragrantIngenuityEffectBlacklist = new ArrayList<>(FRAGRANT_INGENUITY_EFFECT_BLACKLIST.get());
        // 触发
        Config.soulBookDamageThresholdPercent = SOUL_BOOK_DAMAGE_THRESHOLD_PERCENT.get();
        Config.soulBookDamageIntervalThreshold = SOUL_BOOK_DAMAGE_INTERVAL_THRESHOLD.get();
        Config.woundRimeBladeRecordTimes = WOUND_RIME_BLADE_RECORD_DURATION.get();
        // 特殊
        Config.floatingFoxLeafOwnerRange = FLOATING_FOX_LEAF_OWNER_RANGE.get();
        Config.moltenFoxLeafOwnerRange = MOLTEN_FOX_LEAF_OWNER_RANGE.get();
        Config.springRingMaxDamageBonus = SPRING_RING_MAX_DAMAGE_BONUS.get();
        DreamCatCrystalBauble.invalidateBeneficialEffectsCache();
    }

    static {
        BUILDER.comment("饰品系统相关配置", "Bauble system configurations").push("baubles");

        BUILDER.comment("春华归返配置", "Spring Bloom Return configurations").push("spring_bloom_return");
        SPRING_BLOOM_RETURN_MAX_STACKS = BUILDER
                .comment("春华归返最多能存几层，层数就是她替主人挨刀的次数 (默认: 3)",
                        "Spring Bloom Return: maximum number of stored stacks.")
                .defineInRange("maxStacks", 3, 1, 8);
        SPRING_BLOOM_RETURN_STACK_DURATION_TICKS = BUILDER
                .comment("每层能存多久，存太久会自己过期掉 (默认: 400 tick，即 20 秒)",
                        "How long one stack lives before expiring (default: 400 ticks = 20s).")
                .defineInRange("stackDurationTicks", 400, 20, 72000);
        SPRING_BLOOM_RETURN_GAIN_COOLDOWN_TICKS = BUILDER
                .comment("两层之间至少要隔这么久，免得一秒钟叠满 (默认: 20 tick，即 1 秒)",
                        "Minimum delay between gaining two stacks (default: 20 ticks = 1 second).")
                .defineInRange("gainCooldownTicks", 20, 0, 1200);
        SPRING_BLOOM_RETURN_TRIGGER_COOLDOWN_TICKS = BUILDER
                .comment("替主人挡一次之后要等这么久才能再挡 (默认: 200 tick，即 10 秒)",
                        "Cooldown after one trigger (default: 200 ticks = 10 seconds).")
                .defineInRange("triggerCooldownTicks", 200, 0, 72000);
        SPRING_BLOOM_RETURN_DAMAGE_THRESHOLD = BUILDER
                .comment("单次伤害到这个固定值就算「够重」，值得替主人挡一下 (默认: 4.0)",
                        "A single hit at or above this flat amount counts as heavy enough to trigger it.")
                .defineInRange("damageThreshold", 4.0, 0.0, 100.0);
        SPRING_BLOOM_RETURN_DAMAGE_THRESHOLD_RATIO = BUILDER
                .comment("按受击者最大生命计算的重击阈值；与固定伤害阈值满足其一即可",
                        "Heavy-hit threshold as a fraction of max health; either threshold can trigger.")
                .defineInRange("damageThresholdRatio", 0.1, 0.0, 1.0);
        SPRING_BLOOM_RETURN_HEAL_RATIO = BUILDER
                .comment("触发时直接把这一击削掉「保护对象最大生命 × 本值」那么多血，等于替他回了一口 (默认: 0.05，即 5%)",
                        "On trigger the hit is reduced by this fraction of the protected target's max health.")
                .defineInRange("healRatio", 0.05, 0.0, 1.0);
        BUILDER.pop();

        BUILDER.comment("云梦水晶配置", "Dream Crystal configurations").push("dream_crystal");
        DREAM_CRYSTAL_EFFECT_BLACKLIST = BUILDER
                .comment("云梦水晶随机效果黑名单；支持完整 id 和 regex: 正则",
                        "Dream Crystal effect blacklist; supports full ids and regex: patterns.")
                .defineListAllowEmpty(List.of("effectBlacklist"), () -> DEFAULT_BLACKLIST, obj -> obj instanceof String);
        DREAM_CRYSTAL_USE_EFFECT_WHITELIST = BUILDER
                .comment("true: 只抽白名单效果；false: 按黑名单排除",
                        "true: roll only whitelisted effects; false: exclude blacklisted effects.")
                .define("useEffectWhitelist", true);
        DREAM_CRYSTAL_EFFECT_WHITELIST = BUILDER
                .comment("云梦水晶效果白名单；启用白名单模式时生效，支持完整 id 和 regex: 正则",
                        "Dream Crystal effect whitelist; active in whitelist mode, supports ids and regex: patterns.")
                .defineListAllowEmpty(List.of("effectWhitelist"), () -> DEFAULT_WHITELIST, obj -> obj instanceof String);
        DREAM_CRYSTAL_EXTRA_TRUE_DAMAGE_ENABLED = BUILDER
                .comment("云梦水晶是否在女仆命中时追加真伤与 5 格弹幕溅射",
                        "Whether Dream Crystal adds true damage and a 5-block splash on a maid's hit.")
                .define("extraTrueDamageEnabled", true);
        BUILDER.pop();

        BUILDER.comment("伤害类饰品配置", "Damage-related bauble configurations").push("damage");
        SILVER_CERCIS_TRUE_DAMAGE_MULTIPLIER = BUILDER
                .comment("紫荆银冠：累计受击达到阈值后打出的真伤倍率 (默认: 0.8)",
                        "Silver Cercis: true damage multiplier (default: 0.8).")
                .defineInRange("silverCercisTrueDamageMultiplier", 0.8, 0.1, 2.0);
        SILVER_CERCIS_TRIGGER_COUNT = BUILDER
                .comment("紫荆银冠：累计几次受击后触发一次 (默认: 3)",
                        "Silver Cercis: hits required before triggering (default: 3).")
                .defineInRange("silverCercisTriggerCount", 3, 1, 10);
        SILVER_CERCIS_COOLDOWN_TICKS = BUILDER
                .comment("紫荆银冠：触发后的内部冷却（秒）(默认: 5)",
                        "Silver Cercis: internal cooldown in seconds (default: 5).")
                .defineInRange("silverCercisCooldownTicks", 5, 1, 100);
        CHAOS_BOOK_TRUE_DAMAGE_MIN = BUILDER
                .comment("混沌之书：真伤保底值 (默认: 5.0)",
                        "Chaos Book: minimum true damage (default: 5.0).")
                .defineInRange("chaosBookTrueDamageMin", 5.0, 1.0, 50.0);
        CHAOS_BOOK_TRUE_DAMAGE_PERCENT = BUILDER
                .comment("混沌之书：真伤按目标最大生命的比例 (默认: 0.01)",
                        "Chaos Book: true damage as a fraction of target max health (default: 0.01).")
                .defineInRange("chaosBookTrueDamagePercent", 0.01, 0.001, 0.1);
        CHAOS_BOOK_DAMAGE_SPLIT_COUNT = BUILDER
                .comment("混沌之书：把一次攻击拆成几段 (默认: 5)",
                        "Chaos Book: number of damage segments per hit (default: 5).")
                .defineInRange("chaosBookDamageSplitCount", 5, 1, 20);
        CHAOS_BOOK_MIN_SPLIT_DAMAGE = BUILDER
                .comment("混沌之书：每段拆分伤害的下限 (默认: 3.0)",
                        "Chaos Book: minimum damage per segment (default: 3.0).")
                .defineInRange("chaosBookMinSplitDamage", 3.0, 0.1, 100.0);
        BUILDER.pop();

        BUILDER.comment("治疗类饰品配置", "Healing-related bauble configurations").push("healing");
        BLEEDING_HEART_HEAL_RATIO = BUILDER
                .comment("流血之心：按造成伤害的比例给女仆与主人回血 (默认: 0.1，即 10%)",
                        "Bleeding Heart: heal fraction of damage dealt to maid and owner (default: 0.1).")
                .defineInRange("bleedingHeartHealRatio", 0.1, 0.01, 1.0);
        FLOW_CORE_HEALTH_REGEN_RATE = BUILDER
                .comment("流转核心：每次回血按最大生命的比例 (默认: 0.025)",
                        "Flow Core: regeneration as a fraction of max health per tick pass (default: 0.025).")
                .defineInRange("flowCoreHealthRegenRate", 0.025, 0.001, 0.3);
        BUILDER.pop();

        BUILDER.comment("防御类饰品配置", "Defense-related bauble configurations").push("defense");
        FLOW_CORE_DAMAGE_REDUCTION = BUILDER
                .comment("流转核心：每级好感度降低的伤害比例 (默认: 0.15)",
                        "Flow Core: damage reduction per favorability level (default: 0.15).")
                .defineInRange("flowCoreDamageReduction", 0.15, 0.05, 0.5);
        DOUBLE_HEART_CHAIN_SHARE_RATIO = BUILDER
                .comment("心意双链：与主人分担的伤害比例 (默认: 0.5)",
                        "Double Heart Chain: fraction of damage shared with the owner (default: 0.5).")
                .defineInRange("doubleHeartChainShareRatio", 0.5, 0.1, 0.9);
        DOUBLE_HEART_CHAIN_MAX_DISTANCE = BUILDER
                .comment("心意双链：超过这个距离就不再分担 (默认: 32 格)",
                        "Double Heart Chain: max distance to share damage (default: 32 blocks).")
                .defineInRange("doubleHeartChainMaxDistance", 32.0, 4.0, 128.0);
        ROCK_CRYSTAL_KNOCKBACK_RESISTANCE = BUILDER
                .comment("岩石水晶：提供的击退抗性点数 (默认: 8)",
                        "Rock Crystal: knockback resistance points (default: 8).")
                .defineInRange("rockCrystalKnockbackResistance", 8, 1, 20);
        BUILDER.pop();

        BUILDER.comment("实用类饰品配置", "Utility bauble configurations").push("utility");
        FLOW_CORE_TICK_INTERVAL = BUILDER
                .comment("流转核心：每多少 tick 回一次血 (默认: 10)",
                        "Flow Core: tick interval between heals (default: 10).")
                .defineInRange("flowCoreTickInterval", 10, 1, 100);
        HAIRPIN_BENEFICIAL_EFFECT_EXTENSION = BUILDER
                .comment("发簪：正面效果时长的延长倍率 (默认: 1.15)",
                        "Hairpin: beneficial effect duration multiplier (default: 1.15).")
                .defineInRange("hairpinBeneficialEffectExtension", 1.15, 1.0, 10.0);
        HAIRPIN_MIN_EXTENSION_TICKS = BUILDER
                .comment("发簪：低于这个剩余时长的正面效果不再延长 (默认: 300 tick)",
                        "Hairpin: beneficial effects shorter than this are not extended (default: 300 ticks).")
                .defineInRange("hairpinMinExtensionTicks", 300, 0, 10000);
        FRAGRANT_INGENUITY_FAVORABILITY_GAIN = BUILDER
                .comment("馥郁巧思：女仆进食时增加的好感度 (默认: 2)",
                        "Fragrant Ingenuity: favorability gained when the maid eats (default: 2).")
                .defineInRange("fragrantIngenuityFavorabilityGain", 2, 1, 10);
        FRAGRANT_INGENUITY_BUFF_DURATION = BUILDER
                .comment("馥郁巧思：给主人施加的增益时长（tick）(默认: 2400)",
                        "Fragrant Ingenuity: buff duration for the owner in ticks (default: 2400).")
                .defineInRange("fragrantIngenuityBuffDuration", 2400, 200, 120000);
        FRAGRANT_INGENUITY_EFFECT_BLACKLIST = BUILDER
                .comment("馥郁巧思：不抽的效果黑名单；支持完整 id 和 regex: 正则",
                        "Fragrant Ingenuity: effect blacklist; supports ids and regex: patterns.")
                .defineListAllowEmpty(List.of("effectBlacklist"), () -> DEFAULT_BLACKLIST, obj -> obj instanceof String);
        BUILDER.pop();

        BUILDER.comment("触发类饰品配置", "Trigger-related bauble configurations").push("triggers");
        SOUL_BOOK_DAMAGE_THRESHOLD_PERCENT = BUILDER
                .comment("魂之书：单次伤害达到最大生命的这个比例才计入 (默认: 0.2)",
                        "Soul Book: damage fraction of max health required to count (default: 0.2).")
                .defineInRange("soulBookDamageThresholdPercent", 0.2, 0.05, 1.0);
        SOUL_BOOK_DAMAGE_INTERVAL_THRESHOLD = BUILDER
                .comment("魂之书：两次计数伤害之间至少间隔的 tick 数 (默认: 10)",
                        "Soul Book: minimum ticks between two counted hits (default: 10).")
                .defineInRange("soulBookDamageIntervalThreshold", 10, 1, 100);
        WOUND_RIME_BLADE_RECORD_DURATION = BUILDER
                .comment("破愈咒锋：伤害记录的保留时长（秒）(默认: 15)",
                        "Wound Rime Blade: how long a damage record lives in seconds (default: 15).")
                .defineInRange("woundRimeBladeRecordDuration", 15, 5, 100);
        BUILDER.pop();

        BUILDER.comment("特殊饰品配置", "Special bauble configurations").push("special");
        FLOATING_FOX_LEAF_OWNER_RANGE = BUILDER
                .comment("浮狐叶：主人离开这个距离后效果中断 (默认: 16 格)",
                        "Floating Fox Leaf: owner range before the effect stops (default: 16 blocks).")
                .defineInRange("floatingFoxLeafOwnerRange", 16.0, 4.0, 64.0);
        MOLTEN_FOX_LEAF_OWNER_RANGE = BUILDER
                .comment("熔狐叶：主人离开这个距离后效果中断 (默认: 16 格)",
                        "Molten Fox Leaf: owner range before the effect stops (default: 16 blocks).")
                .defineInRange("moltenFoxLeafOwnerRange", 16.0, 4.0, 64.0);
        SPRING_RING_MAX_DAMAGE_BONUS = BUILDER
                .comment("春之指环：残血时最多额外造成的伤害比例 (默认: 0.5)",
                        "Spring Ring: maximum extra damage fraction at low health (default: 0.5).")
                .defineInRange("springRingMaxDamageBonus", 0.5, 0.1, 1.0);
        BUILDER.pop();

        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}

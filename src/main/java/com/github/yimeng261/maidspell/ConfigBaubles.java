package com.github.yimeng261.maidspell;

import com.github.yimeng261.maidspell.item.bauble.dreamCatCrystal.DreamCatCrystalBauble;
import java.util.ArrayList;
import java.util.List;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.event.config.ModConfigEvent;

/**
 * 饰品相关的配置项（批次 A 精简版）。
 * <p>
 * 移植自 1.9.2.1 反编译源 ConfigBaubles.java，只保留批次 A 实际用到的两组：
 * 云梦水晶的效果白/黑名单，以及春华归返的层数/阈值/减伤参数。
 * 其余 40 余项（流血之心、流转核心、紫荆银冠、混沌之书、浮狐叶……）属批次 B，
 * 搬对应饰品时再从反编译源补齐。
 * <p>
 * 与原文的差异：
 * <ul>
 *   <li>删掉 {@code migrateDreamCrystalEffectBlacklist}：那是给已装旧版模组的存档迁移默认值用的，
 *       本工程是全新移植，没有旧配置文件需要迁移。</li>
 *   <li>删掉 {@code springBloomReturnCooldownRefundRatio}：该参数只服务于「春华归返返还法术冷却」
 *       这个纯魔法效果，批次 A 已移除该效果。</li>
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

    private static final ForgeConfigSpec.IntValue SPRING_BLOOM_RETURN_MAX_STACKS;
    private static final ForgeConfigSpec.IntValue SPRING_BLOOM_RETURN_STACK_DURATION_TICKS;
    private static final ForgeConfigSpec.IntValue SPRING_BLOOM_RETURN_GAIN_COOLDOWN_TICKS;
    private static final ForgeConfigSpec.IntValue SPRING_BLOOM_RETURN_TRIGGER_COOLDOWN_TICKS;
    private static final ForgeConfigSpec.DoubleValue SPRING_BLOOM_RETURN_DAMAGE_THRESHOLD;
    private static final ForgeConfigSpec.DoubleValue SPRING_BLOOM_RETURN_DAMAGE_THRESHOLD_RATIO;
    private static final ForgeConfigSpec.DoubleValue SPRING_BLOOM_RETURN_HEAL_RATIO;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> DREAM_CRYSTAL_EFFECT_BLACKLIST;
    private static final ForgeConfigSpec.BooleanValue DREAM_CRYSTAL_USE_EFFECT_WHITELIST;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> DREAM_CRYSTAL_EFFECT_WHITELIST;
    private static final ForgeConfigSpec.BooleanValue DREAM_CRYSTAL_EXTRA_TRUE_DAMAGE_ENABLED;

    static final ForgeConfigSpec SPEC;

    private ConfigBaubles() {
    }

    static void load(ModConfigEvent event) {
        Config.springBloomReturnMaxStacks = SPRING_BLOOM_RETURN_MAX_STACKS.get();
        Config.springBloomReturnStackDurationTicks = SPRING_BLOOM_RETURN_STACK_DURATION_TICKS.get();
        Config.springBloomReturnGainCooldownTicks = SPRING_BLOOM_RETURN_GAIN_COOLDOWN_TICKS.get();
        Config.springBloomReturnTriggerCooldownTicks = SPRING_BLOOM_RETURN_TRIGGER_COOLDOWN_TICKS.get();
        Config.springBloomReturnDamageThreshold = SPRING_BLOOM_RETURN_DAMAGE_THRESHOLD.get();
        Config.springBloomReturnDamageThresholdRatio = SPRING_BLOOM_RETURN_DAMAGE_THRESHOLD_RATIO.get();
        Config.springBloomReturnHealRatio = SPRING_BLOOM_RETURN_HEAL_RATIO.get();
        Config.dreamCrystalEffectBlacklist = new ArrayList<>(DREAM_CRYSTAL_EFFECT_BLACKLIST.get());
        Config.dreamCrystalUseEffectWhitelist = DREAM_CRYSTAL_USE_EFFECT_WHITELIST.get();
        Config.dreamCrystalEffectWhitelist = new ArrayList<>(DREAM_CRYSTAL_EFFECT_WHITELIST.get());
        Config.dreamCrystalExtraTrueDamageEnabled = DREAM_CRYSTAL_EXTRA_TRUE_DAMAGE_ENABLED.get();
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
        // TODO(批次B)：真伤溅射依赖 LivingEntityInvoker / SynchedEntityDataMixin 两个 accessor mixin，
        //  批次 A 未引入 mixin 基础设施，本开关暂时不生效。搬 mixin 后恢复 TrueDamageUtil 的调用。
        DREAM_CRYSTAL_EXTRA_TRUE_DAMAGE_ENABLED = BUILDER
                .comment("云梦水晶是否在女仆命中时追加等量真伤（当前版本尚未接入，见 TODO(批次B)）",
                        "Whether Dream Crystal adds true damage equal to a maid's hit (not wired up yet).")
                .define("extraTrueDamageEnabled", true);
        BUILDER.pop();

        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}

package com.github.yimeng261.maidspell.item.bauble.dreamCatCrystal;

import com.github.tartaricacid.touhoulittlemaid.api.bauble.IMaidBauble;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.inventory.handler.BaubleItemHandler;
import com.github.yimeng261.maidspell.Config;
import com.github.yimeng261.maidspell.Global;
import com.github.yimeng261.maidspell.api.IMaidBaubleLifecycle;
import com.github.yimeng261.maidspell.api.IAuthoritativeHealth;
import com.github.yimeng261.maidspell.dimension.TheRetreatDimension;
import com.github.yimeng261.maidspell.item.MaidSpellItems;
import com.github.yimeng261.maidspell.spell.manager.BaubleStateManager;
import com.github.yimeng261.maidspell.utils.PortableTimerMath;
import com.github.yimeng261.maidspell.utils.TrueDamageUtil;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.wrapper.CombinedInvWrapper;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

/**
 * 云梦水晶（饰品逻辑）—— 批次 A 去魔法版。
 *
 * <p><b>保留（非魔法能力）</b>：最大生命 +50%、攻速 +100%、随维度给正面效果、
 * 20 格内其它女仆攻击力 +50%、定期修复女仆背包耐久、概率复活（含 NBT 复活历史与无敌时间）、
 * 定期随机正面效果、有害效果过滤表。</p>
 *
 * <p><b>删掉的魔法能力</b>（对应计划文档 §2.1 云梦水晶条目）：
 * <ol>
 *   <li>铁魔法属性翻倍：原文 {@code ISS_ATTRIBUTES}（按 {@code attribute.irons_spellbooks.} 前缀扫描）
 *       及其在 tick 中的应用/移除。</li>
 *   <li>法术冷却置 0：原文 static 块里的 {@code Global.baubleCooldownHandlers.put(...)}。</li>
 *   <li>法术冷却除以 3：原文 {@code registerCommonCallbacks()}。</li>
 *   <li>进度 {@code dream_crystal/all_spells_mastered}：原文 {@code ADVANCEMENT_ALL_SPELLS} +
 *       {@code grantAdvancement(EntityMaid)}。</li>
 * </ol>
 *
 * <p><b>批次 B 恢复的三项能力</b>（Mixin 基础设施到位后重新生效）：
 * <ul>
 *   <li>受伤抗性：{@code baubleSetHealthFinalHandlers} 里「伤害 ×0.7、上限 40」，
 *       消费链为 {@code MaidDamageProcessor.applyBaubleHandlers} ← {@code LivingEntityMixin}。</li>
 *   <li>有害效果免疫：{@code baubleEffectBlockFilters}，
 *       消费点为 {@code LivingEntityMixin} 的 {@code addEffect} 头部拦截。</li>
 *   <li>真实伤害 + 5 格弹幕溅射：{@code Global.registerBaubleHurtHeadHandler(...)}，
 *       消费点为 {@code LivingEntityMixin} 的 {@code hurt} 头部（{@code Global.dispatchHurtHeadHandlers}）。</li>
 * </ul>
 *
 * <p><b>仍未恢复（需额外依赖）</b>：
 * <ul>
 *   <li>TODO(批次B) curios 额外槽：{@code compat/curios/DreamCrystalCurios} 依赖 curios 模组 API，
 *       本工程无该依赖，故 {@code applyCuriosSlot} 整段未搬。将来若要恢复需先加 curios 依赖。</li>
 * </ul>
 */
public class DreamCatCrystalBauble implements IMaidBauble, IMaidBaubleLifecycle {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Map<UUID, BoostedMaidState> BOOSTED_MAIDS = new HashMap<>();
    private static final PriorityQueue<ScheduledExpiry> BOOSTED_MAID_EXPIRIES =
            new PriorityQueue<>(Comparator.comparingLong(ScheduledExpiry::expiry));

    private static final String NBT_REVIVE_CLOCK_VERSION = "dream_crystal_revive_clock_version";
    private static final String NBT_REVIVE_TIMESTAMPS = "dream_crystal_revive_timestamps";
    public static final String NBT_INVULNERABLE_TIME = "dream_crystal_invulnerable_time";
    private static final int REVIVE_CLOCK_VERSION = 1;
    private static final int MAX_REVIVE_TIMESTAMPS = 10;
    private static final long REVIVE_WINDOW_TICKS = 2400L;
    private static final long LEGACY_TIMESTAMP_GRACE_TICKS = 20L;

    private static List<MobEffect> CACHED_BENEFICIAL_EFFECTS = null;
    private static final Map<UUID, ItemStack> HAS_DREAM_CRYSTAL = new HashMap<>();

    private static final UUID DC_HP_UUID = UUID.fromString("dc000001-0000-0000-0000-000000000001");
    private static final UUID DC_ATTACK_SPEED_UUID = UUID.fromString("dc000001-0000-0000-0000-000000000002");
    private static final UUID DC_NEARBY_DAMAGE_UUID = UUID.fromString("dc000001-0000-0000-0000-000000000003");

    // ---------------------------------------------------------------- 正面效果候选

    private static List<MobEffect> getBeneficialEffects() {
        if (CACHED_BENEFICIAL_EFFECTS != null) {
            return CACHED_BENEFICIAL_EFFECTS;
        }
        List<MobEffect> candidates = new ArrayList<>();
        boolean useWhitelist = Config.dreamCrystalUseEffectWhitelist;
        EffectMatcher whitelistMatcher = useWhitelist ? EffectMatcher.from(Config.dreamCrystalEffectWhitelist) : EffectMatcher.empty();
        EffectMatcher blacklistMatcher = EffectMatcher.from(Config.dreamCrystalEffectBlacklist);
        // 1.20 的 BuiltInRegistries.MOB_EFFECT 在 1.18.2 对应 ForgeRegistries.MOB_EFFECTS。
        ForgeRegistries.MOB_EFFECTS.getEntries().forEach(entry -> {
            MobEffect effect = entry.getValue();
            if (effect.getCategory() != MobEffectCategory.BENEFICIAL) {
                return;
            }
            if (entry.getKey().location() == null) {
                return;
            }
            String effectId = entry.getKey().location().toString();
            boolean allowedByWhitelist = !useWhitelist || whitelistMatcher.matches(effectId);
            boolean blockedByBlacklist = blacklistMatcher.matches(effectId);
            if (allowedByWhitelist && !blockedByBlacklist) {
                candidates.add(effect);
            }
        });
        CACHED_BENEFICIAL_EFFECTS = candidates;
        return candidates;
    }

    public static void invalidateBeneficialEffectsCache() {
        CACHED_BENEFICIAL_EFFECTS = null;
    }

    // ---------------------------------------------------------------- IMaidBauble

    @Override
    public void onTick(EntityMaid maid, ItemStack baubleItem) {
        this.tickWearer(maid, baubleItem);
    }

    public void tickWearer(EntityMaid maid, ItemStack baubleItem) {
        if (maid.getLevel().isClientSide()) {
            return;
        }
        int tick = maid.tickCount;
        handleInvulnerable(maid, baubleItem);
        if (tick % 20 != 3) {
            return;
        }
        normalizeReviveHistory(maid, baubleItem);
        applyAttributeModifier(maid, Attributes.MAX_HEALTH, DC_HP_UUID, "dream_crystal_hp", 0.5);
        applyAttributeModifier(maid, Attributes.ATTACK_SPEED, DC_ATTACK_SPEED_UUID, "dream_crystal_speed", 1.0);
        applyDimensionBuffs(maid, baubleItem);
        applyRangeMaidBoost(maid);
        repairInventory(maid);
        if (tick % 600 == 3) {
            applyRandomBeneficialEffects(maid);
        }
    }

    // ---------------------------------------------------------------- IMaidBaubleLifecycle

    @Override
    public void onPutOn(EntityMaid maid, ItemStack baubleItem) {
        HAS_DREAM_CRYSTAL.put(maid.getUUID(), baubleItem);
    }

    @Override
    public void onTakeOff(EntityMaid maid, ItemStack baubleItem) {
        HAS_DREAM_CRYSTAL.remove(maid.getUUID());
        removeWearerEffects(maid);
    }

    @Override
    public boolean onDeath(EntityMaid maid, ItemStack baubleItem, DamageSource source) {
        return tryRevive(maid, baubleItem);
    }

    // ---------------------------------------------------------------- 复活

    public boolean tryRevive(LivingEntity maid, ItemStack baubleItem) {
        MinecraftServer server = maid.getServer();
        if (server == null) {
            return false;
        }
        CompoundTag existingTag = baubleItem.getTag();
        int storedClockVersion = existingTag == null ? 0 : existingTag.getInt(NBT_REVIVE_CLOCK_VERSION);
        if (storedClockVersion > REVIVE_CLOCK_VERSION) {
            return false;
        }
        CompoundTag tag = baubleItem.getOrCreateTag();
        long currentTime = globalGameTime(server);
        long legacyTime = maid.getLevel().getGameTime();
        ReviveHistory history = loadReviveHistory(tag, storedClockVersion, legacyTime, currentTime);
        List<Long> timestamps = history.timestamps();
        int reviveCount = timestamps.size();
        float reviveChance = Math.max(0.0f, 1.0f - (float) reviveCount * 0.1f);
        boolean revived = reviveChance > 0.0f && maid.getRandom().nextFloat() < reviveChance;
        if (revived) {
            timestamps.add(currentTime);
        }
        if (history.changed() || revived) {
            saveReviveHistory(tag, timestamps);
        }
        if (!revived) {
            return false;
        }
        float healAmount = maid.getMaxHealth();
        maid.setHealth(healAmount);
        tag.putInt(NBT_INVULNERABLE_TIME, 300);
        maid.playSound(SoundEvents.TOTEM_USE, 1.0f, 1.0f);
        LOGGER.info("女仆 {} 触发梦云水晶概率复活（N={}，概率={}%），恢复至 {} 生命值",
                maid.getCustomName() != null ? maid.getCustomName().getString() : "未命名",
                reviveCount, (int) (reviveChance * 100.0f), healAmount);
        return true;
    }

    private static void handleInvulnerable(LivingEntity maid, ItemStack baubleItem) {
        CompoundTag tag = baubleItem.getTag();
        if (tag == null || !tag.contains(NBT_INVULNERABLE_TIME)) {
            return;
        }
        int invulTime = tag.getInt(NBT_INVULNERABLE_TIME);
        if (invulTime > 0) {
            tag.putInt(NBT_INVULNERABLE_TIME, invulTime - 1);
        } else {
            tag.remove(NBT_INVULNERABLE_TIME);
        }
    }

    // ---------------------------------------------------------------- 属性

    private static void applyAttributeModifier(LivingEntity maid, Attribute attribute, UUID uuid,
                                              String name, double value) {
        AttributeInstance instance = maid.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        if (instance.getModifier(uuid) == null) {
            instance.addTransientModifier(new AttributeModifier(uuid, name, value,
                    AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
    }

    private static void removeAttributeModifier(LivingEntity maid, Attribute attribute, UUID uuid) {
        AttributeInstance instance = maid.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        instance.removeModifier(uuid);
    }

    private static void removeWearerEffects(LivingEntity maid) {
        if (maid.getLevel().isClientSide()) {
            return;
        }
        removeAttributeModifier(maid, Attributes.MAX_HEALTH, DC_HP_UUID);
        removeAttributeModifier(maid, Attributes.ATTACK_SPEED, DC_ATTACK_SPEED_UUID);
        maid.setHealth(Math.min(maid.getHealth(), maid.getMaxHealth()));
    }

    // ---------------------------------------------------------------- 维度/范围/维修/随机效果

    private void applyDimensionBuffs(LivingEntity maid, ItemStack baubleStack) {
        if (TheRetreatDimension.isInRetreat(maid)) {
            // TODO(批次B)：the_retreat 维度还没搬，isInRetreat 恒 false，此分支当前不会命中。
            if (baubleStack != null) {
                CompoundTag tag = baubleStack.getOrCreateTag();
                tag.putInt(NBT_INVULNERABLE_TIME, Math.max(40, tag.getInt(NBT_INVULNERABLE_TIME)));
            }
            return;
        }
        Level level = maid.getLevel();
        if (level.dimension() == Level.OVERWORLD) {
            maid.addEffect(new MobEffectInstance(MobEffects.SATURATION, 300, 1, false, false));
            maid.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 300, 1, false, false));
            maid.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 300, 1, false, false));
        } else if (level.dimension() == Level.NETHER) {
            maid.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 300, 2, false, false));
            maid.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 300, 2, false, false));
            maid.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 300, 2, false, false));
        } else if (level.dimension() == Level.END) {
            maid.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 1360, 3, false, false));
            maid.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 300, 3, false, false));
            maid.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 300, 3, false, false));
        }
    }

    private void applyRangeMaidBoost(LivingEntity sourceMaid) {
        MinecraftServer server = sourceMaid.getServer();
        if (server == null) {
            return;
        }
        long expiry = PortableTimerMath.saturatingAdd(globalGameTime(server), 40L);
        List<EntityMaid> nearbyMaids = sourceMaid.getLevel().getEntitiesOfClass(EntityMaid.class,
                sourceMaid.getBoundingBox().inflate(20.0), m -> m != sourceMaid && m.isAlive());
        for (EntityMaid nearMaid : nearbyMaids) {
            applyOrRefreshMaidBoost(nearMaid, expiry);
        }
    }

    private static void repairInventory(EntityMaid maid) {
        CombinedInvWrapper handler = maid.getAvailableInv(false);
        for (int i = 0; i < handler.getSlots(); ++i) {
            ItemStack stack = handler.getStackInSlot(i);
            if (stack.isEmpty() || !stack.isDamaged()) {
                continue;
            }
            stack.setDamageValue(stack.getDamageValue() - 1);
        }
    }

    public static void applyRandomBeneficialEffects(LivingEntity maid) {
        applyRandomBeneficialEffects(maid, 600, 1200);
    }

    public static void applyRandomBeneficialEffects(LivingEntity maid, int minDuration, int maxDuration) {
        List<MobEffect> candidates = getBeneficialEffects();
        if (candidates.isEmpty()) {
            return;
        }
        Random rng = maid.getRandom();
        Set<Integer> chosen = new HashSet<>();
        for (int attempts = 0; chosen.size() < 2 && attempts < 50; ++attempts) {
            chosen.add(rng.nextInt(candidates.size()));
        }
        for (Integer index : chosen) {
            MobEffect effect = candidates.get(index);
            int amplifier = 1 + rng.nextInt(9);
            int duration = minDuration + rng.nextInt(maxDuration - minDuration + 1);
            maid.addEffect(new MobEffectInstance(effect, duration, amplifier, false, true));
        }
    }

    // ---------------------------------------------------------------- 复活历史

    public static ItemStack findDreamCrystalStack(EntityMaid maid) {
        ItemStack result = HAS_DREAM_CRYSTAL.get(maid.getUUID());
        if (result == null && BaubleStateManager.hasBauble(maid, MaidSpellItems.DREAM_CAT_CRYSTAL)) {
            BaubleItemHandler handler = maid.getMaidBauble();
            for (int i = 0; i < handler.getSlots(); ++i) {
                ItemStack stack = handler.getStackInSlot(i);
                if (stack.getItem() != MaidSpellItems.DREAM_CAT_CRYSTAL.get()) {
                    continue;
                }
                result = stack;
                HAS_DREAM_CRYSTAL.put(maid.getUUID(), stack);
                break;
            }
        }
        return result;
    }

    private static ReviveHistory loadReviveHistory(CompoundTag tag, int storedClockVersion,
                                                   long legacyNow, long serverNow) {
        long[] storedTimestamps = tag.contains(NBT_REVIVE_TIMESTAMPS)
                ? tag.getLongArray(NBT_REVIVE_TIMESTAMPS) : new long[]{};
        List<Long> normalized = new ArrayList<>(Math.min(storedTimestamps.length, MAX_REVIVE_TIMESTAMPS));
        long oldestAllowed = PortableTimerMath.saturatingSubtract(serverNow, REVIVE_WINDOW_TICKS);
        for (long storedTimestamp : storedTimestamps) {
            long timestamp = storedClockVersion < REVIVE_CLOCK_VERSION
                    ? PortableTimerMath.migrateTimestamp(storedTimestamp, legacyNow, serverNow,
                            REVIVE_WINDOW_TICKS, LEGACY_TIMESTAMP_GRACE_TICKS)
                    : storedTimestamp;
            timestamp = Math.min(timestamp, serverNow);
            if (timestamp < oldestAllowed) {
                continue;
            }
            normalized.add(timestamp);
        }
        normalized.sort(Long::compareTo);
        if (normalized.size() > MAX_REVIVE_TIMESTAMPS) {
            normalized = new ArrayList<>(normalized.subList(
                    normalized.size() - MAX_REVIVE_TIMESTAMPS, normalized.size()));
        }
        long[] normalizedTimestamps = toLongArray(normalized);
        boolean changed = storedClockVersion != REVIVE_CLOCK_VERSION
                || !Arrays.equals(storedTimestamps, normalizedTimestamps)
                || (normalized.isEmpty() && tag.contains(NBT_REVIVE_TIMESTAMPS));
        return new ReviveHistory(normalized, changed);
    }

    private static void normalizeReviveHistory(LivingEntity maid, ItemStack baubleItem) {
        CompoundTag tag = baubleItem.getTag();
        if (tag == null || (!tag.contains(NBT_REVIVE_CLOCK_VERSION) && !tag.contains(NBT_REVIVE_TIMESTAMPS))) {
            return;
        }
        int storedClockVersion = tag.getInt(NBT_REVIVE_CLOCK_VERSION);
        if (storedClockVersion > REVIVE_CLOCK_VERSION) {
            return;
        }
        MinecraftServer server = maid.getServer();
        if (server == null) {
            return;
        }
        ReviveHistory history = loadReviveHistory(tag, storedClockVersion,
                maid.getLevel().getGameTime(), globalGameTime(server));
        if (history.changed()) {
            saveReviveHistory(tag, history.timestamps());
        }
    }

    private static void saveReviveHistory(CompoundTag tag, List<Long> timestamps) {
        if (tag.getInt(NBT_REVIVE_CLOCK_VERSION) != REVIVE_CLOCK_VERSION) {
            tag.putInt(NBT_REVIVE_CLOCK_VERSION, REVIVE_CLOCK_VERSION);
        }
        if (timestamps.isEmpty()) {
            if (tag.contains(NBT_REVIVE_TIMESTAMPS)) {
                tag.remove(NBT_REVIVE_TIMESTAMPS);
            }
            return;
        }
        long[] values = toLongArray(timestamps);
        if (!Arrays.equals(tag.getLongArray(NBT_REVIVE_TIMESTAMPS), values)) {
            tag.putLongArray(NBT_REVIVE_TIMESTAMPS, values);
        }
    }

    private static long[] toLongArray(List<Long> timestamps) {
        long[] values = new long[timestamps.size()];
        for (int i = 0; i < timestamps.size(); ++i) {
            values[i] = timestamps.get(i);
        }
        return values;
    }

    // ---------------------------------------------------------------- 范围强化调度

    public static void processScheduledEffects(MinecraftServer server) {
        if (server == null) {
            return;
        }
        processBoostedMaids(globalGameTime(server));
    }

    public static void clearScheduledEffects() {
        BOOSTED_MAIDS.clear();
        BOOSTED_MAID_EXPIRIES.clear();
        HAS_DREAM_CRYSTAL.clear();
    }

    private static boolean isMaidBoosted(EntityMaid maid) {
        BoostedMaidState state = BOOSTED_MAIDS.get(maid.getUUID());
        if (state == null) {
            return false;
        }
        MinecraftServer server = maid.getServer();
        if (server == null) {
            return false;
        }
        state.maid = maid;
        return globalGameTime(server) < state.expiry;
    }

    private static void applyOrRefreshMaidBoost(EntityMaid maid, long expiry) {
        AttributeInstance attackDamage = maid.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackDamage == null) {
            return;
        }
        if (attackDamage.getModifier(DC_NEARBY_DAMAGE_UUID) == null) {
            attackDamage.addTransientModifier(new AttributeModifier(DC_NEARBY_DAMAGE_UUID,
                    "dream_crystal_nearby_boost", 0.5, AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
        UUID maidUUID = maid.getUUID();
        BoostedMaidState existing = BOOSTED_MAIDS.get(maidUUID);
        if (existing != null && existing.expiry >= expiry) {
            existing.maid = maid;
            return;
        }
        BOOSTED_MAIDS.put(maidUUID, new BoostedMaidState(maid, expiry));
        BOOSTED_MAID_EXPIRIES.add(new ScheduledExpiry(maidUUID, expiry));
    }

    private static void processBoostedMaids(long currentTime) {
        while (!BOOSTED_MAID_EXPIRIES.isEmpty()) {
            ScheduledExpiry scheduled = BOOSTED_MAID_EXPIRIES.peek();
            BoostedMaidState state = BOOSTED_MAIDS.get(scheduled.entityId());
            if (state == null || state.expiry != scheduled.expiry()) {
                BOOSTED_MAID_EXPIRIES.poll();
                continue;
            }
            EntityMaid maid = state.maid;
            if (maid == null || !maid.isAlive()) {
                BOOSTED_MAIDS.remove(scheduled.entityId());
                BOOSTED_MAID_EXPIRIES.poll();
                continue;
            }
            if (currentTime < scheduled.expiry()) {
                break;
            }
            removeAttributeModifier(maid, Attributes.ATTACK_DAMAGE, DC_NEARBY_DAMAGE_UUID);
            BOOSTED_MAIDS.remove(scheduled.entityId());
            BOOSTED_MAID_EXPIRIES.poll();
        }
    }

    private static long globalGameTime(MinecraftServer server) {
        return server.overworld().getGameTime();
    }

    // ---------------------------------------------------------------- 分发注册

    static {
        // ① 受伤抗性：伤害 ×0.7、上限 40。消费链 MaidDamageProcessor.applyBaubleHandlers ← LivingEntityMixin。
        Global.baubleSetHealthFinalHandlers.put(MaidSpellItems.DREAM_CAT_CRYSTAL.get(), data -> {
            float amount = data.getAmount();
            amount *= 0.7f;
            amount = Math.min(amount, 40.0f);
            data.setAmount(amount);
            return null;
        });
        // ② 有害效果免疫：非正面效果一律拒绝添加。消费点 LivingEntityMixin 的 addEffect 头部拦截。
        Global.baubleEffectBlockFilters.put(MaidSpellItems.DREAM_CAT_CRYSTAL.get(),
                (maid, effect) -> effect.getCategory() != MobEffectCategory.BENEFICIAL);
        // ③ 真实伤害 + 5 格弹幕溅射。
        //    原模组里 baubleHurtHeadHandlers 的消费方是字节码 coremod（HurtHeadTransformationService），
        //    本工程的消费点改到 LivingEntityMixin 的 hurt 头部调用 Global.dispatchHurtHeadHandlers。
        Global.registerBaubleHurtHeadHandler(MaidSpellItems.DREAM_CAT_CRYSTAL.get(), context -> {
            if (TrueDamageUtil.isApplyingQueuedDamage()) {
                return;
            }
            EntityMaid maid = context.getSourceMaid();
            if (maid == null) {
                return;
            }
            LivingEntity target = context.getTarget();
            if (!IAuthoritativeHealth.combatAlive(target)) {
                return;
            }
            float damage = context.getAmount();
            if (Config.dreamCrystalExtraTrueDamageEnabled) {
                TrueDamageUtil.dealTrueDamage(target, damage, maid);
            }
            if (!maid.getLevel().isClientSide) {
                float barrageDamage = damage * 0.1f;
                maid.getLevel().getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(5.0),
                                entity -> entity != maid && entity != target
                                        && IAuthoritativeHealth.combatAlive(entity)
                                        && !(entity instanceof Player)
                                        && !(entity instanceof EntityMaid))
                        .forEach(nearby -> TrueDamageUtil.dealTrueDamage(nearby, barrageDamage, maid));
            }
        });
    }

    // ---------------------------------------------------------------- 内部类型

    private record EffectMatcher(Set<String> exactMatches, List<Pattern> regexPatterns) {
        private static final String REGEX_PREFIX = "regex:";
        private static final EffectMatcher EMPTY = new EffectMatcher(Collections.emptySet(), Collections.emptyList());

        private static EffectMatcher empty() {
            return EMPTY;
        }

        private static EffectMatcher from(List<String> entries) {
            if (entries == null || entries.isEmpty()) {
                return empty();
            }
            Set<String> exactMatches = new HashSet<>();
            List<Pattern> regexPatterns = new ArrayList<>();
            for (String entry : entries) {
                if (entry == null || entry.isBlank()) {
                    continue;
                }
                if (entry.startsWith(REGEX_PREFIX)) {
                    String regex = entry.substring(REGEX_PREFIX.length());
                    if (regex.isBlank()) {
                        continue;
                    }
                    try {
                        regexPatterns.add(Pattern.compile(regex));
                    } catch (PatternSyntaxException exception) {
                        LOGGER.warn("梦云水晶效果匹配正则无效：{}，已跳过", entry, exception);
                    }
                    continue;
                }
                exactMatches.add(entry);
            }
            if (exactMatches.isEmpty() && regexPatterns.isEmpty()) {
                return empty();
            }
            return new EffectMatcher(exactMatches, regexPatterns);
        }

        private boolean matches(String effectId) {
            if (this.exactMatches.contains(effectId)) {
                return true;
            }
            for (Pattern pattern : this.regexPatterns) {
                if (pattern.matcher(effectId).matches()) {
                    return true;
                }
            }
            return false;
        }
    }

    private record ReviveHistory(List<Long> timestamps, boolean changed) {
    }

    private static final class BoostedMaidState {
        private EntityMaid maid;
        private final long expiry;

        private BoostedMaidState(EntityMaid maid, long expiry) {
            this.maid = maid;
            this.expiry = expiry;
        }
    }

    private record ScheduledExpiry(UUID entityId, long expiry) {
    }
}

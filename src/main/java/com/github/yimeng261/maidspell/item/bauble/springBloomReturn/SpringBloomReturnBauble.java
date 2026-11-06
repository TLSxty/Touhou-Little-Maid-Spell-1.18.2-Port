package com.github.yimeng261.maidspell.item.bauble.springBloomReturn;

import com.github.tartaricacid.touhoulittlemaid.api.bauble.IMaidBauble;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.yimeng261.maidspell.Config;
import com.github.yimeng261.maidspell.Global;
import com.github.yimeng261.maidspell.api.IMaidBaubleLifecycle;
import com.github.yimeng261.maidspell.item.MaidSpellItems;
import com.github.yimeng261.maidspell.utils.PortableTimerMath;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 春华归返（饰品逻辑）—— 批次 A 去魔法版。
 *
 * <p><b>保留（非魔法能力）</b>：
 * <ul>
 *   <li>重击减伤：受击伤害扣除「目标最大生命 × {@code springBloomReturnHealRatio}」，最少到 0。</li>
 *   <li>好感 ≥3 时驱散一个负面效果（保留最长持续时间的那一个）。</li>
 *   <li>层数、触发冷却、NBT 计时器迁移等全部机制。</li>
 * </ul>
 *
 * <p><b>删掉的魔法能力</b>：
 * <ul>
 *   <li>整段 {@code onSpellCast(EntityMaid, String modId, String spellId, LivingEntity target)}
 *       —— 原逻辑是「每次施法给饰品加一层」，本工程没有法术，层数永远拿不到（见下方说明）。</li>
 *   <li>{@code tryTrigger} 里「好感 ≥2 时把法术冷却按比例退回」那一行
 *       （{@code SpellBookManager.refundCooldowns}）—— 纯魔法。</li>
 *   <li>import {@code SpellBookManager}。</li>
 * </ul>
 *
 * <p><b>⚠ 副作用（必须知道）</b>：层数唯一来源就是被删掉的 {@code onSpellCast}，
 * 因此删除后 {@code expiries} 恒为空，{@code tryTrigger} 在
 * 「{@code state.expiries().isEmpty()} → return」处直接返回，
 * <b>减伤与清负面在批次 A 里实际不会触发</b>。这是「去魔法」的必然结果，
 * 代码逻辑本身完整保留；若将来要恢复触发，需要给层数另找一个非魔法来源
 * （例如「女仆受击累计」），届时才需要设计。
 */
public class SpringBloomReturnBauble implements IMaidBauble, IMaidBaubleLifecycle {

    private static final int CLOCK_VERSION = 1;
    private static final int MAX_STORED_STACKS = 8;
    private static final int MIGRATION_GRACE_TICKS = 20;
    private static final String NBT_CLOCK_VERSION = "spring_bloom_return_clock_version";
    private static final String NBT_STACK_EXPIRIES = "spring_bloom_return_expiries";
    private static final String NBT_LAST_GAIN_TICK = "spring_bloom_return_last_gain_tick";
    private static final String NBT_GAIN_COOLDOWN_UNTIL = "spring_bloom_return_gain_cooldown_until";
    private static final String NBT_TRIGGER_COOLDOWN_UNTIL = "spring_bloom_return_trigger_cooldown_until";
    private static final Map<UUID, Integer> EQUIPPED_SLOT_CACHE = new ConcurrentHashMap<>();

    public SpringBloomReturnBauble() {
        // 原实现靠构造器自注册事件总线。注意：BaubleManager.init() 期间只实例化一次，不会重复注册。
        MinecraftForge.EVENT_BUS.register(this);
    }

    // ---------------------------------------------------------------- IMaidBaubleLifecycle

    @Override
    public void onPutOn(EntityMaid maid, ItemStack baubleItem) {
        refreshEquippedSlotCache(maid, baubleItem);
        if (!maid.getLevel().isClientSide && !baubleItem.isEmpty()) {
            long now = getServerGameTime(maid);
            prepareTimerState(baubleItem, maid, now);
        }
    }

    @Override
    public void onTakeOff(EntityMaid maid, ItemStack baubleItem) {
        EQUIPPED_SLOT_CACHE.remove(maid.getUUID());
    }

    // ---------------------------------------------------------------- 受击

    @SubscribeEvent
    public void onLivingDamage(LivingDamageEvent event) {
        if (event.getEntityLiving().getLevel().isClientSide) {
            return;
        }
        LivingEntity target = event.getEntityLiving();
        if (!isHeavyDamage(target, event.getAmount())) {
            return;
        }
        if (target instanceof EntityMaid maid) {
            // TODO(批次B)：层数原本来自已删的 onSpellCast，当前恒为空 → tryTrigger 会提前返回。
            tryTrigger(event, maid, maid, findEquippedStack(maid));
            return;
        }
        if (target instanceof Player player) {
            EntityMaid protector = selectOwnerProtector(player);
            if (protector != null) {
                tryTrigger(event, protector, player, findEquippedStack(protector));
            }
        }
    }

    private static void tryTrigger(LivingDamageEvent event, EntityMaid maid, LivingEntity protectedTarget, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        long now = getServerGameTime(maid);
        TimerState state = prepareTimerState(stack, maid, now);
        if (!state.writable()) {
            return;
        }
        if (state.expiries().isEmpty()) {
            return;
        }
        if (state.triggerCooldownUntil() != null && now < state.triggerCooldownUntil()) {
            return;
        }
        state.expiries().remove(0);
        state.setTriggerCooldownUntil(deadlineOrNull(now, Config.springBloomReturnTriggerCooldownTicks));
        saveTimerState(stack, state, true);
        float reduction = (float) ((double) protectedTarget.getMaxHealth() * Config.springBloomReturnHealRatio);
        event.setAmount(Math.max(0.0f, event.getAmount() - reduction));
        // 原实现的「好感 ≥2 退法术冷却」在批次 A 删除（纯魔法）。
        int favorabilityLevel = maid.getFavorabilityManager().getLevel();
        if (favorabilityLevel >= 3) {
            purgeOneNegativeEffect(protectedTarget);
        }
    }

    private static boolean isHeavyDamage(LivingEntity entity, float amount) {
        return (double) amount >= Config.springBloomReturnDamageThreshold
                || (double) amount >= (double) entity.getMaxHealth() * Config.springBloomReturnDamageThresholdRatio;
    }

    private static EntityMaid selectOwnerProtector(Player player) {
        Map<UUID, EntityMaid> ownedMaids = Global.getOrCreatePlayerMaidMap(player.getUUID());
        return ownedMaids.values().stream()
                .filter(maid -> maid != null && maid.isAlive() && maid.getLevel() == player.getLevel())
                .filter(maid -> !findEquippedStack(maid).isEmpty())
                .max(Comparator.comparingInt(SpringBloomReturnBauble::getAvailableLayerCount))
                .orElse(null);
    }

    private static int getAvailableLayerCount(EntityMaid maid) {
        ItemStack stack = findEquippedStack(maid);
        if (stack.isEmpty()) {
            return 0;
        }
        long now = getServerGameTime(maid);
        TimerState state = prepareTimerState(stack, maid, now);
        if (!state.writable()) {
            return 0;
        }
        return state.expiries().size();
    }

    // ---------------------------------------------------------------- 槽位缓存

    private static ItemStack findEquippedStack(EntityMaid maid) {
        Integer cachedSlot = EQUIPPED_SLOT_CACHE.get(maid.getUUID());
        if (cachedSlot != null && cachedSlot >= 0 && cachedSlot < maid.getMaidBauble().getSlots()) {
            ItemStack cachedStack = maid.getMaidBauble().getStackInSlot(cachedSlot);
            if (cachedStack.is(MaidSpellItems.SPRING_BLOOM_RETURN.get())) {
                return cachedStack;
            }
        }
        for (int i = 0; i < maid.getMaidBauble().getSlots(); ++i) {
            ItemStack stack = maid.getMaidBauble().getStackInSlot(i);
            if (!stack.is(MaidSpellItems.SPRING_BLOOM_RETURN.get())) {
                continue;
            }
            EQUIPPED_SLOT_CACHE.put(maid.getUUID(), i);
            return stack;
        }
        EQUIPPED_SLOT_CACHE.remove(maid.getUUID());
        return ItemStack.EMPTY;
    }

    private static void refreshEquippedSlotCache(EntityMaid maid, ItemStack baubleItem) {
        for (int i = 0; i < maid.getMaidBauble().getSlots(); ++i) {
            ItemStack stack = maid.getMaidBauble().getStackInSlot(i);
            if (stack != baubleItem && !stack.is(MaidSpellItems.SPRING_BLOOM_RETURN.get())) {
                continue;
            }
            EQUIPPED_SLOT_CACHE.put(maid.getUUID(), i);
            return;
        }
        EQUIPPED_SLOT_CACHE.remove(maid.getUUID());
    }

    private static void purgeOneNegativeEffect(LivingEntity entity) {
        entity.getActiveEffects().stream()
                .filter(effect -> !effect.getEffect().isBeneficial())
                .max(Comparator.comparingInt(MobEffectInstance::getDuration))
                .map(MobEffectInstance::getEffect)
                .ifPresent(entity::removeEffect);
    }

    private static long getServerGameTime(EntityMaid maid) {
        return maid.getLevel().getServer().overworld().getGameTime();
    }

    // ---------------------------------------------------------------- 计时器 NBT

    private static TimerState prepareTimerState(ItemStack stack, EntityMaid maid, long serverNow) {
        TimerState state = loadTimerState(stack, maid, serverNow);
        if (state.writable()) {
            pruneExpiredTimers(state, serverNow);
            saveTimerState(stack, state, state.hasManagedData());
        }
        return state;
    }

    private static TimerState loadTimerState(ItemStack stack, EntityMaid maid, long serverNow) {
        CompoundTag tag = stack.getTag();
        int storedVersion = tag != null && tag.contains(NBT_CLOCK_VERSION, 99)
                ? tag.getInt(NBT_CLOCK_VERSION) : 0;
        if (storedVersion > CLOCK_VERSION) {
            return TimerState.readOnly();
        }
        List<Long> expiries = new ArrayList<>();
        if (tag != null && tag.contains(NBT_STACK_EXPIRIES, 12)) {
            for (long expiry : tag.getLongArray(NBT_STACK_EXPIRIES)) {
                expiries.add(expiry);
            }
        }
        Long gainCooldownUntil = getOptionalLong(tag, NBT_GAIN_COOLDOWN_UNTIL);
        Long triggerCooldownUntil = getOptionalLong(tag, NBT_TRIGGER_COOLDOWN_UNTIL);
        boolean legacy = storedVersion < CLOCK_VERSION;
        boolean hadManagedData = hasManagedTimerData(tag);
        long stackDuration = Math.max(0L, Config.springBloomReturnStackDurationTicks);
        if (legacy && hadManagedData) {
            long oldLocalNow = maid.getLevel().getGameTime();
            expiries.replaceAll(expiry -> PortableTimerMath.migrateDeadline(expiry, oldLocalNow, serverNow,
                    stackDuration, MIGRATION_GRACE_TICKS));
            Long lastGainTick = getOptionalLong(tag, NBT_LAST_GAIN_TICK);
            if (lastGainTick != null) {
                long gainCooldown = Math.max(0L, Config.springBloomReturnGainCooldownTicks);
                long migratedLastGain = PortableTimerMath.migrateTimestamp(lastGainTick, oldLocalNow, serverNow,
                        gainCooldown, MIGRATION_GRACE_TICKS);
                gainCooldownUntil = PortableTimerMath.saturatingAdd(migratedLastGain, gainCooldown);
            } else if (gainCooldownUntil != null) {
                gainCooldownUntil = PortableTimerMath.migrateDeadline(gainCooldownUntil, oldLocalNow, serverNow,
                        Config.springBloomReturnGainCooldownTicks, MIGRATION_GRACE_TICKS);
            }
            if (triggerCooldownUntil != null) {
                triggerCooldownUntil = PortableTimerMath.migrateDeadline(triggerCooldownUntil, oldLocalNow, serverNow,
                        Config.springBloomReturnTriggerCooldownTicks, MIGRATION_GRACE_TICKS);
            }
        }
        expiries.replaceAll(expiry -> PortableTimerMath.migrateDeadline(expiry, serverNow, serverNow,
                stackDuration, 0L));
        if (gainCooldownUntil != null) {
            gainCooldownUntil = PortableTimerMath.migrateDeadline(gainCooldownUntil, serverNow, serverNow,
                    Config.springBloomReturnGainCooldownTicks, 0L);
        }
        if (triggerCooldownUntil != null) {
            triggerCooldownUntil = PortableTimerMath.migrateDeadline(triggerCooldownUntil, serverNow, serverNow,
                    Config.springBloomReturnTriggerCooldownTicks, 0L);
        }
        expiries.sort(Long::compareTo);
        int storedStackLimit = Math.min(MAX_STORED_STACKS, Math.max(0, Config.springBloomReturnMaxStacks));
        if (expiries.size() > storedStackLimit) {
            expiries = new ArrayList<>(expiries.subList(0, storedStackLimit));
        }
        return new TimerState(expiries, gainCooldownUntil, triggerCooldownUntil, true, legacy && hadManagedData);
    }

    private static void pruneExpiredTimers(TimerState state, long now) {
        state.expiries().removeIf(expiry -> expiry <= now);
        if (state.gainCooldownUntil() != null && state.gainCooldownUntil() <= now) {
            state.setGainCooldownUntil(null);
        }
        if (state.triggerCooldownUntil() != null && state.triggerCooldownUntil() <= now) {
            state.setTriggerCooldownUntil(null);
        }
    }

    private static void saveTimerState(ItemStack stack, TimerState state, boolean forceVersion) {
        if (!state.writable()) {
            return;
        }
        CompoundTag currentTag = stack.getTag();
        boolean needsWrite = state.requiresVersionWrite()
                || (forceVersion && (currentTag == null || currentTag.getInt(NBT_CLOCK_VERSION) != CLOCK_VERSION))
                || (currentTag != null && currentTag.contains(NBT_LAST_GAIN_TICK))
                || !longArrayEquals(currentTag, NBT_STACK_EXPIRIES, state.expiries())
                || !optionalLongEquals(currentTag, NBT_GAIN_COOLDOWN_UNTIL, state.gainCooldownUntil())
                || !optionalLongEquals(currentTag, NBT_TRIGGER_COOLDOWN_UNTIL, state.triggerCooldownUntil());
        if (!needsWrite) {
            return;
        }
        CompoundTag tag = stack.getOrCreateTag();
        if (forceVersion || state.requiresVersionWrite()) {
            tag.putInt(NBT_CLOCK_VERSION, CLOCK_VERSION);
        }
        tag.remove(NBT_LAST_GAIN_TICK);
        putLongArrayOrRemove(tag, NBT_STACK_EXPIRIES, state.expiries());
        putOptionalLong(tag, NBT_GAIN_COOLDOWN_UNTIL, state.gainCooldownUntil());
        putOptionalLong(tag, NBT_TRIGGER_COOLDOWN_UNTIL, state.triggerCooldownUntil());
        state.markPersisted();
    }

    private static boolean hasManagedTimerData(CompoundTag tag) {
        return tag != null && (tag.contains(NBT_STACK_EXPIRIES)
                || tag.contains(NBT_LAST_GAIN_TICK)
                || tag.contains(NBT_GAIN_COOLDOWN_UNTIL)
                || tag.contains(NBT_TRIGGER_COOLDOWN_UNTIL)
                || tag.contains(NBT_CLOCK_VERSION));
    }

    private static Long getOptionalLong(CompoundTag tag, String key) {
        return tag != null && tag.contains(key, 99) ? tag.getLong(key) : null;
    }

    private static Long deadlineOrNull(long now, long durationTicks) {
        return durationTicks <= 0L ? null : PortableTimerMath.saturatingAdd(now, durationTicks);
    }

    private static boolean longArrayEquals(CompoundTag tag, String key, List<Long> values) {
        if (values.isEmpty()) {
            return tag == null || !tag.contains(key);
        }
        if (tag == null || !tag.contains(key, 12)) {
            return false;
        }
        long[] stored = tag.getLongArray(key);
        if (stored.length != values.size()) {
            return false;
        }
        for (int i = 0; i < stored.length; ++i) {
            if (stored[i] != values.get(i)) {
                return false;
            }
        }
        return true;
    }

    private static boolean optionalLongEquals(CompoundTag tag, String key, Long value) {
        if (value == null) {
            return tag == null || !tag.contains(key);
        }
        return tag != null && tag.contains(key, 99) && tag.getLong(key) == value;
    }

    private static void putLongArrayOrRemove(CompoundTag tag, String key, List<Long> values) {
        if (values.isEmpty()) {
            tag.remove(key);
        } else {
            tag.putLongArray(key, values);
        }
    }

    private static void putOptionalLong(CompoundTag tag, String key, Long value) {
        if (value == null) {
            tag.remove(key);
        } else {
            tag.putLong(key, value);
        }
    }

    private static final class TimerState {
        private final List<Long> expiries;
        private Long gainCooldownUntil;
        private Long triggerCooldownUntil;
        private final boolean writable;
        private boolean requiresVersionWrite;

        private TimerState(List<Long> expiries, Long gainCooldownUntil, Long triggerCooldownUntil,
                           boolean writable, boolean requiresVersionWrite) {
            this.expiries = expiries;
            this.gainCooldownUntil = gainCooldownUntil;
            this.triggerCooldownUntil = triggerCooldownUntil;
            this.writable = writable;
            this.requiresVersionWrite = requiresVersionWrite;
        }

        private static TimerState readOnly() {
            return new TimerState(new ArrayList<>(), null, null, false, false);
        }

        private List<Long> expiries() {
            return this.expiries;
        }

        private Long gainCooldownUntil() {
            return this.gainCooldownUntil;
        }

        private void setGainCooldownUntil(Long gainCooldownUntil) {
            this.gainCooldownUntil = gainCooldownUntil;
        }

        private Long triggerCooldownUntil() {
            return this.triggerCooldownUntil;
        }

        private void setTriggerCooldownUntil(Long triggerCooldownUntil) {
            this.triggerCooldownUntil = triggerCooldownUntil;
        }

        private boolean writable() {
            return this.writable;
        }

        private boolean requiresVersionWrite() {
            return this.requiresVersionWrite;
        }

        private boolean hasManagedData() {
            return this.requiresVersionWrite || !this.expiries.isEmpty()
                    || this.gainCooldownUntil != null || this.triggerCooldownUntil != null;
        }

        private void markPersisted() {
            this.requiresVersionWrite = false;
        }
    }
}

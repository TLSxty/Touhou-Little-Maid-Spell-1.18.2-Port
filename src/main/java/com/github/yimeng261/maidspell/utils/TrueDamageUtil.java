package com.github.yimeng261.maidspell.utils;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.yimeng261.maidspell.api.IAuthoritativeHealth;
import com.github.yimeng261.maidspell.api.ITrueDamageRedirect;
import com.github.yimeng261.maidspell.mixin.accessor.LivingEntityInvoker;
import com.github.yimeng261.maidspell.mixin.accessor.SynchedEntityDataMixin;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 真伤工具：绕过原版受伤流程直接改写生命值。
 * <p>
 * 1.20.1 原版靠 {@code damageSources()} 构造伤害源，1.18.2 没有该类，
 * 因此 {@link #createDamageSource} 改用 {@link DamageSource#playerAttack}/{@link DamageSource#mobAttack}。
 * SynchedEntityData / NBT 双通道写血依赖两个 accessor mixin。
 */
public final class TrueDamageUtil {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final float HEALTH_TOLERANCE = 0.01f;
    private static final int MAX_QUEUED_DAMAGE_PER_TICK = 2048;
    private static final Queue<TrueDamageRequest> TRUE_DAMAGE_QUEUE = new ConcurrentLinkedQueue<>();
    private static final Map<String, List<Integer>> healthIdMap = new HashMap<>();

    private static boolean applyingQueuedDamage;

    private TrueDamageUtil() {
    }

    public static boolean isApplyingQueuedDamage() {
        return applyingQueuedDamage;
    }

    public static boolean dealTrueDamage(LivingEntity target, float damage, LivingEntity attacker) {
        if (canNotBeApplied(target) || damage <= 0.0f || target.getLevel().isClientSide()) {
            return false;
        }
        if (target instanceof ITrueDamageRedirect redirect) {
            redirect.maidspell$onTrueDamageQueued();
        }
        TRUE_DAMAGE_QUEUE.offer(new TrueDamageRequest(target, damage, attacker));
        return true;
    }

    public static void processQueuedTrueDamage() {
        LinkedHashMap<Integer, AggregatedTrueDamage> aggregated = new LinkedHashMap<>();
        int drained = 0;
        TrueDamageRequest request;
        while (drained++ < MAX_QUEUED_DAMAGE_PER_TICK && (request = TRUE_DAMAGE_QUEUE.poll()) != null) {
            LivingEntity target = request.target().get();
            if (canNotBeApplied(target) || !IAuthoritativeHealth.combatAlive(target)) {
                continue;
            }
            LivingEntity attacker = request.attacker().get();
            aggregated.computeIfAbsent(target.getId(), ignored -> new AggregatedTrueDamage(target))
                    .add(request.damage(), attacker);
        }
        if (drained > MAX_QUEUED_DAMAGE_PER_TICK) {
            LOGGER.warn("[TrueDamage] queued true damage processing hit per-tick cap: remaining={}", TRUE_DAMAGE_QUEUE.size());
        }
        aggregated.values().forEach(damage -> {
            LivingEntity target = damage.target.get();
            if (canNotBeApplied(target) || !IAuthoritativeHealth.combatAlive(target)) {
                return;
            }
            if (target instanceof ITrueDamageRedirect redirect) {
                applyingQueuedDamage = true;
                try {
                    redirect.maidspell$redirectTrueDamage(damage.amount, damage.attacker);
                } finally {
                    applyingQueuedDamage = false;
                }
                return;
            }
            float currentHealth = target.getHealth();
            float newHealth = Math.max(0.0f, currentHealth - damage.amount);
            setNewHealth(target, newHealth, damage.attacker);
        });
    }

    public static void clearQueuedTrueDamage() {
        TRUE_DAMAGE_QUEUE.clear();
    }

    private static boolean canNotBeApplied(LivingEntity target) {
        return target == null || target instanceof Player || target instanceof EntityMaid;
    }

    public static boolean setNewHealth(LivingEntity target, float newHealth, LivingEntity attacker) {
        if (canNotBeApplied(target) || target instanceof ITrueDamageRedirect) {
            return false;
        }
        float healthGap = tryEntityDataDamage(target, newHealth);
        if (healthGap <= HEALTH_TOLERANCE) {
            return finishHealthChange(target, newHealth, attacker);
        }
        healthGap = tryNBTDamage(target, newHealth);
        if (healthGap <= HEALTH_TOLERANCE) {
            return finishHealthChange(target, newHealth, attacker);
        }
        return false;
    }

    private static float tryEntityDataDamage(LivingEntity target, float newHealth) {
        try {
            SynchedEntityDataMixin dataMixin = (SynchedEntityDataMixin) target.getEntityData();
            Int2ObjectMap<SynchedEntityData.DataItem<?>> itemsById = dataMixin.getItemsById();
            List<Integer> healthIds = findAllHealthIds(target, itemsById);
            if (healthIds.isEmpty()) {
                return Float.POSITIVE_INFINITY;
            }
            boolean modified = false;
            for (Integer healthId : healthIds) {
                try {
                    SynchedEntityData.DataItem<?> dataItem = itemsById.get(healthId.intValue());
                    if (dataItem == null) {
                        continue;
                    }
                    setEntityDataValue(target, dataItem, newHealth);
                    modified = true;
                } catch (Exception e) {
                    LOGGER.debug("[TrueDamage] Failed to modify EntityData ID {}: {}", healthId, e.getMessage());
                }
            }
            return modified ? getHealthGap(target, newHealth) : Float.POSITIVE_INFINITY;
        } catch (Exception e) {
            LOGGER.debug("[TrueDamage] EntityData damage failed: {}", e.getMessage());
            return Float.POSITIVE_INFINITY;
        }
    }

    /**
     * 通过 SynchedEntityData#set 写入新血量（原始实现直接把 accessor 强转后 set，这里保持同等语义）。
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void setEntityDataValue(LivingEntity target, SynchedEntityData.DataItem<?> dataItem, float newHealth) {
        SynchedEntityData data = target.getEntityData();
        data.set((net.minecraft.network.syncher.EntityDataAccessor) dataItem.getAccessor(), newHealth);
    }

    private static float tryNBTDamage(LivingEntity target, float newHealth) {
        try {
            CompoundTag nbt = new CompoundTag();
            target.saveWithoutId(nbt);
            float currentHealth = target.getHealth();
            boolean modified = false;
            if (nbt.contains("Health")) {
                nbt.putFloat("Health", newHealth);
                modified = true;
            }
            if (modifyFloatValuesInNBT(nbt, currentHealth, newHealth)) {
                modified = true;
            }
            if (modified) {
                target.load(nbt);
            }
            return modified ? getHealthGap(target, newHealth) : Float.POSITIVE_INFINITY;
        } catch (Exception e) {
            LOGGER.debug("[TrueDamage] NBT damage failed: {}", e.getMessage());
            return Float.POSITIVE_INFINITY;
        }
    }

    private static boolean modifyFloatValuesInNBT(CompoundTag nbt, float originalHealth, float newHealth) {
        boolean modified = false;
        for (String key : new ArrayList<>(nbt.getAllKeys())) {
            Tag tag = nbt.get(key);
            if (tag instanceof FloatTag) {
                float value = nbt.getFloat(key);
                if (isHealthMatch(value, originalHealth)) {
                    nbt.putFloat(key, newHealth);
                    modified = true;
                }
                continue;
            }
            if (tag instanceof CompoundTag child && modifyFloatValuesInNBT(child, originalHealth, newHealth)) {
                modified = true;
            }
        }
        return modified;
    }

    private static boolean isHealthMatch(float value1, float value2) {
        return Math.abs(value1 - value2) < HEALTH_TOLERANCE;
    }

    private static float getHealthGap(LivingEntity target, float expectedHealth) {
        return Math.abs(target.getHealth() - expectedHealth);
    }

    private static List<Integer> findAllHealthIds(LivingEntity target, Int2ObjectMap<SynchedEntityData.DataItem<?>> itemsById) {
        String className = target.getClass().getSimpleName();
        float currentHealth = target.getHealth();
        List<Integer> cachedIds = healthIdMap.get(className);
        if (cachedIds != null && !cachedIds.isEmpty()) {
            List<Integer> validIds = new ArrayList<>();
            for (Integer id : cachedIds) {
                SynchedEntityData.DataItem<?> item = itemsById.get(id.intValue());
                if (item != null && item.getValue() instanceof Float) {
                    validIds.add(id);
                }
            }
            if (!validIds.isEmpty()) {
                return validIds;
            }
        }
        List<Integer> healthIds = new ArrayList<>();
        itemsById.forEach((id, dataItem) -> {
            if (dataItem.getValue() instanceof Float floatValue
                    && (isHealthMatch(floatValue, currentHealth)
                    || (floatValue > 0.0f && floatValue <= target.getMaxHealth()))) {
                healthIds.add(id);
            }
        });
        healthIdMap.put(className, healthIds);
        return healthIds;
    }

    private static void handleTrueDamageDeath(LivingEntity target, LivingEntity attacker) {
        target.die(createDamageSource(target, attacker));
        if (!target.isRemoved() && target.isDeadOrDying() && target instanceof LivingEntityInvoker invoker) {
            invoker.maidspell$invokeTickDeath();
        }
    }

    private static boolean finishHealthChange(LivingEntity target, float newHealth, LivingEntity attacker) {
        if (newHealth <= 0.0f && target.getHealth() <= HEALTH_TOLERANCE) {
            handleTrueDamageDeath(target, attacker);
        }
        return true;
    }

    /**
     * 1.18.2 没有 DamageSources 对象，只能用 DamageSource 的静态工厂。
     */
    private static DamageSource createDamageSource(LivingEntity target, LivingEntity attacker) {
        if (attacker instanceof Player player) {
            return DamageSource.playerAttack(player);
        }
        if (attacker != null) {
            return DamageSource.mobAttack(attacker);
        }
        return DamageSource.GENERIC;
    }

    private record TrueDamageRequest(WeakReference<LivingEntity> target, float damage, WeakReference<LivingEntity> attacker) {
        private TrueDamageRequest(LivingEntity target, float damage, LivingEntity attacker) {
            this(new WeakReference<>(target), damage, new WeakReference<>(attacker));
        }
    }

    private static final class AggregatedTrueDamage {
        private final WeakReference<LivingEntity> target;
        private float amount;
        private LivingEntity attacker;

        private AggregatedTrueDamage(LivingEntity target) {
            this.target = new WeakReference<>(target);
        }

        private void add(float damage, LivingEntity attacker) {
            this.amount += damage;
            if (this.attacker == null && attacker != null) {
                this.attacker = attacker;
            }
        }
    }
}

package com.github.yimeng261.maidspell;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.yimeng261.maidspell.spell.manager.BaubleStateManager;
import com.github.yimeng261.maidspell.utils.DataItem;
import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.PotionEvent;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

/**
 * 全局分发表：所有饰品把自己关心的回调注册进这里的 Map/List，由
 * {@code MaidSpellEventHandler} 在对应 Forge 事件里统一遍历分发。
 * <p>
 * 移植自 1.9.2.1 反编译源 Global.java。**分发 Map 的类型与语义原样保留**，
 * 只做以下两处裁剪（均因批次 A 不搬魔法层）：
 * <ul>
 *   <li>删 {@code commonCoolDownCalc} 与 {@code baubleCooldownHandlers}：两者元素类型是
 *       {@code Function<IMaidSpellData.CoolDown, Void>}，而 {@code IMaidSpellData} 是法术数据载体，
 *       属魔法层不搬。批次 A 唯一的消费者（云梦水晶「法术冷却置 0」）本就要删；
 *       批次 B 搬急速咏唱戒指时需要连同 {@code IMaidSpellData.CoolDown} 一起重建。</li>
 *   <li>删 {@code resetCommonCoolDownCalc()}（同上原因）。</li>
 *   <li>{@code resetCommonDamageCalc()} 里删掉「按 Config.spellDamageMultiplier 缩放法术伤害」
 *       那一条，因为本工程没有法术。</li>
 * </ul>
 * <p>
 * 注意：{@code baubleHurtHeadHandlers} 在原模组里由字节码 coremod（{@code HurtHeadTransformationService}）
 * 消费；本工程没有 coremod，这条链暂时是「只写不读」。保留字段是为了批次 B 与未来恢复时结构不塌。
 */
public class Global {

    public static final Logger LOGGER = LogUtils.getLogger();

    public static final List<Consumer<HurtHeadContext>> commonHurtHeadHandlers = new CopyOnWriteArrayList<>();
    public static final List<BiFunction<LivingHurtEvent, EntityMaid, Void>> commonHurtHandlers = new CopyOnWriteArrayList<>();
    public static final List<BiFunction<LivingHurtEvent, EntityMaid, Void>> commonHurtCalc = new CopyOnWriteArrayList<>();
    public static final List<BiFunction<LivingDamageEvent, Player, Void>> playerDamageHandlers = new CopyOnWriteArrayList<>();
    public static final Map<UUID, Map<UUID, EntityMaid>> ownerMaidRegistry = new ConcurrentHashMap<>();
    public static final Set<EntityMaid> activeMaids = ConcurrentHashMap.newKeySet();
    public static final List<BaubleHurtHeadHandler> baubleHurtHeadHandlers = new CopyOnWriteArrayList<>();
    public static final Map<Item, BiFunction<LivingHurtEvent, EntityMaid, Void>> baubleHurtHandlers = new ConcurrentHashMap<>();
    public static final Map<Item, BiFunction<LivingHurtEvent, EntityMaid, Void>> baubleHurtEventHandlers = new ConcurrentHashMap<>();
    public static final Map<Item, BiFunction<LivingDamageEvent, EntityMaid, Void>> baubleDamageHandlers = new ConcurrentHashMap<>();
    public static final Map<Item, Function<DataItem, Void>> baubleSetHealthHandlers = new ConcurrentHashMap<>();
    public static final Map<Item, Function<DataItem, Void>> baubleSetHealthFinalHandlers = new ConcurrentHashMap<>();
    public static final Map<Item, BiFunction<PotionEvent.PotionAddedEvent, EntityMaid, Void>> baubleEffectAddedHandlers = new ConcurrentHashMap<>();
    public static final Map<Item, BiFunction<LivingDeathEvent, EntityMaid, Void>> baubleDeathHandlers = new ConcurrentHashMap<>();
    public static final Map<Item, BiFunction<EntityMaid, MobEffect, Boolean>> baubleEffectBlockFilters = new ConcurrentHashMap<>();

    public static HurtHeadContext dispatchHurtHeadHandlers(LivingEntity entity, DamageSource damageSource, float amount) {
        HurtHeadContext context = new HurtHeadContext(entity, damageSource, amount);
        for (Consumer<HurtHeadContext> handler : commonHurtHeadHandlers) {
            handler.accept(context);
            if (!context.isHandled()) {
                continue;
            }
            return context;
        }
        EntityMaid maid = context.getSourceMaid();
        if (maid == null) {
            return context;
        }
        for (BaubleHurtHeadHandler entry : baubleHurtHeadHandlers) {
            if (entry.matcher().test(maid)) {
                entry.handler().accept(context);
            }
            if (!context.isHandled()) {
                continue;
            }
            return context;
        }
        return context;
    }

    public static void registerBaubleHurtHeadHandler(Item item, Consumer<HurtHeadContext> handler) {
        baubleHurtHeadHandlers.add(new BaubleHurtHeadHandler(maid -> BaubleStateManager.hasBauble(maid, item), handler));
    }

    public static void registerBaubleHurtHeadHandler(RegistryObject<Item> item, Consumer<HurtHeadContext> handler) {
        baubleHurtHeadHandlers.add(new BaubleHurtHeadHandler(maid -> BaubleStateManager.hasBauble(maid, item), handler));
    }

    public static void resetCommonDamageCalc() {
        commonHurtHandlers.clear();
        // 上游 1.20.1 在这里注册了一条「女仆 / 玩家的 LivingHurtEvent 一律 cancel」的处理器，
        // 用来把伤害交给 maidspell 自建的战斗管线接管。
        // 本工程**不搬**那条自建管线（它依赖字节码 coremod + 权威血量系统），
        // 若保留这条 cancel 就会变成「女仆与主人对一切常规伤害无条件免疫」。
        // 因此这里保持为空列表：commonHurtHandlers 目前没有消费点，
        // 属于「先占位、以后由饰品效果显式注册」的扩展位。
        // 真正的伤害拦截只允许出现在有明确饰品来源的地方
        // （如 Global.baubleHurtEventHandlers / baubleSetHealth*Handlers）。
    }

    public static Map<UUID, EntityMaid> getOrCreatePlayerMaidMap(UUID playerUUID) {
        return ownerMaidRegistry.computeIfAbsent(playerUUID, k -> new ConcurrentHashMap<>());
    }

    public static void updateMaidInfo(EntityMaid maid, Boolean add) {
        UUID ownerUUID = maid.getOwnerUUID();
        if (add.booleanValue()) {
            activeMaids.add(maid);
            if (ownerUUID != null) {
                Global.getOrCreatePlayerMaidMap(ownerUUID).put(maid.getUUID(), maid);
            }
        } else {
            activeMaids.remove(maid);
            if (ownerUUID != null) {
                ownerMaidRegistry.computeIfPresent(ownerUUID, (ignored, maids) -> {
                    maids.remove(maid.getUUID());
                    return maids.isEmpty() ? null : maids;
                });
            }
        }
    }

    public static class HurtHeadContext {
        @NotNull
        private final LivingEntity entity;
        @NotNull
        private final DamageSource damageSource;
        private final float amount;
        private Boolean returnValue;

        public HurtHeadContext(@NotNull LivingEntity entity, @NotNull DamageSource damageSource, float amount) {
            this.entity = entity;
            this.damageSource = damageSource;
            this.amount = amount;
        }

        @NotNull
        public LivingEntity getEntity() {
            return this.entity;
        }

        @NotNull
        public LivingEntity getTarget() {
            return this.entity;
        }

        @NotNull
        public DamageSource getDamageSource() {
            return this.damageSource;
        }

        public float getAmount() {
            return this.amount;
        }

        public Entity getSourceEntity() {
            return this.damageSource.getEntity();
        }

        public Entity getDirectEntity() {
            return this.damageSource.getDirectEntity();
        }

        public EntityMaid getSourceMaid() {
            Entity sourceEntity = this.getSourceEntity();
            if (sourceEntity instanceof EntityMaid) {
                return (EntityMaid) sourceEntity;
            }
            Entity directEntity = this.getDirectEntity();
            if (directEntity instanceof EntityMaid) {
                return (EntityMaid) directEntity;
            }
            return null;
        }

        public boolean isHandled() {
            return this.returnValue != null;
        }

        public boolean getReturnValue() {
            return Boolean.TRUE.equals(this.returnValue);
        }

        public void setReturnValue(boolean returnValue) {
            this.returnValue = returnValue;
        }

        public void cancel() {
            this.returnValue = false;
        }

        public void pass() {
            this.returnValue = null;
        }
    }

    public record BaubleHurtHeadHandler(Predicate<EntityMaid> matcher, Consumer<HurtHeadContext> handler) {
    }
}

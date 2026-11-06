package com.github.yimeng261.maidspell.event;

import com.github.tartaricacid.touhoulittlemaid.api.bauble.IMaidBauble;
import com.github.tartaricacid.touhoulittlemaid.api.event.MaidDeathEvent;
import com.github.tartaricacid.touhoulittlemaid.api.event.MaidTickEvent;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.inventory.handler.BaubleItemHandler;
import com.github.tartaricacid.touhoulittlemaid.item.bauble.BaubleManager;
import com.github.yimeng261.maidspell.Config;
import com.github.yimeng261.maidspell.Global;
import com.github.yimeng261.maidspell.api.IMaidBaubleLifecycle;
import com.github.yimeng261.maidspell.item.bauble.dreamCatCrystal.DreamCatCrystalBauble;
import com.github.yimeng261.maidspell.spell.manager.BaubleStateManager;
import com.github.yimeng261.maidspell.utils.TrueDamageUtil;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityLeaveWorldEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.PotionEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 女仆饰品事件分发器（批次 A 精简版）。
 *
 * <p>移植自 1.9.2.1 反编译源 event/MaidSpellEventHandler.java（755 行，
 * 其中大半是 Psi / 法术 / 维度 / 抑制区逻辑，本批次不搬）。</p>
 *
 * <p><b>本类的核心职责：把 TLM 1.2.2 上不存在的饰品回调补回来。</b>
 * TLM 1.2.2 的 {@code IMaidBauble} 只有 {@code onTick} 与 {@code getChatBubbleId}，
 * {@code BaubleItemHandler.onContentsChanged} 也只维护内部缓存、不回调饰品，
 * 因此 {@code onPutOn / onTakeOff / onDeath} 在原模组里是死代码。这里的桥分两处：</p>
 * <ol>
 *   <li>{@link #onMaidTick}：对女仆饰品栏 9 个槽做快照 diff。某槽「上一 tick 空、这一 tick 有」
 *       → 调 {@code onPutOn}；「上一 tick 有、这一 tick 空」或「换成了别的物品」→ 先对旧物品调
 *       {@code onTakeOff}，再对新物品调 {@code onPutOn}。</li>
 *   <li>{@link #onMaidDeath}：遍历 9 槽调 {@code onDeath}，<b>只要有一个饰品返回 true 就取消
 *       {@code MaidDeathEvent}</b>（该事件可取消，取消即阻止死亡），并停止后续饰品。
 *       这正是云梦水晶概率复活的接入点。</li>
 * </ol>
 *
 * <p><b>为什么不能用 {@code LivingDeathEvent}：</b>它触发时死亡已经发生，无法再阻止。</p>
 *
 * <p><b>为什么不用 {@code MaidEquipEvent} 做 put/off 触发：</b>该事件只覆盖 ARMOR / 手槽位，
 * 不覆盖 bauble 9 槽（见 docs/P1_接口对齐改造清单.md §2.2）。</p>
 *
 * <p><b>为什么必须逐 tick diff：</b>{@code BaubleItemHandler} 未对外暴露槽内容变化通知，
 * {@code onContentsChanged} 是 protected 且只维护缓存。逐 tick diff 是唯一不需要 Mixin 的可靠办法。</p>
 */
@Mod.EventBusSubscriber(modid = Config.MOD_ID)
public final class MaidSpellEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(Config.MOD_ID);

    /** 每个女仆上一次看到的饰品栏快照，用于 diff。 */
    private static final Map<UUID, ItemStack[]> BAUBLE_SNAPSHOT = new ConcurrentHashMap<>();

    private MaidSpellEventHandler() {
    }

    // ------------------------------------------------------------------ 桥：onPutOn / onTakeOff

    @SubscribeEvent
    public static void onMaidTick(MaidTickEvent event) {
        EntityMaid maid = event.getMaid();
        if (maid.getLevel().isClientSide()) {
            return;
        }
        try {
            syncBaubleLifecycle(maid);
        } catch (Exception e) {
            LOGGER.error("女仆 {} 饰品生命周期分发失败: {}", maid.getName().getString(), e.getMessage(), e);
            return;
        }
        if (maid.tickCount % 20 == 0) {
            // 维护「主人 → 女仆」归属表，供春华归返「主人受击时找护主女仆」使用。
            Global.updateMaidInfo(maid, true);
            // 云梦水晶的范围攻击强化到期回收。原实现由 ServerTickEvent 驱动；这里按 20 tick
            // 调一次同样正确 —— processScheduledEffects 只按当前 gameTime 弹出到期项，重复调用无害。
            DreamCatCrystalBauble.processScheduledEffects(maid.getServer());
        }
    }

    /** 对饰品栏做快照 diff，分发 onPutOn / onTakeOff。 */
    private static void syncBaubleLifecycle(EntityMaid maid) {
        BaubleItemHandler handler = maid.getMaidBauble();
        int slots = handler.getSlots();
        ItemStack[] current = new ItemStack[slots];
        for (int i = 0; i < slots; ++i) {
            ItemStack stack = handler.getStackInSlot(i);
            current[i] = stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
        }

        ItemStack[] previous = BAUBLE_SNAPSHOT.put(maid.getUUID(), current);
        if (previous == null) {
            // 首次看到该女仆（含重载世界后）：把当前已有饰品视为「刚戴上」，
            // 这样饰品属性/buff 会被重新应用，不会因为跨存档而丢失。
            for (ItemStack stack : current) {
                if (!stack.isEmpty()) {
                    firePutOn(maid, stack);
                }
            }
            return;
        }

        int limit = Math.min(previous.length, current.length);
        for (int i = 0; i < limit; ++i) {
            ItemStack before = previous[i];
            ItemStack after = current[i];
            if (isSameStack(before, after)) {
                continue;
            }
            if (!before.isEmpty()) {
                fireTakeOff(maid, before);
            }
            if (!after.isEmpty()) {
                firePutOn(maid, after);
            }
        }
        // 槽位数量变化兜底（1.2.2 恒为 9，不扩栏；留此分支只为防御）
        for (int i = limit; i < previous.length; ++i) {
            if (!previous[i].isEmpty()) {
                fireTakeOff(maid, previous[i]);
            }
        }
        for (int i = limit; i < current.length; ++i) {
            if (!current[i].isEmpty()) {
                firePutOn(maid, current[i]);
            }
        }
    }

    /** 两个 ItemStack 是否「同一件饰品且数据未变」。NBT 变化不算换饰品，避免每次使用都重跑 put/off。 */
    private static boolean isSameStack(ItemStack a, ItemStack b) {
        if (a.isEmpty() || b.isEmpty()) {
            return a.isEmpty() && b.isEmpty();
        }
        return ItemStack.isSameItemSameTags(a, b);
    }

    private static void firePutOn(EntityMaid maid, ItemStack stack) {
        IMaidBauble bauble = BaubleManager.getBauble(stack);
        if (bauble instanceof IMaidBaubleLifecycle lifecycle) {
            try {
                lifecycle.onPutOn(maid, stack);
            } catch (Exception e) {
                LOGGER.error("饰品 {} 的 onPutOn 抛出异常", stack.getItem(), e);
            }
        }
    }

    private static void fireTakeOff(EntityMaid maid, ItemStack stack) {
        IMaidBauble bauble = BaubleManager.getBauble(stack);
        if (bauble instanceof IMaidBaubleLifecycle lifecycle) {
            try {
                lifecycle.onTakeOff(maid, stack);
            } catch (Exception e) {
                LOGGER.error("饰品 {} 的 onTakeOff 抛出异常", stack.getItem(), e);
            }
        }
    }

    // ------------------------------------------------------------------ 桥：onDeath

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onMaidDeath(MaidDeathEvent event) {
        EntityMaid maid = event.getMaid();
        if (maid.getLevel().isClientSide()) {
            return;
        }
        BaubleItemHandler handler = maid.getMaidBauble();
        for (int i = 0; i < handler.getSlots(); ++i) {
            ItemStack stack = handler.getStackInSlot(i);
            if (stack.isEmpty()) {
                continue;
            }
            IMaidBauble bauble = BaubleManager.getBauble(stack);
            if (!(bauble instanceof IMaidBaubleLifecycle lifecycle)) {
                continue;
            }
            boolean prevented;
            try {
                prevented = lifecycle.onDeath(maid, stack, event.getSource());
            } catch (Exception e) {
                LOGGER.error("饰品 {} 的 onDeath 抛出异常", stack.getItem(), e);
                continue;
            }
            if (prevented) {
                // 语义照抄 1.20 的 IMaidBauble.onDeath：true = 取消死亡 + 后续饰品不再触发。
                event.setCanceled(true);
                BAUBLE_SNAPSHOT.remove(maid.getUUID());
                return;
            }
        }
    }

    // ------------------------------------------------------------------ Global 分发表的消费端

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntityLiving() instanceof EntityMaid maid)) {
            return;
        }
        Global.commonHurtCalc.forEach(function -> function.apply(event, maid));
        Global.baubleHurtEventHandlers.forEach((item, function) -> {
            if (BaubleStateManager.hasBauble(maid, item)) {
                function.apply(event, maid);
            }
        });
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        if (!(event.getEntityLiving() instanceof EntityMaid maid)) {
            return;
        }
        if (Global.baubleDamageHandlers.isEmpty()) {
            return;
        }
        Global.baubleDamageHandlers.forEach((item, function) -> {
            if (BaubleStateManager.hasBauble(maid, item)) {
                function.apply(event, maid);
            }
        });
    }

    @SubscribeEvent
    public static void onMaidEffectAdded(PotionEvent.PotionAddedEvent event) {
        if (!(event.getEntityLiving() instanceof EntityMaid maid)) {
            return;
        }
        Global.baubleEffectAddedHandlers.forEach((item, function) -> {
            if (BaubleStateManager.hasBauble(maid, item)) {
                function.apply(event, maid);
            }
        });
    }

    /** 女仆离开世界时清掉快照，避免 UUID 复用导致误判。 */
    @SubscribeEvent
    public static void onEntityLeaveWorld(EntityLeaveWorldEvent event) {
        if (event.getEntity() instanceof EntityMaid maid) {
            BAUBLE_SNAPSHOT.remove(maid.getUUID());
        }
    }

    /**
     * 真伤队列的消费端。
     * <p>
     * {@code TrueDamageUtil.dealTrueDamage} 只是把伤害入队（绕过原版伤害结算，直接改血量），
     * 必须每 tick 排空一次队列才会真正生效。原模组由 {@code ServerTickEvent} 驱动，
     * 这里同样挂在 server tick 的 END 阶段。
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        TrueDamageUtil.processQueuedTrueDamage();
    }
}

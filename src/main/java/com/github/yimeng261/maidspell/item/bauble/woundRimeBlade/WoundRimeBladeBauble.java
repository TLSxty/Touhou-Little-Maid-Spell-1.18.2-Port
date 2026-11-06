// 移植自 1.9.2.1 反编译源 item/bauble/woundRimeBlade/WoundRimeBladeBauble.java；SRG 名已替换为 1.18.2 official 名。
package com.github.yimeng261.maidspell.item.bauble.woundRimeBlade;

import com.github.tartaricacid.touhoulittlemaid.api.bauble.IMaidBauble;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.yimeng261.maidspell.Config;
import com.github.yimeng261.maidspell.Global;
import com.github.yimeng261.maidspell.api.IAuthoritativeHealth;
import com.github.yimeng261.maidspell.item.MaidSpellItems;
import com.github.yimeng261.maidspell.spell.manager.BaubleStateManager;
import com.github.yimeng261.maidspell.utils.TrueDamageUtil;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import oshi.util.tuples.Pair;

public class WoundRimeBladeBauble
implements IMaidBauble {
    private static final Map<UUID, ConcurrentHashMap<TargetKey, Pair<Float, Integer>>> maidWoundRimeBladeMap = new ConcurrentHashMap<UUID, ConcurrentHashMap<TargetKey, Pair<Float, Integer>>>();

    public void onTakeOff(EntityMaid maid, ItemStack baubleItem) {
        if (!maid.getLevel().isClientSide()) {
            WoundRimeBladeBauble.cleanupMaid(maid.getUUID());
        }
    }

    public static void updateWoundRimeMap(EntityMaid maid, LivingEntity entity, float damage) {
        Pair<Float, Integer> record;
        float recordHealth;
        if (!(maid.getLevel() instanceof ServerLevel && entity.getLevel() instanceof ServerLevel && BaubleStateManager.hasBauble(maid, MaidSpellItems.WOUND_RIME_BLADE))) {
            return;
        }
        if (entity instanceof Player || entity instanceof EntityMaid) {
            return;
        }
        ConcurrentHashMap<TargetKey, Pair<Float, Integer>> targets = maidWoundRimeBladeMap
                .computeIfAbsent(maid.getUUID(), ignored -> new ConcurrentHashMap<>());
        TargetKey targetKey = TargetKey.of(entity);
        float nowHealth = IAuthoritativeHealth.health(entity);
        record = targets.getOrDefault(targetKey, new Pair<>(nowHealth, Config.woundRimeBladeRecordTimes));
        recordHealth = record.getA().floatValue();
        if (nowHealth > recordHealth) {
            TrueDamageUtil.setNewHealth(entity, recordHealth, maid);
        }
        float newHealth = Math.min(recordHealth, nowHealth) - damage;
        targets.put(targetKey, new Pair<>(newHealth, record.getB() + Config.woundRimeBladeRecordTimes));
    }

    public static boolean handleWoundRimeMap(LivingEntity entity, float health) {
        if (!(entity.getLevel() instanceof ServerLevel)) {
            return false;
        }
        AtomicBoolean shouldCancel = new AtomicBoolean(false);
        TargetKey targetKey = TargetKey.of(entity);
        maidWoundRimeBladeMap.forEach((maidId, targets) -> {
            targets.computeIfPresent(targetKey, (ignored, record) -> {
                shouldCancel.set(true);
                int remaining = record.getB() - 1;
                return remaining <= 0 ? null : new Pair<>(health, remaining);
            });
            if (targets.isEmpty()) {
                maidWoundRimeBladeMap.remove(maidId, targets);
            }
        });
        return shouldCancel.get();
    }

    @Override
    public void onTick(EntityMaid maid, ItemStack baubleItem) {
        Level level = maid.getLevel();
        if (!(level instanceof ServerLevel)) {
            return;
        }
        ServerLevel serverLevel = (ServerLevel)level;
        UUID maidId = maid.getUUID();
        ConcurrentHashMap<TargetKey, Pair<Float, Integer>> targets = maidWoundRimeBladeMap.get(maidId);
        if (targets == null) {
            return;
        }
        MinecraftServer server = serverLevel.getServer();
        targets.forEach((targetKey, record) -> {
            if (record.getB() <= 0 || !WoundRimeBladeBauble.isLoadedAndAlive(server, targetKey)) {
                targets.remove(targetKey, record);
            }
        });
        if (targets.isEmpty()) {
            maidWoundRimeBladeMap.remove(maidId, targets);
        }
    }

    public static void cleanupMaid(UUID maidId) {
        maidWoundRimeBladeMap.remove(maidId);
    }

    public static void clearSession() {
        maidWoundRimeBladeMap.clear();
    }

    private static boolean isLoadedAndAlive(MinecraftServer server, TargetKey targetKey) {
        LivingEntity livingEntity;
        ServerLevel level = server.getLevel(targetKey.dimension());
        if (level == null) {
            return false;
        }
        Entity entity = level.getEntity(targetKey.entityId());
        return entity instanceof LivingEntity && IAuthoritativeHealth.combatAlive(livingEntity = (LivingEntity)entity);
    }

    static {
        Global.registerBaubleHurtHeadHandler(MaidSpellItems.WOUND_RIME_BLADE, context -> {
            EntityMaid maid = context.getSourceMaid();
            if (maid != null) {
                WoundRimeBladeBauble.updateWoundRimeMap(maid, context.getTarget(), context.getAmount());
            }
        });
    }

    private record TargetKey(ResourceKey<Level> dimension, UUID entityId) {
        private static TargetKey of(LivingEntity entity) {
            return new TargetKey((ResourceKey<Level>)entity.getLevel().dimension(), entity.getUUID());
        }
    }
}


// 移植自 1.9.2.1 反编译源 item/bauble/fragrantIngenuity/FragrantIngenuityBauble.java；SRG 名已替换为 1.18.2 official 名。
package com.github.yimeng261.maidspell.item.bauble.fragrantIngenuity;

import com.github.tartaricacid.touhoulittlemaid.api.bauble.IMaidBauble;
import com.github.tartaricacid.touhoulittlemaid.api.event.MaidAfterEatEvent;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.util.ItemsUtil;
import com.github.yimeng261.maidspell.Config;
import com.github.yimeng261.maidspell.item.MaidSpellItems;
import com.github.yimeng261.maidspell.spell.manager.BaubleStateManager;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;

public class FragrantIngenuityBauble
implements IMaidBauble {
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final List<MobEffect> POSITIVE_EFFECTS = new ArrayList<MobEffect>();

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        POSITIVE_EFFECTS.clear();
        List<String> blacklist = Config.fragrantIngenuityEffectBlacklist;
        ForgeRegistries.MOB_EFFECTS.getEntries().forEach(entry -> {
            ResourceLocation location;
            MobEffect effect = entry.getValue();
            if (effect.isBeneficial() && (location = ForgeRegistries.MOB_EFFECTS.getKey(effect)) != null && !blacklist.contains(location.toString())) {
                POSITIVE_EFFECTS.add(effect);
            }
        });
    }

    public FragrantIngenuityBauble() {
        MinecraftForge.EVENT_BUS.register((Object)this);
    }

    public static void applyOwnerFeedBuff(EntityMaid maid, Player owner) {
        if (maid == null || owner == null || owner.getLevel().isClientSide()) {
            return;
        }
        if (!BaubleStateManager.hasBauble(maid, MaidSpellItems.FRAGRANT_INGENUITY)) {
            return;
        }
        if (POSITIVE_EFFECTS.isEmpty()) {
            LOGGER.warn("[FragrantIngenuity] No positive effects available!");
            return;
        }
        boolean hasDreamCrystal = BaubleStateManager.hasBauble(maid, MaidSpellItems.DREAM_CAT_CRYSTAL);
        if (hasDreamCrystal) {
            ArrayList<MobEffect> pool = new ArrayList<MobEffect>(POSITIVE_EFFECTS);
            int effectCount = Math.min(2, pool.size());
            for (int i = 0; i < effectCount; ++i) {
                int selectedIndex = owner.getRandom().nextInt(pool.size());
                MobEffect effect = (MobEffect)pool.remove(selectedIndex);
                int dreamDuration = 2400 + owner.getRandom().nextInt(2401);
                int amplifier = 1 + owner.getRandom().nextInt(9);
                owner.addEffect(new MobEffectInstance(effect, dreamDuration, amplifier));
            }
            return;
        }
        MobEffect randomEffect = POSITIVE_EFFECTS.get(owner.getRandom().nextInt(POSITIVE_EFFECTS.size()));
        if (randomEffect != null) {
            owner.addEffect(new MobEffectInstance(randomEffect, Config.fragrantIngenuityBuffDuration, 0));
        }
    }

    @SubscribeEvent
    public void afterMaidEat(MaidAfterEatEvent event) {
        EntityMaid maid = event.getMaid();
        if (ItemsUtil.getBaubleSlotInMaid((EntityMaid)maid, (IMaidBauble)this) >= 0) {
            maid.getFavorabilityManager().add(Config.fragrantIngenuityFavorabilityGain);
            LOGGER.debug("[FragrantIngenuity] Maid {} gained {} favorability from eating", maid.hasCustomName() ? maid.getCustomName().getString() : maid.getUUID().toString(), (Object)Config.fragrantIngenuityFavorabilityGain);
        }
    }
}


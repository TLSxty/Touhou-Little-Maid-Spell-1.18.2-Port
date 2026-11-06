// 移植自 1.9.2.1 反编译源 item/bauble/bleedingHeart/BleedingHeart.java；SRG 名已替换为 1.18.2 official 名。
package com.github.yimeng261.maidspell.item.bauble.bleedingHeart;

import com.github.yimeng261.maidspell.item.MaidSpellCreativeTab;

import com.github.yimeng261.maidspell.Config;
import com.github.yimeng261.maidspell.utils.TooltipHelper;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public class BleedingHeart
extends Item {
    public BleedingHeart() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).tab(MaidSpellCreativeTab.BAUBLE_TAB));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        TooltipHelper.addShiftTooltip(tooltip, List.of(new TranslatableComponent((String)"item.maidspell.bleeding_heart.desc1").withStyle(ChatFormatting.GRAY)), List.of(new TranslatableComponent((String)"item.maidspell.bleeding_heart.desc2", (Object[])new Object[]{String.format("%.0f", Config.bleedingHeartHealRatio * 100.0)}).withStyle(ChatFormatting.BLUE), new TranslatableComponent((String)"item.maidspell.bleeding_heart.desc3").withStyle(ChatFormatting.YELLOW)));
    }
}


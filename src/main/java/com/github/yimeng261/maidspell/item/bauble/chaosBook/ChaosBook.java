// 移植自 1.9.2.1 反编译源 item/bauble/chaosBook/ChaosBook.java；SRG 名已替换为 1.18.2 official 名。
package com.github.yimeng261.maidspell.item.bauble.chaosBook;

import com.github.yimeng261.maidspell.item.MaidSpellCreativeTab;

import com.github.yimeng261.maidspell.Config;
import com.github.yimeng261.maidspell.utils.TooltipHelper;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public class ChaosBook
extends Item {
    public ChaosBook() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.RARE).tab(MaidSpellCreativeTab.BAUBLE_TAB));
    }

    @Override
    public boolean isFoil(@Nonnull ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(@Nonnull ItemStack stack, @Nullable Level level, @Nonnull List<Component> tooltip, @Nonnull TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        TooltipHelper.addShiftTooltip(tooltip, List.of(new TranslatableComponent((String)"item.maidspell.chaos_book.desc1").withStyle(ChatFormatting.DARK_PURPLE)), List.of(new TranslatableComponent((String)"item.maidspell.chaos_book.desc2", (Object[])new Object[]{String.format("%d", Config.chaosBookDamageSplitCount), String.format("%.0f", Config.chaosBookMinSplitDamage)}).withStyle(ChatFormatting.RED), new TranslatableComponent((String)"item.maidspell.chaos_book.desc3", (Object[])new Object[]{String.format("%.0f", Config.chaosBookTrueDamagePercent * 100.0), String.format("%.0f", Config.chaosBookTrueDamageMin)}).withStyle(ChatFormatting.GRAY)));
    }
}


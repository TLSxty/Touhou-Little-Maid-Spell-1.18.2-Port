// 移植自 1.9.2.1 反编译源 item/bauble/woundRimeBlade/WoundRimeBlade.java；SRG 名已替换为 1.18.2 official 名。
package com.github.yimeng261.maidspell.item.bauble.woundRimeBlade;

import com.github.yimeng261.maidspell.item.MaidSpellCreativeTab;

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

public class WoundRimeBlade
extends Item {
    public WoundRimeBlade() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).tab(MaidSpellCreativeTab.BAUBLE_TAB));
    }

    @Override
    public boolean isFoil(@Nonnull ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(@Nonnull ItemStack stack, @Nullable Level level, @Nonnull List<Component> tooltip, @Nonnull TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        TooltipHelper.addShiftTooltip(tooltip, List.of(new TranslatableComponent((String)"item.maidspell.wound_rime_blade.desc1").withStyle(ChatFormatting.GRAY), new TranslatableComponent((String)"item.maidspell.wound_rime_blade.desc3").withStyle(ChatFormatting.RED)), List.of(new TranslatableComponent((String)"item.maidspell.wound_rime_blade.desc2").withStyle(ChatFormatting.DARK_RED), new TranslatableComponent((String)"item.maidspell.wound_rime_blade.desc4").withStyle(ChatFormatting.YELLOW)));
    }
}


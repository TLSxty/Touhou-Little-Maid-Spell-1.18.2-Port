// 移植自 1.9.2.1 反编译源 item/bauble/flowCore/FlowCore.java；SRG 名已替换为 1.18.2 official 名。
package com.github.yimeng261.maidspell.item.bauble.flowCore;

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

public class FlowCore
extends Item {
    public FlowCore() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).tab(MaidSpellCreativeTab.BAUBLE_TAB));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        TooltipHelper.addShiftTooltip(tooltip, List.of(new TranslatableComponent((String)"item.maidspell.flow_core.desc1").withStyle(ChatFormatting.GRAY)), List.of(new TranslatableComponent((String)"item.maidspell.flow_core.desc2", (Object[])new Object[]{String.format("%.1f", (double)Config.flowCoreTickInterval / 20.0), String.format("%.1f", Config.flowCoreHealthRegenRate * 100.0)}).withStyle(ChatFormatting.BLUE), new TranslatableComponent((String)"item.maidspell.flow_core.desc3", (Object[])new Object[]{String.format("%.0f", Config.flowCoreDamageReduction * 100.0)}).withStyle(ChatFormatting.YELLOW)));
    }
}


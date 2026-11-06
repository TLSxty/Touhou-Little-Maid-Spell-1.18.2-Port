package com.github.yimeng261.maidspell.item.bauble.springBloomReturn;

import com.github.yimeng261.maidspell.Config;
import com.github.yimeng261.maidspell.item.MaidSpellCreativeTab;
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

/**
 * 春华归返（物品）。
 * <p>
 * 与原文的差异：desc4 一行原本有两个参数（减伤比例、法术冷却退回比例），
 * 后者对应的能力已在批次 A 删除（纯魔法），因此该行只保留减伤比例一个参数。
 * lang 文件里的对应键需同步改为单参数。
 */
public class SpringBloomReturn extends Item {

    public SpringBloomReturn() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)
                .tab(MaidSpellCreativeTab.BAUBLE_TAB));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        TooltipHelper.addShiftTooltip(tooltip,
                List.of(new TranslatableComponent("item.maidspell.spring_bloom_return.desc1")
                        .withStyle(ChatFormatting.GRAY)),
                List.of(
                        new TranslatableComponent("item.maidspell.spring_bloom_return.desc2",
                                Integer.toString(Config.springBloomReturnMaxStacks),
                                String.format("%.0f", (double) Config.springBloomReturnStackDurationTicks / 20.0))
                                .withStyle(ChatFormatting.LIGHT_PURPLE),
                        new TranslatableComponent("item.maidspell.spring_bloom_return.desc3",
                                String.format("%.1f", Config.springBloomReturnDamageThreshold),
                                String.format("%.0f", Config.springBloomReturnDamageThresholdRatio * 100.0))
                                .withStyle(ChatFormatting.AQUA),
                        new TranslatableComponent("item.maidspell.spring_bloom_return.desc4",
                                String.format("%.0f", Config.springBloomReturnHealRatio * 100.0))
                                .withStyle(ChatFormatting.GREEN),
                        new TranslatableComponent("item.maidspell.spring_bloom_return.desc5")
                                .withStyle(ChatFormatting.YELLOW)
                ));
    }
}

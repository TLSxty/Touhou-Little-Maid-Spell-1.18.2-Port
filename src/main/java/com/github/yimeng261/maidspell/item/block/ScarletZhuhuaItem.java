package com.github.yimeng261.maidspell.item.block;

import com.github.yimeng261.maidspell.item.MaidSpellCreativeTab;
import com.github.yimeng261.maidspell.utils.TooltipHelper;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * 猩红朱华的方块物品。
 * <p>
 * 移植自 1.9.2.1 反编译源 item/block/ScarletZhuhuaItem.java（36 行）。
 * <p>
 * <strong>与原文的差异</strong>：{@code Component.m_237115_} → {@link TranslatableComponent}。
 */
public class ScarletZhuhuaItem extends BlockItem {

    public ScarletZhuhuaItem(Block block) {
        super(block, new Item.Properties().tab(MaidSpellCreativeTab.BAUBLE_TAB));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        TooltipHelper.addShiftTooltip(tooltip,
                List.of(new TranslatableComponent("item.maidspell.scarlet_zhuhua.desc1").withStyle(ChatFormatting.DARK_RED)),
                List.of(new TranslatableComponent("item.maidspell.scarlet_zhuhua.desc2").withStyle(ChatFormatting.BLUE),
                        new TranslatableComponent("item.maidspell.scarlet_zhuhua.desc3").withStyle(ChatFormatting.RED)));
    }
}

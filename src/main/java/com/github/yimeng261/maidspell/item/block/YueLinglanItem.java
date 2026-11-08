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
 * 月铃兰的方块物品。
 * <p>
 * 移植自 1.9.2.1 反编译源 item/block/YueLinglanItem.java（40 行）。
 * <p>
 * <strong>与原文的差异</strong>：原文的 {@code m_7373_} 是
 * {@code appendHoverText}、{@code Component.m_237115_} 是
 * {@code Component.translatable}——1.18.2 没有该静态工厂，
 * 改用 {@link TranslatableComponent} 构造器。
 */
public class YueLinglanItem extends BlockItem {

    public YueLinglanItem(Block block) {
        super(block, new Item.Properties().tab(MaidSpellCreativeTab.BAUBLE_TAB));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        TooltipHelper.addShiftTooltip(tooltip,
                List.of(new TranslatableComponent("item.maidspell.yue_linglan.desc1").withStyle(ChatFormatting.GRAY)),
                List.of(new TranslatableComponent("item.maidspell.yue_linglan.desc2").withStyle(ChatFormatting.BLUE),
                        new TranslatableComponent("item.maidspell.yue_linglan.desc3").withStyle(ChatFormatting.AQUA)));
    }
}

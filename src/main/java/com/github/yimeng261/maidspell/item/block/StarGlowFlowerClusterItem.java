package com.github.yimeng261.maidspell.item.block;

import com.github.yimeng261.maidspell.item.MaidSpellCreativeTab;
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
 * 星荧花簇的方块物品。
 * <p>
 * 移植自 1.9.2.1 反编译源 item/block/StarGlowFlowerClusterItem.java（37 行）。
 * 原文只有一行灰色说明，不走 Shift 展开。
 * <p>
 * <strong>与原文的差异</strong>：{@code Component.m_237115_} → {@link TranslatableComponent}。
 */
public class StarGlowFlowerClusterItem extends BlockItem {

    public StarGlowFlowerClusterItem(Block block) {
        super(block, new Item.Properties().tab(MaidSpellCreativeTab.BAUBLE_TAB));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(new TranslatableComponent("item.touhou_little_maid_spell.star_glow_flower_cluster.desc")
                .withStyle(ChatFormatting.GRAY));
    }
}

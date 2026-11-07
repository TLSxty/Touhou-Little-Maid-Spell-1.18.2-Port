package com.github.yimeng261.maidspell.item.bauble.moltenFoxLeaf;

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
 * 熔岩狐叶（物品本体）。
 * <p>
 * 移植自 1.9.2.1 反编译源 item/bauble/moltenFoxLeaf/MoltenFoxLeaf.java（43 行）。
 */
public class MoltenFoxLeaf extends Item {

    public MoltenFoxLeaf() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)
                .tab(MaidSpellCreativeTab.BAUBLE_TAB));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        TooltipHelper.addShiftTooltip(tooltip,
                List.of(new TranslatableComponent("item.maidspell.molten_fox_leaf.desc1")
                        .withStyle(ChatFormatting.GRAY)),
                List.of(new TranslatableComponent("item.maidspell.molten_fox_leaf.desc2")
                                .withStyle(ChatFormatting.GOLD),
                        new TranslatableComponent("item.maidspell.molten_fox_leaf.desc3")
                                .withStyle(ChatFormatting.RED),
                        new TranslatableComponent("item.maidspell.molten_fox_leaf.desc4")
                                .withStyle(ChatFormatting.YELLOW)));
    }
}

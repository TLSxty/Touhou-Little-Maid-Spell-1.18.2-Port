package com.github.yimeng261.maidspell.item.bauble.dreamCatCrystal;

import com.github.yimeng261.maidspell.Config;
import com.github.yimeng261.maidspell.item.MaidSpellCreativeTab;
import com.github.yimeng261.maidspell.utils.TooltipHelper;
import java.util.ArrayList;
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

/**
 * 云梦水晶（物品）。
 * <p>
 * 移植自 1.9.2.1 反编译源。与原文的差异：
 * <ul>
 *   <li>删掉 {@code if (IronsSpellbooksCompat.isLoaded())} 追加 {@code desc5_iss} 那一行 —— 铁魔法不启用。</li>
 *   <li>1.20 的 {@code Component.m_237115_(key).m_130940_(fmt)} 改写为
 *       {@code new TranslatableComponent(key).withStyle(fmt)}。</li>
 *   <li>{@code desc9_true_damage} 一行保留：该能力依赖 Mixin，本次不生效，见
 *       {@code DreamCatCrystalBauble} 里的 TODO(批次B)。</li>
 * </ul>
 */
public class DreamCatCrystal extends Item {

    public DreamCatCrystal() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)
                .tab(MaidSpellCreativeTab.BAUBLE_TAB));
    }

    @Override
    public boolean isFoil(@Nonnull ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(@Nonnull ItemStack stack, @Nullable Level level,
                                @Nonnull List<Component> tooltip, @Nonnull TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        List<Component> detailLines = new ArrayList<>();
        detailLines.add(new TranslatableComponent("item.touhou_little_maid_spell.dream_cat_crystal.desc5_base")
                .withStyle(ChatFormatting.GOLD));
        detailLines.add(new TranslatableComponent("item.touhou_little_maid_spell.dream_cat_crystal.desc6")
                .withStyle(ChatFormatting.YELLOW));
        detailLines.add(new TranslatableComponent("item.touhou_little_maid_spell.dream_cat_crystal.desc7")
                .withStyle(ChatFormatting.GREEN));
        detailLines.add(new TranslatableComponent("item.touhou_little_maid_spell.dream_cat_crystal.desc8")
                .withStyle(ChatFormatting.AQUA));
        if (Config.dreamCrystalExtraTrueDamageEnabled) {
            detailLines.add(new TranslatableComponent("item.touhou_little_maid_spell.dream_cat_crystal.desc9_true_damage")
                    .withStyle(ChatFormatting.RED));
        }
        detailLines.add(new TranslatableComponent("item.touhou_little_maid_spell.dream_cat_crystal.desc9_splash")
                .withStyle(ChatFormatting.RED));
        detailLines.add(new TranslatableComponent("item.touhou_little_maid_spell.dream_cat_crystal.desc10")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        detailLines.add(new TranslatableComponent("item.touhou_little_maid_spell.dream_cat_crystal.desc11")
                .withStyle(ChatFormatting.BLUE));
        detailLines.add(new TranslatableComponent("item.touhou_little_maid_spell.dream_cat_crystal.desc12")
                .withStyle(ChatFormatting.BLUE));
        TooltipHelper.addShiftTooltip(tooltip, List.of(
                new TranslatableComponent("item.touhou_little_maid_spell.dream_cat_crystal.desc1")
                        .withStyle(ChatFormatting.AQUA),
                new TranslatableComponent("item.touhou_little_maid_spell.dream_cat_crystal.desc2")
                        .withStyle(ChatFormatting.LIGHT_PURPLE),
                new TranslatableComponent("item.touhou_little_maid_spell.dream_cat_crystal.desc3")
                        .withStyle(ChatFormatting.GRAY),
                new TranslatableComponent("item.touhou_little_maid_spell.dream_cat_crystal.desc4")
                        .withStyle(ChatFormatting.DARK_PURPLE)
        ), detailLines);
    }
}

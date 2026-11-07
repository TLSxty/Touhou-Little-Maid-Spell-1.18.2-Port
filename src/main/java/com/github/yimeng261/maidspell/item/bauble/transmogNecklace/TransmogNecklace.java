package com.github.yimeng261.maidspell.item.bauble.transmogNecklace;

import com.github.yimeng261.maidspell.client.gui.TransmogNecklaceScreen;
import com.github.yimeng261.maidspell.item.MaidSpellCreativeTab;
import com.github.yimeng261.maidspell.utils.TooltipHelper;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;

/**
 * 幻化项链：手持右键打开样式选择界面，女仆装备时在头顶渲染所选光环。
 */
public class TransmogNecklace extends Item {

    public static final String NBT_SELECTED_STYLE = "transmog_halo_style";

    public TransmogNecklace() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.RARE).tab(MaidSpellCreativeTab.BAUBLE_TAB));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        // 1.18.2 没有 Inventory.f_35977_ 这种常量名，直接用 selected 字段
        int inventorySlot = hand == InteractionHand.MAIN_HAND
                ? player.getInventory().selected
                : 40;
        if (level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> openClientScreen(inventorySlot, getSelectedStyle(stack)));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @OnlyIn(Dist.CLIENT)
    private static void openClientScreen(int inventorySlot, TransmogHaloStyle currentStyle) {
        Minecraft.getInstance().setScreen(new TransmogNecklaceScreen(inventorySlot, currentStyle));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        TransmogHaloStyle style = getSelectedStyle(stack);
        tooltip.add(new TranslatableComponent(
                "item.touhou_little_maid_spell.transmog_necklace.current_style",
                style.getDisplayName()).withStyle(ChatFormatting.AQUA));
        TooltipHelper.addShiftTooltip(tooltip,
                List.of(new TranslatableComponent(
                        "item.touhou_little_maid_spell.transmog_necklace.desc1")
                        .withStyle(ChatFormatting.LIGHT_PURPLE)),
                List.of(new TranslatableComponent(
                                "item.touhou_little_maid_spell.transmog_necklace.desc2")
                                .withStyle(ChatFormatting.GRAY),
                        new TranslatableComponent(
                                "item.touhou_little_maid_spell.transmog_necklace.desc3")
                                .withStyle(ChatFormatting.BLUE),
                        new TranslatableComponent(
                                "item.touhou_little_maid_spell.transmog_necklace.desc4")
                                .withStyle(ChatFormatting.YELLOW)));
    }

    public static TransmogHaloStyle getSelectedStyle(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(NBT_SELECTED_STYLE)) {
            return TransmogHaloStyle.DEFAULT;
        }
        return TransmogHaloStyle.byId(tag.getString(NBT_SELECTED_STYLE));
    }

    public static void setSelectedStyle(ItemStack stack, TransmogHaloStyle style) {
        stack.getOrCreateTag().putString(NBT_SELECTED_STYLE, style.id());
    }

    public static void setSelectedStyle(ItemStack stack, int styleIndex) {
        setSelectedStyle(stack, TransmogHaloStyle.byIndex(styleIndex));
    }
}

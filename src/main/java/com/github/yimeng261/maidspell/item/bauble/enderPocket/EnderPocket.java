package com.github.yimeng261.maidspell.item.bauble.enderPocket;

import com.github.yimeng261.maidspell.item.MaidSpellCreativeTab;
import com.github.yimeng261.maidspell.utils.TooltipHelper;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

/**
 * 末影腰包（降级版）。
 *
 * <p><b>与上游的差异（用户 m04639 决策）</b>：上游把腰包做成女仆饰品，并在女仆容器界面里
 * 挂一个「打开末影背包」的按钮（依赖 {@code MaidContainerGuiEvent}），外加一整套自定义 GUI
 * 与网络包。TLM 1.2.2 没有 {@code api/event/client/MaidContainerGuiEvent}（实测该包下只有
 * {@code AddClothConfigEvent} / {@code OpenPatchouliBookEvent} / {@code RenderMaidEvent}），
 * 无法给女仆容器挂入口，因此本工程把它降级为「手持右键直接打开玩家末影箱」。</p>
 *
 * <p>副作用：它不再是女仆饰品，{@code MaidBaubleRegistry} 里没有为它 bind。</p>
 */
public class EnderPocket extends Item {

    public EnderPocket() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.RARE).tab(MaidSpellCreativeTab.BAUBLE_TAB));
    }

    @Override
    public boolean isFoil(@Nonnull ItemStack stack) {
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openGui(serverPlayer, new SimpleMenuProvider(
                    (containerId, inventory, owner) -> ChestMenu.threeRows(
                            containerId, inventory, owner.getEnderChestInventory()),
                    new TranslatableComponent("container.enderchest")));
            level.playSound(null, player.blockPosition(), SoundEvents.ENDER_CHEST_OPEN,
                    SoundSource.PLAYERS, 0.6f, 1.0f);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(@Nonnull ItemStack stack, @Nullable Level level,
                                @Nonnull List<Component> tooltip, @Nonnull TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        TooltipHelper.addShiftTooltip(tooltip,
                List.of(new TranslatableComponent("item.maidspell.ender_pocket.desc1").withStyle(ChatFormatting.GRAY)),
                List.of(new TranslatableComponent("item.maidspell.ender_pocket.desc2").withStyle(ChatFormatting.DARK_PURPLE),
                        new TranslatableComponent("item.maidspell.ender_pocket.desc3").withStyle(ChatFormatting.GOLD)));
    }
}

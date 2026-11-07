package com.github.yimeng261.maidspell.client.event;

import com.github.yimeng261.maidspell.Config;
import com.github.yimeng261.maidspell.item.MaidSpellItems;
import com.github.yimeng261.maidspell.item.bauble.transmogNecklace.TransmogHaloStyle;
import com.github.yimeng261.maidspell.item.bauble.transmogNecklace.TransmogNecklace;
import com.github.yimeng261.maidspell.network.NetworkHandler;
import com.github.yimeng261.maidspell.network.message.TransmogNecklaceMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 容器界面里按住左 Shift 滚轮切换幻化项链样式。
 *
 * <p>1.18.2 的事件名是 {@code ScreenEvent.MouseScrollEvent.Pre}，
 * 不是 1.19+ 的 {@code MouseScrolled.Pre}。</p>
 */
@Mod.EventBusSubscriber(modid = Config.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class TransmogNecklaceClientEvent {

    private TransmogNecklaceClientEvent() {
    }

    @SubscribeEvent
    public static void onMouseScrolled(ScreenEvent.MouseScrollEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !Screen.hasShiftDown()) {
            return;
        }
        Screen screen = event.getScreen();
        if (!(screen instanceof AbstractContainerScreen<?> containerScreen)) {
            return;
        }
        Slot hoveredSlot = containerScreen.getSlotUnderMouse();
        if (hoveredSlot == null || hoveredSlot.container != mc.player.getInventory()) {
            return;
        }
        ItemStack stack = hoveredSlot.getItem();
        if (!stack.is(MaidSpellItems.TRANSMOG_NECKLACE.get())) {
            return;
        }
        int inventorySlot = hoveredSlot.getContainerSlot();
        if (inventorySlot < 0) {
            return;
        }
        TransmogHaloStyle currentStyle = TransmogNecklace.getSelectedStyle(stack);
        TransmogHaloStyle nextStyle = event.getScrollDelta() > 0.0
                ? currentStyle.previous()
                : currentStyle.next();
        TransmogNecklace.setSelectedStyle(stack, nextStyle);
        NetworkHandler.CHANNEL.sendToServer(
                new TransmogNecklaceMessage(inventorySlot, nextStyle.getSerializedIndex()));
        mc.player.displayClientMessage(new TranslatableComponent(
                "item.touhou_little_maid_spell.transmog_necklace.current_style",
                nextStyle.getDisplayName()), true);
        mc.player.playSound(SoundEvents.UI_BUTTON_CLICK, 0.45f, 1.1f);
        event.setCanceled(true);
    }
}

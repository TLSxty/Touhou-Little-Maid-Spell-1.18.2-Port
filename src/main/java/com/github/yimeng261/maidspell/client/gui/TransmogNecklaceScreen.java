package com.github.yimeng261.maidspell.client.gui;

import com.github.yimeng261.maidspell.item.bauble.transmogNecklace.TransmogHaloStyle;
import com.github.yimeng261.maidspell.network.NetworkHandler;
import com.github.yimeng261.maidspell.network.message.TransmogNecklaceMessage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.sounds.SoundEvents;

/**
 * 幻化项链的样式选择界面。
 *
 * <p>上游基于 1.20 的 {@code GuiGraphics}，本工程按 1.18.2 的 {@link PoseStack} 时代重写：
 * 所有 fill / blit / 文本绘制都走 {@link GuiComponent} 的静态方法，描边用 4 条 fill 拼出来。</p>
 */
public class TransmogNecklaceScreen extends Screen {

    private static final int PANEL_WIDTH = 196;
    private static final int PANEL_HEIGHT = 104;
    private static final int CARD_SIZE = 42;

    private final int inventorySlot;
    private TransmogHaloStyle selectedStyle;
    private final List<StyleButton> styleButtons = new ArrayList<>();

    public TransmogNecklaceScreen(int inventorySlot, TransmogHaloStyle selectedStyle) {
        super(new TranslatableComponent("gui.maidspell.transmog_necklace.title"));
        this.inventorySlot = inventorySlot;
        this.selectedStyle = selectedStyle;
    }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets();
        this.styleButtons.clear();
        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;
        int startX = left + 20;
        int startY = top + 38;
        int gap = 54;
        for (int i = 0; i < TransmogHaloStyle.values().length; i++) {
            TransmogHaloStyle style = TransmogHaloStyle.byIndex(i);
            StyleButton button = new StyleButton(startX + i * gap, startY, style);
            this.styleButtons.add(button);
            this.addRenderableWidget(button);
        }
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(poseStack);
        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;
        // 上游用 GuiGraphics.fillGradient + renderOutline；1.18.2 用两次 fillGradient/fill 拼
        fillGradient(poseStack, left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, 0xE0202124, 0xF0101012);
        drawBorder(poseStack, left, top, PANEL_WIDTH, PANEL_HEIGHT, 0xFFCC9A3F);
        GuiComponent.drawCenteredString(poseStack, this.font, this.title,
                left + PANEL_WIDTH / 2, top + 10, 0xFFF7D08A);
        GuiComponent.drawCenteredString(poseStack, this.font,
                new TranslatableComponent("gui.maidspell.transmog_necklace.hint"),
                left + PANEL_WIDTH / 2, top + 22, 0xFFD6C4A8);
        super.render(poseStack, mouseX, mouseY, partialTick);
        for (StyleButton button : this.styleButtons) {
            if (!button.isHoveredOrFocused()) {
                continue;
            }
            this.renderTooltip(poseStack, button.style.getDisplayName(), mouseX, mouseY);
            break;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (Screen.hasShiftDown()) {
            TransmogHaloStyle next = delta > 0.0 ? this.selectedStyle.previous() : this.selectedStyle.next();
            this.selectStyle(next);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void selectStyle(TransmogHaloStyle style) {
        if (this.selectedStyle == style) {
            return;
        }
        this.selectedStyle = style;
        NetworkHandler.CHANNEL.sendToServer(new TransmogNecklaceMessage(this.inventorySlot, style.getSerializedIndex()));
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.displayClientMessage(new TranslatableComponent(
                    "item.touhou_little_maid_spell.transmog_necklace.current_style",
                    style.getDisplayName()), true);
            this.minecraft.player.playSound(SoundEvents.UI_BUTTON_CLICK, 0.5f, 1.2f);
        }
    }

    /** 1.18.2 的 Screen 没有 renderOutline，用 4 条 fill 画边框。 */
    private static void drawBorder(PoseStack poseStack, int x, int y, int width, int height, int color) {
        GuiComponent.fill(poseStack, x, y, x + width, y + 1, color);
        GuiComponent.fill(poseStack, x, y + height - 1, x + width, y + height, color);
        GuiComponent.fill(poseStack, x, y, x + 1, y + height, color);
        GuiComponent.fill(poseStack, x + width - 1, y, x + width, y + height, color);
    }

    private class StyleButton extends Button {

        private final TransmogHaloStyle style;

        private StyleButton(int x, int y, TransmogHaloStyle style) {
            super(x, y, CARD_SIZE, CARD_SIZE, style.getDisplayName(),
                    button -> TransmogNecklaceScreen.this.selectStyle(style), Button.NO_TOOLTIP);
            this.style = style;
        }

        @Override
        public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
            boolean isSelected = TransmogNecklaceScreen.this.selectedStyle == this.style;
            int background = isSelected ? 0xD04A6B2E : (this.isHoveredOrFocused() ? 0xB0304A8C : 0xA01A1A1C);
            int border = isSelected ? 0xFFF5D061 : 0xFFAFA383;
            GuiComponent.fill(poseStack, this.x, this.y, this.x + this.width, this.y + this.height, background);
            drawBorder(poseStack, this.x, this.y, this.width, this.height, border);
            RenderSystem.enableBlend();
            RenderSystem.setShaderTexture(0, this.style.iconTexture());
            GuiComponent.blit(poseStack, this.x + 5, this.y + 5, 0.0f, 0.0f, 32, 32, 32, 32);
            RenderSystem.disableBlend();
            if (isSelected) {
                GuiComponent.drawCenteredString(poseStack, Minecraft.getInstance().font,
                        new TranslatableComponent("gui.maidspell.transmog_necklace.selected"),
                        this.x + this.width / 2, this.y + this.height - 10, 0xFFFFF3B0);
            }
        }
    }
}

package com.github.yimeng261.maidspell.utils;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;

/**
 * 物品 tooltip 的「按住 Shift 展开」工具。
 * <p>
 * 移植自 1.9.2.1 反编译源 utils/TooltipHelper.java。
 * 1.20 的 {@code Component.m_237110_ / m_237117_} 静态工厂在 1.18.2 不存在，
 * 这里改用 1.18.2 的构造器写法 {@link TranslatableComponent} / {@link TextComponent}。
 * <p>
 * 注意：这里引用 {@link Screen}（客户端类）。原模组同样在通用侧引用它，
 * 因为 tooltip 只在客户端调用链上执行，Forge 侧不会在服务端加载本方法。
 */
public final class TooltipHelper {

    private TooltipHelper() {
    }

    public static void addShiftTooltip(List<Component> tooltip, List<Component> flavorLines, List<Component> detailLines) {
        tooltip.addAll(flavorLines);
        if (detailLines.isEmpty()) {
            return;
        }
        if (Screen.hasShiftDown()) {
            tooltip.addAll(detailLines);
            return;
        }
        tooltip.add(new TranslatableComponent("tooltip.touhou_little_maid_spell.hold_shift",
                new TextComponent("key.keyboard.left.shift").withStyle(ChatFormatting.YELLOW))
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}

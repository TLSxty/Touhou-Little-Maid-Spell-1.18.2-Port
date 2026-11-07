package com.github.yimeng261.maidspell.item.bauble.transmogNecklace;

import com.github.tartaricacid.touhoulittlemaid.api.bauble.IMaidBauble;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.inventory.handler.BaubleItemHandler;
import com.github.yimeng261.maidspell.item.MaidSpellItems;
import net.minecraft.world.item.ItemStack;

/**
 * 幻化项链的女仆侧。
 *
 * <p>它本身不做事：光环由客户端渲染层读取饰品栏里的项链来决定。
 * 上游有一个 {@code syncClient} 覆写，但 TLM 1.2.2 的 IMaidBauble 没有该方法，
 * 已随其余饰品一并移除。</p>
 */
public class TransmogNecklaceBauble implements IMaidBauble {

    public static ItemStack findTransmogNecklace(EntityMaid maid) {
        BaubleItemHandler handler = maid.getMaidBauble();
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (stack.is(MaidSpellItems.TRANSMOG_NECKLACE.get())) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }
}

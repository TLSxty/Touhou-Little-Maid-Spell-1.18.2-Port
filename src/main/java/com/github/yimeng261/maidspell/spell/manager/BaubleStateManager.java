package com.github.yimeng261.maidspell.spell.manager;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.RegistryObject;

/**
 * 判断女仆饰品栏（9 格）里是否装着某个饰品。
 * <p>
 * 移植自 1.9.2.1 反编译源 spell/manager/BaubleStateManager.java。
 * <strong>改动点</strong>：原实现调用 {@code maid.getMaidBauble().containsItem(item)}，
 * 但 1.18.2 Forge 的 {@code ItemStackHandler} 没有 {@code containsItem} 方法
 * （1.20 才加，见 docs/已验证结论.md），这里改成显式遍历槽位比对物品。
 */
public class BaubleStateManager {

    private BaubleStateManager() {
    }

    public static boolean hasBauble(EntityMaid maid, ItemStack stack) {
        return hasBauble(maid, stack.getItem());
    }

    public static boolean hasBauble(EntityMaid maid, RegistryObject<Item> item) {
        return hasBauble(maid, item.get());
    }

    public static boolean hasBauble(EntityMaid maid, Item item) {
        if (maid == null || item == null) {
            return false;
        }
        for (int i = 0; i < maid.getMaidBauble().getSlots(); ++i) {
            if (maid.getMaidBauble().getStackInSlot(i).getItem() == item) {
                return true;
            }
        }
        return false;
    }
}

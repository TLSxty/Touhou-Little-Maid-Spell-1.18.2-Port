package com.github.yimeng261.maidspell.item;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;

/**
 * 创造模式物品栏分页（批次 A）。
 * <p>
 * 1.18.2 的 {@link CreativeModeTab} 在构造时自动把自身登记进 {@code CreativeModeTab.TABS}
 * （构造器内部调用 {@code addGroupSafe}），因此这里用匿名子类 + {@code makeIcon()} 建一个分页即可，
 * 不需要像 1.19+ 那样走 {@code DeferredRegister}。
 * <p>
 * 与原文的差异：原文有两个分页（饰品 / 杂项），批次 A 只有两个饰品物品，因此只建饰品页；
 * 杂项页（星陨石、星芒短剑等）留到批次 B 随物品一起补。
 * <p>
 * 图标用云梦水晶，{@code makeIcon()} 在注册完成之后才被调用，此处取 {@link RegistryObject#get()}
 * 是安全的（不会在静态初始化阶段提前解析注册表）。
 */
public final class MaidSpellCreativeTab {

    public static final CreativeModeTab BAUBLE_TAB = new CreativeModeTab("touhou_little_maid_spell") {
        @Override
        @Nonnull
        public ItemStack makeIcon() {
            return new ItemStack(MaidSpellItems.DREAM_CAT_CRYSTAL.get());
        }
    };

    private MaidSpellCreativeTab() {
    }
}

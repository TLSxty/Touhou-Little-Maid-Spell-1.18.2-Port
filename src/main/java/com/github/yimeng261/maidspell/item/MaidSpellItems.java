package com.github.yimeng261.maidspell.item;

import com.github.yimeng261.maidspell.Config;
import com.github.yimeng261.maidspell.item.bauble.dreamCatCrystal.DreamCatCrystal;
import com.github.yimeng261.maidspell.item.bauble.springBloomReturn.SpringBloomReturn;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 物品注册表（批次 A 精简版）。
 * <p>
 * 移植自 1.9.2.1 反编译源 item/MaidSpellItems.java。原文注册了 40 个物品，
 * 本批次只注册云梦水晶与春华归返两个饰品。
 * <p>
 * 与原文的差异：
 * <ul>
 *   <li>删掉 {@code SPELL_ENHANCEMENT_CORE / ARC_CROSS} 与 35 个 Goety curio 相关注册。</li>
 *   <li>删掉 {@code ensureGoetyItemsResolved() / getUnholyHat() / getUnholyHatHalo()}：
 *       这三段用反射从 {@code com.Polarice3.Goety.common.items.ModItems} 取不洁圣冠物品，
 *       是「UNHOLY_HAT 的 goety 依赖」的实际所在。批次 A 不启用 Goety，不洁圣冠按计划放弃，
 *       因此整段删除；若将来要恢复非魔法部分，需另建独立物品而非反射外部模组物品。</li>
 *   <li>其余 20 余个饰品与材料物品（星陨石、星芒短剑、抑制石……）属批次 B，搬时再补。</li>
 * </ul>
 */
public class MaidSpellItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, Config.MOD_ID);

    // ---- 批次 A：云梦水晶 + 春华归返 ----
    public static final RegistryObject<Item> DREAM_CAT_CRYSTAL =
            ITEMS.register("dream_cat_crystal", DreamCatCrystal::new);
    public static final RegistryObject<Item> SPRING_BLOOM_RETURN =
            ITEMS.register("spring_bloom_return", SpringBloomReturn::new);

    // TODO(批次B)：以下是计划文档 §2.1 里保留、但本批次未搬的饰品，搬时在此补注册并同步
    //  MaidBaubleRegistry.bindMaidBauble 与对应的 bauble/item 类：
    //  bleeding_heart / flow_core / spring_ring / double_heart_chain / rock_crystal / hairpin
    //  transmog_necklace / chaos_book / soul_book / ender_pocket / floating_fox_leaf
    //  molten_fox_leaf / wound_rime_blade / anchor_core / fragrant_ingenuity / staranchor_pearl

    private MaidSpellItems() {
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}

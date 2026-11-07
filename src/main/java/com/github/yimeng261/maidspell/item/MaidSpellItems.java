package com.github.yimeng261.maidspell.item;

import com.github.yimeng261.maidspell.Config;
import com.github.yimeng261.maidspell.item.bauble.bleedingHeart.BleedingHeart;
import com.github.yimeng261.maidspell.item.bauble.chaosBook.ChaosBook;
import com.github.yimeng261.maidspell.item.bauble.doubleHeartChain.DoubleHeartChain;
import com.github.yimeng261.maidspell.item.bauble.dreamCatCrystal.DreamCatCrystal;
import com.github.yimeng261.maidspell.item.bauble.flowCore.FlowCore;
import com.github.yimeng261.maidspell.item.bauble.floatingFoxLeaf.FloatingFoxLeaf;
import com.github.yimeng261.maidspell.item.bauble.fragrantIngenuity.FragrantIngenuity;
import com.github.yimeng261.maidspell.item.bauble.hairpin.Hairpin;
import com.github.yimeng261.maidspell.item.bauble.moltenFoxLeaf.MoltenFoxLeaf;
import com.github.yimeng261.maidspell.item.bauble.rockCrystal.RockCrystal;
import com.github.yimeng261.maidspell.item.bauble.silverCercis.SilverCercis;
import com.github.yimeng261.maidspell.item.bauble.soulBook.SoulBook;
import com.github.yimeng261.maidspell.item.bauble.springBloomReturn.SpringBloomReturn;
import com.github.yimeng261.maidspell.item.bauble.springRing.SpringRing;
import com.github.yimeng261.maidspell.item.bauble.staranchorPearl.StaranchorPearl;
import com.github.yimeng261.maidspell.item.bauble.transmogNecklace.TransmogNecklace;
import com.github.yimeng261.maidspell.item.bauble.woundRimeBlade.WoundRimeBlade;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 物品注册表。
 * <p>
 * 移植自 1.9.2.1 反编译源 item/MaidSpellItems.java。原文注册了 40 个物品；
 * 本工程只注册计划文档 §2.1 保留清单里的饰品，放弃清单（法术强化核心 / 电弧十字 /
 * 急速咏唱戒指 / 法术白名单 / 法术超限核心 / 法术射程仪 / 不洁圣冠 / 35 个 Goety curio）
 * 与纯材料物品（星陨石、星芒短剑、抑制石……）一律不注册。
 * <p>
 * 与原文的差异：
 * <ul>
 *   <li>删掉 {@code SPELL_ENHANCEMENT_CORE / ARC_CROSS} 与 35 个 Goety curio 相关注册。</li>
 *   <li>删掉 {@code ensureGoetyItemsResolved() / getUnholyHat() / getUnholyHatHalo()}：
 *       这三段用反射从 {@code com.Polarice3.Goety.common.items.ModItems} 取不洁圣冠物品，
 *       是「UNHOLY_HAT 的 goety 依赖」的实际所在。本工程不启用 Goety，不洁圣冠按计划放弃。</li>
 *   <li>锚定核心（ANCHOR_CORE）暂未注册：它依赖 {@code ChunkLoadingManager}（1407 行），
 *       而该类硬引用 the_retreat 维度链（{@code PlayerRetreatManager}）。该维度已按用户决策
 *       拆到 P3 之前单独一轮做，届时一并恢复锚定核心。见 TODO(锚定核心)。</li>
 * </ul>
 */
public class MaidSpellItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, Config.MOD_ID);

    // ---- 批次 A ----
    public static final RegistryObject<Item> DREAM_CAT_CRYSTAL =
            ITEMS.register("dream_cat_crystal", DreamCatCrystal::new);
    public static final RegistryObject<Item> SPRING_BLOOM_RETURN =
            ITEMS.register("spring_bloom_return", SpringBloomReturn::new);

    // ---- 批次 B ----
    public static final RegistryObject<Item> BLEEDING_HEART =
            ITEMS.register("bleeding_heart", BleedingHeart::new);
    public static final RegistryObject<Item> FLOW_CORE =
            ITEMS.register("flow_core", FlowCore::new);
    public static final RegistryObject<Item> SPRING_RING =
            ITEMS.register("spring_ring", SpringRing::new);
    public static final RegistryObject<Item> DOUBLE_HEART_CHAIN =
            ITEMS.register("double_heart_chain", DoubleHeartChain::new);
    public static final RegistryObject<Item> ROCK_CRYSTAL =
            ITEMS.register("rock_crystal", RockCrystal::new);
    public static final RegistryObject<Item> HAIRPIN =
            ITEMS.register("hairpin", Hairpin::new);
    public static final RegistryObject<Item> CHAOS_BOOK =
            ITEMS.register("chaos_book", ChaosBook::new);
    public static final RegistryObject<Item> SOUL_BOOK =
            ITEMS.register("soul_book", SoulBook::new);
    public static final RegistryObject<Item> SLIVER_CERCIS =
            ITEMS.register("sliver_cercis", SilverCercis::new);
    public static final RegistryObject<Item> WOUND_RIME_BLADE =
            ITEMS.register("wound_rime_blade", WoundRimeBlade::new);
    public static final RegistryObject<Item> FRAGRANT_INGENUITY =
            ITEMS.register("fragrant_ingenuity", FragrantIngenuity::new);

    // ---- 批次 B 第三档（遗留项） ----
    public static final RegistryObject<Item> STARANCHOR_PEARL =
            ITEMS.register("staranchor_pearl", StaranchorPearl::new);
    public static final RegistryObject<Item> FLOATING_FOX_LEAF =
            ITEMS.register("floating_fox_leaf", FloatingFoxLeaf::new);
    public static final RegistryObject<Item> MOLTEN_FOX_LEAF =
            ITEMS.register("molten_fox_leaf", MoltenFoxLeaf::new);
    public static final RegistryObject<Item> TRANSMOG_NECKLACE =
            ITEMS.register("transmog_necklace", TransmogNecklace::new);

    // TODO(批次B 后续两档)：以下饰品需要连带搬入较大支撑类，本次未注册：
    //  · ender_pocket         → 按用户决策降级为「右键物品直接开末影箱」，不做女仆饰品
    //  · anchor_core          → 需要 ChunkLoadingManager（连带 the_retreat 维度链）

    private MaidSpellItems() {
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}

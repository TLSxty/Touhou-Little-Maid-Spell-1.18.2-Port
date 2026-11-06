package com.github.yimeng261.maidspell.item;

import com.github.tartaricacid.touhoulittlemaid.api.ILittleMaid;
import com.github.tartaricacid.touhoulittlemaid.api.LittleMaidExtension;
import com.github.tartaricacid.touhoulittlemaid.item.bauble.BaubleManager;
import com.github.yimeng261.maidspell.item.bauble.dreamCatCrystal.DreamCatCrystalBauble;
import com.github.yimeng261.maidspell.item.bauble.springBloomReturn.SpringBloomReturnBauble;

/**
 * 女仆饰品注册（批次 A 瘦身版）。
 *
 * <p>移植自 1.9.2.1 反编译源 item/MaidBaubleRegistry.java（153 行）。</p>
 *
 * <p><b>删掉的内容</b>：
 * <ul>
 *   <li>{@code SPELL_ENHANCEMENT_CORE}（法术强化核心）与 {@code ARC_CROSS}（电弧十字）两条 bind ——
 *       原实现外层是 {@code IronsSpellbooksCompat.isLoaded()} 门控，铁魔法不启用，按计划放弃。</li>
 *   <li>整组 35 个 Goety curio 的 bind（{@code GOETY_CURIO_ITEM_IDS} + {@code bindGoetyCurios}）——
 *       Goety 不启用、不移植。</li>
 *   <li>{@code UNHOLY_HAT / UNHOLY_HAT_HALO} 的 bind —— 这两个物品原本是反射 Goety 的
 *       {@code com.Polarice3.Goety.common.items.ModItems} 取得的，属 goety 依赖，按计划放弃。</li>
 *   <li>其余 19 个保留饰品（流血之心、流心核心……）的 bind 一并注释成 TODO(批次B)。</li>
 * </ul>
 *
 * <p><b>零改动的部分</b>：{@code @LittleMaidExtension} + {@code implements ILittleMaid} +
 * {@code bindMaidBauble(BaubleManager)} 这套注册机制在 TLM 1.2.2 上完全可用，
 * 由 {@code BaubleManager.init()} 通过 {@code TouhouLittleMaid.EXTENSIONS} 遍历调用。</p>
 *
 * <p><b>注意</b>：{@code BaubleManager.init()} 末尾会把内部 Map 换成 {@code ImmutableMap}，
 * 因此 {@code manager.bind(...)} 只能在 init 期间调用，不能延迟到运行期。</p>
 */
@LittleMaidExtension
public class MaidBaubleRegistry implements ILittleMaid {

    @Override
    public void bindMaidBauble(BaubleManager manager) {
        // ---- 批次 A：只有这两个饰品 ---- 
        manager.bind(MaidSpellItems.DREAM_CAT_CRYSTAL, new DreamCatCrystalBauble());
        manager.bind(MaidSpellItems.SPRING_BLOOM_RETURN, new SpringBloomReturnBauble());

        // TODO(批次B)：以下是计划文档 §2.1 保留、本批次未搬的 19 项饰品，
        //  搬入对应 bauble/item 类与 MaidSpellItems 注册后，在此逐条恢复：
        //  manager.bind(MaidSpellItems.BLEEDING_HEART, new BleedingHeartBauble());
        //  manager.bind(MaidSpellItems.FLOW_CORE, new FlowCoreBauble());
        //  manager.bind(MaidSpellItems.SPRING_RING, new SpringBauble());
        //  manager.bind(MaidSpellItems.DOUBLE_HEART_CHAIN, new DoubleHeartChainBauble());
        //  manager.bind(MaidSpellItems.ROCK_CRYSTAL, new RockCrystalBauble());
        //  manager.bind(MaidSpellItems.HAIRPIN, new HairpinBauble());
        //  manager.bind(MaidSpellItems.TRANSMOG_NECKLACE, new TransmogNecklaceBauble());
        //  manager.bind(MaidSpellItems.CHAOS_BOOK, new ChaosBookBauble());
        //  manager.bind(MaidSpellItems.SOUL_BOOK, new SoulBookBauble());
        //  manager.bind(MaidSpellItems.ENDER_POCKET, new EnderPocketBauble());
        //  manager.bind(MaidSpellItems.FLOATING_FOX_LEAF, new FloatingFoxLeafBauble());
        //  manager.bind(MaidSpellItems.MOLTEN_FOX_LEAF, new MoltenFoxLeafBauble());
        //  manager.bind(MaidSpellItems.WOUND_RIME_BLADE, new WoundRimeBladeBauble());
        //  manager.bind(MaidSpellItems.ANCHOR_CORE, new AnchorCoreBauble());
        //  manager.bind(MaidSpellItems.FRAGRANT_INGENUITY, new FragrantIngenuityBauble());
        //  manager.bind(MaidSpellItems.STARANCHOR_PEARL, new StaranchorPearlBauble());
    }
}

package com.github.yimeng261.maidspell.client.renderer.layer;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.yimeng261.maidspell.item.MaidSpellItems;
import com.github.yimeng261.maidspell.spell.manager.BaubleStateManager;

/**
 * 头顶光环的优先级仲裁。
 *
 * <p>与上游一致：云梦水晶 &gt; 幻化项链 &gt; 不洁圣冠（本工程未移植不洁圣冠，
 * 故第三档恒为 false）。</p>
 */
final class MaidHaloRenderPriority {

    private MaidHaloRenderPriority() {
    }

    static boolean shouldRenderDreamHalo(EntityMaid maid) {
        return resolve(maid) == HaloRenderType.DREAM;
    }

    static boolean shouldRenderTransmogHalo(EntityMaid maid) {
        return resolve(maid) == HaloRenderType.TRANSMOG;
    }

    private static HaloRenderType resolve(EntityMaid maid) {
        if (BaubleStateManager.hasBauble(maid, MaidSpellItems.DREAM_CAT_CRYSTAL)) {
            return HaloRenderType.DREAM;
        }
        if (BaubleStateManager.hasBauble(maid, MaidSpellItems.TRANSMOG_NECKLACE)) {
            return HaloRenderType.TRANSMOG;
        }
        return HaloRenderType.NONE;
    }

    private enum HaloRenderType {
        NONE,
        DREAM,
        TRANSMOG
    }
}

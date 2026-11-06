// 移植自 1.9.2.1 反编译源 item/bauble/flowCore/FlowCoreBauble.java；SRG 名已替换为 1.18.2 official 名。
package com.github.yimeng261.maidspell.item.bauble.flowCore;

import com.github.tartaricacid.touhoulittlemaid.api.bauble.IMaidBauble;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.yimeng261.maidspell.Config;
import com.github.yimeng261.maidspell.Global;
import com.github.yimeng261.maidspell.item.MaidSpellItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class FlowCoreBauble
implements IMaidBauble {
    @Override
    public void onTick(EntityMaid maid, ItemStack baubleItem) {
        if (maid.tickCount % Config.flowCoreTickInterval == 0) {
            maid.setHealth(maid.getHealth() + maid.getMaxHealth() * (float)Config.flowCoreHealthRegenRate * (float)maid.getFavorabilityManager().getLevel());
        }
    }

    static {
        Global.baubleSetHealthHandlers.put((Item)MaidSpellItems.FLOW_CORE.get(), data -> {
            EntityMaid maid = data.getMaid();
            data.setAmount(data.getAmount() * (1.0f - (float)Config.flowCoreDamageReduction * (float)maid.getFavorabilityManager().getLevel()));
            return null;
        });
    }
}


// 移植自 1.9.2.1 反编译源 item/bauble/bleedingHeart/BleedingHeartBauble.java；SRG 名已替换为 1.18.2 official 名。
package com.github.yimeng261.maidspell.item.bauble.bleedingHeart;

import com.github.tartaricacid.touhoulittlemaid.api.bauble.IMaidBauble;
import com.github.yimeng261.maidspell.Config;
import com.github.yimeng261.maidspell.Global;
import com.github.yimeng261.maidspell.item.MaidSpellItems;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

public class BleedingHeartBauble
implements IMaidBauble {
    static {
        Global.baubleDamageHandlers.put((Item)MaidSpellItems.BLEEDING_HEART.get(), (event, maid) -> {
            Float amount = Float.valueOf(event.getAmount());
            Player owner = (Player)maid.getOwner();
            if (owner != null) {
                owner.heal(amount.floatValue() * (float)Config.bleedingHeartHealRatio);
            }
            maid.heal(amount.floatValue() * (float)Config.bleedingHeartHealRatio);
            return null;
        });
    }
}


// 移植自 1.9.2.1 反编译源 item/bauble/springRing/SpringBauble.java；SRG 名已替换为 1.18.2 official 名。
package com.github.yimeng261.maidspell.item.bauble.springRing;

import com.github.tartaricacid.touhoulittlemaid.api.bauble.IMaidBauble;
import com.github.yimeng261.maidspell.Config;
import com.github.yimeng261.maidspell.Global;
import com.github.yimeng261.maidspell.item.MaidSpellItems;
import net.minecraft.world.item.Item;

public class SpringBauble
implements IMaidBauble {
    static {
        Global.baubleDamageHandlers.put((Item)MaidSpellItems.SPRING_RING.get(), (event, maid) -> {
            Float percent = Float.valueOf(1.0f - maid.getHealth() / maid.getMaxHealth());
            if ((double)percent.floatValue() > Config.springRingMaxDamageBonus) {
                percent = Float.valueOf((float)Config.springRingMaxDamageBonus);
            }
            event.setAmount(event.getAmount() * (1.0f + percent.floatValue()));
            return null;
        });
    }
}


// 移植自 1.9.2.1 反编译源 item/bauble/rockCrystal/RockCrystalBauble.java；SRG 名已替换为 1.18.2 official 名。
package com.github.yimeng261.maidspell.item.bauble.rockCrystal;

import com.github.tartaricacid.touhoulittlemaid.api.bauble.IMaidBauble;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.yimeng261.maidspell.Config;
import java.util.UUID;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

public class RockCrystalBauble
implements IMaidBauble {
    private static final UUID KNOCKBACK_RESISTANCE_UUID = UUID.fromString("8b5c7a26-3e4f-4c45-a7b3-2f8d9e1a5c47");

    @Override
    public void onTick(EntityMaid maid, ItemStack baubleItem) {
        if (maid.tickCount % 10 != 0) {
            return;
        }
        AttributeInstance knockbackResistance = maid.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (knockbackResistance == null) {
            return;
        }
        if (knockbackResistance.getModifier(KNOCKBACK_RESISTANCE_UUID) == null) {
            knockbackResistance.addPermanentModifier(new AttributeModifier(KNOCKBACK_RESISTANCE_UUID, "Rock Crystal Knockback Resistance", (double)Config.rockCrystalKnockbackResistance, AttributeModifier.Operation.ADDITION));
        }
    }

    public void onTakeOff(EntityMaid maid, ItemStack baubleItem) {
        AttributeInstance knockbackResistance = maid.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (knockbackResistance == null) {
            return;
        }
        knockbackResistance.removeModifier(KNOCKBACK_RESISTANCE_UUID);
    }
}


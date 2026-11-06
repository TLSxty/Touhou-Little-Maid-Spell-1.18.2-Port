package com.github.yimeng261.maidspell.effect;

import com.github.yimeng261.maidspell.Config;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 本模组状态效果注册（仅星锚珍珠需要的两个）。
 */
public final class MaidSpellEffects {

    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, Config.MOD_ID);

    public static final RegistryObject<MobEffect> VOID_WALK =
            MOB_EFFECTS.register("void_walk", VoidWalkEffect::new);
    public static final RegistryObject<MobEffect> STARANCHOR_STUN =
            MOB_EFFECTS.register("staranchor_stun", StaranchorStunEffect::new);

    private MaidSpellEffects() {
    }

    public static void register(IEventBus eventBus) {
        MOB_EFFECTS.register(eventBus);
    }
}

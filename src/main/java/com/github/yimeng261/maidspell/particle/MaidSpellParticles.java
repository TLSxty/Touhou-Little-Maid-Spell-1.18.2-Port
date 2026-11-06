package com.github.yimeng261.maidspell.particle;

import com.github.yimeng261.maidspell.Config;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 本模组的粒子类型。
 * <p>
 * 批次 B 只保留虚空行走（{@code void_spell}）用到的这一个；原模组的法术粒子属魔法，未移植。
 */
public final class MaidSpellParticles {

    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, Config.MOD_ID);

    public static final RegistryObject<SimpleParticleType> VOID_SPELL =
            PARTICLES.register("void_spell", () -> new SimpleParticleType(false));

    private MaidSpellParticles() {
    }

    public static void register(net.minecraftforge.eventbus.api.IEventBus bus) {
        PARTICLES.register(bus);
    }
}

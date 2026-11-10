package com.github.yimeng261.maidspell.sound;

import com.github.yimeng261.maidspell.Config;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 音效注册（批次 5 精简版）。
 *
 * <p>上游 {@code sound/MaidSpellSounds.java} 注册了 18 个音效，其中 15 个属于未移植的
 * 星之魔女 / 法术系统。本工程只保留寻风之铃。</p>
 */
public final class MaidSpellSounds {

    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, Config.MOD_ID);

    /**
     * 寻风之铃的音效。
     *
     * <p>1.18.2 的 {@link SoundEvent} 只有单参构造器（上游的
     * {@code SoundEvent.m_262856_(location, range)} 是 1.19+ 才有），因此这里只用
     * {@code new SoundEvent(location)}；可听距离走 vanilla 默认。</p>
     */
    public static final RegistryObject<SoundEvent> WIND_SEEKING_BELL =
            SOUNDS.register("wind_seeking_bell",
                    () -> new SoundEvent(new ResourceLocation(Config.MOD_ID, "wind_seeking_bell")));

    private MaidSpellSounds() {
    }

    public static void register(net.minecraftforge.eventbus.api.IEventBus eventBus) {
        SOUNDS.register(eventBus);
    }
}

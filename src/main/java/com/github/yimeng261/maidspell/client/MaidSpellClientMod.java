package com.github.yimeng261.maidspell.client;

import com.github.yimeng261.maidspell.Config;
import com.github.yimeng261.maidspell.client.model.SharedHaloModel;
import com.github.yimeng261.maidspell.client.particle.VoidSpellParticle;
import com.github.yimeng261.maidspell.particle.MaidSpellParticles;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ParticleFactoryRegisterEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 客户端专用注册入口。
 * <p>
 * 与 1.20.1 源实现的差异：1.20.1 用 {@code RegisterParticleProvidersEvent} 直接注册
 * SpriteSet 提供者；1.18.2 没有这个事件（实测 forge 官方映射 jar 内不存在该类），
 * 对应物是 {@link ParticleFactoryRegisterEvent}（MOD 总线），在处理器里通过
 * {@code Minecraft.getInstance().particleEngine.register(type, SpriteParticleRegistration)}
 * 完成注册。
 */
@Mod.EventBusSubscriber(modid = Config.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class MaidSpellClientMod {

    private MaidSpellClientMod() {
    }

    @SubscribeEvent
    public static void onParticleFactoryRegister(ParticleFactoryRegisterEvent event) {
        Minecraft.getInstance().particleEngine.register(
                MaidSpellParticles.VOID_SPELL.get(),
                VoidSpellParticle.Provider::new);
    }

    /** 头顶光环的模型层要在这里注册，否则 bakeLayer 会抛「Unknown layer」异常。 */
    @SubscribeEvent
    public static void onRegisterLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(SharedHaloModel.LAYER_LOCATION, SharedHaloModel::createBodyLayer);
    }
}

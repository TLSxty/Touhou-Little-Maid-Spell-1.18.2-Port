package com.github.yimeng261.maidspell.client.renderer;

import com.github.tartaricacid.touhoulittlemaid.api.ILittleMaid;
import com.github.tartaricacid.touhoulittlemaid.api.LittleMaidExtension;
import com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.GeckoEntityMaidRenderer;
import com.github.yimeng261.maidspell.client.renderer.layer.GeckoLayerTransmogHalo;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 把幻化光环渲染层挂到女仆的 Gecko 渲染器上。
 *
 * <p>{@code @LittleMaidExtension} 的扫描机制在 TLM 1.2.2 与上游一致，
 * 只需类有无参构造即可被自动实例化。</p>
 */
@LittleMaidExtension
public class TransmogNecklaceRenderExtension implements ILittleMaid {

    @Override
    @OnlyIn(Dist.CLIENT)
    public void addAdditionGeckoMaidLayer(GeckoEntityMaidRenderer<? extends Mob> renderer,
                                          EntityRendererProvider.Context context) {
        renderer.addGeoLayerRenderer(new GeckoLayerTransmogHalo<>(renderer));
    }
}

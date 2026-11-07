package com.github.yimeng261.maidspell.client.renderer.layer;

import com.github.tartaricacid.touhoulittlemaid.api.entity.IMaid;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.GeoLayerRenderer;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.IGeoEntityRenderer;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.animated.ILocationModel;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.util.RenderUtils;
import com.github.yimeng261.maidspell.client.model.SharedHaloModel;
import com.github.yimeng261.maidspell.item.bauble.transmogNecklace.TransmogHaloStyle;
import com.github.yimeng261.maidspell.item.bauble.transmogNecklace.TransmogNecklace;
import com.github.yimeng261.maidspell.item.bauble.transmogNecklace.TransmogNecklaceBauble;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;

/**
 * 在女仆头顶渲染幻化项链选中的光环。
 *
 * <p>1.18.2 的 TLM 1.2.2 已提供 {@link GeoLayerRenderer} 抽象类与
 * {@link RenderUtils#prepMatrixForLocator}，与上游 1.20 结构一致；差异只在
 * {@code Minecraft.m_167973_} 之类 SRG 名改用 official 名。</p>
 */
public class GeckoLayerTransmogHalo<T extends Mob, R extends IGeoEntityRenderer<T>>
        extends GeoLayerRenderer<T, R> {

    /** 全亮，避免光环在阴影里发暗。 */
    private static final int LIGHT_LEVEL = 0xFF00F0;
    private static final float HALO_SCALE = 2.24f;

    private SharedHaloModel model;

    public GeckoLayerTransmogHalo(R entityRendererIn) {
        super(entityRendererIn);
    }

    @Override
    public GeoLayerRenderer<T, R> copy(R entityRendererIn) {
        return new GeckoLayerTransmogHalo<>(entityRendererIn);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, T entity,
                       float limbSwing, float limbSwingAmount, float partialTicks,
                       float ageInTicks, float netHeadYaw, float headPitch) {
        EntityMaid maid = IMaid.convertToMaid(entity);
        if (maid == null || maid.isInvisible() || maid.isSleeping()) {
            return;
        }
        if (!MaidHaloRenderPriority.shouldRenderTransmogHalo(maid)) {
            return;
        }
        ItemStack necklace = TransmogNecklaceBauble.findTransmogNecklace(maid);
        if (necklace.isEmpty()) {
            return;
        }
        if (this.model == null) {
            this.model = new SharedHaloModel(
                    Minecraft.getInstance().getEntityModels().bakeLayer(SharedHaloModel.LAYER_LOCATION));
        }
        ILocationModel locationModel = this.getLocationModel(entity);
        if (locationModel == null || locationModel.headBones().isEmpty()) {
            return;
        }
        TransmogHaloStyle style = TransmogNecklace.getSelectedStyle(necklace);
        poseStack.pushPose();
        RenderUtils.prepMatrixForLocator(poseStack, locationModel.headBones());
        poseStack.translate(0.0, 0.4, 0.6);
        this.renderHalo(poseStack, buffer, style);
        poseStack.popPose();
    }

    private void renderHalo(PoseStack poseStack, MultiBufferSource buffer, TransmogHaloStyle style) {
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(style.texture()));
        poseStack.pushPose();
        poseStack.scale(HALO_SCALE, HALO_SCALE, HALO_SCALE);
        this.model.renderToBuffer(poseStack, consumer, LIGHT_LEVEL, OverlayTexture.NO_OVERLAY,
                1.0f, 1.0f, 1.0f, 1.0f);
        poseStack.popPose();
    }
}

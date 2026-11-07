package com.github.yimeng261.maidspell.item.bauble.transmogNecklace;

import java.util.Arrays;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;

/**
 * 幻化项链可选的光环样式。
 *
 * <p>上游用 {@code Component.m_237115_}（1.19+ 的静态工厂）造名称，1.18.2 没有该类方法，
 * 本工程统一改用 {@link TranslatableComponent}。</p>
 */
public enum TransmogHaloStyle {
    DREAM_1("dream_1", 1),
    DREAM_2("dream_2", 2),
    DREAM_3("dream_3", 3);

    public static final TransmogHaloStyle DEFAULT = DREAM_1;

    private final String id;
    private final ResourceLocation texture;
    private final ResourceLocation iconTexture;
    private final String translationKey;

    TransmogHaloStyle(String id, int index) {
        this.id = id;
        this.texture = new ResourceLocation("touhou_little_maid_spell",
                "textures/entity/maid/halo_deco/dream_halo_dec_" + index + ".png");
        this.iconTexture = new ResourceLocation("touhou_little_maid_spell",
                "textures/entity/maid/halo_deco/dream_halo_dec_" + index + "_gui.png");
        this.translationKey = "gui.maidspell.transmog_necklace.style." + id;
    }

    public String id() {
        return this.id;
    }

    public ResourceLocation texture() {
        return this.texture;
    }

    public ResourceLocation iconTexture() {
        return this.iconTexture;
    }

    public String translationKey() {
        return this.translationKey;
    }

    public Component getDisplayName() {
        return new TranslatableComponent(this.translationKey);
    }

    public int getSerializedIndex() {
        return this.ordinal();
    }

    public TransmogHaloStyle next() {
        TransmogHaloStyle[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }

    public TransmogHaloStyle previous() {
        TransmogHaloStyle[] values = values();
        return values[(this.ordinal() - 1 + values.length) % values.length];
    }

    public static TransmogHaloStyle byId(String id) {
        return Arrays.stream(values())
                .filter(style -> style.id.equals(id))
                .findFirst()
                .orElse(DEFAULT);
    }

    public static TransmogHaloStyle byIndex(int index) {
        TransmogHaloStyle[] values = values();
        if (index < 0 || index >= values.length) {
            return DEFAULT;
        }
        return values[index];
    }
}

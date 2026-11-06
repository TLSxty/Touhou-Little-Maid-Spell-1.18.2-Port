package com.github.yimeng261.maidspell.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 虚空法术粒子（由 {@code assets/touhou_little_maid_spell/particles/void_spell.json} 提供）
 * 的资源定义驱动，运行期由 {@link Provider} 创建。
 * <p>
 * 移植说明：1.20.1 源使用 SRG 字段名（{@code f_107663_} 等），此处已替换为 1.18.2 official 名
 * （{@code quadSize} / {@code lifetime} / {@code gravity} / {@code friction} / {@code hasPhysics} /
 * {@code age} / {@code alpha} / {@code random} / {@code xd,yd,zd}）。字段名对照由
 * {@code srg_to_official_1.18.2.tsrg} 逐条确认。
 */
public class VoidSpellParticle extends TextureSheetParticle {

    private static final float BASE_QUAD_SIZE = 0.42f;
    private static final float SIZE_VARIATION = 0.35f;
    private static final int MIN_LIFETIME = 12;
    private static final int LIFETIME_VARIATION = 12;
    private static final double DRIFT = 0.012;

    private final SpriteSet sprites;

    protected VoidSpellParticle(ClientLevel level, double x, double y, double z,
                                double dx, double dy, double dz, SpriteSet sprites) {
        super(level, x, y, z, dx, dy, dz);
        this.sprites = sprites;
        this.quadSize = BASE_QUAD_SIZE * (1.0f - SIZE_VARIATION * this.random.nextFloat());
        this.lifetime = MIN_LIFETIME + this.random.nextInt(LIFETIME_VARIATION);
        this.gravity = 0.0f;
        this.friction = 0.96f;
        this.hasPhysics = false;
        this.xd = dx + (this.random.nextDouble() - 0.5) * DRIFT;
        this.yd = dy + this.random.nextDouble() * DRIFT;
        this.zd = dz + (this.random.nextDouble() - 0.5) * DRIFT;
        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        this.setSpriteFromAge(this.sprites);
        if (this.age > this.lifetime / 2) {
            this.alpha = 1.0f - ((float) this.age - (float) this.lifetime / 2.0f) / ((float) this.lifetime / 2.0f);
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return 0xF000F0;
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<SimpleParticleType> {

        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public VoidSpellParticle createParticle(SimpleParticleType type, ClientLevel level,
                                                double x, double y, double z,
                                                double dx, double dy, double dz) {
            return new VoidSpellParticle(level, x, y, z, dx, dy, dz, this.sprites);
        }
    }
}

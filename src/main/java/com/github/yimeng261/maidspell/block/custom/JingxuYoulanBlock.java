package com.github.yimeng261.maidspell.block.custom;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.yimeng261.maidspell.block.entity.JingxuYoulanBlockEntity;
import com.mojang.math.Vector3f;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.MaterialColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;

/**
 * 静虚幽兰：淡蓝色小花，净化女仆身上的负面效果、清除其他生物身上的全部效果。
 * <p>
 * 移植自 1.9.2.1 反编译源 block/custom/JingxuYoulanBlock.java（118 行）。
 * <strong>与原文的差异</strong>：
 * <ul>
 *   <li>1.18.2 没有 {@code Properties.of()}（无参）/ {@code MapColor} / {@code offsetType(...)} /
 *       {@code pushReaction(...)}，同 {@link YueLinglanBlock} 的处理方式。</li>
 *   <li>原文对非女仆生物的 {@code m_21219_} 是 {@code LivingEntity.removeAllEffects()}，
 *       不是治疗——这一点容易看错，此处按字节码语义照搬。</li>
 *   <li>{@code RandomSource} → {@code java.util.Random}；{@code org.joml.Vector3f} →
 *       {@code com.mojang.math.Vector3f}。</li>
 * </ul>
 */
public class JingxuYoulanBlock extends BushBlock implements EntityBlock {

    /** 发光等级。 */
    private static final int LIGHT_LEVEL = 10;
    /** 光环半径（格）。 */
    private static final int AURA_RANGE = 3;

    public JingxuYoulanBlock() {
        super(createProperties());
    }

    /** 地栽形态的属性。 */
    public static BlockBehaviour.Properties createProperties() {
        return BlockBehaviour.Properties.of(Material.PLANT, MaterialColor.PLANT)
                .noCollission()
                .instabreak()
                .sound(SoundType.GRASS)
                .lightLevel(state -> LIGHT_LEVEL);
    }

    /** 盆栽形态的属性（以原版蓝色兰花花盆为基准）。 */
    public static BlockBehaviour.Properties createPottedProperties() {
        return BlockBehaviour.Properties.copy(Blocks.POTTED_BLUE_ORCHID)
                .lightLevel(state -> LIGHT_LEVEL);
    }

    @Override
    public BlockBehaviour.OffsetType getOffsetType() {
        return BlockBehaviour.OffsetType.XZ;
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.DESTROY;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new JingxuYoulanBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : JingxuYoulanBlockEntity.createTicker(type);
    }

    /** 女仆：移除全部负面效果；其他生物：移除全部效果。由 BlockEntity 定时调用。 */
    public static void applyAura(ServerLevel level, BlockPos pos) {
        AABB area = new AABB(pos).inflate(AURA_RANGE);
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, area, LivingEntity::isAlive)) {
            if (living instanceof EntityMaid) {
                removeHarmfulEffects((EntityMaid) living);
            } else {
                living.removeAllEffects();
            }
        }
    }

    /** 只摘掉带 {@link MobEffectCategory#HARMFUL} 的效果，增益保留。 */
    private static void removeHarmfulEffects(EntityMaid maid) {
        List<MobEffect> harmfulEffects = new ArrayList<>();
        for (MobEffectInstance instance : maid.getActiveEffects()) {
            MobEffect effect = instance.getEffect();
            if (effect.getCategory() == MobEffectCategory.HARMFUL) {
                harmfulEffects.add(effect);
            }
        }
        harmfulEffects.forEach(maid::removeEffect);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, Random random) {
        if (random.nextFloat() > 0.32f) {
            return;
        }
        double x = pos.getX() + 0.25 + random.nextDouble() * 0.5;
        double y = pos.getY() + 0.45 + random.nextDouble() * 0.45;
        double z = pos.getZ() + 0.25 + random.nextDouble() * 0.5;
        level.addParticle(new DustParticleOptions(new Vector3f(0.82f, 0.88f, 1.0f), 0.88f),
                x, y, z, 0.0, 0.008 + random.nextDouble() * 0.01, 0.0);
    }
}

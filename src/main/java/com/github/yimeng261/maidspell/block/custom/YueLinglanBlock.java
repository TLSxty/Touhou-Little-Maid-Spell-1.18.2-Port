package com.github.yimeng261.maidspell.block.custom;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.yimeng261.maidspell.block.entity.YueLinglanBlockEntity;
import com.mojang.math.Vector3f;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
 * 月铃兰：会发光的白蓝色小花，给周围女仆提供生命恢复与抗性提升。
 * <p>
 * 移植自 1.9.2.1 反编译源 block/custom/YueLinglanBlock.java（107 行）。
 * <strong>与原文的差异</strong>：
 * <ul>
 *   <li>1.18.2 没有 {@code BlockBehaviour.Properties.of()}（无参）与 {@code MapColor}，改用
 *       {@code Properties.of(Material.PLANT, MaterialColor.PLANT)}。</li>
 *   <li>1.18.2 的 {@code Properties} 没有 {@code offsetType(...)} / {@code pushReaction(...)}，
 *       改为覆写 {@link #getOffsetType()} 与 {@link #getPistonPushReaction(BlockState)}。</li>
 *   <li>{@code RandomSource} 在 1.18.2 不存在，{@code animateTick} 第四参用 {@code java.util.Random}；
 *       {@code org.joml.Vector3f} 也不存在，改用 {@code com.mojang.math.Vector3f}。</li>
 * </ul>
 * 种植条件沿用 {@link BushBlock} 默认（可种在泥土类方块与耕地上），与原文一致。
 */
public class YueLinglanBlock extends BushBlock implements EntityBlock {

    /** 发光等级。 */
    private static final int LIGHT_LEVEL = 10;
    /** 光环半径（格）。 */
    private static final int AURA_RANGE = 3;
    /** 单次光环效果的持续 tick。 */
    private static final int EFFECT_DURATION_TICKS = 100;

    public YueLinglanBlock() {
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

    /** 盆栽形态的属性（以原版绒球葱花盆为基准，只改发光）。 */
    public static BlockBehaviour.Properties createPottedProperties() {
        return BlockBehaviour.Properties.copy(Blocks.POTTED_ALLIUM)
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
        return new YueLinglanBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : YueLinglanBlockEntity.createTicker(type);
    }

    /** 给半径内的女仆挂上生命恢复 + 抗性提升。由 BlockEntity 定时调用。 */
    public static void applyAura(ServerLevel level, BlockPos pos) {
        AABB area = new AABB(pos).inflate(AURA_RANGE);
        for (EntityMaid maid : level.getEntitiesOfClass(EntityMaid.class, area, LivingEntity::isAlive)) {
            maid.addEffect(new MobEffectInstance(MobEffects.REGENERATION, EFFECT_DURATION_TICKS, 0, false, false, true));
            maid.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, EFFECT_DURATION_TICKS, 0, false, false, true));
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, Random random) {
        if (random.nextFloat() > 0.3f) {
            return;
        }
        double x = pos.getX() + 0.25 + random.nextDouble() * 0.5;
        double y = pos.getY() + 0.5 + random.nextDouble() * 0.4;
        double z = pos.getZ() + 0.25 + random.nextDouble() * 0.5;
        level.addParticle(new DustParticleOptions(new Vector3f(0.72f, 1.0f, 0.94f), 0.9f),
                x, y, z, 0.0, 0.008 + random.nextDouble() * 0.01, 0.0);
    }
}

package com.github.yimeng261.maidspell.block.custom;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.yimeng261.maidspell.block.entity.ScarletZhuhuaBlockEntity;
import com.mojang.math.Vector3f;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.level.BlockGetter;
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
 * 绯红朱华：可长在绯红菌岩上的红色花，给范围内生物力量 II，并给非女仆、非癫狂骑士施加凋零。
 * <p>
 * 移植自 1.9.2.1 反编译源 block/custom/ScarletZhuhuaBlock.java（133 行）。
 * <strong>与原文的差异</strong>：
 * <ul>
 *   <li>1.18.2 没有 {@code Properties.of()}（无参）/ {@code MapColor} / {@code offsetType(...)} /
 *       {@code pushReaction(...)}，同 {@link YueLinglanBlock} 的处理方式。</li>
 *   <li>原文用 {@code living.getType().builtInRegistryHolder().key().location()} 取实体 id，
 *       1.18.2 用 {@link EntityType#getKey(net.minecraft.world.entity.EntityType)}。</li>
 *   <li>{@code RandomSource} → {@code java.util.Random}；{@code org.joml.Vector3f} →
 *       {@code com.mojang.math.Vector3f}。</li>
 * </ul>
 * 本工程未移植 {@code corrupted_knight} 实体，因此 {@link #isCorruptedKnight} 恒为 {@code false}：
 * 除女仆外的一切生物都会吃到凋零，与原版「没有该实体时」的行为一致。
 */
public class ScarletZhuhuaBlock extends BushBlock implements EntityBlock {

    /** 发光等级。 */
    private static final int LIGHT_LEVEL = 10;
    /** 光环半径（格）。 */
    private static final int AURA_RANGE = 3;
    /** 单次光环效果的持续 tick。 */
    private static final int EFFECT_DURATION_TICKS = 100;
    /** 癫狂骑士的实体 id（豁免凋零）。 */
    private static final ResourceLocation CORRUPTED_KNIGHT_ID =
            new ResourceLocation("touhou_little_maid_spell", "corrupted_knight");

    public ScarletZhuhuaBlock() {
        super(createProperties());
    }

    /** 地栽形态的属性。 */
    public static BlockBehaviour.Properties createProperties() {
        return BlockBehaviour.Properties.of(Material.PLANT, MaterialColor.COLOR_RED)
                .noCollission()
                .instabreak()
                .sound(SoundType.GRASS)
                .lightLevel(state -> LIGHT_LEVEL);
    }

    /** 盆栽形态的属性（以原版虞美人花盆为基准）。 */
    public static BlockBehaviour.Properties createPottedProperties() {
        return BlockBehaviour.Properties.copy(Blocks.POTTED_POPPY)
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

    /** 除泥土类与耕地外，额外允许长在绯红菌岩上。 */
    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(Blocks.CRIMSON_NYLIUM) || super.mayPlaceOn(state, level, pos);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ScarletZhuhuaBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : ScarletZhuhuaBlockEntity.createTicker(type);
    }

    /** 力量 II 给所有非魔法 NPC；凋零只给非女仆、非癫狂骑士。由 BlockEntity 定时调用。 */
    public static void applyAura(ServerLevel level, BlockPos pos) {
        AABB area = new AABB(pos).inflate(AURA_RANGE);
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, area, ScarletZhuhuaBlock::shouldAffect)) {
            living.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, EFFECT_DURATION_TICKS, 1, true, false, true));
            if (living instanceof EntityMaid || isCorruptedKnight(living)) {
                continue;
            }
            living.addEffect(new MobEffectInstance(MobEffects.WITHER, EFFECT_DURATION_TICKS, 0, true, false, true));
        }
    }

    private static boolean shouldAffect(LivingEntity living) {
        return living.isAlive() && !isIronsSpellbooksNpc(living);
    }

    /** 癫狂骑士豁免凋零（本工程未注册该实体，实际恒 false）。 */
    private static boolean isCorruptedKnight(LivingEntity living) {
        return CORRUPTED_KNIGHT_ID.equals(EntityType.getKey(living.getType()));
    }

    /** 铁魔法的商人 NPC 不受影响——本工程不装铁魔法，实际恒 false。 */
    private static boolean isIronsSpellbooksNpc(LivingEntity living) {
        return living instanceof Merchant && !(living instanceof Enemy)
                && "irons_spellbooks".equals(EntityType.getKey(living.getType()).getNamespace());
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, Random random) {
        if (random.nextFloat() > 0.35f) {
            return;
        }
        double x = pos.getX() + 0.25 + random.nextDouble() * 0.5;
        double y = pos.getY() + 0.45 + random.nextDouble() * 0.45;
        double z = pos.getZ() + 0.25 + random.nextDouble() * 0.5;
        level.addParticle(new DustParticleOptions(new Vector3f(1.0f, 0.18f, 0.35f), 0.95f),
                x, y, z, 0.0, 0.01 + random.nextDouble() * 0.01, 0.0);
    }
}

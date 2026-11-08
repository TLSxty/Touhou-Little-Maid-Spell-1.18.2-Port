package com.github.yimeng261.maidspell.block.custom;

import com.mojang.math.Vector3f;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.MaterialColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 星光花丛：贴地生长的发光花瓣堆，落在末地石上会撒出蓝紫色光尘。
 * <p>
 * 移植自 1.9.2.1 反编译源 block/custom/StarGlowFlowerClusterBlock.java（62 行）。
 * <strong>与原文的差异（本批最大的一处降级）</strong>：
 * <ul>
 *   <li>原文 {@code extends PinkPetalsBlock}。{@code PinkPetalsBlock} 是 1.20 才加入的类，
 *       1.18.2 的客户端 jar 里连它的模型父 {@code minecraft:block/flowerbed_N} 都不存在，
 *       因此这里改写为 {@link BushBlock} 的等价实现。</li>
 *   <li>原文的 {@code AMOUNT} 属性继承自 {@code PinkPetalsBlock}；这里自建
 *       {@link #FLOWER_AMOUNT}，取值范围与名称（{@code flower_amount} 1..4）都保持一致，
 *       以便直接吃下原 jar 里的 {@code star_glow_flower_cluster.json} blockstate 与
 *       {@code blue_petals_N} / {@code purple_petals_N} 模型。</li>
 *   <li>{@code RandomSource} → {@code java.util.Random}；{@code org.joml.Vector3f} →
 *       {@code com.mojang.math.Vector3f}。</li>
 *   <li>模型父由 {@code flowerbed_N} 改为本工程自建的 {@code petal_bed_N}（见 assets）。</li>
 * </ul>
 */
public class StarGlowFlowerClusterBlock extends BushBlock {

    /** 发光等级。 */
    private static final int LIGHT_LEVEL = 10;
    /** 花瓣数量，1..4，与 blockstate 的 {@code flower_amount} 对应。 */
    public static final IntegerProperty FLOWER_AMOUNT = IntegerProperty.create("flower_amount", 1, 4);
    /** 朝向，决定模型旋转。 */
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static final Vector3f BLUE_GLOW = new Vector3f(0.5f, 0.92f, 1.0f);
    private static final Vector3f PURPLE_GLOW = new Vector3f(0.86f, 0.58f, 1.0f);

    /** 贴地薄片碰撞箱（高度随花瓣数量略有变化，取值宽松即可，本方块无碰撞）。 */
    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 3.0, 16.0);

    public StarGlowFlowerClusterBlock() {
        super(createProperties());
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FLOWER_AMOUNT, 1)
                .setValue(FACING, Direction.NORTH));
    }

    /** 方块属性：贴地、无碰撞、可被活塞破坏、发光。 */
    public static BlockBehaviour.Properties createProperties() {
        return BlockBehaviour.Properties.of(Material.REPLACEABLE_PLANT, MaterialColor.PLANT)
                .noCollission()
                .instabreak()
                .sound(SoundType.GRASS)
                .lightLevel(state -> LIGHT_LEVEL);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FLOWER_AMOUNT, FACING);
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.DESTROY;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /** 除泥土类与耕地外，额外允许长在末地石上（原文 {@code Blocks.END_STONE}）。 */
    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(Blocks.END_STONE) || super.mayPlaceOn(state, level, pos);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, Random random) {
        int amount = state.getValue(FLOWER_AMOUNT);
        if (random.nextFloat() > 0.12f + (float) amount * 0.05f) {
            return;
        }
        double x = pos.getX() + 0.15 + random.nextDouble() * 0.7;
        double y = pos.getY() + 0.08 + random.nextDouble() * 0.2;
        double z = pos.getZ() + 0.15 + random.nextDouble() * 0.7;
        Vector3f color = random.nextBoolean() ? BLUE_GLOW : PURPLE_GLOW;
        level.addParticle(new DustParticleOptions(color, 0.75f),
                x, y, z, 0.0, 0.004 + random.nextDouble() * 0.006, 0.0);
    }
}

package com.github.yimeng261.maidspell.block.custom;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.WaterlilyBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 会自行消失的狐叶踏痕（浮波狐叶 / 熔岩狐叶在水面、岩浆面上留下的荷叶状方块）。
 * <p>
 * 移植自 1.9.2.1 反编译源 block/custom/TransientFoxLeafTrailBlock.java（100 行）。
 * <strong>与原文的差异</strong>：
 * <ul>
 *   <li>原文 {@code Properties} 末尾调用 {@code m_222994_}（SRG），该方法名在 1.18.2 的映射表里查不到。
 *       从上下文（方块必须走 {@code randomTick} 才会过期消失）推断为 {@code randomTicks()}，
 *       这里按 1.18.2 官方名写入。</li>
 *   <li>1.18.2 的 {@code randomTick} 第四参是 {@code java.util.Random}，不是 1.19+ 的 {@code RandomSource}。</li>
 * </ul>
 */
public class TransientFoxLeafTrailBlock extends WaterlilyBlock {

    /** 实体在叶面上站立时，每 5 tick 才刷新一次寿命，避免每 tick 都排计划刻。 */
    private static final long REFRESH_INTERVAL_TICKS = 5L;

    private final int minLifetimeTicks;
    private final int maxLifetimeTicks;

    public TransientFoxLeafTrailBlock(int minLifetimeTicks, int maxLifetimeTicks) {
        super(BlockBehaviour.Properties.copy(Blocks.LILY_PAD)
                .strength(0.0f)
                .noCollission()
                .noOcclusion()
                .randomTicks());
        this.minLifetimeTicks = minLifetimeTicks;
        this.maxLifetimeTicks = maxLifetimeTicks;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!level.isClientSide && !state.is(oldState.getBlock())) {
            level.scheduleTick(pos, this, getLifetime(level.random));
        }
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, Random random) {
        // 有实体（女仆或玩家）正踩在上面时续命，否则消失。
        if (entityStandingOnLeaf(level, pos)) {
            level.scheduleTick(pos, this, getLifetime(random));
            return;
        }
        if (state.is(this)) {
            level.removeBlock(pos, false);
        }
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        // 叶痕是不可获取的临时方块，中键取不出物品。
        return ItemStack.EMPTY;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.isFaceSturdy(level, pos, Direction.UP)
                || state.is(Blocks.ICE)
                || state.getBlock() instanceof LiquidBlock;
    }

    private int getLifetime(Random random) {
        if (maxLifetimeTicks <= minLifetimeTicks) {
            return minLifetimeTicks;
        }
        return minLifetimeTicks + random.nextInt(maxLifetimeTicks - minLifetimeTicks + 1);
    }

    /** 供 {@code FoxLeafSurfaceHelper.tryPlaceTrail} 在续命时调用。 */
    public void refreshLifetime(Level level, BlockPos pos) {
        if (!level.isClientSide && level.getGameTime() % REFRESH_INTERVAL_TICKS == 0L) {
            level.scheduleTick(pos, this, getLifetime(level.random));
        }
    }

    private static boolean entityStandingOnLeaf(Level level, BlockPos pos) {
        AABB area = new AABB(pos).inflate(0.35, 1.2, 0.35).move(0.0, 1.0, 0.0);
        if (!level.getEntitiesOfClass(EntityMaid.class, area,
                maid -> maid.isAlive() && !maid.isPassenger()).isEmpty()) {
            return true;
        }
        return !level.getEntitiesOfClass(Player.class, area,
                player -> player.isAlive() && !player.isPassenger()).isEmpty();
    }
}

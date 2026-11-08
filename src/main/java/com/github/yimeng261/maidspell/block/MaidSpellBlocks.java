package com.github.yimeng261.maidspell.block;

import com.github.yimeng261.maidspell.Config;
import com.github.yimeng261.maidspell.block.custom.JingxuYoulanBlock;
import com.github.yimeng261.maidspell.block.custom.PottedJingxuYoulanBlock;
import com.github.yimeng261.maidspell.block.custom.PottedScarletZhuhuaBlock;
import com.github.yimeng261.maidspell.block.custom.PottedYueLinglanBlock;
import com.github.yimeng261.maidspell.block.custom.ScarletZhuhuaBlock;
import com.github.yimeng261.maidspell.block.custom.StarGlowFlowerClusterBlock;
import com.github.yimeng261.maidspell.block.custom.SuppressionStoneBlock;
import com.github.yimeng261.maidspell.block.custom.TransientFoxLeafTrailBlock;
import com.github.yimeng261.maidspell.block.custom.YueLinglanBlock;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 方块注册表。
 * <p>
 * 移植自 1.9.2.1 反编译源 block/MaidSpellBlocks.java（88 行）。原文注册 15 个方块，
 * 本工程到 P3 批次 0 为止已全部补齐：6 个踏痕方块（批次 B 搬入）+
 * 绯红朱华 / 月铃兰 / 静虚幽兰三种花及其花盆 + 星光花丛 + 压制之石（P3 批次 0 搬入）。
 * <p>
 * <strong>与原文的差异</strong>：原文的 {@code getRandomXxxTrail(RandomSource)} 参数是
 * 1.19+ 才有的 {@code RandomSource}，1.18.2 用 {@link Random}。
 */
public final class MaidSpellBlocks {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, Config.MOD_ID);

    /** 绯红朱华：可长在绯红菌岩上，给周围生物力量 II 并对非女仆施加凋零。 */
    public static final RegistryObject<Block> SCARLET_ZHUHUA =
            BLOCKS.register("scarlet_zhuhua", ScarletZhuhuaBlock::new);
    public static final RegistryObject<Block> POTTED_SCARLET_ZHUHUA =
            BLOCKS.register("potted_scarlet_zhuhua", () -> new PottedScarletZhuhuaBlock(
                    MaidSpellBlocks::emptyFlowerPot, (Supplier<? extends Block>) SCARLET_ZHUHUA));

    /** 月铃兰：会发光，给周围女仆生命恢复与抗性提升，并朝精灵领域撒光尘尾迹。 */
    public static final RegistryObject<Block> YUE_LINGLAN =
            BLOCKS.register("yue_linglan", YueLinglanBlock::new);
    public static final RegistryObject<Block> POTTED_YUE_LINGLAN =
            BLOCKS.register("potted_yue_linglan", () -> new PottedYueLinglanBlock(
                    MaidSpellBlocks::emptyFlowerPot, (Supplier<? extends Block>) YUE_LINGLAN));

    /** 静虚幽兰：净化女仆的负面效果、清空其他生物的全部效果。 */
    public static final RegistryObject<Block> JINGXU_YOULAN =
            BLOCKS.register("jingxu_youlan", JingxuYoulanBlock::new);
    public static final RegistryObject<Block> POTTED_JINGXU_YOULAN =
            BLOCKS.register("potted_jingxu_youlan", () -> new PottedJingxuYoulanBlock(
                    MaidSpellBlocks::emptyFlowerPot, (Supplier<? extends Block>) JINGXU_YOULAN));

    /** 星光花丛：贴地发光花瓣堆。 */
    public static final RegistryObject<Block> STAR_GLOW_FLOWER_CLUSTER =
            BLOCKS.register("star_glow_flower_cluster", StarGlowFlowerClusterBlock::new);

    /** 浮波狐叶在水面留下的荷叶踏痕：普通 / 大 / 小三种尺寸，寿命（tick）各不相同。 */
    public static final RegistryObject<Block> FLOATING_FOX_LEAF_TRAIL =
            BLOCKS.register("floating_fox_leaf_trail", () -> new TransientFoxLeafTrailBlock(10, 18));
    public static final RegistryObject<Block> FLOATING_FOX_LEAF_TRAIL_BIG =
            BLOCKS.register("floating_fox_leaf_trail_big", () -> new TransientFoxLeafTrailBlock(12, 20));
    public static final RegistryObject<Block> FLOATING_FOX_LEAF_TRAIL_SMALL =
            BLOCKS.register("floating_fox_leaf_trail_small", () -> new TransientFoxLeafTrailBlock(8, 16));

    /** 熔岩狐叶在岩浆面留下的踏痕，参数同上。 */
    public static final RegistryObject<Block> MOLTEN_FOX_LEAF_TRAIL =
            BLOCKS.register("molten_fox_leaf_trail", () -> new TransientFoxLeafTrailBlock(10, 18));
    public static final RegistryObject<Block> MOLTEN_FOX_LEAF_TRAIL_BIG =
            BLOCKS.register("molten_fox_leaf_trail_big", () -> new TransientFoxLeafTrailBlock(12, 20));
    public static final RegistryObject<Block> MOLTEN_FOX_LEAF_TRAIL_SMALL =
            BLOCKS.register("molten_fox_leaf_trail_small", () -> new TransientFoxLeafTrailBlock(8, 16));

    /** 压制之石：标记区块，使附近月铃兰的光尘尾迹停手。 */
    public static final RegistryObject<Block> SUPPRESSION_STONE =
            BLOCKS.register("suppression_stone", SuppressionStoneBlock::new);

    private static final List<RegistryObject<Block>> FLOATING_FOX_LEAF_TRAILS =
            List.of(FLOATING_FOX_LEAF_TRAIL, FLOATING_FOX_LEAF_TRAIL_BIG, FLOATING_FOX_LEAF_TRAIL_SMALL);
    private static final List<RegistryObject<Block>> MOLTEN_FOX_LEAF_TRAILS =
            List.of(MOLTEN_FOX_LEAF_TRAIL, MOLTEN_FOX_LEAF_TRAIL_BIG, MOLTEN_FOX_LEAF_TRAIL_SMALL);

    private MaidSpellBlocks() {
    }

    /** 空花盆——三个花盆方块都要拿它当「容器」参数。 */
    private static FlowerPotBlock emptyFlowerPot() {
        return (FlowerPotBlock) Blocks.FLOWER_POT;
    }

    /**
     * 把三个花的「普通方块 → 花盆方块」映射注册进原版花盆。
     * <p>必须在 {@code FMLCommonSetupEvent} 阶段调用，此时方块注册表已就绪。
     */
    public static void registerPottedPlants() {
        FlowerPotBlock emptyPot = emptyFlowerPot();
        emptyPot.addPlant(SCARLET_ZHUHUA.getId(), POTTED_SCARLET_ZHUHUA);
        emptyPot.addPlant(YUE_LINGLAN.getId(), POTTED_YUE_LINGLAN);
        emptyPot.addPlant(JINGXU_YOULAN.getId(), POTTED_JINGXU_YOULAN);
    }

    public static BlockState getRandomFloatingFoxLeafTrail(Random random) {
        return FLOATING_FOX_LEAF_TRAILS.get(random.nextInt(FLOATING_FOX_LEAF_TRAILS.size()))
                .get().defaultBlockState();
    }

    public static boolean isFloatingFoxLeafTrail(BlockState state) {
        return FLOATING_FOX_LEAF_TRAILS.stream().anyMatch(entry -> state.is(entry.get()));
    }

    public static BlockState getRandomMoltenFoxLeafTrail(Random random) {
        return MOLTEN_FOX_LEAF_TRAILS.get(random.nextInt(MOLTEN_FOX_LEAF_TRAILS.size()))
                .get().defaultBlockState();
    }

    public static boolean isMoltenFoxLeafTrail(BlockState state) {
        return MOLTEN_FOX_LEAF_TRAILS.stream().anyMatch(entry -> state.is(entry.get()));
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}

package com.github.yimeng261.maidspell.block;

import com.github.yimeng261.maidspell.Config;
import com.github.yimeng261.maidspell.block.custom.TransientFoxLeafTrailBlock;
import java.util.List;
import java.util.Random;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 方块注册表。
 * <p>
 * 移植自 1.9.2.1 反编译源 block/MaidSpellBlocks.java（88 行）。原文注册 15 个方块，
 * 本工程只搬浮波狐叶 / 熔岩狐叶需要的 6 个踏痕方块；其余 9 个
 * （scarlet_zhuhua / yue_linglan / jingxu_youlan 及其花盆、star_glow_flower_cluster、suppression_stone）
 * 属建筑与装饰内容，等 P3 建筑移植时再补。
 */
public final class MaidSpellBlocks {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, Config.MOD_ID);

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

    private static final List<RegistryObject<Block>> FLOATING_FOX_LEAF_TRAILS =
            List.of(FLOATING_FOX_LEAF_TRAIL, FLOATING_FOX_LEAF_TRAIL_BIG, FLOATING_FOX_LEAF_TRAIL_SMALL);
    private static final List<RegistryObject<Block>> MOLTEN_FOX_LEAF_TRAILS =
            List.of(MOLTEN_FOX_LEAF_TRAIL, MOLTEN_FOX_LEAF_TRAIL_BIG, MOLTEN_FOX_LEAF_TRAIL_SMALL);

    private MaidSpellBlocks() {
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

    public static void register(net.minecraftforge.eventbus.api.IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}

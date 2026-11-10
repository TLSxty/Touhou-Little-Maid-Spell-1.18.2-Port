package com.github.yimeng261.maidspell;

import com.github.yimeng261.maidspell.block.MaidSpellBlocks;
import com.github.yimeng261.maidspell.block.entity.MaidSpellBlockEntities;
import com.github.yimeng261.maidspell.block.entity.SuppressionStoneBlockEntity;
import com.github.yimeng261.maidspell.effect.MaidSpellEffects;
import com.github.yimeng261.maidspell.item.MaidSpellItems;
import com.github.yimeng261.maidspell.network.NetworkHandler;
import com.github.yimeng261.maidspell.particle.MaidSpellParticles;
import com.github.yimeng261.maidspell.sound.MaidSpellSounds;
import com.github.yimeng261.maidspell.worldgen.MaidSpellStructurePieceTypes;
import com.github.yimeng261.maidspell.worldgen.MaidSpellStructures;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.event.server.ServerStoppedEvent;

/**
 * 主类。
 *
 * <p>移植自 1.9.2.1 反编译源 MaidSpellMod.java（141 行）。原文注册了
 * Blocks / BlockEntities / Items / CreativeTab / Containers / Sounds / Particles / Effects /
 * Entities / Paintings / Structures / StructurePieceTypes，另有
 * {@code IronsSpellbooksCompat.init} / {@code GeckoLibCompat.init} / {@code OptionalModIngredientSerializer} /
 * {@code ForgeChunkManager} 回调 / {@code NetworkHandler.registerMessages()} / 模型包安装 / 6 份 config。</p>
 *
 * <p>本工程只保留「方块 + 方块实体 + 饰品物品 + 药水效果 + 粒子 + 1 份 config」。
 * 其余全部属于魔法（铁魔法 / Goety 兼容、法术系统、维度）或需要网络的饰品，
 * 分批搬入时再在此补注册。</p>
 */
@Mod(MaidSpellMod.MOD_ID)
public class MaidSpellMod {

    public static final String MOD_ID = Config.MOD_ID;

    public MaidSpellMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        MaidSpellBlocks.register(modEventBus);
        MaidSpellBlockEntities.register(modEventBus);
        MaidSpellItems.register(modEventBus);
        MaidSpellEffects.register(modEventBus);
        MaidSpellParticles.register(modEventBus);
        MaidSpellStructures.register(modEventBus);
        MaidSpellStructurePieceTypes.register(modEventBus);
        MaidSpellSounds.register(modEventBus);
        NetworkHandler.register();
        modEventBus.addListener(this::commonSetup);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.BAUBLES_SPEC, "touhou_little_maid_spell-baubles.toml");
        // 事件订阅者（MaidSpellEventHandler）由 @Mod.EventBusSubscriber 自动注册，无需在此登记。
    }

    /** 通用初始化：花盆映射必须等方块注册表就绪后才能注册。 */
    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(MaidSpellBlocks::registerPottedPlants);
    }

    /** 卸服时清空压制之石的静态登记表，避免跨存档残留。 */
    @Mod.EventBusSubscriber(modid = MaidSpellMod.MOD_ID)
    public static final class ForgeEvents {

        private ForgeEvents() {
        }

        @SubscribeEvent
        public static void onServerStopped(ServerStoppedEvent event) {
            SuppressionStoneBlockEntity.clearAll();
        }
    }
}

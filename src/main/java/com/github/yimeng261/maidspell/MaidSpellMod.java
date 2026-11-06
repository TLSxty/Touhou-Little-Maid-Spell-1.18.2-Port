package com.github.yimeng261.maidspell;

import com.github.yimeng261.maidspell.item.MaidSpellItems;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * 主类（批次 A 精简版）。
 *
 * <p>移植自 1.9.2.1 反编译源 MaidSpellMod.java（141 行）。原文注册了
 * Blocks / BlockEntities / Items / CreativeTab / Containers / Sounds / Particles / Effects /
 * Entities / Paintings / Structures / StructurePieceTypes，另有
 * {@code IronsSpellbooksCompat.init} / {@code GeckoLibCompat.init} / {@code OptionalModIngredientSerializer} /
 * {@code ForgeChunkManager} 回调 / {@code NetworkHandler.registerMessages()} / 模型包安装 /
 * 6 份 config。批次 A 只保留「物品 + 1 份 config」，其余全部属于魔法或批次 B。</p>
 */
@Mod(MaidSpellMod.MOD_ID)
public class MaidSpellMod {

    public static final String MOD_ID = Config.MOD_ID;

    public MaidSpellMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        MaidSpellItems.register(modEventBus);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.BAUBLES_SPEC, "touhou_little_maid_spell-baubles.toml");
        // 事件订阅者（MaidSpellEventHandler）由 @Mod.EventBusSubscriber 自动注册，无需在此登记。
    }
}

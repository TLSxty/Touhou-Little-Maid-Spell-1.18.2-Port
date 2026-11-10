// 移植自 1.9.2.1 反编译源 mixin/tlm/EntityMaidStructureSpawnMixin.java（1.20.1）。
//
// 目的：TLM 的 EntityMaid#finalizeSpawn 在 reason == STRUCTURE 时会给女仆随机挑一个模型
// （ServerCustomPackLoader.SERVER_MAID_MODELS.getModelIdSet().stream().skip(rand).findFirst()），
// 把结构模板里预设的 ModelId 覆盖掉。本模组 4 个结构的 nbt 都带「预设外观 + 预设饰品」的女仆，
// 被覆盖后就变成随机外观，玩家看到的不是我们摆的那只。
//
// 做法：在 finalizeSpawn 的 HEAD 处直接 return spawnDataIn，整段跳过。只在
// PROTECTED_STRUCTURES 里的结构内生效，其它地方的女仆生成完全不受影响。
//
// 与上游的差异（1.18.2 适配）：
//   1. 1.18.2 的结构是多层注册：StructureFeature（玩法）→ ConfiguredStructureFeature（配置）→ StructureSet（分布）。
//      没有 1.20 的 Registry<Structure>，因此用 Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY 取 holder；
//      结构管理器是 StructureFeatureManager，查询用 getStructureAt(BlockPos, ConfiguredStructureFeature)。
//   2. 上游在 hidden_retreat 里还会调 MaidOriginData.markHiddenRetreatMaid(maid) 打来源标记，
//      本工程未移植那个数据类（属 1.20 的记忆/溯源系统），此处不做。
package com.github.yimeng261.maidspell.mixin.tlm;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.yimeng261.maidspell.Config;
import com.github.yimeng261.maidspell.Global;
import com.github.yimeng261.maidspell.worldgen.structure.MaidStructureLoadout;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.levelgen.feature.ConfiguredStructureFeature;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(value = EntityMaid.class, remap = false)
public class EntityMaidStructureSpawnMixin {

    /** 需要保留预设外观与本模组预设内容的四个结构。 */
    private static final List<String> PROTECTED_STRUCTURES = List.of(
            "hidden_retreat", "relic_sanctum", "fallen_sanctum", "elven_realm");

    @Shadow
    private boolean structureSpawn;

    @Inject(
            method = "finalizeSpawn(Lnet/minecraft/world/level/ServerLevelAccessor;Lnet/minecraft/world/DifficultyInstance;"
                    + "Lnet/minecraft/world/entity/MobSpawnType;Lnet/minecraft/world/entity/SpawnGroupData;"
                    + "Lnet/minecraft/nbt/CompoundTag;)Lnet/minecraft/world/entity/SpawnGroupData;",
            at = @At("HEAD"),
            cancellable = true,
            remap = true)
    private void maidspell$skipRandomModelForProtectedStructures(ServerLevelAccessor worldIn,
                                                                DifficultyInstance difficultyIn,
                                                                MobSpawnType reason,
                                                                SpawnGroupData spawnDataIn,
                                                                CompoundTag dataTag,
                                                                CallbackInfoReturnable<SpawnGroupData> cir) {
        try {
            if (reason != MobSpawnType.STRUCTURE) {
                return;
            }
            EntityMaid maid = (EntityMaid) (Object) this;
            BlockPos maidPos = maid.blockPosition();
            // 先套用按维度的血量/背包/饰品载荷，再决定是否跳过随机外观。
            // WorldGenLevel#getLevel() 在 1.18.2 的静态类型已是 ServerLevel，无需 instanceof。
            MaidStructureLoadout.apply(maid, worldIn.getLevel());
            if (maidspell$shouldSkipRandomModel(worldIn, maidPos)) {
                this.structureSpawn = false;
                Global.LOGGER.debug("已跳过结构女仆的随机外观初始化（保留结构预设 NBT），位置 {}", maidPos);
                cir.setReturnValue(spawnDataIn);
            }
        } catch (Exception e) {
            Global.LOGGER.error("判断结构女仆是否应跳过随机外观时出错", e);
        }
    }

    @Unique
    private boolean maidspell$shouldSkipRandomModel(ServerLevelAccessor worldIn, BlockPos pos) {
        StructureFeatureManager structureManager = worldIn.getLevel().structureFeatureManager();
        Registry<ConfiguredStructureFeature<?, ?>> registry = worldIn.getLevel().registryAccess()
                .registryOrThrow(Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY);
        for (String id : PROTECTED_STRUCTURES) {
            if (maidspell$isInsideStructure(structureManager, registry, pos, id)) {
                return true;
            }
        }
        return false;
    }

    @Unique
    private static boolean maidspell$isInsideStructure(StructureFeatureManager structureManager,
                                                       Registry<ConfiguredStructureFeature<?, ?>> registry,
                                                       BlockPos pos, String structureId) {
        ConfiguredStructureFeature<?, ?> configured =
                registry.get(new ResourceLocation(Config.MOD_ID, structureId));
        if (configured == null) {
            return false;
        }
        StructureStart start = structureManager.getStructureAt(pos, configured);
        return start != null && start.isValid();
    }
}

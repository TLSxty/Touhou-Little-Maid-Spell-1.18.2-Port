package com.github.yimeng261.maidspell.mixin;

import com.github.tartaricacid.touhoulittlemaid.entity.ai.navigation.MaidNodeEvaluator;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.yimeng261.maidspell.item.MaidSpellItems;
import com.github.yimeng261.maidspell.spell.manager.BaubleStateManager;
import com.github.yimeng261.maidspell.utils.FoxLeafSurfaceHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 让带熔岩狐叶的女仆把「岩浆表面」当作可行走节点（否则寻路会绕开岩浆）。
 * <p>
 * 移植自 1.9.2.1 反编译源 mixin/tlm/MaidNodeEvaluatorMixin.java。
 * TLM 1.18.2 的 {@code MaidNodeEvaluator} 只覆写了一个路径计算方法
 * {@code getBlockPathType(BlockGetter,int,int,int): BlockPathTypes}（javap 实测，
 * 其 SRG 名 {@code m_8086_} 在 official 映射下就是 {@code getBlockPathType}），
 * 因此注入点写 official 名 + 完整描述符（该抽象类有两个同名的 {@code getBlockPathType} 重载）。
 */
@Mixin(MaidNodeEvaluator.class)
public abstract class MaidNodeEvaluatorMixin {

    /** 继承自 {@code NodeEvaluator} 的受保护字段，当前正在寻路的生物。 */
    @Shadow
    protected Mob mob;

    @Inject(method = "getBlockPathType(Lnet/minecraft/world/level/BlockGetter;III)Lnet/minecraft/world/level/pathfinder/BlockPathTypes;",
            at = @At("HEAD"), cancellable = true)
    private void maidspell$lavaSurfaceWalkable(BlockGetter level, int x, int y, int z,
                                               CallbackInfoReturnable<BlockPathTypes> cir) {
        if (!(this.mob instanceof EntityMaid maid)) {
            return;
        }
        if (!BaubleStateManager.hasBauble(maid, MaidSpellItems.MOLTEN_FOX_LEAF)) {
            return;
        }
        BlockPos pos = new BlockPos(x, y, z);
        BlockState state = level.getBlockState(pos);
        if (!state.getCollisionShape(level, pos).isEmpty()) {
            return;
        }
        if (FoxLeafSurfaceHelper.isFluidSurface(level, pos, FluidTags.LAVA)) {
            cir.setReturnValue(BlockPathTypes.WALKABLE);
        }
    }
}

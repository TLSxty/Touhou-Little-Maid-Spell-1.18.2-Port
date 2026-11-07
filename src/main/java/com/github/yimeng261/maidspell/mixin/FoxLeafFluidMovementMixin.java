package com.github.yimeng261.maidspell.mixin;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.yimeng261.maidspell.item.MaidSpellItems;
import com.github.yimeng261.maidspell.spell.manager.BaubleStateManager;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 熔岩狐叶：站在岩浆表面时不被打散（流体推力关闭）。
 * <p>
 * 移植自 1.9.2.1 反编译源 mixin/tlm/EntityMaidFoxLeafMovementMixin.java。
 * <strong>与上游的差异</strong>：
 * <ul>
 *   <li>上游注入的是 {@code EntityMaid}，但 TLM 1.18.2 的 {@code EntityMaid} 并未覆写
 *       {@code isPushedByFluid}（javap 实测），无法作为注入目标；这里改为注入
 *       {@link Entity} 并在方法内判定实例类型，效果相同。</li>
 *   <li>上游该 mixin 还有一条 {@code canTeleportTo(BlockPos)} 注入，用于让女仆传送到
 *       岩浆面上的安全点。1.18.2 的 {@code Entity}/{@code EntityMaid} 都没有这个方法
 *       （全 jar 只在 {@code FollowOwnerGoal} 里出现过同名字符串），无法移植，已删除。</li>
 * </ul>
 */
@Mixin(Entity.class)
public abstract class FoxLeafFluidMovementMixin {

    @Inject(method = "isPushedByFluid", at = @At("HEAD"), cancellable = true)
    private void maidspell$ignoreLavaPush(CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof EntityMaid maid) || maid.isUnderWater()) {
            return;
        }
        if (!BaubleStateManager.hasBauble(maid, MaidSpellItems.MOLTEN_FOX_LEAF)) {
            return;
        }
        FluidState feet = maid.getLevel().getFluidState(maid.blockPosition());
        FluidState below = maid.getLevel().getFluidState(maid.blockPosition().below());
        if (feet.is(FluidTags.LAVA) || below.is(FluidTags.LAVA)) {
            cir.setReturnValue(false);
        }
    }
}

package com.github.yimeng261.maidspell.mixin;

import com.github.yimeng261.maidspell.dimension.RetreatDimensionFactory;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 在新建世界之前把缺失的退隐维度补进世界生成设置。
 * <p>
 * 原版 {@code createLevels} 只遍历 level.dat 里的 {@code WorldGenSettings.dimensions()} 快照，
 * 旧存档不会因为数据包新增了维度定义就跟着补建。这里挂在 HEAD：抢在遍历之前把 LevelStem 注册好，
 * 之后仍由原版循环创建 ServerLevel（原版那套初始化步骤一步都不能少，否则区块不会生成）。
 */
@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {

    @Inject(method = "createLevels", at = @At("HEAD"))
    private void maidspell$registerRetreatDimensionStem(CallbackInfo ci) {
        RetreatDimensionFactory.registerSharedStemIfMissing((MinecraftServer) (Object) this);
    }
}

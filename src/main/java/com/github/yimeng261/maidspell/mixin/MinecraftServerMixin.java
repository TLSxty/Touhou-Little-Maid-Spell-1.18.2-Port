package com.github.yimeng261.maidspell.mixin;

import com.github.yimeng261.maidspell.dimension.RetreatDimensionFactory;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 在原版维度全部建完之后补建退隐维度。
 * <p>
 * 原版 {@code loadLevel} 里调的 {@code createLevels} 只认 level.dat 里的
 * {@code WorldGenSettings.dimensions()} 快照，旧存档不会因为数据包新增了维度定义就跟着补建。
 * 这里挂在 TAIL：此时原版维度都已建好、{@code storageSource} 也可用，而玩家尚未登录。
 */
@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {

    @Shadow
    @Final
    protected LevelStorageSource.LevelStorageAccess storageSource;

    @Inject(method = "loadLevel", at = @At("TAIL"))
    private void maidspell$createRetreatDimension(CallbackInfo ci) {
        RetreatDimensionFactory.createSharedIfMissing((MinecraftServer) (Object) this, this.storageSource);
    }
}

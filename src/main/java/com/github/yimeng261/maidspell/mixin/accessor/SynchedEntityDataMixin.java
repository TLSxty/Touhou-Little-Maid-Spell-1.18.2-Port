package com.github.yimeng261.maidspell.mixin.accessor;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.network.syncher.SynchedEntityData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 读取 SynchedEntityData 的 id → DataItem 映射（真伤写血的依据）。
 */
@Mixin(SynchedEntityData.class)
public interface SynchedEntityDataMixin {

    @Accessor("itemsById")
    Int2ObjectMap<SynchedEntityData.DataItem<?>> getItemsById();
}

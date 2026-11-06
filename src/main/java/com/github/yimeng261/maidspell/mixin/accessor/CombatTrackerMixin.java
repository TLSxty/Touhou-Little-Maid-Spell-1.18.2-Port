package com.github.yimeng261.maidspell.mixin.accessor;

import net.minecraft.world.damagesource.CombatEntry;
import net.minecraft.world.damagesource.CombatTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

/**
 * 读取 CombatTracker 内部战斗记录列表（1.18.2 无公开 getter）。
 */
@Mixin(CombatTracker.class)
public interface CombatTrackerMixin {

    @Accessor("entries")
    List<CombatEntry> getEntries();
}

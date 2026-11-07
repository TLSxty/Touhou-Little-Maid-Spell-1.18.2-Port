// 移植自 1.9.2.1 反编译源 mixin/tlm/MaidFeedOwnerTaskMixin.java。
// 上游还有一段「竞技场抑制喂食」（MaidSuppressionZone），本工程未移植该区域系统，已删；
// 只保留「女仆喂食主人后触发馥郁巧思的正面效果」这一段。
package com.github.yimeng261.maidspell.mixin.tlm;

import com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.task.MaidFeedOwnerTask;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.yimeng261.maidspell.item.bauble.fragrantIngenuity.FragrantIngenuityBauble;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 给 TLM 的「喂食主人」行为挂钩子，使馥郁巧思的「喂食给主人正面效果」真正生效。
 *
 * <p>为什么不放在自己的事件里：喂食动作完全发生在 {@link MaidFeedOwnerTask#start} 内部
 * （把食物从女仆背包取出、喂给主人），TLM 没有对外的喂食完成事件，
 * 只能在 {@code Optional.ifPresent(...)} 之后注入 —— 那一刻食物已经交到主人手上。</p>
 */
@Mixin(value = MaidFeedOwnerTask.class, remap = false)
public class MaidFeedOwnerTaskMixin {

    // 目标方法必须写「方法名 + 完整描述符」：TLM 的方法名不在 MC 映射表里，
    // 只写 "start" 会报 Unable to locate obfuscation mapping for @Inject target start（实测）。
    @Inject(method = "start(Lnet/minecraft/server/level/ServerLevel;Lcom/github/tartaricacid/touhoulittlemaid/entity/passive/EntityMaid;J)V",
            at = @At(value = "INVOKE",
            target = "Ljava/util/Optional;ifPresent(Ljava/util/function/Consumer;)V",
            shift = At.Shift.AFTER))
    private void maidspell$applyOwnerBuffAfterFeed(ServerLevel level, EntityMaid maid, long gameTime, CallbackInfo ci) {
        LivingEntity owner = maid.getOwner();
        if (owner instanceof Player player) {
            FragrantIngenuityBauble.applyOwnerFeedBuff(maid, player);
        }
    }
}

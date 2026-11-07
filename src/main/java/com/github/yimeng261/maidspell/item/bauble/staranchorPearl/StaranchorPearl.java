package com.github.yimeng261.maidspell.item.bauble.staranchorPearl;

import com.github.yimeng261.maidspell.effect.MaidSpellEffects;
import com.github.yimeng261.maidspell.effect.StaranchorStunEffect;
import com.github.yimeng261.maidspell.effect.VoidWalkEffect;
import com.github.yimeng261.maidspell.item.MaidSpellCreativeTab;
import com.github.yimeng261.maidspell.utils.TooltipHelper;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * 星锚珍珠（物品）。
 * <p>
 * 移植自 1.9.2.1 反编译源 item/bauble/staranchorPearl/StaranchorPearl.java。
 * 与原文的差异只有命名：1.20 的 {@code m_237115_ / m_237110_} 改写为
 * {@code new TranslatableComponent(...)}，SRG 方法名换成 1.18.2 官方名。
 * 效果本身不含任何魔法内容（自定义投掷物在原文里就没有，物品直接用 {@code use} 触发）。
 */
public class StaranchorPearl extends Item {

    public static final int VOID_WALK_TICKS = 700;
    public static final int USE_COOLDOWN_TICKS = 600;
    public static final int DURABILITY = 64;
    public static final int PLAYER_USE_COST = 8;
    public static final int MAID_BLOCK_COST = 1;

    public StaranchorPearl() {
        super(new Item.Properties()
                .stacksTo(1)
                .durability(DURABILITY)
                .setNoRepair()
                .rarity(Rarity.RARE)
                .tab(MaidSpellCreativeTab.BAUBLE_TAB));
    }

    @Override
    public boolean isFoil(@Nonnull ItemStack stack) {
        return true;
    }

    @Override
    @Nonnull
    public InteractionResultHolder<ItemStack> use(@Nonnull Level level, @Nonnull Player player,
                                                  @Nonnull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.success(stack);
        }
        if (player.hasEffect(MaidSpellEffects.VOID_WALK.get())) {
            return InteractionResultHolder.fail(stack);
        }
        if (level instanceof ServerLevel serverLevel) {
            player.getCooldowns().addCooldown(this, USE_COOLDOWN_TICKS);
            player.addEffect(new MobEffectInstance(MaidSpellEffects.VOID_WALK.get(),
                    VOID_WALK_TICKS, 0, false, false, true));
            VoidWalkEffect.spawnRing(serverLevel, player);
            StaranchorStunEffect.shockwave(serverLevel, player);
            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 1.0f, 1.0f);
            stack.hurtAndBreak(PLAYER_USE_COST, player, broken -> broken.broadcastBreakEvent(hand));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(@Nonnull ItemStack stack, @Nullable Level level,
                                @Nonnull List<Component> tooltip, @Nonnull TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        TooltipHelper.addShiftTooltip(
                tooltip,
                List.of(new TranslatableComponent("item.touhou_little_maid_spell.staranchor_pearl.desc1")
                        .withStyle(ChatFormatting.GRAY)),
                List.of(
                        new TranslatableComponent("item.touhou_little_maid_spell.staranchor_pearl.desc2",
                                PLAYER_USE_COST, VOID_WALK_TICKS / 20, USE_COOLDOWN_TICKS / 20)
                                .withStyle(ChatFormatting.LIGHT_PURPLE),
                        new TranslatableComponent("item.touhou_little_maid_spell.staranchor_pearl.desc3",
                                MAID_BLOCK_COST)
                                .withStyle(ChatFormatting.DARK_PURPLE),
                        new TranslatableComponent("item.touhou_little_maid_spell.staranchor_pearl.desc4")
                                .withStyle(ChatFormatting.YELLOW)));
    }
}

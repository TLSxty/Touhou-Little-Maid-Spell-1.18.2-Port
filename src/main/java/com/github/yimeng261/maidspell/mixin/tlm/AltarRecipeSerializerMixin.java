package com.github.yimeng261.maidspell.mixin.tlm;

import com.github.tartaricacid.touhoulittlemaid.crafting.AltarRecipe;
import com.github.tartaricacid.touhoulittlemaid.crafting.AltarRecipeSerializer;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 祭坛配方反序列化补丁 —— 过滤掉解析失败的空 Ingredient。
 *
 * <p><b>为什么必须保留</b>：TLM 1.2.2 的祭坛配方（{@code data/<mod>/recipes/altar/*.json}）
 * 在 {@code fromJson} 里把每个 ingredient 原样塞进数组；本模组的配方里含有需要可选模组的
 * ingredient，缺失时会解析成空 {@link Ingredient}。空 ingredient 会污染
 * {@code AltarRecipe.matches}，导致整条配方**永远匹配不上**，饰品因此无法合成。
 * 这里在 {@code fromJson} 返回后重建一个只含非空 ingredient 的配方。
 *
 * <p><b>{@code remap = false} 的原因</b>：{@code AltarRecipeSerializer} 是 TLM 的类，
 * 不在 MC 映射表里。但它的 {@code fromJson} 实现的是 MC 的
 * {@code RecipeSerializer#fromJson}，运行时名字是 SRG 名 {@code m_6729_}（javap 实测
 * TLM 1.2.2 jar 内同时存在 {@code fromJson} 与 {@code m_6729_} 两个签名）。因此这里
 * 同时给出两个候选名：开发环境匹配 {@code fromJson}，生产环境匹配 {@code m_6729_}。
 * 方法体里对 {@code getIngredients()}/{@code Ingredient.isEmpty()} 的引用走 refmap 正常重映射。
 */
@Mixin(value = AltarRecipeSerializer.class, remap = false)
public class AltarRecipeSerializerMixin {

    @Inject(
            method = {"fromJson", "m_6729_"},
            at = @At(value = "RETURN"),
            cancellable = true,
            remap = false
    )
    private void maidspell$filterEmptyIngredients(ResourceLocation recipeId, JsonObject json,
                                                  CallbackInfoReturnable<AltarRecipe> cir) {
        AltarRecipe recipe = cir.getReturnValue();
        if (recipe == null) {
            return;
        }
        Ingredient[] filtered = recipe.getIngredients().stream()
                .filter(ingredient -> ingredient != null && !ingredient.isEmpty())
                .toArray(Ingredient[]::new);
        if (filtered.length == 0 || filtered.length == recipe.getIngredients().size()) {
            return;
        }
        cir.setReturnValue(new AltarRecipe(recipeId, recipe.getEntityType(), recipe.getExtraData(),
                recipe.getPowerCost(), recipe.getCopyInput(), recipe.getCopyTag(), filtered));
    }
}

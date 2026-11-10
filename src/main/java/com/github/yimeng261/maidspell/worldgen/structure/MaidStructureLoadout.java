package com.github.yimeng261.maidspell.worldgen.structure;

import com.github.tartaricacid.touhoulittlemaid.entity.backpack.BackpackManager;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.item.bauble.BaubleManager;
import com.github.yimeng261.maidspell.Config;
import com.github.yimeng261.maidspell.Global;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * 结构女仆的「按维度载荷」：血量、背包类型、随机背包物品、随机饰品。
 * <p>
 * 设计口径（用户 m02662 / m02813 / m02827 / m02841 拍板）：
 * <ul>
 *   <li>主世界：50 血 / 小背包(12 格) / 低档饰品 ×1；</li>
 *   <li>下界：100 血 / 中背包(24 格) / 中档饰品 ×1；</li>
 *   <li>末地：100 血 / 大背包(36 格) / 中档饰品 ×3（同一只女仆不重复）；</li>
 *   <li>隐世之地（{@code touhou_little_maid_spell:the_retreat}）：整档跳过，保留手工摆的预设女仆；</li>
 *   <li>其它维度：同样跳过，不去猜。</li>
 * </ul>
 * <p>
 * 只在 {@code MobSpawnType.STRUCTURE} 时由 {@code EntityMaidStructureSpawnMixin} 调用，
 * 不影响玩家驯服、刷怪蛋、女仆繁殖等途径生成的女仆。
 * <p>
 * 为什么在 {@code finalizeSpawn} 里做：{@code StructureTemplate.addEntitiesToWorld} 的顺序是
 * 读 nbt → {@code finalizeSpawn} → {@code addFreshEntityWithPassengers}，此处写入是最终值，
 * 不会被 nbt 覆盖回去。
 */
public final class MaidStructureLoadout {

    /** TLM 本体命名空间，低档饰品池从中扫描。 */
    private static final String TLM_NAMESPACE = "touhou_little_maid";

    /**
     * 中档（下界 / 末地）用品：用户手工圈定的 7 个。
     * 淘汰掉的是全部 EPIC 级，以及会改写伤害结算链的几个（发簪 / 双心之链 / 烬血之戒 / 梦云水晶 /
     * 星锚珍珠 / 混沌之书 / 魂之书 / 喋血之心 / 流转核心 / 破愈咒锋 / 馥郁巧思）。
     */
    private static final List<String> MID_TIER_IDS = List.of(
            "sliver_cercis",
            "transmog_necklace",
            "ender_pocket",
            "rock_crystal",
            "floating_fox_leaf",
            "molten_fox_leaf",
            "spring_bloom_return");

    private static final ResourceLocation OVERWORLD_GEAR_TABLE =
            new ResourceLocation(Config.MOD_ID, "chests/maid_gear_overworld");
    private static final ResourceLocation NETHER_GEAR_TABLE =
            new ResourceLocation(Config.MOD_ID, "chests/maid_gear_nether");
    private static final ResourceLocation END_GEAR_TABLE =
            new ResourceLocation(Config.MOD_ID, "chests/maid_gear_end");

    /** 隐世之地的维度键，整档不处理。 */
    private static final ResourceLocation RETREAT_DIMENSION =
            new ResourceLocation(Config.MOD_ID, "the_retreat");

    /** 饰品池档次。 */
    private enum Tier {
        /** TLM 本体饰品（默认 COMMON，只保护、不改伤害链）。 */
        LOW,
        /** 用户圈定的 7 个中档饰品。 */
        MID
    }

    /** 单个维度的载荷结论。 */
    private record Profile(double maxHealth, ResourceLocation backpackId, String backpackName,
                           int baubleCount, Tier tier, ResourceLocation gearTable) {
    }

    private MaidStructureLoadout() {
    }

    /**
     * 给一只刚由结构生成的女仆套用对应维度的载荷。
     *
     * @param maid  目标女仆
     * @param level 她所在的服务器世界
     */
    public static void apply(EntityMaid maid, ServerLevel level) {
        if (!Config.structureMaidLoadoutEnabled) {
            return;
        }
        Profile profile = maidspell$profileFor(level);
        if (profile == null) {
            return;
        }
        Random random = level.getRandom();

        maidspell$applyHealth(maid, profile.maxHealth());
        maidspell$applyBackpack(maid, profile);
        maidspell$applyGear(maid, level, profile, random);
        maidspell$applyBaubles(maid, profile, random);

        Global.LOGGER.debug("已套用结构女仆载荷：维度 {}，血量 {}，背包 {}，饰品 {} 个（{}）",
                level.dimension().location(), profile.maxHealth(), profile.backpackName(),
                profile.baubleCount(), profile.tier());
    }

    /** 维度 → 载荷表；返回 {@code null} 表示这一维度不参与。 */
    private static Profile maidspell$profileFor(ServerLevel level) {
        ResourceLocation dimension = level.dimension().location();
        if (RETREAT_DIMENSION.equals(dimension)) {
            return null;
        }
        if (Level.OVERWORLD.location().equals(dimension)) {
            return new Profile(50.0D, new ResourceLocation(TLM_NAMESPACE, "small_backpack"),
                    "small_backpack", 1, Tier.LOW, OVERWORLD_GEAR_TABLE);
        }
        if (Level.NETHER.location().equals(dimension)) {
            return new Profile(100.0D, new ResourceLocation(TLM_NAMESPACE, "middle_backpack"),
                    "middle_backpack", 1, Tier.MID, NETHER_GEAR_TABLE);
        }
        if (Level.END.location().equals(dimension)) {
            return new Profile(100.0D, new ResourceLocation(TLM_NAMESPACE, "big_backpack"),
                    "big_backpack", 3, Tier.MID, END_GEAR_TABLE);
        }
        return null;
    }

    private static void maidspell$applyHealth(EntityMaid maid, double maxHealth) {
        if (maid.getAttribute(Attributes.MAX_HEALTH) != null) {
            maid.getAttribute(Attributes.MAX_HEALTH).setBaseValue(maxHealth);
        }
        maid.setHealth((float) maxHealth);
    }

    private static void maidspell$applyBackpack(EntityMaid maid, Profile profile) {
        BackpackManager.findBackpack(profile.backpackId())
                .ifPresentOrElse(maid::setMaidBackpackType,
                        () -> Global.LOGGER.warn("找不到背包类型 {}，保留女仆原有背包", profile.backpackId()));
    }

    /**
     * 清空主背包后按维度表抽 6~10 次填入。
     * <p>
     * 只动 {@code MaidInventory}（底层恒为 36 格），不碰隐藏栏与任务栏。
     * 背包类型只决定 GUI 能显示多少格，所以小背包也放得下。
     */
    private static void maidspell$applyGear(EntityMaid maid, ServerLevel level, Profile profile, Random random) {
        for (int slot = 0; slot < maid.getMaidInv().getSlots(); slot++) {
            maid.getMaidInv().extractItem(slot, Integer.MAX_VALUE, false);
        }
        LootTable table = level.getServer().getLootTables().get(profile.gearTable());
        if (table == LootTable.EMPTY) {
            Global.LOGGER.warn("找不到结构女仆物品表 {}，本次只清空背包", profile.gearTable());
            return;
        }
        LootContext context = new LootContext.Builder(level)
                .withParameter(LootContextParams.ORIGIN, maid.position())
                .withOptionalParameter(LootContextParams.THIS_ENTITY, maid)
                .withRandom(random)
                .create(LootContextParamSets.CHEST);
        List<ItemStack> loot = table.getRandomItems(context);
        int slot = 0;
        for (ItemStack stack : loot) {
            if (stack.isEmpty() || slot >= maid.getMaidInv().getSlots()) {
                continue;
            }
            maid.getMaidInv().insertItem(slot, stack, false);
            slot++;
        }
    }

    /** 清空饰品栏后按池抽不重复的 N 个，从 0 号槽起连续摆放。 */
    private static void maidspell$applyBaubles(EntityMaid maid, Profile profile, Random random) {
        for (int slot = 0; slot < maid.getMaidBauble().getSlots(); slot++) {
            maid.getMaidBauble().extractItem(slot, Integer.MAX_VALUE, false);
        }
        List<Item> pool = MaidBaubleCache.pool(profile.tier());
        if (pool.isEmpty()) {
            Global.LOGGER.warn("结构女仆饰品池（{}）为空，跳过饰品发放", profile.tier());
            return;
        }
        List<Item> shuffled = new ArrayList<>(pool);
        Collections.shuffle(shuffled, random);
        int count = Math.min(profile.baubleCount(), Math.min(shuffled.size(), maid.getMaidBauble().getSlots()));
        for (int index = 0; index < count; index++) {
            maid.getMaidBauble().insertItem(index, new ItemStack(shuffled.get(index)), false);
        }
    }

    /** 中档 id 列表，供日志与测试核对。 */
    public static List<String> midTierIds() {
        return MID_TIER_IDS;
    }

    /** 供外部在物品注册表重载时清缓存。 */
    public static void invalidateCaches() {
        MaidBaubleCache.invalidate();
    }

    /**
     * 饰品池缓存。
     * <p>
     * TLM 的 {@code BaubleManager.BAUBLES} 是 private static，无法直接枚举，
     * 因此低档池靠遍历物品注册表 + {@link BaubleManager#getBauble(ItemStack)} 判定，
     * 且只在第一次用到时扫一次。
     */
    private static final class MaidBaubleCache {

        private static volatile List<Item> lowTier;
        private static volatile List<Item> midTier;

        private MaidBaubleCache() {
        }

        static List<Item> pool(Tier tier) {
            return tier == Tier.LOW ? lowTier() : midTier();
        }

        private static List<Item> lowTier() {
            List<Item> cached = lowTier;
            if (cached == null) {
                cached = maidspell$scanLowTier();
                lowTier = cached;
            }
            return cached;
        }

        private static List<Item> midTier() {
            List<Item> cached = midTier;
            if (cached == null) {
                List<Item> items = new ArrayList<>();
                for (String id : MID_TIER_IDS) {
                    Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(Config.MOD_ID, id));
                    if (item == null) {
                        Global.LOGGER.warn("结构女仆中档饰品池缺少物品 {}", id);
                        continue;
                    }
                    if (BaubleManager.getBauble(new ItemStack(item)) == null) {
                        Global.LOGGER.warn("物品 {} 未注册为女仆饰品，从中档池剔除", id);
                        continue;
                    }
                    items.add(item);
                }
                cached = List.copyOf(items);
                midTier = cached;
            }
            return cached;
        }

        /** 扫 TLM 命名空间下所有注册为饰品（{@code BaubleManager.getBauble != null}）的物品。 */
        private static List<Item> maidspell$scanLowTier() {
            List<Item> items = new ArrayList<>();
            for (Item item : ForgeRegistries.ITEMS) {
                ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
                if (id == null || !TLM_NAMESPACE.equals(id.getNamespace())) {
                    continue;
                }
                if (BaubleManager.getBauble(new ItemStack(item)) != null) {
                    items.add(item);
                }
            }
            Global.LOGGER.info("结构女仆低档饰品池已建立，共 {} 项", items.size());
            return List.copyOf(items);
        }

        static void invalidate() {
            lowTier = null;
            midTier = null;
        }
    }
}

package com.github.yimeng261.maidspell.item.common;

import com.github.yimeng261.maidspell.Config;
import com.github.yimeng261.maidspell.dimension.RetreatChunkTickets;
import com.github.yimeng261.maidspell.dimension.TheRetreatDimension;
import com.github.yimeng261.maidspell.item.MaidSpellCreativeTab;
import com.github.yimeng261.maidspell.sound.MaidSpellSounds;
import com.github.yimeng261.maidspell.utils.TooltipHelper;
import com.mojang.datafixers.util.Pair;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.ConfiguredStructureFeature;

/**
 * 寻风之铃（{@code wind_seeking_bell}）。
 *
 * <p>移植自 1.9.2.1 反编译源 {@code item/common/WindSeekingBell/WindSeekingBell.java}（303 行）。
 * 上游是「进入退隐维度 / 搜索隐世之境 / 返回主世界」的唯一玩家入口。</p>
 *
 * <p><b>与上游的三处差异（D1=C / D9 / D10）</b></p>
 * <ol>
 *   <li><b>不做个人维度、不做配额</b>：上游走 {@code PlayerRetreatManager} /
 *       {@code RetreatDimensionData} / {@code SharedRetreatManager} 三层，本工程只认共享维度
 *       {@code touhou_little_maid_spell:the_retreat}，因此没有「首次登记发放配额」这一步。</li>
 *   <li><b>搜索改用 {@code ChunkGenerator.findNearestMapFeature}</b>（D9）：不搬
 *       {@code StructureSearchWorker}（297 行 + 逐 tick 环形扫描 + 负缓存），一次性同步查询。
 *       代价是没有「等待队列」的中间反馈，因此 {@code waiting_for_structure} /
 *       {@code waiting_in_queue} / {@code structure_generated} 三条文案本版不会用到。</li>
 *   <li><b>不生成寻风铃实体</b>：上游会 spawn 一个 {@code WindSeekingBellEntity} 飞向结构，
 *       该实体属未移植内容，本版改为直接在聊天栏给出可点击的 {@code /tp} 指令。</li>
 * </ol>
 *
 * <p><b>必须注意</b>：{@code findNearestMapFeature} 的第一参是<b>要搜索的那个维度自己的
 * {@link ServerLevel}</b>（它内部取 {@code level.getSeed()} 与该 level 的
 * {@code structureFeatureManager()}，biome 过滤用 generator 自己的 {@code runtimeBiomeSource}）。
 * 用主世界的 level + 退隐维度的 generator 混搭会得到错误结果。</p>
 */
public class WindSeekingBell extends Item {

    /** 隐世之境的结构 id（与 {@code worldgen/configured_structure_feature/hidden_retreat.json} 对应）。 */
    private static final ResourceKey<ConfiguredStructureFeature<?, ?>> HIDDEN_RETREAT_KEY =
            ResourceKey.create(Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY,
                    new ResourceLocation(Config.MOD_ID, "hidden_retreat"));

    /** 搜索半径（区块）。上游用 StructureSearchWorker 的 maxRadius=100。 */
    private static final int SEARCH_RADIUS_CHUNKS = 100;

    /** 找到结构后强制加载的区块半径（保证 /tp 落地不是虚空）。 */
    private static final int FORCE_LOAD_RADIUS = 2;

    /** 搜索冷却（tick）。上游无冷却（靠配额限制），本版无配额，改用 5 秒冷却防连点。 */
    private static final int SEARCH_COOLDOWN_TICKS = 100;

    public WindSeekingBell() {
        super(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)
                .tab(MaidSpellCreativeTab.BAUBLE_TAB));
    }

    @Override
    public boolean isFoil(@Nonnull ItemStack stack) {
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);

        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.consume(stack);
        }

        MinecraftServer server = serverPlayer.getServer();
        if (server == null) {
            return InteractionResultHolder.success(stack);
        }

        boolean inRetreat = TheRetreatDimension.isInRetreat(serverPlayer);

        // ① 退隐维度内 + Shift：返回主世界。
        if (inRetreat && serverPlayer.isShiftKeyDown()) {
            TheRetreatDimension.teleportFromRetreat(serverPlayer);
            message(serverPlayer, "return_to_overworld", ChatFormatting.GREEN);
            return InteractionResultHolder.success(stack);
        }

        // ② 退隐维度内：搜索最近的隐世之境。
        if (inRetreat) {
            findHiddenRetreat(serverPlayer, serverPlayer.getLevel());
            return InteractionResultHolder.success(stack);
        }

        // ③ 主世界（或其他维度）：进入共享退隐维度。
        ServerLevel retreatLevel = server.getLevel(TheRetreatDimension.getSharedRetreatDimension());
        if (retreatLevel == null) {
            message(serverPlayer, "teleport_failed", ChatFormatting.RED);
            return InteractionResultHolder.success(stack);
        }
        TheRetreatDimension.teleportToRetreat(serverPlayer, retreatLevel);
        message(serverPlayer, "entered_retreat", ChatFormatting.GREEN);
        playBell(serverPlayer);
        return InteractionResultHolder.success(stack);
    }

    // ------------------------------------------------------------------ 搜索

    private void findHiddenRetreat(ServerPlayer player, ServerLevel retreatLevel) {
        if (player.getCooldowns().isOnCooldown(this)) {
            return;
        }
        player.getCooldowns().addCooldown(this, SEARCH_COOLDOWN_TICKS);

        long startedAt = System.currentTimeMillis();
        playBell(player);

        Holder<ConfiguredStructureFeature<?, ?>> holder = retreatLevel.registryAccess()
                .registryOrThrow(Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY)
                .getHolder(HIDDEN_RETREAT_KEY)
                .orElse(null);
        if (holder == null) {
            // 数据包没加载到本结构：属于配置错误，不能静默当成「附近没有」。
            player.displayClientMessage(
                    new TranslatableComponent("item." + Config.MOD_ID + ".wind_seeking_bell.search_error")
                            .withStyle(ChatFormatting.RED), false);
            return;
        }

        // 必须用退隐维度自己的 level（见类注释）。
        Pair<BlockPos, Holder<ConfiguredStructureFeature<?, ?>>> found =
                retreatLevel.getChunkSource().getGenerator().findNearestMapFeature(
                        retreatLevel, HolderSet.direct(holder), player.blockPosition(),
                        SEARCH_RADIUS_CHUNKS, false);

        long elapsed = System.currentTimeMillis() - startedAt;

        if (found == null || found.getFirst() == null) {
            player.displayClientMessage(
                    new TranslatableComponent("item." + Config.MOD_ID + ".wind_seeking_bell.no_structure")
                            .withStyle(ChatFormatting.RED), false);
            sendSearchTime(player, elapsed);
            return;
        }

        BlockPos structurePos = found.getFirst();
        // 传送前强制加载 5×5 区块，否则 /tp 过去是未生成的虚空。
        // 必须走 RetreatChunkTickets —— 它会排定 15 秒后自动解除；直接调 setChunkForced
        // 会把这片区块永久钉在存档里（上游有 unforceLoadStructureChunks，本版自建）。
        RetreatChunkTickets.forceTemporarily(retreatLevel, structurePos, FORCE_LOAD_RADIUS,
                RetreatChunkTickets.DEFAULT_LIFETIME_TICKS);

        int surfaceY = retreatLevel.getChunkSource().getGenerator().getBaseHeight(
                structurePos.getX(), structurePos.getZ(),
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, retreatLevel);

        int distance = (int) Math.sqrt(player.blockPosition().distSqr(structurePos));

        Component teleportCommand = new TextComponent(
                "/tp " + structurePos.getX() + " " + surfaceY + " " + structurePos.getZ())
                .withStyle(style -> style
                        .withColor(ChatFormatting.AQUA)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,
                                "/tp " + structurePos.getX() + " " + surfaceY + " " + structurePos.getZ()))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                new TranslatableComponent(
                                        "item." + Config.MOD_ID + ".wind_seeking_bell.click_to_suggest")
                                        .withStyle(ChatFormatting.YELLOW))));

        player.displayClientMessage(
                new TranslatableComponent("item." + Config.MOD_ID + ".wind_seeking_bell.found_structure",
                        structurePos.getX(), surfaceY, structurePos.getZ(), distance)
                        .withStyle(ChatFormatting.GOLD)
                        .append(new TextComponent(" "))
                        .append(teleportCommand), false);
        sendSearchTime(player, elapsed);
    }

    private static void sendSearchTime(ServerPlayer player, long elapsedMillis) {
        player.displayClientMessage(
                new TranslatableComponent("item." + Config.MOD_ID + ".wind_seeking_bell.search_time",
                        elapsedMillis).withStyle(ChatFormatting.GRAY), false);
    }

    private static void playBell(ServerPlayer player) {
        player.getLevel().playSound(null, player.blockPosition(),
                MaidSpellSounds.WIND_SEEKING_BELL.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    private static void message(ServerPlayer player, String key, ChatFormatting color) {
        player.displayClientMessage(
                new TranslatableComponent("item." + Config.MOD_ID + ".wind_seeking_bell." + key)
                        .withStyle(color), false);
    }

    // ------------------------------------------------------------------ tooltip

    @Override
    public void appendHoverText(@Nonnull ItemStack stack, @Nullable Level level,
                                @Nonnull List<Component> tooltip, @Nonnull TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        String prefix = "item." + Config.MOD_ID + ".wind_seeking_bell.";
        TooltipHelper.addShiftTooltip(tooltip,
                List.of(new TranslatableComponent(prefix + "desc4").withStyle(ChatFormatting.LIGHT_PURPLE)),
                List.of(new TranslatableComponent(prefix + "desc1").withStyle(ChatFormatting.GOLD),
                        new TranslatableComponent(prefix + "desc2").withStyle(ChatFormatting.AQUA),
                        new TranslatableComponent(prefix + "desc3").withStyle(ChatFormatting.GREEN)));
    }
}

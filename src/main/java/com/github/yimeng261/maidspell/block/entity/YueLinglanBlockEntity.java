package com.github.yimeng261.maidspell.block.entity;

import com.github.yimeng261.maidspell.block.custom.YueLinglanBlock;
import com.mojang.math.Vector3f;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredStructureFeature;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 月铃兰的方块实体：定时给女仆上光环，并朝最近的「精灵领域」结构撒出一条光尘尾迹。
 * <p>
 * 移植自 1.9.2.1 反编译源 block/entity/YueLinglanBlockEntity.java（276 行）。
 * <strong>与原文的差异（1.18.2 结构系统换架构）</strong>：
 * <ul>
 *   <li>1.18.2 没有 {@code Structure} / {@code Registries}。结构注册表是
 *       {@link Registry#CONFIGURED_STRUCTURE_FEATURE_REGISTRY}，元素类型是
 *       {@link ConfiguredStructureFeature}，因此 {@code ELVEN_REALM_TAG} 的类型从
 *       {@code TagKey<Structure>} 变为 {@code TagKey<ConfiguredStructureFeature<?, ?>>}。</li>
 *   <li>1.18.2 没有 {@code level.findNearestStructure(TagKey<Structure>, ...)}，
 *       对应方法为 {@link ServerLevel#findNearestMapFeature}（第四参 {@code skipExistingChunks}）。</li>
 *   <li>1.18.2 没有 {@code StructureManager.getStructureAt(pos, TagKey)}。原文「点是否在结构内」
 *       改为遍历 tag 内的每个 {@link ConfiguredStructureFeature}，逐个
 *       {@code getStructureWithPieceAt} 判断 {@code isValid()}。</li>
 *   <li>{@code StructureManager} 在 1.18.2 叫 {@code StructureFeatureManager}，
 *       取结构用 {@code getStructureWithPieceAt} / {@code getStructureAt}，参数是
 *       {@link ConfiguredStructureFeature} 而不是 1.20 的 {@code Structure}。</li>
 *   <li>{@code org.joml.Vector3f} 不存在 → {@code com.mojang.math.Vector3f}；
 *       {@code level.random} 在 1.18.2 是 public final 字段，直接取用。</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = "touhou_little_maid_spell")
public class YueLinglanBlockEntity extends BlockEntity {

    /** 光环间隔（tick）。 */
    private static final int TICK_INTERVAL = 40;
    /** 尾迹粒子间隔（tick）。 */
    private static final int PARTICLE_INTERVAL = 8;
    /** 结构搜索缓存按 16 区块 × 16 区块分区。 */
    private static final int STRUCTURE_SEARCH_CACHE_REGION_CHUNKS = 16;
    /** 目标结构 id。 */
    private static final ResourceLocation ELVEN_REALM_ID =
            new ResourceLocation("touhou_little_maid_spell", "elven_realm");
    /** 目标结构的注册表键。 */
    private static final ResourceKey<ConfiguredStructureFeature<?, ?>> ELVEN_REALM_KEY =
            ResourceKey.create(Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY, ELVEN_REALM_ID);
    /** 同 id 的 tag（结构 tag 目录下有一份同名条目）。 */
    private static final TagKey<ConfiguredStructureFeature<?, ?>> ELVEN_REALM_TAG =
            TagKey.create(Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY, ELVEN_REALM_ID);
    /** 结构搜索半径（格）。 */
    private static final int STRUCTURE_SEARCH_RADIUS = 2560;
    /** 距结构多近就停止撒粒子（格）。 */
    private static final double MIN_PARTICLE_DISTANCE_TO_STRUCTURE = 48.0;
    private static final double MIN_PARTICLE_DISTANCE_TO_STRUCTURE_SQR =
            MIN_PARTICLE_DISTANCE_TO_STRUCTURE * MIN_PARTICLE_DISTANCE_TO_STRUCTURE;

    private static final Map<StructureSearchCacheKey, StructureSearchCacheEntry> STRUCTURE_SEARCH_CACHE =
            new ConcurrentHashMap<>();

    @Nullable
    private BlockPos cachedStructurePos;
    @Nullable
    private BoundingBox cachedStructureBoundingBox;
    private boolean structureSearchComplete;
    private boolean structureTrailInitialized;
    private boolean structureTrailDisabled;

    public YueLinglanBlockEntity(BlockPos pos, BlockState state) {
        super(MaidSpellBlockEntities.YUE_LINGLAN.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, YueLinglanBlockEntity blockEntity) {
        if (!(level instanceof ServerLevel)) {
            return;
        }
        ServerLevel serverLevel = (ServerLevel) level;
        long gameTime = serverLevel.getGameTime();
        blockEntity.tickStructureTrail(serverLevel, pos, gameTime);
        int offset = Math.floorMod(pos.asLong(), TICK_INTERVAL);
        if ((gameTime + (long) offset) % TICK_INTERVAL != 0L) {
            return;
        }
        YueLinglanBlock.applyAura(serverLevel, pos);
    }

    /** 朝最近的目标结构撒一条弧形光尘尾迹。 */
    private void tickStructureTrail(ServerLevel level, BlockPos pos, long gameTime) {
        if (this.structureTrailDisabled) {
            return;
        }
        // 首次：若自身已在结构内，直接关闭尾迹
        if (!this.structureTrailInitialized) {
            this.structureTrailInitialized = true;
            if (this.isInsideTrackedStructure(level, pos)) {
                this.structureTrailDisabled = true;
                return;
            }
        }
        // 首次：搜索最近的结构并缓存包围盒
        if (!this.structureSearchComplete) {
            this.structureSearchComplete = true;
            this.cachedStructurePos = this.findNearestTrackedStructure(level, pos);
            if (this.cachedStructurePos != null) {
                this.cachedStructureBoundingBox = this.resolveStructureBoundingBox(level);
                if (this.isWithinSuppressionRange(pos)) {
                    this.structureTrailDisabled = true;
                    return;
                }
            }
        }
        if (this.cachedStructurePos == null) {
            this.structureTrailDisabled = true;
            return;
        }
        int particleOffset = Math.floorMod(pos.asLong(), PARTICLE_INTERVAL);
        if ((gameTime + (long) particleOffset) % PARTICLE_INTERVAL != 0L) {
            return;
        }
        if (this.isWithinSuppressionRange(pos)) {
            this.structureTrailDisabled = true;
            return;
        }
        double dx = (double) this.cachedStructurePos.getX() + 0.5 - ((double) pos.getX() + 0.5);
        double dz = (double) this.cachedStructurePos.getZ() + 0.5 - ((double) pos.getZ() + 0.5);
        double horizontalDistanceSqr = dx * dx + dz * dz;
        if (horizontalDistanceSqr <= MIN_PARTICLE_DISTANCE_TO_STRUCTURE_SQR) {
            return;
        }
        double horizontalLength = Math.sqrt(horizontalDistanceSqr);
        if (horizontalLength < 1.0E-4) {
            return;
        }
        double dirX = dx / horizontalLength;
        double dirZ = dz / horizontalLength;
        double perpX = -dirZ;
        double perpZ = dirX;
        double phase = (double) (gameTime % 40L) / 40.0 * (Math.PI * 2);
        // 9 段弧形尾迹
        for (int i = 0; i < 9; ++i) {
            double progress = 0.06 + (double) i * 0.11;
            double nextProgress = progress + 0.24;
            double curve = Math.sin(phase + progress * 2.9) * 0.18;
            double nextCurve = Math.sin(phase + nextProgress * 2.9) * 0.18;
            double fan = (level.random.nextDouble() - 0.5) * 0.05;
            double x = (double) pos.getX() + 0.5 + dirX * progress + perpX * (curve + fan);
            double y = (double) pos.getY() + 0.7 + (double) i * 0.018 + Math.sin(phase + (double) i * 0.6) * 0.03;
            double z = (double) pos.getZ() + 0.5 + dirZ * progress + perpZ * (curve + fan);
            double nextX = (double) pos.getX() + 0.5 + dirX * nextProgress + perpX * nextCurve;
            double nextY = (double) pos.getY() + 0.74 + (double) i * 0.02 + Math.sin(phase + (double) i * 0.6 + 0.45) * 0.03;
            double nextZ = (double) pos.getZ() + 0.5 + dirZ * nextProgress + perpZ * nextCurve;
            double vx = (nextX - x) * 0.18;
            double vy = (nextY - y) * 0.18 + 0.004;
            double vz = (nextZ - z) * 0.18;
            level.sendParticles(
                    new DustParticleOptions(new Vector3f(0.92f, 1.0f, 0.97f), i > 7 ? 1.28f : 1.08f),
                    x, y, z, 0, vx, vy, vz, 1.0);
        }
        // 尖端亮点
        double tipX = (double) pos.getX() + 0.5 + dirX * 1.75 + perpX * Math.sin(phase + 1.4) * 0.08;
        double tipY = (double) pos.getY() + 1.05 + Math.sin(phase + 0.8) * 0.04;
        double tipZ = (double) pos.getZ() + 0.5 + dirZ * 1.75 + perpZ * Math.sin(phase + 1.4) * 0.08;
        level.sendParticles(
                new DustParticleOptions(new Vector3f(0.98f, 1.0f, 1.0f), 1.5f),
                tipX, tipY, tipZ, 0, dirX * 0.04, 0.018, dirZ * 0.04, 1.0);
    }

    /** 搜索最近的目标结构；结果按 16×16 区块分区缓存。 */
    @Nullable
    private BlockPos findNearestTrackedStructure(ServerLevel level, BlockPos origin) {
        StructureSearchCacheKey key = StructureSearchCacheKey.create(level, origin);
        StructureSearchCacheEntry cached = STRUCTURE_SEARCH_CACHE.get(key);
        if (cached != null) {
            return cached.structurePos();
        }
        BlockPos structurePos = level.findNearestMapFeature(ELVEN_REALM_TAG, origin, STRUCTURE_SEARCH_RADIUS, false);
        STRUCTURE_SEARCH_CACHE.put(key, new StructureSearchCacheEntry(structurePos));
        return structurePos;
    }

    public static void clearStructureSearchCache(ServerLevel level) {
        STRUCTURE_SEARCH_CACHE.keySet().removeIf(key -> key.level() == level);
    }

    public static void clearAllStructureSearchCaches() {
        STRUCTURE_SEARCH_CACHE.clear();
    }

    @SubscribeEvent
    public static void onLevelUnload(WorldEvent.Unload event) {
        LevelAccessor levelAccessor = event.getWorld();
        if (levelAccessor instanceof ServerLevel) {
            clearStructureSearchCache((ServerLevel) levelAccessor);
        }
    }

    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        clearAllStructureSearchCaches();
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        clearAllStructureSearchCaches();
    }

    /** 取目标结构的包围盒（用于判断是否进入「压制范围」）。 */
    @Nullable
    private BoundingBox resolveStructureBoundingBox(ServerLevel level) {
        if (this.cachedStructurePos == null) {
            return null;
        }
        Optional<ConfiguredStructureFeature<?, ?>> feature =
                level.registryAccess()
                        .registryOrThrow(Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY)
                        .getOptional(ELVEN_REALM_ID);
        if (feature.isEmpty()) {
            return null;
        }
        StructureStart start = level.structureFeatureManager()
                .getStructureWithPieceAt(this.cachedStructurePos, feature.get());
        if (!start.isValid()) {
            start = level.structureFeatureManager()
                    .getStructureAt(this.cachedStructurePos, feature.get());
        }
        return start.isValid() ? start.getBoundingBox() : null;
    }

    /**
     * 点是否落在目标结构的 tag 内。
     * <p>1.18.2 没有按 tag 一次取结构的方法，这里遍历 tag 内容逐个判断。
     */
    private boolean isInsideTrackedStructure(ServerLevel level, BlockPos pos) {
        for (Holder<ConfiguredStructureFeature<?, ?>> holder :
                level.registryAccess()
                        .registryOrThrow(Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY)
                        .getTagOrEmpty(ELVEN_REALM_TAG)) {
            if (level.structureFeatureManager().getStructureWithPieceAt(pos, holder.value()).isValid()) {
                return true;
            }
        }
        return false;
    }

    /** 是否进入压制之石的压制范围（结构包围盒外扩 48 格）。 */
    private boolean isWithinSuppressionRange(BlockPos pos) {
        if (this.cachedStructureBoundingBox == null) {
            return false;
        }
        int padding = 48;
        BoundingBox box = this.cachedStructureBoundingBox;
        return pos.getX() >= box.minX() - padding && pos.getX() <= box.maxX() + padding
                && pos.getZ() >= box.minZ() - padding && pos.getZ() <= box.maxZ() + padding;
    }

    public static <T extends BlockEntity> BlockEntityTicker<T> createTicker(BlockEntityType<T> type) {
        return type == MaidSpellBlockEntities.YUE_LINGLAN.get()
                ? (level, pos, state, blockEntity) -> serverTick(level, pos, state, (YueLinglanBlockEntity) blockEntity)
                : null;
    }

    private record StructureSearchCacheKey(ServerLevel level, int regionX, int regionZ) {

        private static StructureSearchCacheKey create(ServerLevel level, BlockPos pos) {
            int chunkX = pos.getX() >> 4;
            int chunkZ = pos.getZ() >> 4;
            return new StructureSearchCacheKey(level,
                    Math.floorDiv(chunkX, STRUCTURE_SEARCH_CACHE_REGION_CHUNKS),
                    Math.floorDiv(chunkZ, STRUCTURE_SEARCH_CACHE_REGION_CHUNKS));
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof StructureSearchCacheKey)) {
                return false;
            }
            StructureSearchCacheKey other = (StructureSearchCacheKey) obj;
            return this.level == other.level && this.regionX == other.regionX && this.regionZ == other.regionZ;
        }

        @Override
        public int hashCode() {
            int result = System.identityHashCode(this.level);
            result = 31 * result + this.regionX;
            return 31 * result + this.regionZ;
        }
    }

    private record StructureSearchCacheEntry(@Nullable BlockPos structurePos) {
    }
}

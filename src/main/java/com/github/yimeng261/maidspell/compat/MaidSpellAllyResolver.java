// 移植自 1.9.2.1 反编译源 compat/MaidSpellAllyResolver.java
// SRG 名已替换为 1.18.2 official 名；反射目标类名原样保留（缺类自动降级）。
package com.github.yimeng261.maidspell.compat;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.scores.Team;

public final class MaidSpellAllyResolver {
    private static final int OWNER_TRACE_LIMIT = 8;
    private static final String IRONS_MAGIC_SUMMON = "io.redspace.ironsspellbooks.entity.mobs.MagicSummon";
    private static final String ARS_SUMMON = "com.hollingsworth.arsnouveau.api.entity.ISummon";
    private static final String GOETY_OWNED = "com.Polarice3.Goety.api.entities.IOwned";
    private static final String SLASHBLADE_SHOOTABLE = "mods.flammpfeil.slashblade.entity.IShootable";
    private static final String WIZARDRY_SERVICES = "com.binaris.wizardry.core.platform.Services";
    private static final Map<String, Optional<Class<?>>> OPTIONAL_TYPES = new ConcurrentHashMap();
    private static final Map<MethodKey, Optional<Method>> METHODS = new ConcurrentHashMap<MethodKey, Optional<Method>>();
    private static final Map<Class<?>, Boolean> OWNER_CAPABLE_CACHE = new ConcurrentHashMap();
    private static final Optional<Object> WIZARDRY_OBJECT_DATA = MaidSpellAllyResolver.loadStaticField("com.binaris.wizardry.core.platform.Services", "OBJECT_DATA");

    private MaidSpellAllyResolver() {
    }

    public static boolean areFriendly(@Nullable Entity first, @Nullable Entity second) {
        if (first == null || second == null || first == second) {
            return first != null && first == second;
        }
        if (!MaidSpellAllyResolver.couldHaveOwner(first) && !MaidSpellAllyResolver.couldHaveOwner(second)) {
            return false;
        }
        if (MaidSpellAllyResolver.hasExplicitTeamAlliance(first, second)) {
            return true;
        }
        Set<UUID> firstOwners = MaidSpellAllyResolver.collectAffinityIds(first);
        Set<UUID> secondOwners = MaidSpellAllyResolver.collectAffinityIds(second);
        if (firstOwners.isEmpty() || secondOwners.isEmpty()) {
            return false;
        }
        for (UUID id : firstOwners) {
            if (!secondOwners.contains(id)) continue;
            return true;
        }
        return false;
    }

    private static boolean hasExplicitTeamAlliance(Entity first, Entity second) {
        Team firstTeam = first.getTeam();
        Team secondTeam = second.getTeam();
        if (firstTeam == null || secondTeam == null) {
            return false;
        }
        return first.isAlliedTo(second) || second.isAlliedTo(first);
    }

    public static boolean isFriendlyDamage(LivingEntity target, @Nullable Entity causing, @Nullable Entity direct) {
        if (causing instanceof Player || direct instanceof Player) {
            return false;
        }
        return MaidSpellAllyResolver.areFriendly((Entity)target, causing) || MaidSpellAllyResolver.areFriendly((Entity)target, direct) || MaidSpellAllyResolver.resolveResponsibleEntity(direct).map(owner -> MaidSpellAllyResolver.areFriendly((Entity)target, owner)).orElse(false) != false;
    }

    public static boolean isOwnedBy(@Nullable Entity entity, Class<?> type) {
        Entity current = entity;
        for (int depth = 0; depth < 8 && current != null; ++depth) {
            if (type.isInstance(current)) {
                return true;
            }
            current = MaidSpellAllyResolver.getDirectOwner(current);
        }
        return false;
    }

    @Nullable
    public static Player maidOwningPlayer(@Nullable Entity entity) {
        EntityMaid maid = null;
        Entity current = entity;
        for (int depth = 0; depth < 8 && current != null; ++depth) {
            if (current instanceof EntityMaid) {
                EntityMaid found;
                maid = found = (EntityMaid)current;
            } else if (current instanceof Player) {
                Player player = (Player)current;
                return player;
            }
            current = MaidSpellAllyResolver.getDirectOwner(current);
        }
        if (maid == null) {
            return null;
        }
        UUID ownerId = maid.getOwnerUUID();
        return ownerId == null ? null : maid.getLevel().getPlayerByUUID(ownerId);
    }

    public static Optional<Entity> resolveResponsibleEntity(@Nullable Entity entity) {
        if (entity == null) {
            return Optional.empty();
        }
        HashSet<UUID> visited = new HashSet<UUID>();
        Entity current = entity;
        for (int depth = 0; depth < 8 && current != null && visited.add(current.getUUID()); ++depth) {
            Entity owner = MaidSpellAllyResolver.getDirectOwner(current);
            if (owner == null) {
                return current == entity ? Optional.empty() : Optional.of(current);
            }
            current = owner;
        }
        return current == null || current == entity ? Optional.empty() : Optional.of(current);
    }

    public static Set<UUID> collectAffinityIds(@Nullable Entity entity) {
        HashSet<UUID> ids = new HashSet<UUID>();
        MaidSpellAllyResolver.collectAffinityIds(entity, ids, new HashSet<UUID>(), 0);
        return ids;
    }

    private static void collectAffinityIds(@Nullable Entity entity, Set<UUID> ids, Set<UUID> visited, int depth) {
        EntityMaid maid;
        UUID maidOwnerId;
        UUID ownerId;
        if (entity == null || depth > 8 || !visited.add(entity.getUUID())) {
            return;
        }
        ids.add(entity.getUUID());
        Entity owner = MaidSpellAllyResolver.getDirectOwner(entity);
        if (owner != null) {
            MaidSpellAllyResolver.collectAffinityIds(owner, ids, visited, depth + 1);
        }
        if ((ownerId = MaidSpellAllyResolver.getDirectOwnerId(entity)) != null) {
            ids.add(ownerId);
            Entity resolved = MaidSpellAllyResolver.findEntity(entity, ownerId);
            if (resolved != null) {
                MaidSpellAllyResolver.collectAffinityIds(resolved, ids, visited, depth + 1);
            }
        }
        if (entity instanceof EntityMaid && (maidOwnerId = (maid = (EntityMaid)entity).getOwnerUUID()) != null) {
            ids.add(maidOwnerId);
            MaidSpellAllyResolver.collectAffinityIds((Entity)maid.getOwner(), ids, visited, depth + 1);
        }
    }

    @Nullable
    private static Entity getDirectOwner(Entity entity) {
        if (entity instanceof Player) {
            return null;
        }
        if (MaidSpellAllyResolver.isOptionalInstance(entity, IRONS_MAGIC_SUMMON)) {
            return MaidSpellAllyResolver.invokeOptionalEntity(entity, IRONS_MAGIC_SUMMON, "getSummoner");
        }
        if (MaidSpellAllyResolver.isOptionalInstance(entity, GOETY_OWNED)) {
            Entity owner = MaidSpellAllyResolver.invokeOptionalEntity(entity, GOETY_OWNED, "getTrueOwner");
            return owner != null ? owner : MaidSpellAllyResolver.invokeOptionalEntity(entity, GOETY_OWNED, "getMasterOwner");
        }
        if (MaidSpellAllyResolver.isOptionalInstance(entity, ARS_SUMMON)) {
            Entity owner = MaidSpellAllyResolver.invokeOptionalEntity(entity, ARS_SUMMON, "getOwnerAlt");
            if (owner != null) {
                return owner;
            }
            return MaidSpellAllyResolver.findEntity(entity, MaidSpellAllyResolver.invokeOptionalUuid(entity, ARS_SUMMON, "getOwnerUUID"));
        }
        if (entity instanceof OwnableEntity) {
            OwnableEntity ownable = (OwnableEntity)entity;
            return ownable.getOwner();
        }
        if (MaidSpellAllyResolver.isOptionalInstance(entity, SLASHBLADE_SHOOTABLE)) {
            return MaidSpellAllyResolver.invokeOptionalEntity(entity, SLASHBLADE_SHOOTABLE, "getShooter");
        }
        if (entity instanceof Mob) {
            Mob mob = (Mob)entity;
            return MaidSpellAllyResolver.getWizardryMinionOwner(mob);
        }
        if (entity instanceof Projectile) {
            Projectile projectile = (Projectile)entity;
            return projectile.getOwner();
        }
        return null;
    }

    @Nullable
    private static UUID getDirectOwnerId(Entity entity) {
        if (entity instanceof EntityMaid) {
            EntityMaid maid = (EntityMaid)entity;
            return maid.getOwnerUUID();
        }
        if (MaidSpellAllyResolver.isOptionalInstance(entity, ARS_SUMMON)) {
            return MaidSpellAllyResolver.invokeOptionalUuid(entity, ARS_SUMMON, "getOwnerUUID");
        }
        if (MaidSpellAllyResolver.isOptionalInstance(entity, GOETY_OWNED)) {
            return MaidSpellAllyResolver.invokeOptionalUuid(entity, GOETY_OWNED, "getOwnerId");
        }
        if (entity instanceof Player) {
            return null;
        }
        if (entity instanceof Mob) {
            Mob mob = (Mob)entity;
            UUID ownerId = MaidSpellAllyResolver.getWizardryMinionOwnerId(mob);
            if (ownerId != null) {
                return ownerId;
            }
        } else {
            if (entity instanceof OwnableEntity) {
                OwnableEntity ownable = (OwnableEntity)entity;
                return ownable.getOwnerUUID();
            }
            if (entity instanceof Projectile) {
                Projectile projectile = (Projectile)entity;
                Entity owner = projectile.getOwner();
                return owner != null ? owner.getUUID() : null;
            }
        }
        return null;
    }

    private static boolean isOptionalInstance(Entity entity, String className) {
        return OPTIONAL_TYPES.computeIfAbsent(className, MaidSpellAllyResolver::loadOptionalType).map(type -> type.isInstance(entity)).orElse(false);
    }

    private static Optional<Class<?>> loadOptionalType(String className) {
        try {
            return Optional.of(Class.forName(className, false, MaidSpellAllyResolver.class.getClassLoader()));
        }
        catch (ClassNotFoundException | LinkageError | RuntimeException ignored) {
            return Optional.empty();
        }
    }

    private static Optional<Object> loadStaticField(String className, String fieldName) {
        try {
            Class<?> type = Class.forName(className, false, MaidSpellAllyResolver.class.getClassLoader());
            return Optional.ofNullable(type.getField(fieldName).get(null));
        }
        catch (LinkageError | ReflectiveOperationException | RuntimeException ignored) {
            return Optional.empty();
        }
    }

    @Nullable
    private static Entity getWizardryMinionOwner(Mob mob) {
        Entity entity;
        Object data = MaidSpellAllyResolver.getWizardryMinionData(mob);
        if (data == null || !MaidSpellAllyResolver.invokeBoolean(data, "isSummoned")) {
            return null;
        }
        Object owner = MaidSpellAllyResolver.invokeNoArg(data, "getOwner");
        return owner instanceof Entity ? (entity = (Entity)owner) : MaidSpellAllyResolver.findEntity((Entity)mob, MaidSpellAllyResolver.invokeUuid(data, "getOwnerUUID"));
    }

    @Nullable
    private static UUID getWizardryMinionOwnerId(Mob mob) {
        Object data = MaidSpellAllyResolver.getWizardryMinionData(mob);
        if (data == null || !MaidSpellAllyResolver.invokeBoolean(data, "isSummoned")) {
            return null;
        }
        return MaidSpellAllyResolver.invokeUuid(data, "getOwnerUUID");
    }

    @Nullable
    private static Object getWizardryMinionData(Mob mob) {
        return WIZARDRY_OBJECT_DATA.map(objectData -> MaidSpellAllyResolver.invokeNoArg(objectData, "getMinionData", Mob.class, mob)).orElse(null);
    }

    @Nullable
    private static Entity invokeOptionalEntity(Entity entity, String className, String methodName) {
        Entity owner;
        Object value = MaidSpellAllyResolver.invokeOptionalNoArg(entity, className, methodName, Entity.class);
        return value instanceof Entity ? (owner = (Entity)value) : null;
    }

    @Nullable
    private static UUID invokeOptionalUuid(Entity entity, String className, String methodName) {
        UUID uuid;
        Object value = MaidSpellAllyResolver.invokeOptionalNoArg(entity, className, methodName, UUID.class);
        return value instanceof UUID ? (uuid = (UUID)value) : null;
    }

    @Nullable
    private static UUID invokeUuid(Object target, String methodName) {
        UUID uuid;
        Object value = MaidSpellAllyResolver.invokeNoArg(target, methodName, UUID.class);
        return value instanceof UUID ? (uuid = (UUID)value) : null;
    }

    @Nullable
    private static Object invokeOptionalNoArg(Object target, String className, String methodName, @Nullable Class<?> returnType) {
        Optional type = OPTIONAL_TYPES.computeIfAbsent(className, MaidSpellAllyResolver::loadOptionalType);
        if (type.isEmpty() || !((Class)type.get()).isInstance(target)) {
            return null;
        }
        Optional cached = METHODS.computeIfAbsent(new MethodKey((Class)type.get(), methodName, returnType, null), MaidSpellAllyResolver::findPublicNoArgMethod);
        if (cached.isEmpty()) {
            return null;
        }
        try {
            return ((Method)cached.get()).invoke(target, new Object[0]);
        }
        catch (LinkageError | ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    private static boolean invokeBoolean(Object target, String methodName) {
        Boolean result;
        Object value = MaidSpellAllyResolver.invokeNoArg(target, methodName, Boolean.class);
        return value instanceof Boolean && (result = (Boolean)value) != false;
    }

    @Nullable
    private static Object invokeNoArg(Object target, String methodName) {
        return MaidSpellAllyResolver.invokeNoArg(target, methodName, null);
    }

    @Nullable
    private static Object invokeNoArg(Object target, String methodName, @Nullable Class<?> returnType) {
        Optional cached = METHODS.computeIfAbsent(new MethodKey(target.getClass(), methodName, returnType, null), MaidSpellAllyResolver::findNoArgMethod);
        if (cached.isEmpty()) {
            return null;
        }
        try {
            return ((Method)cached.get()).invoke(target, new Object[0]);
        }
        catch (LinkageError | ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    @Nullable
    private static Object invokeNoArg(Object target, String methodName, Class<?> parameterType, Object parameter) {
        Optional cached = METHODS.computeIfAbsent(new MethodKey(target.getClass(), methodName, null, parameterType), MaidSpellAllyResolver::findMethod);
        if (cached.isEmpty()) {
            return null;
        }
        try {
            return ((Method)cached.get()).invoke(target, parameter);
        }
        catch (LinkageError | ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    private static Optional<Method> findNoArgMethod(MethodKey key) {
        return MaidSpellAllyResolver.findMethod(key);
    }

    private static Optional<Method> findPublicNoArgMethod(MethodKey key) {
        try {
            Method method = key.type().getMethod(key.name(), new Class[0]);
            if (MaidSpellAllyResolver.isUsableMethod(method, key)) {
                return Optional.of(method);
            }
        }
        catch (LinkageError | ReflectiveOperationException | RuntimeException throwable) {
            // empty catch block
        }
        return Optional.empty();
    }

    private static Optional<Method> findMethod(MethodKey key) {
        return MaidSpellAllyResolver.findExactMethod(key);
    }

    private static Optional<Method> findExactMethod(MethodKey key) {
        for (Class<?> type = key.type(); type != null && type != Object.class; type = type.getSuperclass()) {
            try {
                Method method;
                Method method2 = method = key.parameterType() == null ? type.getDeclaredMethod(key.name(), new Class[0]) : type.getDeclaredMethod(key.name(), key.parameterType());
                if (!MaidSpellAllyResolver.isUsableMethod(method, key)) continue;
                return Optional.of(method);
            }
            catch (LinkageError | ReflectiveOperationException | RuntimeException throwable) {
                // empty catch block
            }
        }
        return Optional.empty();
    }

    private static boolean isUsableMethod(Method method, MethodKey key) {
        if (key.parameterType() == null ? method.getParameterCount() != 0 : method.getParameterCount() != 1 || !method.getParameterTypes()[0].isAssignableFrom(key.parameterType())) {
            return false;
        }
        Class<?> returnType = key.returnType();
        if (returnType != null && !MaidSpellAllyResolver.isReturnTypeCompatible(returnType, method.getReturnType())) {
            return false;
        }
        if (!Modifier.isPublic(method.getModifiers()) || !Modifier.isPublic(method.getDeclaringClass().getModifiers())) {
            method.setAccessible(true);
        }
        return true;
    }

    private static boolean isReturnTypeCompatible(Class<?> expected, Class<?> actual) {
        if (expected.isAssignableFrom(actual)) {
            return true;
        }
        return expected == Boolean.class && actual == Boolean.TYPE || expected == Integer.class && actual == Integer.TYPE || expected == Long.class && actual == Long.TYPE || expected == Float.class && actual == Float.TYPE || expected == Double.class && actual == Double.TYPE;
    }

    @Nullable
    private static Entity findEntity(Entity context, @Nullable UUID id) {
        Player player;
        EntityMaid maid;
        Level level;
        if (id == null || !((level = context.getLevel()) instanceof ServerLevel)) {
            return null;
        }
        ServerLevel serverLevel = (ServerLevel)level;
        Entity entity = serverLevel.getEntity(id);
        if (entity == null && context instanceof EntityMaid && (maid = (EntityMaid)context).getOwner() != null && id.equals(maid.getOwnerUUID())) {
            return maid.getOwner();
        }
        if (entity == null && (player = serverLevel.getPlayerByUUID(id)) != null) {
            return player;
        }
        return entity;
    }

    private static boolean couldHaveOwner(Entity entity) {
        return OWNER_CAPABLE_CACHE.computeIfAbsent(entity.getClass(), MaidSpellAllyResolver::checkOwnerCapable);
    }

    private static boolean checkOwnerCapable(Class<?> clazz) {
        if (EntityMaid.class.isAssignableFrom(clazz) || OwnableEntity.class.isAssignableFrom(clazz) || Projectile.class.isAssignableFrom(clazz) || Player.class.isAssignableFrom(clazz)) {
            return true;
        }
        if (MaidSpellAllyResolver.isTypeAssignableTo(clazz, IRONS_MAGIC_SUMMON) || MaidSpellAllyResolver.isTypeAssignableTo(clazz, GOETY_OWNED) || MaidSpellAllyResolver.isTypeAssignableTo(clazz, ARS_SUMMON) || MaidSpellAllyResolver.isTypeAssignableTo(clazz, SLASHBLADE_SHOOTABLE)) {
            return true;
        }
        return WIZARDRY_OBJECT_DATA.isPresent() && Mob.class.isAssignableFrom(clazz);
    }

    private static boolean isTypeAssignableTo(Class<?> clazz, String interfaceName) {
        return OPTIONAL_TYPES.computeIfAbsent(interfaceName, MaidSpellAllyResolver::loadOptionalType).map(type -> type.isAssignableFrom(clazz)).orElse(false);
    }

    private record MethodKey(Class<?> type, String name, @Nullable Class<?> returnType, @Nullable Class<?> parameterType) {
    }
}


package com.github.yimeng261.maidspell.utils;

/**
 * 纯算术工具：带饱和的加减与时间戳/截止时间迁移。
 * <p>
 * 移植自 1.9.2.1 反编译源 utils/PortableTimerMath.java，原样保留（零 MC / 零 SRG 依赖）。
 * 用途：物品 NBT 里存的是「服务器全局 tick」的时间戳，跨存档/跨维度迁移时需要夹紧到合法区间，
 * 避免旧版本用本地游戏时间写入的数值被直接当成全局时间使用而永久过期或永久有效。
 */
public final class PortableTimerMath {

    private PortableTimerMath() {
    }

    public static long saturatingAdd(long left, long right) {
        if (right > 0L && left > Long.MAX_VALUE - right) {
            return Long.MAX_VALUE;
        }
        if (right < 0L && left < Long.MIN_VALUE - right) {
            return Long.MIN_VALUE;
        }
        return left + right;
    }

    public static long saturatingSubtract(long left, long right) {
        if (right > 0L && left < Long.MIN_VALUE + right) {
            return Long.MIN_VALUE;
        }
        if (right < 0L && left > Long.MAX_VALUE + right) {
            return Long.MAX_VALUE;
        }
        return left - right;
    }

    public static long clampRemaining(long remainingTicks, long maxRemainingTicks, long expiredGraceTicks) {
        long maximum = Math.max(0L, maxRemainingTicks);
        if (maximum == 0L) {
            return 0L;
        }
        if (remainingTicks <= 0L) {
            return Math.min(maximum, Math.max(0L, expiredGraceTicks));
        }
        return Math.min(remainingTicks, maximum);
    }

    public static long migrateDeadline(long oldDeadline, long oldNow, long newNow, long maxRemainingTicks, long expiredGraceTicks) {
        long remaining = saturatingSubtract(oldDeadline, oldNow);
        long boundedRemaining = clampRemaining(remaining, maxRemainingTicks, expiredGraceTicks);
        return saturatingAdd(newNow, boundedRemaining);
    }

    public static long migrateTimestamp(long legacyTimestamp, long legacyNow, long serverNow, long maxAge, long graceTicks) {
        long boundedMaxAge = Math.max(0L, maxAge);
        long legacyDeadline = saturatingAdd(legacyTimestamp, boundedMaxAge);
        long serverDeadline = migrateDeadline(legacyDeadline, legacyNow, serverNow, boundedMaxAge, graceTicks);
        return saturatingSubtract(serverDeadline, boundedMaxAge);
    }
}

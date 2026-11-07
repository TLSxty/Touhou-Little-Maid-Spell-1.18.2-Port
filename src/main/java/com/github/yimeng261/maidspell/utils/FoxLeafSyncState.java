package com.github.yimeng261.maidspell.utils;

/**
 * 主人在「狐叶共享」上的同步状态：**由服务端算、同步给本人的客户端**。
 *
 * <p><b>为什么需要这个东西：</b>玩家自身的移动判定（{@code LivingEntity.travel} 里的
 * {@code canStandOnFluid}）只在<b>客户端</b>执行 —— 服务端玩家没有本地控制权，
 * 那条分支根本不会走。而 TLM 1.2.2 不会把女仆饰品栏同步到客户端
 * （{@code MaidBaubleInventory} 只写在 NBT 里，没有任何同步包），
 * 所以客户端 {@code getMaidBauble()} 永远读不到狐叶，
 * 判断「附近有没有带狐叶的女仆」在客户端必然失败 ⇒ 主人踩水会直接穿下去。</p>
 *
 * <p>上游 1.20.1 靠 {@code IMaidBauble#syncClient} 解决这件事（浮狐叶/熔岩狐叶都返回 true），
 * 但 TLM 1.2.2 的 {@code IMaidBauble} 没有这个方法，只能自建同步：
 * 服务端每 10 tick 用真实饰品数据算一次结果，变化时发
 * {@code FoxLeafStateMessage} 给该玩家；客户端存到本类，供 mixin 里的玩家分支读取。</p>
 *
 * <p>本类刻意<b>不引用任何客户端类型</b>，因此可以安全地在双端加载。</p>
 */
public final class FoxLeafSyncState {

    private static volatile boolean floatingReady;
    private static volatile boolean moltenReady;

    private FoxLeafSyncState() {
    }

    public static void update(boolean floating, boolean molten) {
        floatingReady = floating;
        moltenReady = molten;
    }

    /** 主人当前是否共享「踏水」。 */
    public static boolean hasFloating() {
        return floatingReady;
    }

    /** 主人当前是否共享「踏熔岩 + 火焰保护」。 */
    public static boolean hasMolten() {
        return moltenReady;
    }

    public static void reset() {
        floatingReady = false;
        moltenReady = false;
    }
}

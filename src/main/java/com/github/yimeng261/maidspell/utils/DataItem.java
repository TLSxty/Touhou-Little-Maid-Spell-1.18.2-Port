package com.github.yimeng261.maidspell.utils;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;

/**
 * 受伤结算数据载体：在 {@code Global.baubleSetHealthHandlers} 之间传递可变的伤害值。
 * <p>
 * 移植自 1.9.2.1 反编译源 utils/DataItem.java，零 MC 版本差异，原样保留。
 */
public class DataItem {

    EntityMaid maid;
    float amount;
    boolean isCanceled;

    public DataItem(EntityMaid maid, float amount, boolean isCanceled) {
        this.maid = maid;
        this.amount = amount;
        this.isCanceled = isCanceled;
    }

    public DataItem(EntityMaid maid, float amount) {
        this.maid = maid;
        this.amount = amount;
        this.isCanceled = false;
    }

    public EntityMaid getMaid() {
        return this.maid;
    }

    public float getAmount() {
        return this.amount;
    }

    public boolean isCanceled() {
        return this.isCanceled;
    }

    public void setCanceled(boolean isCanceled) {
        this.isCanceled = isCanceled;
    }

    public void setAmount(float amount) {
        this.amount = amount;
    }

    public void setMaid(EntityMaid maid) {
        this.maid = maid;
    }
}

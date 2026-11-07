package com.github.yimeng261.maidspell.network.message;

import com.github.yimeng261.maidspell.utils.FoxLeafSyncState;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

/**
 * 把「主人是否正在共享狐叶效果」从服务端同步到该玩家本人的客户端。
 *
 * <p>背景见 {@link FoxLeafSyncState}：TLM 1.2.2 不把女仆饰品栏同步给客户端，
 * 而玩家踩水/踩岩浆的判定发生在客户端 {@code LivingEntity.travel} 里，因此必须由服务端算好再送过来。</p>
 */
public record FoxLeafStateMessage(boolean floating, boolean molten) {

    public static void encode(FoxLeafStateMessage message, FriendlyByteBuf buf) {
        buf.writeBoolean(message.floating);
        buf.writeBoolean(message.molten);
    }

    public static FoxLeafStateMessage decode(FriendlyByteBuf buf) {
        return new FoxLeafStateMessage(buf.readBoolean(), buf.readBoolean());
    }

    public static void handle(FoxLeafStateMessage message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> FoxLeafSyncState.update(message.floating, message.molten));
        context.setPacketHandled(true);
    }
}

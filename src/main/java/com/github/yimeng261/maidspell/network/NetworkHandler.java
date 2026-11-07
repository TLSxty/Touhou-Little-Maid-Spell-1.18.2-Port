package com.github.yimeng261.maidspell.network;

import com.github.yimeng261.maidspell.Config;
import com.github.yimeng261.maidspell.network.message.TransmogNecklaceMessage;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * 本工程的网络通道。
 *
 * <p>上游有一个大而全的 NetworkHandler 承载末影腰包、幻化项链等全部消息；
 * 本工程按已移植内容裁剪，目前只有幻化项链的一条消息。</p>
 */
public final class NetworkHandler {

    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Config.MOD_ID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    private NetworkHandler() {
    }

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, TransmogNecklaceMessage.class,
                TransmogNecklaceMessage::encode,
                TransmogNecklaceMessage::decode,
                TransmogNecklaceMessage::handle);
    }
}

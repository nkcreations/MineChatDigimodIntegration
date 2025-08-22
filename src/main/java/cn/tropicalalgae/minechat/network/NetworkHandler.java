package cn.tropicalalgae.minechat.network;

import cn.tropicalalgae.minechat.MineChat;
import cn.tropicalalgae.minechat.network.packets.SendGPTRequestPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class NetworkHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            ResourceLocation.tryParse(MineChat.MOD_ID + ":main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void register() {
        int index = 0;
        INSTANCE.registerMessage(index++,
                SendGPTRequestPacket.class,
                SendGPTRequestPacket::toBytes,
                SendGPTRequestPacket::new,
                SendGPTRequestPacket::handle
        );
    }

    public static void sendToServer(Object msg) {
        INSTANCE.send(PacketDistributor.SERVER.with(() -> null), msg);
    }
}
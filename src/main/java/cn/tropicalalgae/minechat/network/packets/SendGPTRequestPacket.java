package cn.tropicalalgae.minechat.network.packets;

import cn.tropicalalgae.minechat.common.gpt.GPTEmotionAnalyst;
import cn.tropicalalgae.minechat.common.gpt.GPTTalkerManager;
import cn.tropicalalgae.minechat.common.gpt.GPTTextTalker;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

import static cn.tropicalalgae.minechat.utils.Util.canPlayerTalkToEntity;
import static cn.tropicalalgae.minechat.utils.Util.findEntityByUUID;


public class SendGPTRequestPacket {
    private final String message;
//    private final String playerName;
    private final String receiverUUID;

    public SendGPTRequestPacket(String message, String receiverUUID) {
        this.message = message;
//        this.playerName = playerName;
        this.receiverUUID = receiverUUID;
    }

    public SendGPTRequestPacket(FriendlyByteBuf buf) {
        this.message = buf.readUtf();
//        this.playerName = buf.readUtf();
        this.receiverUUID = buf.readUtf();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(this.message);
//        buf.writeUtf(this.playerName);
        buf.writeUtf(this.receiverUUID);
    }

    public static void handle(SendGPTRequestPacket msg, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender != null) {
                MinecraftServer server = sender.getServer();
                String senderName = sender.getGameProfile().getName();

                if (server != null) {
                    // 找到对话者
                    Entity receiver = findEntityByUUID(server, msg.receiverUUID);
                    // 权限判断
                    if (canPlayerTalkToEntity(senderName) && receiver != null){
                        GPTTalkerManager.runAsync(
                                msg.receiverUUID,
                                new GPTTextTalker(sender, receiver, msg.message, server)
                        );
                        GPTTalkerManager.runAsync(
                                msg.receiverUUID,
                                new GPTEmotionAnalyst(sender, receiver)
                        );

                    } else {
                        Component message = Component.literal("You are unable to chat due to certain reasons.")
                                .withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW));
                        sender.sendSystemMessage(message);
                    }
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}

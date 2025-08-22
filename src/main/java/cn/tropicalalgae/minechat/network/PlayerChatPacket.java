package cn.tropicalalgae.minechat.network;

import cn.tropicalalgae.minechat.common.gpt.GPTTalkerManager;
import cn.tropicalalgae.minechat.common.gpt.GPTTextTalker;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class PlayerChatPacket {
    private final UUID entityUUID;
    private final String message;

    public PlayerChatPacket(UUID entityUUID, String message) {
        this.entityUUID = entityUUID;
        this.message = message;
    }

    public PlayerChatPacket(FriendlyByteBuf buf) {
        this.entityUUID = buf.readUUID();
        this.message = buf.readUtf();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(entityUUID);
        buf.writeUtf(message);
    }

    public UUID getEntityUUID() { return entityUUID; }

    public String getMessage() { return message; }

    public static void handle(PlayerChatPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }

            ServerLevel serverLevel = player.serverLevel();
            Entity targetEntity = serverLevel.getEntity(packet.getEntityUUID());

            if (targetEntity != null) {
                if (cn.tropicalalgae.minechat.utils.Util.canPlayerTalkToEntity(player.getGameProfile().getName())) {
                    GPTTalkerManager.runAsync(
                            packet.getEntityUUID().toString(),
                            new GPTTextTalker(player, targetEntity, packet.getMessage(), player.getServer())
                    );
                } else {
                    player.sendSystemMessage(Component.literal("You are unable to chat due to certain reasons.")
                            .withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)));
                }
            }
        });
        context.setPacketHandled(true);
    }
}

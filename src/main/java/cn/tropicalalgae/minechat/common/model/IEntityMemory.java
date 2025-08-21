package cn.tropicalalgae.minechat.common.model;

import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

import net.minecraft.server.level.ServerPlayer;

public interface IEntityMemory <T extends IEntityMessage> {
    String getChatRequestBody();
    String getJulesChatRequestBody(ServerPlayer player);
    void addNewMessage(IEntityMessage newMessage);
    List<T> getHistory();
    T getMessageByUUID(UUID messageUUID);
    T getReplyMessageByUUID(UUID messageUUID);
    List<T> getMessagesBySenderUUID(UUID senderUUID);
}

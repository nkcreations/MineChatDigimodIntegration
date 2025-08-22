package cn.tropicalalgae.minechat.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public class DisplayBubblePacket {
    private final int entityId;
    private final String message;

    public DisplayBubblePacket(int entityId, String message) {
        this.entityId = entityId;
        this.message = message;
    }

    public DisplayBubblePacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.message = buf.readUtf();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(this.entityId);
        buf.writeUtf(this.message);
    }

    public int getEntityId() {
        return entityId;
    }

    public String getMessage() {
        return message;
    }

    public static void handle(DisplayBubblePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            // Client-side handler logic
            cn.tropicalalgae.minechat.client.SpeechBubbleManager.addBubble(packet.getEntityId(), packet.getMessage());
        });
        context.setPacketHandled(true);
    }
}

package cn.tropicalalgae.minechat.common.events;

import cn.tropicalalgae.minechat.MineChat;
import cn.tropicalalgae.minechat.client.Keybinds;
import cn.tropicalalgae.minechat.client.SpeechBubbleManager;
import cn.tropicalalgae.minechat.client.gui.DigimonChatScreen;
import cn.tropicalalgae.minechat.utils.Util;
import cn.tropicalalgae.minechat.utils.Ways;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

@Mod.EventBusSubscriber(modid = MineChat.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClientRenderEvents {

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            SpeechBubbleManager.tick();
            handleKeybinds();
        }
    }

    private static void handleKeybinds() {
        if (Keybinds.OPEN_CHAT_KEY.consumeClick()) {
            Player player = Minecraft.getInstance().player;
            if (player != null) {
                Entity targetEntity = Ways.getPointedEntity(player, 64.0);
                if (Util.isEntitySupported(targetEntity, cn.tropicalalgae.minechat.common.enumeration.MessageType.CHAT)) {
                    Minecraft.getInstance().setScreen(new DigimonChatScreen(targetEntity));
                }
            }
        }
    }

    @SubscribeEvent
    public static void onRenderLiving(RenderLivingEvent.Post<LivingEntity, ?> event) {
        LivingEntity entity = event.getEntity();
        String text = SpeechBubbleManager.getBubbleText(entity.getId());

        if (text == null) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource bufferSource = event.getMultiBufferSource();
        Font font = Minecraft.getInstance().font;

        float textWidth = font.width(text);
        float scale = 0.025f;

        poseStack.pushPose();
        poseStack.translate(0, entity.getBbHeight() + 0.75f, 0); // Increased vertical offset
        poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().camera.rotation());
        poseStack.scale(-scale, -scale, scale);

        Matrix4f matrix4f = poseStack.last().pose();

        // Draw background
        int backgroundColor = 0x80000000; // Semi-transparent black
        font.drawInBatch(text, -textWidth / 2, 0, 0xFFFFFF, false, matrix4f, bufferSource, Font.DisplayMode.NORMAL, backgroundColor, 15728880);

        poseStack.popPose();
    }
}

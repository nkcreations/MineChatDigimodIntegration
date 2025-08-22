package cn.tropicalalgae.minechat.common.events;

import cn.tropicalalgae.minechat.MineChat;
import cn.tropicalalgae.minechat.client.Keybinds;
import cn.tropicalalgae.minechat.client.gui.DigimonChatScreen;
import cn.tropicalalgae.minechat.utils.Ways;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static cn.tropicalalgae.minechat.utils.Util.isEntitySupported;

@Mod.EventBusSubscriber(modid = MineChat.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class KeyInputHandler {

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (Keybinds.OPEN_CHAT_KEY.consumeClick()) {
            Player player = Minecraft.getInstance().player;
            if (player != null) {
                Entity targetEntity = Ways.getPointedEntity(player, 64.0);
                if (isEntitySupported(targetEntity, cn.tropicalalgae.minechat.common.enumeration.MessageType.CHAT)) {
                    Minecraft.getInstance().setScreen(new DigimonChatScreen(targetEntity));
                }
            }
        }
    }
}

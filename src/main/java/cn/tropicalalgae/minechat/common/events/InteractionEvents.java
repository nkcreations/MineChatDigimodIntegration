package cn.tropicalalgae.minechat.common.events;

import cn.tropicalalgae.minechat.MineChat;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.modderg.thedigimod.server.entity.DigimonEntity;

@Mod.EventBusSubscriber(modid = MineChat.MOD_ID)
public class InteractionEvents {

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        // We only want to open the GUI on the client.
        if (!event.getLevel().isClientSide) {
            return;
        }

        var target = event.getTarget();

        // Check if the target is a DigimonEntity.
        if (target instanceof DigimonEntity) {
            // This is a talkable Digimon.
            System.out.println("Player right-clicked a Digimon: " + target.getName().getString());

            // Open the chat GUI screen.
            net.minecraft.client.Minecraft.getInstance().setScreen(new cn.tropicalalgae.minechat.client.gui.DigimonChatScreen(target));
        }
    }
}

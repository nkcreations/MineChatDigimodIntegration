package cn.tropicalalgae.minechat.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;

public class Keybinds {
    public static final String KEY_CATEGORY_MINECHAT = "key.category.minechat";
    public static final String KEY_OPEN_CHAT = "key.minechat.open_chat";

    public static final KeyMapping OPEN_CHAT_KEY = new KeyMapping(
            KEY_OPEN_CHAT,
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_6, // Default key
            KEY_CATEGORY_MINECHAT
    );
}

package cn.tropicalalgae.minechat.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;

public class DigimonChatScreen extends Screen {

    private final Entity targetEntity;
    private EditBox chatInput;
    private Button sendButton;

    public DigimonChatScreen(Entity targetEntity) {
        super(Component.literal("Chat with " + targetEntity.getName().getString()));
        this.targetEntity = targetEntity;
    }

    @Override
    protected void init() {
        super.init();
        // this.minecraft.keyboardHandler.setSendRepeatsToGui(true); // Removed

        // Create the text input field
        this.chatInput = new EditBox(this.font, this.width / 2 - 150, this.height - 40, 200, 20, Component.literal("Type to chat..."));
        this.addWidget(this.chatInput);

        // Create the send button
        this.sendButton = Button.builder(Component.literal("Send"), (button) -> {
            sendMessage();
        }).bounds(this.width / 2 + 55, this.height - 40, 100, 20).build();
        this.addWidget(this.sendButton);

        this.setInitialFocus(this.chatInput);
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        String s = this.chatInput.getValue();
        this.init(minecraft, width, height);
        this.chatInput.setValue(s);
    }

    @Override
    public void onClose() {
        // this.minecraft.keyboardHandler.setSendRepeatsToGui(false); // Removed
        super.onClose();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // 257 is the key code for Enter
        if (keyCode == 257) {
            sendMessage();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics); // Use guiGraphics
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF); // Use guiGraphics
        this.chatInput.render(guiGraphics, mouseX, mouseY, partialTicks); // Pass guiGraphics
        this.sendButton.render(guiGraphics, mouseX, mouseY, partialTicks); // Pass guiGraphics
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    private void sendMessage() {
        String message = this.chatInput.getValue().trim();
        if (!message.isEmpty()) {
            cn.tropicalalgae.minechat.network.NetworkHandler.sendToServer(new cn.tropicalalgae.minechat.network.PlayerChatPacket(this.targetEntity.getUUID(), message));
            this.onClose();
        }
    }
}

package cn.tropicalalgae.minechat.common.gpt;


import cn.tropicalalgae.minechat.common.enumeration.MessageType;
import cn.tropicalalgae.minechat.common.model.IEntityMemory;
import cn.tropicalalgae.minechat.common.model.impl.ChatMessage;
import cn.tropicalalgae.minechat.common.persistence.JulesSavedData;
import cn.tropicalalgae.minechat.common.persistence.RelationshipData;
import cn.tropicalalgae.minechat.utils.Config;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.MinecraftServer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.modderg.thedigimod.server.entity.DigimonEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

import static cn.tropicalalgae.minechat.MineChat.LOGGER;
import static cn.tropicalalgae.minechat.common.capability.ChatMemoryProvider.getChatMemory;
import static cn.tropicalalgae.minechat.common.gpt.GPTTalkerManager.gptRun;
import static cn.tropicalalgae.minechat.common.gpt.GPTTalkerManager.gptRunContext;
import static cn.tropicalalgae.minechat.utils.Util.*;
import static cn.tropicalalgae.minechat.utils.Util.getEntityCustomName;


public class GPTTextTalker implements Runnable {
    private final ServerPlayer sender;
    private final Entity receiver;
    private final String message;
    private final MinecraftServer server;
    private IEntityMemory<ChatMessage> memory = null;


    public GPTTextTalker(ServerPlayer sender, Entity receiver, String message, MinecraftServer server){
        this.sender = sender;
        this.receiver = receiver;
        this.message = message;
        this.server = server;
        initReceiverName();
    }

    private static String buildEntityNameRequestBody() {
        JsonObject root = new JsonObject();
        JsonArray messages = new JsonArray();

        JsonObject msgContent = new JsonObject();
        msgContent.addProperty("role", "user");
        msgContent.addProperty("content",
                String.format(ENTITY_NAME_PROMPT.formatted(Config.USER_LANGUAGE.get().toString()))
        );

        messages.add(msgContent);
        root.addProperty("model", Config.GPT_MODEL.get());
        root.add("messages", messages);
        return new Gson().toJson(root);
    }

    private void messageBroadcast(Component replyComp) {
        for (ServerPlayer player : this.server.getPlayerList().getPlayers()) {
            player.sendSystemMessage(replyComp);
        }
    }

    @NotNull
    private Boolean canReceiverTalk() {
        if (isEntitySupported(this.receiver, MessageType.CHAT)) {
            this.memory = getChatMemory(this.receiver);
            return this.memory != null;
        }
        return false;
    }

    private void initReceiverName() {
        if (this.receiver instanceof DigimonEntity) {
            return; // Digimon use their species name, no need to generate one.
        }
        if (this.receiver.getCustomName() == null) {
            String receiverName = gptRun(buildEntityNameRequestBody());
            receiverName = (receiverName == null) ? "Tropical Algae" : receiverName;
            this.receiver.setCustomName(Component.literal(receiverName));
            LOGGER.info("Init entity name [%s]".formatted(receiverName));
        }
    }

    @Override
    public void run() {
        if (canReceiverTalk()){

            // Update memory (player's message)
            ChatMessage msgCont = new ChatMessage(this.sender, this.message, null);
            this.memory.addNewMessage(msgCont);

            // Get the request body using the new method
            String requestBody = this.memory.getJulesChatRequestBody(this.sender);
            String rawJsonReply = gptRun(requestBody);

            // Update memory (model's message)
            Component replyComp;
            if (rawJsonReply != null) {
                try {
                    // Store the full JSON response in the history
                    ChatMessage rplCont = new ChatMessage(this.receiver, msgCont.getUUID(), rawJsonReply);
                    memory.addNewMessage(rplCont);

                    // Parse the JSON to get the text to display
                    JsonObject replyJson = JsonParser.parseString(rawJsonReply).getAsJsonObject();
                    String displayText = replyJson.get("text").getAsString();
                    int relationshipDelta = replyJson.get("relationshipDelta").getAsInt();

                    // Update relationship score
                    JulesSavedData savedData = JulesSavedData.get(this.server.overworld());
                    RelationshipData relationshipData = savedData.getRelationship(this.sender.getUUID(), this.receiver.getUUID());
                    relationshipData.adjustRelationshipScore(relationshipDelta);
                    savedData.setDirty();

                    replyComp = Component.literal("<%s>: %s".formatted(getEntityCustomName(this.receiver), displayText));

                    // TODO: Process actions from replyJson.get("actions") in a later step
                    if (replyJson.has("actions")) {
                        for (JsonElement actionElement : replyJson.getAsJsonArray("actions")) {
                            JsonObject actionObject = actionElement.getAsJsonObject();
                            String actionType = actionObject.get("type").getAsString();
                            if ("ATTACK_PLAYER".equals(actionType)) {
                                handleAttack(this.receiver, this.sender);
                                break; // Priority action
                            }
                            if ("GIVE_EGG".equals(actionType)) {
                                handleGiveEgg(this.sender, this.receiver, actionObject);
                                break; // Priority action
                            }
                            if ("WARN".equals(actionType)) {
                                handleWarn(this.sender, this.receiver, actionObject);
                                // Don't break, as this is low priority
                            }
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("Failed to parse LLM JSON response: " + rawJsonReply, e);
                    replyComp = Component.literal("[ERROR] Failed to parse LLM response.").withStyle(Style.EMPTY.withColor(ChatFormatting.RED));
                }
            } else {
                // Handle inference failure
                String errorReply = "[ERROR] MineChat inference failed. Please check your config!";
                replyComp = Component.literal(errorReply).withStyle(Style.EMPTY.withColor(ChatFormatting.RED));
                LOGGER.error("Error for model inference, latest message: %s".formatted(this.message));
            }
            // Broadcast message
            messageBroadcast(replyComp);
        }
    }

    private void handleAttack(Entity entity, ServerPlayer target) {
        if (entity instanceof Mob mob) {
            mob.setTarget(target);
            LOGGER.info(String.format("Action: %s is now targeting %s for attack.", entity.getName().getString(), target.getName().getString()));
        } else {
            LOGGER.warn(String.format("Action: ATTACK_PLAYER failed. %s is not a Mob entity.", entity.getName().getString()));
        }
    }

    private void handleGiveEgg(ServerPlayer player, Entity entity, JsonObject action) {
        JulesSavedData savedData = JulesSavedData.get(player.serverLevel());
        RelationshipData relationshipData = savedData.getRelationship(player.getUUID(), entity.getUUID());

        long currentTime = System.currentTimeMillis();
        long lastEggTime = relationshipData.getLastEggTimestamp();
        long cooldown = TimeUnit.DAYS.toMillis(Config.EGG_COOLDOWN_DAYS.get());

        if (currentTime - lastEggTime < cooldown) {
            LOGGER.info(String.format("Action: GIVE_EGG for %s failed due to cooldown.", player.getName().getString()));
            return;
        }

        String eggId = action.get("eggId").getAsString();
        Item eggItem = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(eggId));

        if (eggItem == null) {
            LOGGER.error(String.format("Action: GIVE_EGG failed. Unknown eggId: %s", eggId));
            return;
        }

        ItemStack eggStack = new ItemStack(eggItem);
        boolean added = player.getInventory().add(eggStack);
        if (!added) {
            // Drop the item in the world if inventory is full
            ItemEntity itemEntity = new ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(), eggStack);
            player.level().addFreshEntity(itemEntity);
        }

        relationshipData.setLastEggTimestamp(currentTime);
        savedData.setDirty();
        LOGGER.info(String.format("Action: %s gives egg %s to %s", entity.getName().getString(), eggId, player.getName().getString()));
    }

    private void handleWarn(ServerPlayer player, Entity entity, JsonObject action) {
        // The text from the LLM is the primary warning.
        // We can add additional feedback here if needed, like a sound effect.
        String reason = action.has("reason") ? action.get("reason").getAsString() : "unknown";
        LOGGER.info(String.format("Action: %s warns %s. Reason: %s", entity.getName().getString(), player.getName().getString(), reason));
    }
}

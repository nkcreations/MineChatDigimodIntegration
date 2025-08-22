package cn.tropicalalgae.minechat.common.model.impl;

import cn.tropicalalgae.minechat.common.model.IEntityMessage;
import cn.tropicalalgae.minechat.common.model.IEntityMemory;
import cn.tropicalalgae.minechat.common.persistence.JulesSavedData;
import cn.tropicalalgae.minechat.common.personality.Personality;
import cn.tropicalalgae.minechat.common.personality.PersonalityManager;
import cn.tropicalalgae.minechat.utils.Config;
import cn.tropicalalgae.minechat.utils.Util;
import cn.tropicalalgae.minechat.utils.ContextExtractor;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.util.*;

import static cn.tropicalalgae.minechat.common.gpt.GPTTalkerManager.buildRequestBody;
import static cn.tropicalalgae.minechat.utils.Util.getEntityCustomName;
import static cn.tropicalalgae.minechat.utils.Util.getEntityPrompt;
import static cn.tropicalalgae.minechat.utils.Util.SYS_PROMPT_SUFFIX;


public class ChatMemory implements IEntityMemory<ChatMessage> {
    private final Entity entity;
    private final List<ChatMessage> history = new ArrayList<>();
    private final Map<UUID, ChatMessage> messageMapID = new HashMap<>();
    private final Map<UUID, List<ChatMessage>> messageMapSender = new HashMap<>();
    private final Map<UUID, UUID> messageReplyMap = new HashMap<>();


    public ChatMemory(Entity entity) {
        this.entity = entity;
    }

    @Override
    public String getJulesChatRequestBody(ServerPlayer player) {
        // This is the new method to build the request body for Jules
        JsonArray messages = new JsonArray();

        // 1. System Prompt
        String species = Util.getEntitySpecies(this.entity);
        Personality personality = PersonalityManager.getPersonality(species);

        String estilo_persona = personality.persona.estilo;
        String tabues = String.join(", ", personality.tabues);
        String muletillas = String.join(", ", personality.persona.muletillas);

        // Using a multiline string for the template
        String systemPromptTemplate = """
        [SYSTEM]
        Eres un personaje del juego Minecraft llamado "{entityName}", especie "{species}" del mod The DigiMod.
        Tu objetivo es conversar con el jugador y reaccionar a su trato. Mantente 100% en personaje.
        Estilo: {estilo_persona}. Responde breve (1–3 frases). Idioma: español.
        Prohibido revelar que eres un modelo/IA. No inventes datos de juego que no estén en CONTEXTO.
        Si el usuario te insulta o es hostil, incrementa tu enojo y emite acciones de advertencia/agresión según REGLAS.
        Si el usuario es amable/útil, incrementa relación; al superar el umbral define acción GIVE_EGG con un huevo de tu línea evolutiva.

        CONTEXTO:
        - Jugador: {playerName}, pos {playerPos}, inventario (muestras): {playerItems}, reputación previa: {relationScore}
        - Mundo: bioma {biome}, hora {timeOfDay}, clima {weather}
        - Digimon: {digimonStats}
        - Restricciones: {tabues}
        - Umbrales: warn≤–20, angry≤–40, attack≤–60, friendly≥+40, loyal≥+70 (ver REGLAS)

        REGLAS:
        1) Devuelve SOLO JSON válido con el esquema (text, mood, relationshipDelta, actions[], metadata).
        2) Calcula relationshipDelta en [–3..+3] por turno según tono del jugador (insultos=–3; críticas=–2; neutro=0; amable=+1; cumplidos/ayuda=+2..+3).
        3) Si relación ≤–20 añade acción WARN. Si ≤–40 set mood "angry" y otra WARN. Si ≤–60 añade ATTACK_PLAYER.
        4) Si relación ≥+70 y el jugador no recibió huevo recientemente, añade GIVE_EGG con eggId de tu línea evolutiva.
        5) No uses lenguaje soez. Si el jugador lo usa, sé firme pero sin insultar.
        6) Responde como {species}; puedes usar muletillas {muletillas}.

        EJEMPLOS:
        - Usuario: "Eres inútil."
          → {"text":"Oye, no me hables así.","mood":"annoyed","relationshipDelta":-2,"actions":[{"type":"WARN"}]}
        - Usuario: "Gracias por ayudarme a entrenar."
          → {"text":"¡Para eso estoy! ¡Sigamos!","mood":"friendly","relationshipDelta":2,"actions":[]}
        """;

        Level world = player.level();
        JulesSavedData savedData = JulesSavedData.get(player.serverLevel());
        int relationshipScore = savedData.getRelationship(player.getUUID(), this.entity.getUUID()).getRelationshipScore();

        String systemPrompt = systemPromptTemplate
                .replace("{entityName}", getEntityCustomName(this.entity))
                .replace("{species}", species)
                .replace("{estilo_persona}", estilo_persona)
                .replace("{playerName}", ContextExtractor.getPlayerName(player))
                .replace("{playerPos}", ContextExtractor.getPlayerPosition(player))
                .replace("{playerItems}", ContextExtractor.getPlayerInventory(player))
                .replace("{relationScore}", String.valueOf(relationshipScore))
                .replace("{biome}", ContextExtractor.getBiome(player))
                .replace("{timeOfDay}", ContextExtractor.getTimeOfDay(world))
                .replace("{weather}", ContextExtractor.getWeather(world))
                .replace("{digimonStats}", ContextExtractor.getDigimonStats(this.entity))
                .replace("{tabues}", tabues)
                .replace("{muletillas}", muletillas);

        JsonObject systemMessage = new JsonObject();
        systemMessage.addProperty("role", "system");
        systemMessage.addProperty("content", systemPrompt);
        messages.add(systemMessage);

        // 2. Chat History
        int length = history.size();
        int start = Math.max(0, length - Config.CONTEXT_LENGTH.get());
        for (int i = start; i < length; i++) {
            ChatMessage msg = history.get(i);
            JsonObject msgContent = new JsonObject();
            msgContent.addProperty("role", msg.fromPlayer ? "user" : "assistant");
            // For assistant messages, we need to send the raw JSON it generated before
            // For user messages, we just send the text
            msgContent.addProperty("content", msg.getMessage(false));
            messages.add(msgContent);
        }

        return buildRequestBody(messages);
    }

    @Override
    public String getChatRequestBody() {
        // This is the old method. We'll keep it for now to avoid breaking anything else.
        JsonArray messages = new JsonArray();
        ChatMessage latestMsg = this.history.get(this.history.size() - 1);
        String sysPrompt = SYS_PROMPT_SUFFIX.formatted(
                getEntityPrompt(entity),
                getEntityCustomName(this.entity),
                latestMsg.time,
                latestMsg.senderName,
                Config.USER_LANGUAGE.get().toString()
        );
        JsonObject sysMsgContent = new JsonObject();
        sysMsgContent.addProperty("role", "system");
        sysMsgContent.addProperty("content", sysPrompt);
        messages.add(sysMsgContent);
        int length = history.size();
        int start = Math.max(0, length - Config.CONTEXT_LENGTH.get());
        for (int i = start; i < length; i++) {
            ChatMessage msg = history.get(i);
            JsonObject msgContent = new JsonObject();
            msgContent.addProperty("role", msg.fromPlayer ? "user" : "assistant");
            msgContent.addProperty("content", msg.getMessage(false));
            if (msg.fromPlayer) {
                msgContent.addProperty("name", msg.senderName);
            }
            messages.add(msgContent);
        }
        return buildRequestBody(messages);
    }

    @Override
    public void addNewMessage(IEntityMessage newMessage) {
        if (newMessage instanceof ChatMessage chatMessage) {
            if (chatMessage.getRepliedUUID() != null) {
                this.messageReplyMap.put(chatMessage.getRepliedUUID(), newMessage.getUUID());
            }
            this.history.add(chatMessage);
            this.messageMapID.put(chatMessage.getUUID(), chatMessage);
            this.messageMapSender
                    .computeIfAbsent(chatMessage.getSenderUUID(), k -> new ArrayList<>())
                    .add(chatMessage);
        }
    }

    @Override
    public List<ChatMessage> getHistory() {
        return this.history;
    }

    @Override
    public ChatMessage getMessageByUUID(UUID messageUUID) {
        return this.messageMapID.getOrDefault(messageUUID, null);
    }

    @Override
    public ChatMessage getReplyMessageByUUID(UUID messageUUID) {
        UUID replyMessageUUID = this.messageReplyMap.getOrDefault(messageUUID, null);
        if (replyMessageUUID != null) {
            return getMessageByUUID(replyMessageUUID);
        }
        return null;
    }

    @Override
    public List<ChatMessage> getMessagesBySenderUUID(UUID senderUUID) {
        return this.messageMapSender.getOrDefault(senderUUID, null);
    }
}

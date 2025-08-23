require("dotenv").config();
const express = require("express");
const { GoogleGenerativeAI } = require("@google/generative-ai");

const app = express();
app.use(express.json({ limit: "2mb" }));

const port = process.env.PORT || 3000;
const genAI = new GoogleGenerativeAI(process.env.GEMINI_API_KEY);

// Fallback por si no llega system
const DEFAULT_SYSTEM_PROMPT =
  "Eres un asistente útil. Responde en español de forma clara y concisa.";

app.post("/v1/chat/completions", async (req, res) => {
  try {
    const model = genAI.getGenerativeModel({
      model: "gemini-2.5-flash",
    });

    // Mensajes estilo OpenAI
    const openAI_messages = Array.isArray(req.body?.messages)
      ? req.body.messages
      : [];

    if (openAI_messages.length === 0) {
      return res.status(400).json({ error: "messages is required" });
    }

    // 1) Extraer system (opcional) y normalizar a string
    const systemMsg = openAI_messages.find((m) => m.role === "system");
    const systemPromptText =
      typeof systemMsg?.content === "string"
        ? systemMsg.content
        : Array.isArray(systemMsg?.content)
        ? systemMsg.content.map((c) => (typeof c === "string" ? c : "")).join("\n")
        : systemMsg?.content?.text || DEFAULT_SYSTEM_PROMPT;

    // 2) Resto del historial (user/assistant) y último mensaje (debe ser user)
    const chatMessages = openAI_messages.filter((m) => m.role !== "system");
    if (chatMessages.length === 0) {
      return res
        .status(400)
        .json({ error: "At least one non-system message is required" });
    }

    const lastMessage = chatMessages[chatMessages.length - 1];
    if (lastMessage.role !== "user") {
      return res.status(400).json({
        error: "Last message must be from the user.",
      });
    }

    // 3) Adaptar historial a formato Gemini (history) => alterna user/model
    //    Convertimos assistant -> model; user -> user; content -> parts[{text}]
    const toText = (content) => {
      if (typeof content === "string") return content;
      if (Array.isArray(content)) {
        // Compat con formato OpenAI multi-part (si lo usan)
        return content
          .map((c) => (typeof c === "string" ? c : c?.text || ""))
          .join("\n");
      }
      if (content && typeof content === "object" && content.text) return content.text;
      return String(content ?? "");
    };

    let history = chatMessages.slice(0, -1).map((msg) => ({
      role: msg.role === "assistant" ? "model" : "user",
      parts: [{ text: toText(msg.content) }],
    }));

    // 3.a) Gemini exige que el primer elemento del history sea 'user'
    if (history.length > 0 && history[0].role === "model") {
      // Insertamos un user sintético para cumplir la regla
      history.unshift({
        role: "user",
        parts: [{ text: "(inicio del historial)" }],
      });
    }

    // 4) Crear sesión de chat con systemInstruction en el formato correcto
    const chat = model.startChat({
      history,
      systemInstruction: {
        parts: [{ text: systemPromptText || DEFAULT_SYSTEM_PROMPT }],
      },
      generationConfig: {
        // Si quieres JSON estrictamente válido, mantenlo:
        responseMimeType: "application/json",
        maxOutputTokens: 2048,
      },
    });

    // 5) Enviar el último mensaje del usuario
    const userText = toText(lastMessage.content);
    const result = await chat.sendMessage([{ text: userText }]);
    const geminiContent = result.response.text();

    // 6) Responder en formato OpenAI-compatible
    const openAIResponse = {
      id: `chatcmpl-${Date.now()}`,
      object: "chat.completion",
      created: Math.floor(Date.now() / 1000),
      model: "gemini-2.5-flash",
      choices: [
        {
          index: 0,
          message: {
            role: "assistant",
            content: geminiContent, // si forzaste JSON, aquí vendrá JSON (string)
          },
          finish_reason: "stop",
        },
      ],
      usage: {
        prompt_tokens: 0, // sin conteo preciso disponible
        completion_tokens: 0,
        total_tokens: 0,
      },
    };

    res.json(openAIResponse);
  } catch (error) {
    console.error("Error processing request:", error);
    // Intenta exponer info útil sin perder seguridad
    const status = error?.status || 500;
    res.status(status).json({
      error: "Internal server error",
      details:
        error?.errorDetails ||
        error?.message ||
        "Unknown error from @google/generative-ai",
    });
  }
});

app.listen(port, () => {
  console.log(`OpenAI-to-Gemini proxy server listening on port ${port}`);
});

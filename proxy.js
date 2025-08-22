require('dotenv').config();
const express = require('express');
const { GoogleGenerativeAI } = require('@google/generative-ai');

const app = express();
app.use(express.json());

const port = process.env.PORT || 3000;
const genAI = new GoogleGenerativeAI(process.env.GEMINI_API_KEY);

app.post('/v1/chat/completions', async (req, res) => {
    try {
        const requestedModel = req.body.model || "gemini-1.5-flash-latest";
        const model = genAI.getGenerativeModel({ model: requestedModel });

        const openAI_messages = req.body.messages || [];

        const systemPromptFull = openAI_messages.find(m => m.role === 'system')?.content || "";
        const chatMessages = openAI_messages.filter(m => m.role !== 'system');

        // Separate the core instruction from the detailed context
        const contextMarker = "CONTEXTO:";
        const parts = systemPromptFull.split(contextMarker);
        const systemInstruction = parts[0].replace("[SYSTEM]", "").trim();
        const detailedContext = parts.length > 1 ? `${contextMarker}${parts[1]}` : "";

        // Prepend the detailed context to the first user message if it exists
        if (chatMessages.length > 0 && chatMessages[0].role === 'user' && detailedContext) {
            chatMessages[0].content = `${detailedContext}\n\n${chatMessages[0].content}`;
        }

        const history = chatMessages.slice(0, -1).map(msg => ({
            role: msg.role === 'assistant' ? 'model' : 'user',
            parts: [{ text: msg.content }],
        }));

        const lastMessage = chatMessages[chatMessages.length - 1];
        if (!lastMessage || lastMessage.role !== 'user') {
            return res.status(400).json({ error: "Last message must be from the user." });
        }

        const chat = model.startChat({
            history: history,
            generationConfig: {
                responseMimeType: "application/json",
                maxOutputTokens: 2048,
            },
            systemInstruction: systemInstruction,
        });

        const result = await chat.sendMessage(lastMessage.content);
        const response = await result.response;
        const geminiContent = response.text();

        const openAIResponse = {
            id: `chatcmpl-${Date.now()}`,
            object: "chat.completion",
            created: Math.floor(Date.now() / 1000),
            model: requestedModel,
            choices: [{
                index: 0,
                message: {
                    role: "assistant",
                    content: geminiContent,
                },
                finish_reason: "stop",
            }],
            usage: {
                prompt_tokens: 0,
                completion_tokens: 0,
                total_tokens: 0,
            },
        };

        res.json(openAIResponse);

    } catch (error) {
        console.error("Error processing request:", error);
        const errorDetails = error.errorDetails || [{ message: error.message }];
        res.status(500).json({ error: "Internal server error", details: errorDetails });
    }
});

app.listen(port, () => {
    console.log(`OpenAI-to-Gemini proxy server listening on port ${port}`);
});

require('dotenv').config();
const express = require('express');
const { GoogleGenerativeAI } = require('@google/generative-ai');

const app = express();
app.use(express.json());

const port = process.env.PORT || 3000;
const genAI = new GoogleGenerativeAI(process.env.GEMINI_API_KEY);

app.post('/v1/chat/completions', async (req, res) => {
    try {
        const model = genAI.getGenerativeModel({ model: "gemini-1.5-flash-latest" });

        // The Minecraft mod sends a full message history.
        // We need to adapt this to Gemini's format.
        // Gemini expects a `history` array of alternating user/model roles, and a final `msg` for the new prompt.
        const openAI_messages = req.body.messages || [];

        // Find the system prompt
        const systemPrompt = openAI_messages.find(m => m.role === 'system');
        const chatMessages = openAI_messages.filter(m => m.role !== 'system');

        const history = chatMessages.slice(0, -1).map(msg => ({
            role: msg.role === 'assistant' ? 'model' : 'user',
            parts: [{ text: msg.content }],
        }));

        const lastMessage = chatMessages[chatMessages.length - 1];
        if (!lastMessage || lastMessage.role !== 'user') {
            return res.status(400).json({ error: "Last message must be from the user." });
        }

        const systemPromptText = systemPrompt ? systemPrompt.content : "You are a helpful assistant.";
        const systemInstruction = {
            role: "system",
            parts: [{ text: systemPromptText }],
        };

        const chat = model.startChat({
            history: history,
            generationConfig: {
                responseMimeType: "application/json", // Crucial for structured output
                maxOutputTokens: 2048,
            },
            systemInstruction: systemInstruction,
        });

        const result = await chat.sendMessage(lastMessage.content);
        const response = await result.response;
        const geminiContent = response.text();

        // Translate the Gemini response back to OpenAI format
        const openAIResponse = {
            id: `chatcmpl-${Date.now()}`,
            object: "chat.completion",
            created: Math.floor(Date.now() / 1000),
            model: "gemini-1.5-flash-latest",
            choices: [{
                index: 0,
                message: {
                    role: "assistant",
                    content: geminiContent, // This will be the stringified JSON
                },
                finish_reason: "stop",
            }],
            usage: {
                prompt_tokens: 0, // Placeholder
                completion_tokens: 0, // Placeholder
                total_tokens: 0, // Placeholder
            },
        };

        res.json(openAIResponse);

    } catch (error) {
        console.error("Error processing request:", error);
        res.status(500).json({ error: "Internal server error" });
    }
});

app.listen(port, () => {
    console.log(`OpenAI-to-Gemini proxy server listening on port ${port}`);
});
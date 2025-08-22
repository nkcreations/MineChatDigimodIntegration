package cn.tropicalalgae.minechat.utils;

import cn.tropicalalgae.minechat.common.enumeration.LanguageType;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

public class Config {
    public static final ForgeConfigSpec COMMON_CONFIG;
    public static final ForgeConfigSpec.ConfigValue<String> PROXY_URL;
    public static final ForgeConfigSpec.ConfigValue<String> GPT_MODEL;
    public static final ForgeConfigSpec.ConfigValue<Integer> CONTEXT_LENGTH;

    public static final ForgeConfigSpec.EnumValue<LanguageType> USER_LANGUAGE;
    public static final ForgeConfigSpec.ConfigValue<String> DEFAULT_PROMPT;
    public static final ForgeConfigSpec.ConfigValue<String> FAMOUS_PROMPT;
    public static final ForgeConfigSpec.ConfigValue<String> ARMORER_PROMPT;

    public static ForgeConfigSpec.BooleanValue MOD_ENABLE;
    public static ForgeConfigSpec.BooleanValue USE_WHITE_LIST;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> WHITE_LIST;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> BLACK_LIST;

    public static ForgeConfigSpec.BooleanValue TRADE_ADJUST_ENABLED;
    public static ForgeConfigSpec.ConfigValue<Integer> DISCOUNT_TURNS;
    public static ForgeConfigSpec.DoubleValue MAX_COST_ADJUST_RATIO;

    public static ForgeConfigSpec.EnumValue<LLMProvider> LLM_PROVIDER;
    public static ForgeConfigSpec.IntValue TIMEOUT;
    public static ForgeConfigSpec.IntValue EGG_COOLDOWN_DAYS;

    public static ForgeConfigSpec.IntValue THRESHOLD_WARN;
    public static ForgeConfigSpec.IntValue THRESHOLD_ANGRY;
    public static ForgeConfigSpec.IntValue THRESHOLD_ATTACK;
    public static ForgeConfigSpec.IntValue THRESHOLD_FRIENDLY;
    public static ForgeConfigSpec.IntValue THRESHOLD_LOYAL;


    public static final ForgeConfigSpec.ConfigValue<String> KEY_OPEN_CHAT_GUI;


    public enum LLMProvider {
        GEMINI, OPENAI, LOCAL
    }

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        /* 基础配置 */
        builder.comment("LLM and Proxy Configuration").push("llm");

        LLM_PROVIDER = builder
                .comment("The LLM provider to use.")
                .defineEnum("llm_provider", LLMProvider.GEMINI);

        PROXY_URL = builder
                .comment("URL for the OpenAI-compatible proxy (e.g. http://localhost:3000/v1/chat/completions)")
                .define("proxy_url", "http://localhost:3000/v1/chat/completions");

        GPT_MODEL = builder
                .comment("Model name")
                .define("gpt_model", "gpt-4o-mini");

        TIMEOUT = builder
                .comment("Request timeout in seconds.")
                .defineInRange("timeout", 30, 5, 120);

        CONTEXT_LENGTH = builder
                .comment("Max context length for each chat")
                .define("context_length", 8);

        builder.pop();

        builder.comment("Relationship Thresholds").push("thresholds");
        THRESHOLD_WARN = builder.defineInRange("warn", -20, -100, 0);
        THRESHOLD_ANGRY = builder.defineInRange("angry", -40, -100, 0);
        THRESHOLD_ATTACK = builder.defineInRange("attack", -60, -100, 0);
        THRESHOLD_FRIENDLY = builder.defineInRange("friendly", 40, 0, 100);
        THRESHOLD_LOYAL = builder.defineInRange("loyal", 70, 0, 100);
        builder.pop();

        builder.comment("Cooldowns").push("cooldowns");
        EGG_COOLDOWN_DAYS = builder
                .comment("Cooldown in days for receiving a DigiEgg from the same entity.")
                .defineInRange("egg_cooldown_days", 7, 1, 365);
        builder.pop();

        /* prompt管理 */
        builder.comment("Config for prompt").push("prompt_config");

        USER_LANGUAGE = builder
                .comment("The language used during user interaction. Default Chinese")
                .translation("LanguageType")
                .defineEnum("user_language", LanguageType.Chinese);

        DEFAULT_PROMPT = builder
                .comment("Default prompt for villagers of all professions")
                .define("default_prompt", "你是Minecraft中的一位村民，你友好且善良");

        FAMOUS_PROMPT = builder
                .comment("Default prompt for villagers of famous")
                .define("famous_prompt", "");

        ARMORER_PROMPT = builder
                .comment("Default prompt for villagers of armorer")
                .define("armorer_prompt", "");

        builder.pop();

        /* 权限管理 */
        builder.comment("Config for player authorization").push("authorization");

        MOD_ENABLE = builder
                .comment("Allow entities to speak or not. If false, entities will not chat anymore")
                .define("mod_enable", true);

        USE_WHITE_LIST = builder
                .comment("Use white list or not. If True, only the players who on the white_list can chat with entities (villager).")
                .define("use_white_list", false);

        WHITE_LIST = builder
                .comment("Player white list")
                .defineListAllowEmpty(
                        "white_list",
                        List.of("Steve", "Alex"),
                        obj -> obj instanceof String
                );

        BLACK_LIST = builder
                .comment("Player black list. Specify the list of player names to block from interacting with entities (villagers).")
                .defineListAllowEmpty(
                        "black_list",
                        List.of(),
                        obj -> obj instanceof String
                );

        builder.pop();

        /* 附加功能 交易折扣 (好感度影响) */
        builder.comment("Config for villager trade").push("trade_adjustment");

         TRADE_ADJUST_ENABLED = builder
                .comment("Enable trade adjustment. If True, the transaction with villagers will be influenced by your actions.")
                .define("trade_adjust_enabled", true);

         DISCOUNT_TURNS = builder
                .comment("The required number of conversations triggered by discount detection")
                .define("discount_turns", 12);

         MAX_COST_ADJUST_RATIO = builder
                 .comment("Maximum price fluctuation ratio for transaction")
                 .defineInRange("max_cost_adjust_ratio", 0.5f, 0.0f, 1.0f);

        builder.pop();

        builder.comment("Keybind Configuration").push("keybinds");
        KEY_OPEN_CHAT_GUI = builder
                .comment("The key to press to open the chat GUI for the entity you are looking at.")
                .define("key_open_chat", "key.keyboard.6");
        builder.pop();

        COMMON_CONFIG = builder.build();
    }
}
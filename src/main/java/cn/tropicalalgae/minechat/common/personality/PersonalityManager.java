package cn.tropicalalgae.minechat.common.personality;

import cn.tropicalalgae.minechat.MineChat;
import com.google.gson.Gson;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.File;
import java.io.FileReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class PersonalityManager {
    private static final Map<String, Personality> personalities = new HashMap<>();
    private static final Personality defaultPersonality = new Personality();
    private static final Path CONFIG_DIR = FMLPaths.CONFIGDIR.get().resolve("jules");

    public static void loadPersonalities() {
        File dir = CONFIG_DIR.toFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
        if (files == null) {
            MineChat.LOGGER.error("Could not list personality files in " + CONFIG_DIR);
            return;
        }

        Gson gson = new Gson();
        for (File file : files) {
            try (FileReader reader = new FileReader(file)) {
                Personality personality = gson.fromJson(reader, Personality.class);
                if (personality != null && personality.species != null) {
                    personalities.put(personality.species.toLowerCase(), personality);
                    MineChat.LOGGER.info("Loaded personality for: " + personality.species);
                }
            } catch (Exception e) {
                MineChat.LOGGER.error("Failed to load personality file: " + file.getName(), e);
            }
        }

        // Setup default personality
        defaultPersonality.species = "default";
        defaultPersonality.persona = new Personality.Persona();
        defaultPersonality.persona.rasgos = new ArrayList<>();
        defaultPersonality.persona.rasgos.add("neutral");
        defaultPersonality.persona.estilo = "directo y simple";
        defaultPersonality.persona.muletillas = new ArrayList<>();
        defaultPersonality.tabues = new ArrayList<>();
    }

    public static Personality getPersonality(String species) {
        return personalities.getOrDefault(species.toLowerCase(), defaultPersonality);
    }
}

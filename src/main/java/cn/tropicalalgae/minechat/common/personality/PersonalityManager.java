package cn.tropicalalgae.minechat.common.personality;

import cn.tropicalalgae.minechat.MineChat;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PersonalityManager {
    private static final Map<String, Personality> personalities = new HashMap<>();
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
    }

    public static Personality getPersonality(String species) {
        String speciesKey = species.toLowerCase();
        if (!personalities.containsKey(speciesKey)) {
            MineChat.LOGGER.info("No personality file found for " + species + ". Creating a default one.");
            return createAndLoadPersonalityFile(species);
        }
        return personalities.get(speciesKey);
    }

    private static Personality createAndLoadPersonalityFile(String species) {
        Personality defaultPersonality = new Personality();
        defaultPersonality.species = species;
        defaultPersonality.persona = new Personality.Persona();
        defaultPersonality.persona.rasgos = new ArrayList<>(List.of("neutral"));
        defaultPersonality.persona.estilo = "directo y simple";
        defaultPersonality.persona.muletillas = new ArrayList<>();
        defaultPersonality.tabues = new ArrayList<>();

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        File personalityFile = CONFIG_DIR.resolve(species.toLowerCase() + ".json").toFile();

        try (FileWriter writer = new FileWriter(personalityFile)) {
            gson.toJson(defaultPersonality, writer);
            MineChat.LOGGER.info("Created default personality file at: " + personalityFile.getAbsolutePath());
        } catch (IOException e) {
            MineChat.LOGGER.error("Failed to create default personality file for " + species, e);
            // Return an in-memory default without saving if file creation fails
            return defaultPersonality;
        }

        // Add to the map and return it
        personalities.put(species.toLowerCase(), defaultPersonality);
        return defaultPersonality;
    }
}

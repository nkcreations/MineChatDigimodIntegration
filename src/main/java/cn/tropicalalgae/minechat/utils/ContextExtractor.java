package cn.tropicalalgae.minechat.utils;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.modderg.thedigimod.server.entity.DigimonEntity;

import java.util.stream.Collectors;

public class ContextExtractor {

    public static String getPlayerName(ServerPlayer player) {
        return player.getGameProfile().getName();
    }

    public static String getPlayerPosition(ServerPlayer player) {
        return player.blockPosition().toString();
    }

    public static String getPlayerInventory(ServerPlayer player) {
        return player.getInventory().items.stream()
                .filter(item -> !item.isEmpty())
                .limit(5)
                .map(ItemStack::getDisplayName)
                .map(component -> component.getString())
                .collect(Collectors.joining(", "));
    }

    public static String getBiome(ServerPlayer player) {
        return player.level().getBiome(player.blockPosition()).unwrapKey().map(key -> key.location().toString()).orElse("desconocido");
    }

    public static String getTimeOfDay(Level level) {
        long time = level.getDayTime() % 24000;
        if (time >= 0 && time < 6000) return "día";
        if (time >= 6000 && time < 12000) return "mediodía";
        if (time >= 12000 && time < 18000) return "tarde";
        return "noche";
    }

    public static String getWeather(Level level) {
        if (level.isThundering()) return "tormenta";
        if (level.isRaining()) return "lluvia";
        return "despejado";
    }

    public static String getEntitySpecies(Entity entity) {
        if (entity instanceof Villager) {
            return "Aldeano";
        }
        if (entity instanceof DigimonEntity) {
            // This is a placeholder. We'll need a way to get the actual species name.
            return "Digimon";
        }
        return "desconocido";
    }

    public static String getDigimonStats(Entity entity) {
        if (entity instanceof DigimonEntity) {
            // Placeholder values. These would be replaced with actual API calls to the DigimonEntity object.
            String level = "5";
            String hunger = "saciado";
            String mood = "neutral";
            String hp = String.format("%.0f/%.0f", ((DigimonEntity) entity).getHealth(), ((DigimonEntity) entity).getMaxHealth());
            String evoLine = "desconocido";
            return String.format("Nivel: %s, Hambre: %s, Humor: %s, Salud: %s, Línea Evolutiva: %s",
                    level, hunger, mood, hp, evoLine);
        }
        return "No aplicable";
    }
}

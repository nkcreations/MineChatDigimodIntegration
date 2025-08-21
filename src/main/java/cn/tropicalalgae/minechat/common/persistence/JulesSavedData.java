package cn.tropicalalgae.minechat.common.persistence;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class JulesSavedData extends SavedData {
    private static final String DATA_NAME = "jules_relationships";
    private final Map<UUID, Map<UUID, RelationshipData>> relationships = new HashMap<>();

    @Override
    public CompoundTag save(CompoundTag compoundTag) {
        CompoundTag playersTag = new CompoundTag();
        relationships.forEach((playerUUID, entityMap) -> {
            CompoundTag entitiesTag = new CompoundTag();
            entityMap.forEach((entityUUID, data) -> {
                entitiesTag.put(entityUUID.toString(), data.save(new CompoundTag()));
            });
            playersTag.put(playerUUID.toString(), entitiesTag);
        });
        compoundTag.put("relationships", playersTag);
        return compoundTag;
    }

    public static JulesSavedData load(CompoundTag compoundTag) {
        JulesSavedData savedData = new JulesSavedData();
        CompoundTag playersTag = compoundTag.getCompound("relationships");
        for (String playerUUIDStr : playersTag.getAllKeys()) {
            UUID playerUUID = UUID.fromString(playerUUIDStr);
            CompoundTag entitiesTag = playersTag.getCompound(playerUUIDStr);
            Map<UUID, RelationshipData> entityMap = new HashMap<>();
            for (String entityUUIDStr : entitiesTag.getAllKeys()) {
                UUID entityUUID = UUID.fromString(entityUUIDStr);
                entityMap.put(entityUUID, RelationshipData.load(entitiesTag.getCompound(entityUUIDStr)));
            }
            savedData.relationships.put(playerUUID, entityMap);
        }
        return savedData;
    }

    public static JulesSavedData get(ServerLevel level) {
        DimensionDataStorage storage = level.getServer().overworld().getDataStorage();
        return storage.computeIfAbsent(JulesSavedData::load, JulesSavedData::new, DATA_NAME);
    }

    public RelationshipData getRelationship(UUID playerUUID, UUID entityUUID) {
        return relationships.computeIfAbsent(playerUUID, k -> new HashMap<>())
                            .computeIfAbsent(entityUUID, k -> new RelationshipData());
    }

    public void clearData() {
        relationships.clear();
    }
}

package cn.tropicalalgae.minechat.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import com.google.common.collect.ImmutableSet;

import java.util.HashMap;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.structure.Structure;
import java.util.Map;
import java.util.stream.Collectors;

public class EnvironmentScanner {

    // A predefined set of "interesting" blocks to look for.
    private static final ImmutableSet<String> INTERESTING_BLOCK_KEYWORDS = ImmutableSet.of(
            "ore", "chest", "spawner", "portal", "table", "furnace", "anvil"
    );

    public static String scanNearbyBlocks(Entity center, int radius) {
        Map<String, Integer> blockCounts = new HashMap<>();
        Level level = center.level();
        BlockPos centerPos = center.blockPosition();

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos currentPos = centerPos.offset(x, y, z);
                    BlockState blockState = level.getBlockState(currentPos);
                    if (!blockState.isAir()) {
                        String blockId = BuiltInRegistries.BLOCK.getKey(blockState.getBlock()).toString();
                        if (INTERESTING_BLOCK_KEYWORDS.stream().anyMatch(blockId::contains)) {
                            blockCounts.merge(blockState.getBlock().getName().getString(), 1, Integer::sum);
                        }
                    }
                }
            }
        }

        if (blockCounts.isEmpty()) {
            return "nada de interés";
        }

        return blockCounts.entrySet().stream()
                .map(entry -> entry.getValue() + " " + entry.getKey())
                .collect(Collectors.joining(", "));
    }

    public static String scanNearbyEntities(Entity center, int radius) {
        Level level = center.level();
        AABB area = new AABB(center.blockPosition()).inflate(radius);
        List<Entity> nearbyEntities = level.getEntities(center, area, entity -> !(entity instanceof Player) && entity != center);

        if (nearbyEntities.isEmpty()) {
            return "nadie más";
        }

        Map<String, Long> entityCounts = nearbyEntities.stream()
                .collect(Collectors.groupingBy(e -> e.getType().getDescription().getString(), Collectors.counting()));

        return entityCounts.entrySet().stream()
                .map(entry -> entry.getValue() + " " + entry.getKey())
                .collect(Collectors.joining(", "));
    }

    public static String findNearestStructure(Entity center, int radius) {
        if (!(center.level() instanceof ServerLevel serverLevel)) {
            return "información de estructuras no disponible";
        }

        List<TagKey<Structure>> tagsToSearch = List.of(
            TagKey.create(Registries.STRUCTURE, new ResourceLocation("minecraft:village")),
            TagKey.create(Registries.STRUCTURE, new ResourceLocation("minecraft:ruined_portal")),
            TagKey.create(Registries.STRUCTURE, new ResourceLocation("minecraft:mineshaft"))
        );

        BlockPos centerPos = center.blockPosition();
        String closestStructure = "ninguna";
        int closestDistSq = radius * radius;

        for (TagKey<Structure> tag : tagsToSearch) {
            var result = serverLevel.getChunkSource().getGenerator().findNearestMapStructure(serverLevel, tag, centerPos, radius, false);
            if (result != null) {
                BlockPos structurePos = result.getFirst();
                int distSq = (int) centerPos.distSqr(structurePos);
                if (distSq < closestDistSq) {
                    closestDistSq = distSq;
                    closestStructure = tag.location().getPath().replace("_", " ");
                }
            }
        }

        if (!closestStructure.equals("ninguna")) {
             return "una " + closestStructure + " cercana";
        }

        return "ninguna estructura de interés cercana";
    }
}

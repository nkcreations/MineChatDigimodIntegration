package cn.tropicalalgae.minechat.common.memory;

import cn.tropicalalgae.minechat.common.model.IEntityMemory;
import cn.tropicalalgae.minechat.common.model.impl.ChatMessage;
import cn.tropicalalgae.minechat.common.model.impl.ChatMemory;
import net.minecraft.world.entity.Entity;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SharedMemoryManager {

    private static final Map<String, IEntityMemory<ChatMessage>> SHARED_MEMORIES = new ConcurrentHashMap<>();

    // We pass a dummy entity just to satisfy the ChatMemory constructor, but it's not actually used for identification.
    public static IEntityMemory<ChatMessage> get(String species, Entity dummyEntity) {
        return SHARED_MEMORIES.computeIfAbsent(species.toLowerCase(), k -> new ChatMemory(dummyEntity));
    }
}

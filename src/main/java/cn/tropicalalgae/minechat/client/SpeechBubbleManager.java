package cn.tropicalalgae.minechat.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SpeechBubbleManager {

    private static final Map<Integer, BubbleData> ACTIVE_BUBBLES = new ConcurrentHashMap<>();
    private static final int BUBBLE_DURATION_TICKS = 100; // 5 seconds (20 ticks/sec)

    // Inner class to hold bubble information
    private static class BubbleData {
        private final String text;
        private int ticksRemaining;

        public BubbleData(String text) {
            this.text = text;
            this.ticksRemaining = BUBBLE_DURATION_TICKS;
        }
    }

    // Called from our packet handler
    public static void addBubble(int entityId, String text) {
        ACTIVE_BUBBLES.put(entityId, new BubbleData(text));
    }

    // Called from our render handler
    public static String getBubbleText(int entityId) {
        BubbleData data = ACTIVE_BUBBLES.get(entityId);
        return (data != null) ? data.text : null;
    }

    // Called every client tick
    public static void tick() {
        if (ACTIVE_BUBBLES.isEmpty()) {
            return;
        }
        // Using removeIf is a clean way to iterate and remove
        ACTIVE_BUBBLES.entrySet().removeIf(entry -> {
            entry.getValue().ticksRemaining--;
            return entry.getValue().ticksRemaining <= 0;
        });
    }
}

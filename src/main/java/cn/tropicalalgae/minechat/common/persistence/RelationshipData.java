package cn.tropicalalgae.minechat.common.persistence;

import net.minecraft.nbt.CompoundTag;

public class RelationshipData {
    private int relationshipScore;
    private long lastEggTimestamp;

    public RelationshipData() {
        this.relationshipScore = 0;
        this.lastEggTimestamp = 0;
    }

    public int getRelationshipScore() {
        return relationshipScore;
    }

    public void setRelationshipScore(int relationshipScore) {
        this.relationshipScore = Math.max(-100, Math.min(100, relationshipScore));
    }

    public void adjustRelationshipScore(int delta) {
        setRelationshipScore(this.relationshipScore + delta);
    }

    public long getLastEggTimestamp() {
        return lastEggTimestamp;
    }

    public void setLastEggTimestamp(long lastEggTimestamp) {
        this.lastEggTimestamp = lastEggTimestamp;
    }

    public static RelationshipData load(CompoundTag tag) {
        RelationshipData data = new RelationshipData();
        data.setRelationshipScore(tag.getInt("relationshipScore"));
        data.setLastEggTimestamp(tag.getLong("lastEggTimestamp"));
        return data;
    }

    public CompoundTag save(CompoundTag tag) {
        tag.putInt("relationshipScore", this.relationshipScore);
        tag.putLong("lastEggTimestamp", this.lastEggTimestamp);
        return tag;
    }
}

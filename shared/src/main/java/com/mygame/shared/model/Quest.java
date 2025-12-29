package com.mygame.shared.model;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoSerializable;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * Quest model for daily quest system
 */
public class Quest implements KryoSerializable {
    public int questId;
    public String description;
    public String gameType;
    public int targetCount;
    public long rewardCredits;
    public int currentProgress;
    public boolean isClaimed;
    
    public Quest() {}
    
    public Quest(int questId, String description, String gameType, int targetCount, 
                 long rewardCredits, int currentProgress, boolean isClaimed) {
        this.questId = questId;
        this.description = description;
        this.gameType = gameType;
        this.targetCount = targetCount;
        this.rewardCredits = rewardCredits;
        this.currentProgress = currentProgress;
        this.isClaimed = isClaimed;
    }
    
    public boolean isCompleted() {
        return currentProgress >= targetCount;
    }
    
    @Override
    public void write(Kryo kryo, Output output) {
        output.writeInt(questId);
        output.writeString(description);
        output.writeString(gameType);
        output.writeInt(targetCount);
        output.writeLong(rewardCredits);
        output.writeInt(currentProgress);
        output.writeBoolean(isClaimed);
    }
    
    @Override
    public void read(Kryo kryo, Input input) {
        questId = input.readInt();
        description = input.readString();
        gameType = input.readString();
        targetCount = input.readInt();
        rewardCredits = input.readLong();
        currentProgress = input.readInt();
        isClaimed = input.readBoolean();
    }
}


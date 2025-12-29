package com.mygame.shared.network.packets;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoSerializable;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * Response for quest claim attempt
 */
public class ClaimQuestResponse implements KryoSerializable {
    public boolean success;
    public int questId;
    public long creditsAwarded;
    public long newTotalCredits;
    public String errorMessage;
    
    public ClaimQuestResponse() {}
    
    @Override
    public void write(Kryo kryo, Output output) {
        output.writeBoolean(success);
        output.writeInt(questId);
        output.writeLong(creditsAwarded);
        output.writeLong(newTotalCredits);
        output.writeString(errorMessage);
    }
    
    @Override
    public void read(Kryo kryo, Input input) {
        success = input.readBoolean();
        questId = input.readInt();
        creditsAwarded = input.readLong();
        newTotalCredits = input.readLong();
        errorMessage = input.readString();
    }
}


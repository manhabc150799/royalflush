package com.mygame.shared.network.packets;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoSerializable;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * Request to claim a completed quest reward
 */
public class ClaimQuestRequest implements KryoSerializable {
    public int questId;
    
    public ClaimQuestRequest() {}
    
    public ClaimQuestRequest(int questId) {
        this.questId = questId;
    }
    
    @Override
    public void write(Kryo kryo, Output output) {
        output.writeInt(questId);
    }
    
    @Override
    public void read(Kryo kryo, Input input) {
        questId = input.readInt();
    }
}


package com.mygame.shared.network.packets;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoSerializable;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.mygame.shared.model.Quest;

import java.util.ArrayList;
import java.util.List;

/**
 * Response containing user's daily quests
 */
public class GetQuestsResponse implements KryoSerializable {
    public boolean success;
    public List<Quest> quests = new ArrayList<>();
    public String errorMessage;
    
    public GetQuestsResponse() {}
    
    @Override
    public void write(Kryo kryo, Output output) {
        output.writeBoolean(success);
        output.writeInt(quests.size());
        for (Quest quest : quests) {
            kryo.writeObject(output, quest);
        }
        output.writeString(errorMessage);
    }
    
    @Override
    public void read(Kryo kryo, Input input) {
        success = input.readBoolean();
        int size = input.readInt();
        quests = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            quests.add(kryo.readObject(input, Quest.class));
        }
        errorMessage = input.readString();
    }
}


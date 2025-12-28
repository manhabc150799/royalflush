package com.mygame.shared.network.packets;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoSerializable;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.mygame.shared.model.MatchHistoryEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * Packet phản hồi lịch sử trận đấu từ server
 */
public class MatchHistoryResponse implements KryoSerializable {
    public List<MatchHistoryEntry> entries = new ArrayList<>();
    
    public MatchHistoryResponse() {
    }
    
    @Override
    public void write(Kryo kryo, Output output) {
        output.writeInt(entries.size());
        for (MatchHistoryEntry entry : entries) {
            kryo.writeObject(output, entry);
        }
    }
    
    @Override
    public void read(Kryo kryo, Input input) {
        int size = input.readInt();
        entries = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            entries.add(kryo.readObject(input, MatchHistoryEntry.class));
        }
    }
}


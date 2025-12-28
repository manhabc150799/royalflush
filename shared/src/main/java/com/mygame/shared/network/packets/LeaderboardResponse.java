package com.mygame.shared.network.packets;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoSerializable;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.mygame.shared.model.LeaderboardEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * Leaderboard response packet.
 */
public class LeaderboardResponse implements KryoSerializable {
    public List<LeaderboardEntry> entries = new ArrayList<>();
    
    public LeaderboardResponse() {}
    
    @Override
    public void write(Kryo kryo, Output output) {
        output.writeInt(entries.size());
        for (LeaderboardEntry entry : entries) {
            kryo.writeObject(output, entry);
        }
    }
    
    @Override
    public void read(Kryo kryo, Input input) {
        int size = input.readInt();
        entries = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            entries.add(kryo.readObject(input, LeaderboardEntry.class));
        }
    }
}

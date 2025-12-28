package com.mygame.shared.network.packets;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoSerializable;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * Leaderboard request packet.
 */
public class LeaderboardRequest implements KryoSerializable {
    public int limit;
    
    public LeaderboardRequest() {}
    
    @Override
    public void write(Kryo kryo, Output output) {
        output.writeInt(limit);
    }
    
    @Override
    public void read(Kryo kryo, Input input) {
        limit = input.readInt();
    }
}

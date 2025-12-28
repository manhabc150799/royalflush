package com.mygame.shared.network.packets;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoSerializable;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * Packet yêu cầu lấy lịch sử trận đấu
 */
public class MatchHistoryRequest implements KryoSerializable {
    public int userId;
    public int limit = 5; // Default
    
    public MatchHistoryRequest() {
    }
    
    public MatchHistoryRequest(int userId, int limit) {
        this.userId = userId;
        this.limit = limit;
    }
    
    @Override
    public void write(Kryo kryo, Output output) {
        output.writeInt(userId);
        output.writeInt(limit);
    }
    
    @Override
    public void read(Kryo kryo, Input input) {
        userId = input.readInt();
        limit = input.readInt();
    }
}


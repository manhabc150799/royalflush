package com.mygame.shared.network.packets;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoSerializable;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * Packet yêu cầu nhận daily reward
 */
public class DailyRewardRequest implements KryoSerializable {
    public int userId;
    
    public DailyRewardRequest() {
    }
    
    public DailyRewardRequest(int userId) {
        this.userId = userId;
    }
    
    @Override
    public void write(Kryo kryo, Output output) {
        output.writeInt(userId);
    }
    
    @Override
    public void read(Kryo kryo, Input input) {
        userId = input.readInt();
    }
}


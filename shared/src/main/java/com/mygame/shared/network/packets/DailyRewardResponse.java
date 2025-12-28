package com.mygame.shared.network.packets;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoSerializable;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.time.LocalDateTime;

/**
 * Packet phản hồi daily reward từ server
 */
public class DailyRewardResponse implements KryoSerializable {
    public boolean success;
    public long creditsReceived;
    public LocalDateTime nextRewardTime;
    public String errorMessage;
    
    public DailyRewardResponse() {
    }
    
    @Override
    public void write(Kryo kryo, Output output) {
        output.writeBoolean(success);
        output.writeLong(creditsReceived);
        kryo.writeObjectOrNull(output, nextRewardTime, LocalDateTime.class);
        output.writeString(errorMessage);
    }
    
    @Override
    public void read(Kryo kryo, Input input) {
        success = input.readBoolean();
        creditsReceived = input.readLong();
        nextRewardTime = kryo.readObjectOrNull(input, LocalDateTime.class);
        errorMessage = input.readString();
    }
}


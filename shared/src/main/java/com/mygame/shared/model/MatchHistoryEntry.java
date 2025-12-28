package com.mygame.shared.model;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoSerializable;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.util.Date;

/**
 * Match history entry model.
 */
public class MatchHistoryEntry implements KryoSerializable {
    public String gameType;
    public String matchMode;
    public String result;
    public long creditsChange;
    public Date timestamp;
    
    public MatchHistoryEntry() {}
    
    @Override
    public void write(Kryo kryo, Output output) {
        output.writeString(gameType);
        output.writeString(matchMode);
        output.writeString(result);
        output.writeLong(creditsChange);
        output.writeLong(timestamp != null ? timestamp.getTime() : 0);
    }
    
    @Override
    public void read(Kryo kryo, Input input) {
        gameType = input.readString();
        matchMode = input.readString();
        result = input.readString();
        creditsChange = input.readLong();
        long time = input.readLong();
        timestamp = time > 0 ? new Date(time) : null;
    }
}

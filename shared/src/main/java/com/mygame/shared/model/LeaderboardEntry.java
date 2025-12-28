package com.mygame.shared.model;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoSerializable;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * Leaderboard entry model.
 */
public class LeaderboardEntry implements KryoSerializable {
    public int rank;
    public String username;
    public long credits;
    public Rank rankEnum;
    
    public LeaderboardEntry() {}
    
    @Override
    public void write(Kryo kryo, Output output) {
        output.writeInt(rank);
        output.writeString(username);
        output.writeLong(credits);
        kryo.writeObject(output, rankEnum);
    }
    
    @Override
    public void read(Kryo kryo, Input input) {
        rank = input.readInt();
        username = input.readString();
        credits = input.readLong();
        rankEnum = kryo.readObject(input, Rank.class);
    }
}

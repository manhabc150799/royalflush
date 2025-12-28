package com.mygame.shared.model;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoSerializable;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * Player profile model with Kryo serialization support.
 */
public class PlayerProfile implements KryoSerializable {
    public int id;
    public String username;
    public long credits;
    public Rank rank;
    public int totalWins;
    public int totalLosses;
    
    public PlayerProfile() {}
    
    public PlayerProfile(int id, String username, long credits, Rank rank, int totalWins, int totalLosses) {
        this.id = id;
        this.username = username;
        this.credits = credits;
        this.rank = rank;
        this.totalWins = totalWins;
        this.totalLosses = totalLosses;
    }
    
    @Override
    public void write(Kryo kryo, Output output) {
        output.writeInt(id);
        output.writeString(username);
        output.writeLong(credits);
        kryo.writeObject(output, rank);
        output.writeInt(totalWins);
        output.writeInt(totalLosses);
    }
    
    @Override
    public void read(Kryo kryo, Input input) {
        id = input.readInt();
        username = input.readString();
        credits = input.readLong();
        rank = kryo.readObject(input, Rank.class);
        totalWins = input.readInt();
        totalLosses = input.readInt();
    }
}

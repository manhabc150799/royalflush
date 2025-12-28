package com.mygame.shared.network.packets;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoSerializable;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.mygame.shared.model.PlayerProfile;

/**
 * Packet phản hồi từ server sau khi đăng ký
 */
public class RegisterResponse implements KryoSerializable {
    public boolean success;
    public PlayerProfile playerProfile;
    public String errorMessage;
    
    public RegisterResponse() {
    }
    
    @Override
    public void write(Kryo kryo, Output output) {
        output.writeBoolean(success);
        kryo.writeObjectOrNull(output, playerProfile, PlayerProfile.class);
        output.writeString(errorMessage);
    }
    
    @Override
    public void read(Kryo kryo, Input input) {
        success = input.readBoolean();
        playerProfile = kryo.readObjectOrNull(input, PlayerProfile.class);
        errorMessage = input.readString();
    }
}


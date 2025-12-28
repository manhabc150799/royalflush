package com.mygame.shared.network.packets;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoSerializable;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * Login request packet.
 */
public class LoginRequest implements KryoSerializable {
    public String username;
    public String password;
    
    public LoginRequest() {}
    
    @Override
    public void write(Kryo kryo, Output output) {
        output.writeString(username);
        output.writeString(password);
    }
    
    @Override
    public void read(Kryo kryo, Input input) {
        username = input.readString();
        password = input.readString();
    }
}

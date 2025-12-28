package com.mygame.shared.network.packets;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoSerializable;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * Packet gửi từ client để đăng ký
 */
public class RegisterRequest implements KryoSerializable {
    public String username;
    public String password;
    
    public RegisterRequest() {
    }
    
    public RegisterRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }
    
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


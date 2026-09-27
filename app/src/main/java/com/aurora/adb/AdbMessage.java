package com.cyrust.fftoolspromaxnew.adb;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class AdbMessage {
    public static final int HEADER_LENGTH = 24;

    public final int command;
    public final int arg0;
    public final int arg1;
    public final int data_length;
    public final int data_crc32;
    public final int magic;
    public final byte[] data;

    public AdbMessage(int command, int arg0, int arg1, int data_length, int data_crc32, int magic, byte[] data) {
        this.command = command;
        this.arg0 = arg0;
        this.arg1 = arg1;
        this.data_length = data_length;
        this.data_crc32 = data_crc32;
        this.magic = magic;
        this.data = data;
    }

    public AdbMessage(int command, int arg0, int arg1, String data) {
        this(command, arg0, arg1, (data + "\0").getBytes());
    }

    public AdbMessage(int command, int arg0, int arg1, byte[] data) {
        this(command, arg0, arg1, 
             data != null ? data.length : 0, 
             crc32(data), 
             command ^ 0xFFFFFFFF, 
             data);
    }

    public boolean validate() {
        if (command != (magic ^ 0xFFFFFFFF)) return false;
        if (data_length != 0 && crc32(data) != data_crc32) return false;
        return true;
    }

    public void validateOrThrow() {
        if (!validate()) throw new IllegalArgumentException("Bad message");
    }

    public byte[] toByteArray() {
        int length = HEADER_LENGTH + (data != null ? data.length : 0);
        ByteBuffer buffer = ByteBuffer.allocate(length).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(command);
        buffer.putInt(arg0);
        buffer.putInt(arg1);
        buffer.putInt(data_length);
        buffer.putInt(data_crc32);
        buffer.putInt(magic);
        if (data != null) {
            buffer.put(data);
        }
        return buffer.array();
    }

    private static int crc32(byte[] data) {
        if (data == null) return 0;
        int res = 0;
        for (byte b : data) {
            if (b >= 0) res += b;
            else res += b + 256;
        }
        return res;
    }
}

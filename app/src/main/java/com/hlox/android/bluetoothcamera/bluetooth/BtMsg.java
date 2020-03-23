package com.hlox.android.bluetoothcamera.bluetooth;

public class BtMsg {
    public static final byte HEAR = 1;
    public static final byte CONTROL = 2;
    public static final byte PREVIEW = 3;
    public static final byte EXTRA = 4;
    public static final byte TEXT =5;
    private byte[] data;

    public BtMsg(byte[] data) {
        this.data = data;
    }

    public byte[] getData() {
        return data;
    }
}

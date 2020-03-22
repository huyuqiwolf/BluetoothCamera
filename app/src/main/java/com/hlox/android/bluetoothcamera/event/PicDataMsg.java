package com.hlox.android.bluetoothcamera.event;

public class PicDataMsg {
    private byte[] data;
    public PicDataMsg(byte[] data) {
        this.data = data;
    }

    public byte[] getData() {
        return data;
    }
}

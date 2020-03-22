package com.hlox.android.bluetoothcamera.event;

public class BtStateMsg {
    private boolean connected;
    private String msg;

    public BtStateMsg(boolean connected, String msg) {
        this.connected = connected;
        this.msg = msg;
    }

    @Override
    public String toString() {
        return "BtStateMsg{" +
                "connected=" + connected +
                ", msg='" + msg + '\'' +
                '}';
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}

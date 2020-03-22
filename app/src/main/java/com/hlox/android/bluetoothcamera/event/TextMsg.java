package com.hlox.android.bluetoothcamera.event;

public class TextMsg {
    private String msg;

    public TextMsg(String msg) {
        this.msg =msg;
    }

    public String getMsg() {
        return msg;
    }

    @Override
    public String toString() {
        return "TextMsg{" +
                "msg='" + msg + '\'' +
                '}';
    }
}

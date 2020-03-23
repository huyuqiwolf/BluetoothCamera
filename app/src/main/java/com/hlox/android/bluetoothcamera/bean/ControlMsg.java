package com.hlox.android.bluetoothcamera.bean;


public class ControlMsg {

    private boolean connect;

    private boolean changeCamera;

    private boolean takePhoto;

    private boolean response;

    private String file;

    public ControlMsg(boolean connect, boolean changeCamera, boolean takePhoto, boolean response, String file) {
        this.connect = connect;
        this.changeCamera = changeCamera;
        this.takePhoto = takePhoto;
        this.response = response;
        this.file = file;
    }

    public boolean isResponse() {
        return response;
    }

    public void setResponse(boolean response) {
        this.response = response;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public boolean isConnect() {
        return connect;
    }

    public void setConnect(boolean connect) {
        this.connect = connect;
    }

    public boolean isChangeCamera() {
        return changeCamera;
    }

    public void setChangeCamera(boolean changeCamera) {
        this.changeCamera = changeCamera;
    }

    public boolean isTakePhoto() {
        return takePhoto;
    }

    public void setTakePhoto(boolean takePhoto) {
        this.takePhoto = takePhoto;
    }

    @Override
    public String toString() {
        return "ControlMsg{" +
                "connect=" + connect +
                ", changeCamera=" + changeCamera +
                ", takePhoto=" + takePhoto +
                '}';
    }
}

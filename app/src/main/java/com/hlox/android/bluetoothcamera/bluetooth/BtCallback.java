package com.hlox.android.bluetoothcamera.bluetooth;

import java.io.IOException;

public interface BtCallback {
    /**
     * 发送消息失败
     * @param e 失败原因
     */
    void onWriteError(IOException e);

    /**
     * 主动连接失败,或者在消息发送过程前失败
     */
    void onConnectFailed();

    /**
     * 连接成功
     * @param name 设备名
     */
    void onConnected(String name);

    /**
     * 接收到数据
     * @param msg 数据
     */
    void onDataReceived(BtMsg msg);

    /**
     * 读数据失败
     * @param e 失败原因
     */
    void onReadError(IOException e);

    /**
     * 数据发送完成
     */
    void onWriteDone();

    /**
     * 等待连接失败
     * @param e 失败原因
     */
    void onWaitError(IOException e);

    /**
     * 等待被连接
     */
    void onWaiting();
}

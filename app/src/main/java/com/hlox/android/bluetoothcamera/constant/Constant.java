package com.hlox.android.bluetoothcamera.constant;

import java.util.UUID;

public class Constant {
    public static final String SERVER_NAME = "BluetoothDemo";
    // 8ce255c0-200a-11e0-ac64-0800200c9a66
    // fa87c0d0-afac-11de-8a39-0800200c9a66
    public static final UUID BLUETOOTH_UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");

    public static final int BLUETOOTH_DATA_RECEIVED = 0x3000;
    public static final int BLUETOOTH_CONNECTED = 0x3001;
    public static final int BLUETOOTH_DISCONNECTED = 0x3002;
    public static final int BLUETOOTH_SEND_ERROR = 0x3003; // 异常
    public static final int BLUETOOTH_SEND_SUCCESS = 0x3004;
    public static final int BLUETOOTH_SEND_FAILED = 0x3005;// 未连接
    public static final int BLUETOOTH_TOAST = 0x3006;
    public static final int BLUETOOTH_DEVICE_NAME = 0x3007;
    public static final String TOAST = "toast";
    public static final String DEVICE_NAME = "deviceName";
}

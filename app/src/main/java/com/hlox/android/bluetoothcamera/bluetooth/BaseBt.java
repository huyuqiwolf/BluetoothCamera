package com.hlox.android.bluetoothcamera.bluetooth;

import android.app.Service;
import android.bluetooth.BluetoothSocket;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public abstract class BaseBt extends Service {
    private BtCallback mCallback;
    protected abstract void setCallback();
    protected class ConnectedThread implements Runnable{

        private BluetoothSocket mSocket;
        private DataOutputStream mOut;
        private DataInputStream mIn;

        public ConnectedThread(BluetoothSocket socket) {
            this.mSocket = socket;
            try {
                mOut = new DataOutputStream(mSocket.getOutputStream());
                mIn = new DataInputStream(mSocket.getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {

        }
    }
}

package com.hlox.android.bluetoothcamera.bluetooth;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.hlox.android.bluetoothcamera.constant.Constant;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class BtClientService extends Service {
    private static final String TAG = "BtClientService";
    private final LocalBinder mBinder = new LocalBinder();
    private BtCallback mCallback;
    private ConnectedThread mConnectedThread;
    private ConnectThread mConnectThread;

    public class LocalBinder extends Binder {
        public BtClientService getService(){
            return BtClientService.this;
        }
    }

    public void init(BluetoothDevice device,BtCallback callback){
        this.mCallback = callback;
        connect(device);
    }

    public void sendMessage(BtMsg msg){
        if(mConnectedThread!=null){
            mConnectedThread.write(msg);
        }
    }


    private void connect(BluetoothDevice device){
        if(mConnectThread != null){
            mConnectThread .cancel();
            mConnectThread = null;
        }
        if(mConnectedThread!=null){
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        mConnectThread =new ConnectThread(device);
        mConnectThread.start();
    }

    private void manageConnection(BluetoothSocket socket){
        if(mConnectedThread !=null){
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();
    }

    private class ConnectThread extends Thread {
        private BluetoothSocket mSocket;

        public ConnectThread(BluetoothDevice device) {
            BluetoothSocket temp = null;
            try {
                temp = device.createRfcommSocketToServiceRecord(Constant.BLUETOOTH_UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
            mSocket = temp;
        }

        @Override
        public void run() {
            BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
            try {
                mSocket.connect();
            } catch (IOException e) {
                e.printStackTrace();
                try {
                    mSocket.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                mCallback.onConnectFailed();
                return;
            }
            manageConnection(mSocket);
        }

        public void cancel(){
            if(mSocket!=null){
                try {
                    mSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                mSocket = null;
            }
        }
    }

    private class ConnectedThread extends Thread {

        private BluetoothSocket mSocket;
        private DataOutputStream mOut;
        private DataInputStream mIn;
        private volatile boolean isRead = true;

        public ConnectedThread(BluetoothSocket socket) {
            this.mSocket = socket;
            try {
                mOut = new DataOutputStream(mSocket.getOutputStream());
                mIn = new DataInputStream(mSocket.getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
                isRead = false;
            }
        }

        @Override
        public void run() {
            if(isRead){
                mCallback.onConnected(mSocket.getRemoteDevice().getName());
            }else{
                mCallback.onConnectFailed();
            }
            while (isRead) {
                try {
                    int available = mIn.available();
                    if (available > 0) {
                        int length = mIn.readInt();
                        byte[] buffer = new byte[length];
                        mIn.readFully(buffer, 0, length);
                        int end = mIn.readByte();
                        Log.d(TAG, "run: length " + length + " end: " + end);
                        mCallback.onDataReceived(new BtMsg(buffer));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    mCallback.onReadError(e);
                    cancel();
                }
            }
        }

        public void cancel() {
            this.isRead = false;
        }

        public void write(BtMsg msg) {
            if (mOut != null) {
                try {
                    mOut.writeInt(msg.getData().length);
                    mOut.write(msg.getData(), 0, msg.getData().length);
                    mOut.writeByte(0xFF);
                    mCallback.onWriteDone();
                } catch (IOException e) {
                    e.printStackTrace();
                    mCallback.onWriteError(e);
                }
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // TODO 销毁资源
        return super.onUnbind(intent);
    }
}

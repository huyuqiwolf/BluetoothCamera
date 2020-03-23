package com.hlox.android.bluetoothcamera;

import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.hlox.android.bluetoothcamera.bean.ControlMsg;
import com.hlox.android.bluetoothcamera.bluetooth.BtCallback;
import com.hlox.android.bluetoothcamera.bluetooth.BtClientService;
import com.hlox.android.bluetoothcamera.bluetooth.BtMsg;
import com.hlox.android.bluetoothcamera.util.GsonUtil;

import java.io.IOException;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;


public class ClientActivity extends AppCompatActivity {
    private static final String TAG = "ClientActivity";
    private static final int CONNECT = 0x1000;
    public static final String DEVICE = "device";
    private BluetoothDevice device;
    private ImageView ivReceived;
    private BtClientService mClientService;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case CONNECT:
                    ControlMsg controlMsg = new ControlMsg(true, false, false, false, "");
                    byte[] bytes = GsonUtil.bean2Json(controlMsg).getBytes();
                    byte[] data = new byte[1 + bytes.length];
                    data[0] = (byte) (BtMsg.CONTROL & 0xFF);
                    System.arraycopy(bytes, 0, data, 1, bytes.length);
                    mClientService.sendMessage(new BtMsg(data));
                    break;
                default:
            }
        }
    };

    private ServiceConnection mConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mClientService = ((BtClientService.LocalBinder) service).getService();
            mClientService.init(device, mCallback);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mClientService = null;
        }
    };

    private BtCallback mCallback = new BtCallback() {

        @Override
        public void onWaiting() {
            Log.d(TAG, "onWaiting: ");
            showToast("onWaiting");
        }

        @Override
        public void onWriteError(IOException e) {
            Log.e(TAG, "onWriteError: ", e);
            showToast("onWriteError");
        }

        @Override
        public void onConnectFailed() {
            Log.d(TAG, "onConnectFailed: ");
            showToast("onConnectFailed");
        }

        @Override
        public void onConnected(String name) {
            Log.d(TAG, "onConnected: " + name);
            showToast(name);
            mHandler.sendEmptyMessageDelayed(CONNECT, 50);
        }

        @Override
        public void onDataReceived(BtMsg msg) {
            Log.d(TAG, "onDataReceived: " + msg.getData()[0]);
            switch (msg.getData()[0]) {
                case BtMsg.PREVIEW:
                    processPreview(msg.getData());
                    break;
                case BtMsg.TEXT:
                    processText(msg.getData());
                    break;
                case BtMsg.HEAR:
                    processHeart(msg.getData());
                    break;
            }
        }

        @Override
        public void onReadError(IOException e) {
            showToast("onReadError");
        }

        @Override
        public void onWriteDone() {
            Log.d(TAG, "onWriteDone: ");
        }

        @Override
        public void onWaitError(IOException e) {
            showToast("onWaitError");
        }
    };

    private void processHeart(byte[] data) {
        String msg = new String(data,1,data.length-1);
        Log.d(TAG, "onDataReceived: "+msg);
        byte[] outStr = "heart from client".getBytes();
        byte[] out = new byte[outStr.length+1];
        out[0] = BtMsg.HEAR;
        System.arraycopy(outStr,0,out,1,outStr.length);
        mClientService.sendMessage(new BtMsg(out));

    }

    private void processText(byte[] data) {
        String msg = new String(data, 1, data.length - 1);
        runOnUiThread(() -> showToast(msg));
    }

    private void processPreview(byte[] data) {
        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 1, data.length - 1);
        if (bitmap != null) {
            runOnUiThread(() -> ivReceived.setImageBitmap(bitmap));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);
        this.device = getIntent().getParcelableExtra(DEVICE);
        ivReceived = findViewById(R.id.iv_received);
        Intent intent = new Intent(this, BtClientService.class);
        bindService(intent, mConn, Context.BIND_AUTO_CREATE);
        findViewById(R.id.btn_switch).setOnClickListener((v) -> {
            if (mClientService != null) {
                ControlMsg controlMsg = new ControlMsg(true, true, false, false, "");
                byte[] bytes = GsonUtil.bean2Json(controlMsg).getBytes();
                byte[] data = new byte[1 + bytes.length];
                data[0] = (byte) (BtMsg.CONTROL & 0xFF);
                System.arraycopy(bytes, 0, data, 1, bytes.length);
                mClientService.sendMessage(new BtMsg(data));
            } else {
                showToast("not connected");
            }
        });
        findViewById(R.id.btn_take_photo).setOnClickListener((v) -> {
            if (mClientService != null) {
                ControlMsg controlMsg = new ControlMsg(true, false, true, false, "");
                byte[] bytes = GsonUtil.bean2Json(controlMsg).getBytes();
                byte[] data = new byte[1 + bytes.length];
                data[0] = (byte) (BtMsg.CONTROL & 0xFF);
                System.arraycopy(bytes, 0, data, 1, bytes.length);
                mClientService.sendMessage(new BtMsg(data));
            } else {
                showToast("not connected");
            }
        });
    }


    @Override
    protected void onDestroy() {
        if (mClientService != null) {
            ControlMsg controlMsg = new ControlMsg(false, false, false, false, "");
            byte[] bytes = GsonUtil.bean2Json(controlMsg).getBytes();
            byte[] data = new byte[1 + bytes.length];
            data[0] = (byte) (BtMsg.CONTROL & 0xFF);
            System.arraycopy(bytes, 0, data, 1, bytes.length);
            mClientService.sendMessage(new BtMsg(data));
            mClientService.deInit();
        } else {
            showToast("not connected");
        }
        unbindService(mConn);
        super.onDestroy();
    }

    private void showToast(String msg) {
        runOnUiThread(() -> Toast.makeText(ClientActivity.this, msg, Toast.LENGTH_SHORT).show());
    }
}

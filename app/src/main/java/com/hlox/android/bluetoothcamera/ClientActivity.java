package com.hlox.android.bluetoothcamera;

import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
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
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case CONNECT:
                    ControlMsg controlMsg = new ControlMsg(true,false,false,false,"");
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
        public void onWriteError(IOException e) {
            Log.e(TAG, "onWriteError: ", e);

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
            mHandler.sendEmptyMessageDelayed(CONNECT,50);
        }

        @Override
        public void onDataReceived(BtMsg msg) {
            Log.d(TAG, "onDataReceived: " + msg.getData()[0]);
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);
        this.device = getIntent().getParcelableExtra(DEVICE);
        ivReceived = findViewById(R.id.iv_received);
        Intent intent = new Intent(this, BtClientService.class);
        bindService(intent, mConn, Context.BIND_AUTO_CREATE);
        findViewById(R.id.btn_switch).setOnClickListener((v)->{
            if (mClientService != null) {
                ControlMsg controlMsg = new ControlMsg(true,true,false,false,"");
                byte[] bytes = GsonUtil.bean2Json(controlMsg).getBytes();
                byte[] data = new byte[1 + bytes.length];
                data[0] = (byte) (BtMsg.CONTROL & 0xFF);
                System.arraycopy(bytes, 0, data, 1, bytes.length);
                mClientService.sendMessage(new BtMsg(data));
            }else {
                showToast("not connected");
            }
        });
        findViewById(R.id.btn_take_photo).setOnClickListener((v) -> {
            if (mClientService != null) {
                ControlMsg controlMsg = new ControlMsg(true,false,true,false,"");
                byte[] bytes = GsonUtil.bean2Json(controlMsg).getBytes();
                byte[] data = new byte[1 + bytes.length];
                data[0] = (byte) (BtMsg.CONTROL & 0xFF);
                System.arraycopy(bytes, 0, data, 1, bytes.length);
                mClientService.sendMessage(new BtMsg(data));
            }else {
                showToast("not connected");
            }
        });
    }


    @Override
    protected void onDestroy() {
        if(mClientService!=null){
            if (mClientService != null) {
                ControlMsg controlMsg = new ControlMsg(false,false,false,false,"");
                byte[] bytes = GsonUtil.bean2Json(controlMsg).getBytes();
                byte[] data = new byte[1 + bytes.length];
                data[0] = (byte) (BtMsg.CONTROL & 0xFF);
                System.arraycopy(bytes, 0, data, 1, bytes.length);
                mClientService.sendMessage(new BtMsg(data));
            }else {
                showToast("not connected");
            }
        }
        unbindService(mConn);
        super.onDestroy();
    }

    private void showToast(String msg) {
        runOnUiThread(() -> Toast.makeText(ClientActivity.this, msg, Toast.LENGTH_SHORT).show());
    }
}

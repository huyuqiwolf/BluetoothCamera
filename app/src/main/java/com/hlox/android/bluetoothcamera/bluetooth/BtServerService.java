package com.hlox.android.bluetoothcamera.bluetooth;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import com.google.gson.Gson;
import com.hlox.android.bluetoothcamera.bean.ControlMsg;
import com.hlox.android.bluetoothcamera.constant.Constant;
import com.hlox.android.bluetoothcamera.event.BtStateMsg;
import com.hlox.android.bluetoothcamera.event.ChangeCameraMsg;
import com.hlox.android.bluetoothcamera.event.PicDataMsg;
import com.hlox.android.bluetoothcamera.event.TakePhotoMsg;
import com.hlox.android.bluetoothcamera.event.TextMsg;
import com.hlox.android.bluetoothcamera.service.PhotoService;
import com.hlox.android.bluetoothcamera.util.AppUtil;
import com.hlox.android.bluetoothcamera.util.GsonUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import androidx.annotation.NonNull;

public class BtServerService extends Service {
    private static final String TAG = "BtServerService";
    private static final int HEART = 0x1000;
    private MyHandler mHandler = new MyHandler();
    private CountDownTimer mTimer = null;
    private volatile long mLastUpdate = 0L;
    private BtCallback mCallback = new BtCallback() {

        @Override
        public void onWaiting() {
            Log.d(TAG, "onWaiting: ");
            EventBus.getDefault().post(new BtStateMsg(false, "waiting for connect"));
        }

        @Override
        public void onWriteError(IOException e) {
            Log.d(TAG, "onWriteError: ");
            EventBus.getDefault().post(new BtStateMsg(false, e.getMessage()));
            init();
        }

        @Override
        public void onConnectFailed() {
            Log.d(TAG, "onConnectFailed: ");
            EventBus.getDefault().post(new BtStateMsg(false, "connect failed"));
            init();
        }

        @Override
        public void onConnected(String name) {
            Log.d(TAG, "onConnected: " + name);
            EventBus.getDefault().post(new BtStateMsg(true, "connected " + name));
            sendHeartMsg();

        }

        @Override
        public void onDataReceived(BtMsg msg) {
            Log.d(TAG, "onDataReceived: ");
            switch (msg.getData()[0]) {
                case BtMsg.HEAR:
                    Log.d(TAG, "onDataReceived: HEART");
                    mLastUpdate = System.currentTimeMillis();
                    break;
                case BtMsg.CONTROL:
                    processControl(msg.getData());
                    break;
                case BtMsg.PREVIEW:
                    break;
                case BtMsg.EXTRA:
                    break;
                default:
            }
        }

        @Override
        public void onReadError(IOException e) {
            Log.e(TAG, "onReadError: ", e);
            EventBus.getDefault().post(new BtStateMsg(false, e.getMessage()));
            init();
        }

        @Override
        public void onWriteDone() {
            Log.d(TAG, "onWriteDone: ");
        }

        @Override
        public void onWaitError(IOException e) {
            Log.e(TAG, "onWaitError: ", e);
            EventBus.getDefault().post(new BtStateMsg(false, "wait error"));
            init();
        }
    };

    private void processControl(byte[] data) {
        ControlMsg msg = GsonUtil.parseJson(new String(data, 1, data.length - 1), ControlMsg.class);
        Log.d(TAG, "processControl: " + msg);
        if (msg.isConnect()) {
            // 判断是否已经存在了
            if (!AppUtil.isServiceRunning(this, PhotoService.class.getName())) {
                startService(new Intent(this, PhotoService.class));
                return;
            }
            if (msg.isTakePhoto()) {
                EventBus.getDefault().post(new TakePhotoMsg());
                return;
            }
            if (msg.isChangeCamera()) {
                EventBus.getDefault().post(new ChangeCameraMsg());
                return;
            }
        } else {
            stopService(new Intent(this, PhotoService.class));
            init();
        }
    }

    private ConnectedThread mConnectedThread;
    private AcceptThread mAcceptThread;

    @Override
    public void onCreate() {
        super.onCreate();
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
        init();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    /**
     * 预览数据发送到这里，之后传给客户端
     */
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onPreview(PicDataMsg picDataMsg) {
        if (mConnectedThread != null) {
            byte[] data = new byte[picDataMsg.getData().length + 1];
            data[0] = (byte) (BtMsg.PREVIEW & 0xFF);
            System.arraycopy(picDataMsg.getData(), 0, data, 1, picDataMsg.getData().length);
            mConnectedThread.write(new BtMsg(data));
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onTextMsg(TextMsg textMsg) {
        if (mConnectedThread != null) {
            byte[] bytes = textMsg.getMsg().getBytes();
            byte[] data = new byte[bytes.length + 1];
            data[0] = (byte) (BtMsg.TEXT & 0xFF);
            System.arraycopy(bytes, 0, data, 1, bytes.length);
            mConnectedThread.write(new BtMsg(data));
        }
    }

    @Override
    public void onDestroy() {
        close();
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }
        super.onDestroy();
    }

    private void close() {
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        if (mAcceptThread != null) {
            mAcceptThread.cancel();
            mAcceptThread = null;
        }
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
    }

    private void init() {
        close();
        mAcceptThread = new AcceptThread();
        mAcceptThread.start();
    }

    private void manageConnection(BluetoothSocket socket) {
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();
        initTimer();
    }

    private void checkAlive() {
        if (System.currentTimeMillis() - mLastUpdate >= 3000) {
            init();
        } else {
            initTimer();
        }
    }

    private void initTimer() {
        if(mTimer!=null){
            mTimer.cancel();
        }
        mTimer = new CountDownTimer(3000L, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                Log.d(TAG, "onTick: ");
                if (mConnectedThread != null) {
                    sendHeartMsg();
                }
            }

            @Override
            public void onFinish() {
                checkAlive();
            }
        };
        mTimer.start();
    }

    private void sendHeartMsg() {
        if (mConnectedThread != null) {
            byte[] heartData = "HeartFromServer".getBytes();
            byte[] data = new byte[heartData.length + 1];
            data[0] = BtMsg.HEAR;
            System.arraycopy(heartData, 0, data, 1, heartData.length);
            BtMsg btMsg = new BtMsg(data);
            mConnectedThread.write(btMsg);
        }
    }

    private class MyHandler extends Handler {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
        }
    }

    private class AcceptThread extends Thread {
        private BluetoothServerSocket mServerSocket;
        private volatile boolean isWaiting = false;

        public AcceptThread() {
            setName("AcceptThread");
            BluetoothServerSocket temp = null;
            try {
                temp = BluetoothAdapter.getDefaultAdapter().listenUsingRfcommWithServiceRecord(Constant.SERVER_NAME, Constant.BLUETOOTH_UUID);
                isWaiting = true;
            } catch (IOException e) {
                e.printStackTrace();
                isWaiting = false;
            }
            mServerSocket = temp;
        }

        @Override
        public void run() {
            mCallback.onWaiting();
            BluetoothSocket socket = null;
            while (isWaiting) {
                try {
                    socket = mServerSocket.accept();
                } catch (IOException e) {
                    e.printStackTrace();
                    mCallback.onWaitError(e);
                    cancel();
                    break;
                }
                if (socket != null) {
                    manageConnection(socket);
                    isWaiting = false;
                    cancel();
                }
            }
        }

        public void cancel() {
            isWaiting = false;
            if (mServerSocket != null) {
                try {
                    mServerSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                mServerSocket = null;
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
            if (isRead) {
                mCallback.onConnected(mSocket.getRemoteDevice().getName());
            } else {
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
                    cancel();
                }
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}

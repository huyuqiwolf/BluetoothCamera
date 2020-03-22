package com.hlox.android.bluetoothcamera;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import com.hlox.android.bluetoothcamera.bluetooth.BtServerService;
import com.hlox.android.bluetoothcamera.event.BtStateMsg;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 *
 */
public class ServerActivity extends AppCompatActivity {
    private static final String TAG = "ServerActivity";
    private Switch mSwitch;
    private TextView mInfoView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);
        mSwitch = findViewById(R.id.switch1);
        mInfoView = findViewById(R.id.textView);
        mSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Intent intent = new Intent(ServerActivity.this, BtServerService.class);
                if (isChecked) {
                    startService(intent);
                } else {
                    stopService(intent);
                }
            }
        });
        Log.d(TAG, "onCreate: ");
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
    }
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(BtStateMsg msg){
        mInfoView.setText(msg.toString());
    }


    @Override
    protected void onDestroy() {
        if(EventBus.getDefault().isRegistered(this)){
            EventBus.getDefault().unregister(this);
        }

        Log.d(TAG, "onDestroy: ");
        super.onDestroy();
    }
}

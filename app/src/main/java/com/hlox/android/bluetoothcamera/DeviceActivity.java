package com.hlox.android.bluetoothcamera;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.hlox.android.bluetoothcamera.adapter.DeviceAdapter;
import java.util.ArrayList;
import java.util.List;

public class DeviceActivity extends AppCompatActivity implements DeviceAdapter.OnItemClickListener {
  private static final int ENABLE_DISCOVERABLE = 0x1001;
  private static final int ENABLE_BLUETOOTH = 0x1000;
  private static final String TAG = "FindDeviceActivity";
  private RecyclerView recyclerView;
  private BluetoothAdapter bluetoothAdapter;
  private BluetoothManager bluetoothManager;
  private TextView tvTitle;

  private List<BluetoothDevice> deviceList = new ArrayList<>();
  private DeviceAdapter adapter;

  private Handler handler = new Handler() {
    @Override
    public void handleMessage(@NonNull Message msg) {
      super.handleMessage(msg);
    }
  };

  private BroadcastReceiver receiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      String action = intent.getAction();
      if (BluetoothDevice.ACTION_FOUND.equals(action)) {
        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        if (!deviceList.contains(device)) {
          deviceList.add(device);
          adapter.notifyDataSetChanged();
          Log.d(TAG, "onReceive: " + device);
        }
      }
    }
  };

  private IntentFilter filter;

  @SuppressLint("MissingPermission") @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_device);
    recyclerView = findViewById(R.id.recycler_view);
    tvTitle = findViewById(R.id.tv_title);
    bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
    if (bluetoothManager == null) {
      Toast.makeText(this, "设备不支持蓝牙", Toast.LENGTH_SHORT).show();
      finish();
      return;
    }
    bluetoothAdapter = bluetoothManager.getAdapter();
    if (bluetoothAdapter == null) {
      Toast.makeText(this, "设备不支持蓝牙", Toast.LENGTH_SHORT).show();
      finish();
      return;
    }

    tvTitle.setOnClickListener(view -> {
      if (bluetoothAdapter != null && bluetoothAdapter.isDiscovering()) {
        bluetoothAdapter.cancelDiscovery();
      }
    });
    filter = new IntentFilter();
    filter.addAction(BluetoothDevice.ACTION_FOUND);
    registerReceiver(receiver, filter);
    adapter = new DeviceAdapter(deviceList);
    adapter.setOnItemClickListener(this);
    recyclerView.setLayoutManager(new LinearLayoutManager(this));
    recyclerView.setAdapter(adapter);
    if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled()) {
      startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), ENABLE_BLUETOOTH);
    } else {
      startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE),
          ENABLE_DISCOVERABLE);
    }
  }

  @Override
  protected void onDestroy() {
    unregisterReceiver(receiver);
    super.onDestroy();
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    if (requestCode == ENABLE_BLUETOOTH) {
      if (resultCode != RESULT_OK) {
        Toast.makeText(this, "请打开蓝牙", Toast.LENGTH_SHORT).show();
        return;
      }
      startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE),
          ENABLE_DISCOVERABLE);
    } else if (requestCode == ENABLE_DISCOVERABLE) {
      scanBluetoothDevice();
    } else {
      super.onActivityResult(requestCode, resultCode, data);
    }
  }

  @SuppressLint("MissingPermission") private void scanBluetoothDevice() {
    if (bluetoothAdapter.isDiscovering()) {
      bluetoothAdapter.cancelDiscovery();
    }
    deviceList.clear();
    deviceList.addAll(bluetoothAdapter.getBondedDevices());
    adapter.notifyDataSetChanged();
    bluetoothAdapter.startDiscovery();
    Toast.makeText(this, "start", Toast.LENGTH_SHORT).show();
    handler.postDelayed(() -> {
      bluetoothAdapter.cancelDiscovery();
      Toast.makeText(this, "finish", Toast.LENGTH_SHORT).show();
      Log.d(TAG, "scanBluetoothDevice: finish");
    }, 10000);
  }
    @SuppressLint("MissingPermission")
  @Override
  public void onItemClick(View view, int position) {
    Log.d(TAG, "onItemClick: " + position);
    if (bluetoothAdapter != null) {
      bluetoothAdapter.cancelDiscovery();
    }
    handler.removeCallbacksAndMessages(null);
    BluetoothDevice device = deviceList.get(position);
    if (device.getBondState() == BluetoothDevice.BOND_NONE) {
      device.createBond();
    } else if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
      Intent intent = new Intent(this, ClientActivity.class);
      intent.putExtra(ClientActivity.DEVICE, device);
      startActivity(intent);
    }
  }
}

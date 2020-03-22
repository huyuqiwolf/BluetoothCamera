package com.hlox.android.bluetoothcamera.adapter;

import android.bluetooth.BluetoothDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.hlox.android.bluetoothcamera.R;
import java.util.List;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder> {


    private List<BluetoothDevice> deviceList;
    private OnItemClickListener onItemClickListener;

    public DeviceAdapter(List<BluetoothDevice> deviceList) {
        this.deviceList = deviceList;
    }

    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        DeviceViewHolder holder = new DeviceViewHolder(LayoutInflater.from(parent.getContext()).inflate(
            R.layout.device_item, parent, false));
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        BluetoothDevice device = deviceList.get(position);
        holder.tvName.setText(device.getName());
        holder.tvMac.setText(device.getAddress());
        String state = "";
        switch (device.getBondState()) {
            case BluetoothDevice.BOND_BONDED:
                state = "已绑定";
                break;
            case BluetoothDevice.BOND_BONDING:
                state = "绑定中";
                break;
            case BluetoothDevice.BOND_NONE:
                state = "未绑定";
                break;
            default:
                break;
        }
        holder.tvState.setText(state);
        if(onItemClickListener != null){
            holder.itemView.setOnClickListener(view -> {
                onItemClickListener.onItemClick(holder.itemView,position);
            });
        }
    }

    @Override
    public int getItemCount() {
        return deviceList == null ? 0 : deviceList.size();
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public class DeviceViewHolder extends RecyclerView.ViewHolder {
        private TextView tvName;
        private TextView tvMac;
        private TextView tvState;

        public DeviceViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_name);
            tvMac = itemView.findViewById(R.id.tv_mac);
            tvState = itemView.findViewById(R.id.tv_state);
        }
    }

    public interface OnItemClickListener {
        void onItemClick(View view, int position);
    }
}

package com.hlox.android.bluetoothcamera;

import android.net.Uri;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

import pub.devrel.easypermissions.EasyPermissions;
import pub.devrel.easypermissions.PermissionRequest;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks {
    private static final String TAG = "MainActivity";
    private static final String[] PERMISSIONS = new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(!Settings.canDrawOverlays(this)){
            startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:"+getPackageName())));
        }
    }

    public void server(View view) {
        if (EasyPermissions.hasPermissions(this, PERMISSIONS)) {
            startActivity(new Intent(this, ServerActivity.class));
            return;
        }
        EasyPermissions.requestPermissions(
                new PermissionRequest.Builder(this, 0x1000, PERMISSIONS[0], PERMISSIONS[1],
                         PERMISSIONS[2]).build());
    }

    public void client(View view) {
        if (EasyPermissions.hasPermissions(this, PERMISSIONS)) {
            startActivity(new Intent(this, DeviceActivity.class));
            return;
        }
        EasyPermissions.requestPermissions(
                new PermissionRequest.Builder(this, 0x1000, PERMISSIONS[0], PERMISSIONS[1],
                        PERMISSIONS[3]).build());
    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {
        Log.d(TAG, "onPermissionsGranted: "+perms);
    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
        Log.d(TAG, "onPermissionsDenied: " + perms);
        finish();
    }
}

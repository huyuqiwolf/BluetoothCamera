package com.hlox.android.bluetoothcamera.util;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;

public class AppUtil {
    public static boolean isServiceRunning(Context context,String name){
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo serviceInfo : manager.getRunningServices(Integer.MAX_VALUE)) {
            if(name.equals(serviceInfo.service.getClassName())){
                return true;
            }
        }
        return false;
    }
}

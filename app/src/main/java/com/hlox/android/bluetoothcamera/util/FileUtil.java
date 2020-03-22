package com.hlox.android.bluetoothcamera.util;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;


public class FileUtil {
    public static String saveJpeg(Context context, byte[] data) {
        File file = context.getExternalFilesDir("JPG");
        OutputStream os = null;
        try{
            File jpg = new File(file,System.currentTimeMillis()+".jpg");
            os = new FileOutputStream(jpg);
            os.write(data);
            os.flush();
            return jpg.getAbsolutePath();
        }catch (IOException e){
            e.printStackTrace();
        }
        finally {
            if(os!=null){
                try {
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }
}

package com.hlox.android.bluetoothcamera.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class GsonUtil {
    private static Gson gson;

    private static void init() {
        gson = new GsonBuilder().create();
    }

    public static <T> T parseJson(String json, Class<T> clazz) {
        if (gson == null) {
            init();
        }
        return gson.fromJson(json, clazz);
    }

    public static String bean2Json(Object obj) {
        if (gson == null) {
            init();
        }
        return gson.toJson(obj);
    }

}

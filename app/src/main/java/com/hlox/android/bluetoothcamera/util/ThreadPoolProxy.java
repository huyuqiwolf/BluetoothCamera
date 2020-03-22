package com.hlox.android.bluetoothcamera.util;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadPoolProxy {
    private static ExecutorService executor;

    private ThreadPoolProxy() {
    }

    private static void init() {
        if (executor == null) {
            executor = new ThreadPoolExecutor(4, 8, 5,
                    TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(8));
        }
    }

    public static ExecutorService getExecutor() {
        if (executor == null) {
            init();
        }
        return executor;
    }
}

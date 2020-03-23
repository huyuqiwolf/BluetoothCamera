package com.hlox.android.bluetoothcamera.service;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.Surface;
import android.view.TextureView;
import android.view.WindowManager;

import com.hlox.android.bluetoothcamera.bean.ControlMsg;
import com.hlox.android.bluetoothcamera.event.ChangeCameraMsg;
import com.hlox.android.bluetoothcamera.event.PicDataMsg;
import com.hlox.android.bluetoothcamera.event.TakePhotoMsg;
import com.hlox.android.bluetoothcamera.event.TextMsg;
import com.hlox.android.bluetoothcamera.util.FileUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;

/**
 * @author huyuqi
 */
public class PhotoService extends Service {
    private static final String TAG = "PhotoService";
    private static final int WIDTH = 640;
    private static final int HEIGHT = 480;

    private WindowManager mWindowManager;
    private WindowManager.LayoutParams mLayoutParams;
    private TextureView mTextureView;
    private CameraManager mCameraManager;
    private String mCameraId;
    private String mFrontCameraId;
    private String mBackCameraId;
    private Matrix matrix;
    private ByteArrayOutputStream baos;
    private TextureView.SurfaceTextureListener mTextureListener =
            new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                    Log.d(TAG, "onSurfaceTextureAvailable: ");
                    openCamera();
                }

                @Override
                public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                    Log.d(TAG, "onSurfaceTextureSizeChanged: ");
                }

                @Override
                public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                    Log.d(TAG, "onSurfaceTextureDestroyed: ");
                    return false;
                }

                @Override
                public void onSurfaceTextureUpdated(SurfaceTexture surface) {

                    Bitmap bitmap = mTextureView.getBitmap(WIDTH, HEIGHT);
                    bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                    bitmap.compress(Bitmap.CompressFormat.WEBP, 20, baos);
                    byte[] data = baos.toByteArray();
                    baos.reset();
                    EventBus.getDefault().post(new PicDataMsg(data));
                }
            };

    private Handler mCameraHandler;
    private HandlerThread mCameraThread;
    private ImageReader mImageReader;
    private ImageReader.OnImageAvailableListener mImageListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Log.d(TAG, "onImageAvailable: ");
            Image image = reader.acquireNextImage();
            if(image!=null){
                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                byte[] data= new byte[buffer.remaining()];
                buffer.get(data);
                String file = FileUtil.saveJpeg(PhotoService.this,data);
                image.close();
                EventBus.getDefault().post(new TextMsg(file+" saved"));
            }
        }
    };
    private CameraDevice mCameraDevice;
    private CaptureRequest.Builder mPreviewBuilder;
    private CameraCaptureSession mSession;
    private CameraDevice.StateCallback mDeviceCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            Log.d(TAG, "onOpened: ");
            mCameraDevice = camera;
            startPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            Log.d(TAG, "onDisconnected: ");
            camera.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            Log.d(TAG, "onError: ");
            camera.close();
            mCameraDevice = null;
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        if(!EventBus.getDefault().isRegistered(this)){
            EventBus.getDefault().register(this);
        }
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mTextureView = new TextureView(this);
        mLayoutParams = new WindowManager.LayoutParams();
        mLayoutParams.gravity = Gravity.LEFT;
        mLayoutParams.width = 320;
        mLayoutParams.height = 240;
        mLayoutParams.format = PixelFormat.RGBA_8888;
        mLayoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        mLayoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;

        mWindowManager.addView(mTextureView, mLayoutParams);
        mCameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
        mFrontCameraId = String.valueOf(CameraCharacteristics.LENS_FACING_BACK);
        mBackCameraId = String.valueOf(CameraCharacteristics.LENS_FACING_FRONT);
        mCameraId = mBackCameraId;
        startThread();
        matrix = new Matrix();
        matrix.setScale(0.2f, 0.2f);
        baos = new ByteArrayOutputStream();
        mTextureView.setSurfaceTextureListener(mTextureListener);
    }

    @Override
    public void onDestroy() {
        if (baos != null) {
            try {
                baos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            baos = null;
        }
        closeCamera();
        if (mWindowManager != null) {
            mWindowManager.removeViewImmediate(mTextureView);
        }
        stopThread();
        if(EventBus.getDefault().isRegistered(this)){
            EventBus.getDefault().unregister(this);
        }
        super.onDestroy();
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onChangeCamera(ChangeCameraMsg changeCameraMsg) {
        closeCamera();
        if (mCameraId.equals(mFrontCameraId)) {
            mCameraId = mBackCameraId;
        } else if (mCameraId.equals(mBackCameraId)) {
            mCameraId = mFrontCameraId;
        }
        openCamera();
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onTakePhoto(TakePhotoMsg takePhotoMsg) {
        try {
            CaptureRequest.Builder builder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);

            builder.addTarget(mImageReader.getSurface());
            SurfaceTexture surfaceTexture = mTextureView.getSurfaceTexture();
            surfaceTexture.setDefaultBufferSize(WIDTH,HEIGHT);
            Surface surface = new Surface(surfaceTexture);
            builder.addTarget(surface);

            mSession.capture(builder.build(), new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    rePreview();
                }
            },mCameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void rePreview(){
        try {
            mSession.setRepeatingRequest(mPreviewBuilder.build(),null,mCameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    @SuppressLint("MissingPermission")
    private void openCamera() {
        try {
            mCameraManager.openCamera(mCameraId, mDeviceCallback, mCameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "openCamera: ");
    }

    private void startPreview() {
        Log.d(TAG, "startPreview: ");
        if (mCameraDevice != null) {
            try {
                mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                SurfaceTexture surfaceTexture = mTextureView.getSurfaceTexture();
                surfaceTexture.setDefaultBufferSize(WIDTH, HEIGHT);
                Surface surface = new Surface(surfaceTexture);
                mPreviewBuilder.addTarget(surface);
                List<Surface> list = new ArrayList<>();
                list.add(surface);
                list.add(mImageReader.getSurface());
                mCameraDevice.createCaptureSession(list, new CameraCaptureSession.StateCallback() {
                    @Override
                    public void onConfigured(@NonNull CameraCaptureSession session) {
                        Log.d(TAG, "onConfigured: ");
                        mSession = session;
                        try {
                            mSession.setRepeatingRequest(mPreviewBuilder.build(), null, mCameraHandler);
                        } catch (CameraAccessException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                        Log.d(TAG, "onConfigureFailed: ");
                    }
                }, mCameraHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }

    private void closeCamera() {
        if (mSession != null) {
            mSession.close();
            mSession = null;
        }
        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
    }

    private void startThread() {
        Log.d(TAG, "startThread: ");
        mCameraThread = new HandlerThread("CAMERA");
        mCameraThread.start();
        mCameraHandler = new Handler(mCameraThread.getLooper());
        mImageReader = ImageReader.newInstance(WIDTH, HEIGHT, ImageFormat.JPEG, 3);
        mImageReader.setOnImageAvailableListener(mImageListener, mCameraHandler);
    }

    private void stopThread() {
        Log.d(TAG, "stopThread: ");
        if (mImageReader != null) {
            mImageReader.close();
            mImageReader = null;
        }
        if (mCameraHandler != null) {
            mCameraHandler.removeCallbacksAndMessages(null);
        }
        if (mCameraThread != null) {
            mCameraThread.quitSafely();
            try {
                mCameraThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mCameraThread = null;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}

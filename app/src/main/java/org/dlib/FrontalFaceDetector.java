package org.dlib;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class FrontalFaceDetector {

    static {
        System.loadLibrary("native-lib");
    }

    private File landMarksFile = new File("sdcard/shape_predictor_68_face_landmarks.dat");

    public boolean isLandmarksLoaded() {
        return landMarksFile.exists();
    }

    public void loadLandmarks(final Context context, final Callbacks callbaks) {
        final Handler handler = new Handler(Looper.getMainLooper());
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    InputStream is = context.getAssets().open("shape_predictor_68_face_landmarks.dat");
                    int size = is.available();
                    byte[] buffer = new byte[size];
                    is.read(buffer);
                    is.close();
                    FileOutputStream fos = new FileOutputStream(landMarksFile);
                    fos.write(buffer);
                    fos.close();
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            callbaks.onSuccess();
                        }
                    });
                } catch (final IOException e) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            callbaks.onError(e);
                        }
                    });
                }
            }
        }).start();
    }

    public void initDetector() {
        initFrontalFaceDetector(landMarksFile.getAbsolutePath());
    }

    private native void initFrontalFaceDetector(String path);

    public native int[][][] detectLandmarksFromFace(Bitmap bitmap);

    public native int[][][] detectLandmarksFromFaceRect(Bitmap bitmap, int left, int top, int right, int bottom);

    public native int[][][] detectLandmarksFromFace(byte[] data);

    public interface Callbacks {
        void onSuccess();

        void onError(Throwable throwable);
    }

}

package com.google.android.gms.samples.vision.face.facetracker;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.util.Log;
import android.util.SparseArray;

import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.Landmark;
import com.google.android.gms.vision.face.internal.client.FaceSettingsParcel;
import com.google.android.gms.vision.face.internal.client.zzf;
import com.google.android.gms.vision.internal.client.FrameMetadataParcel;
import com.google.android.gms.vision.zza;

import org.dlib.FrontalFaceDetector;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.HashSet;

public class CustomFaceDetector extends Detector<Face> {

    public static final int NO_LANDMARKS = 0;
    public static final int ALL_LANDMARKS = 1;
    public static final int NO_CLASSIFICATIONS = 0;
    public static final int ALL_CLASSIFICATIONS = 1;
    public static final int FAST_MODE = 0;
    public static final int ACCURATE_MODE = 1;
    private final zza aEK;
    private final zzf aEL;
    private final Object zzail;
    FrontalFaceDetector frontalFaceDetector;
    private boolean aEM;
    private volatile boolean isDatLoaded;

    private CustomFaceDetector() {
        this.aEK = new zza();
        this.zzail = new Object();
        this.aEM = true;
        throw new IllegalStateException("Default constructor called");
    }

    private CustomFaceDetector(zzf var1, Context context) {
        this.aEK = new zza();
        this.zzail = new Object();
        this.aEM = true;
        this.aEL = var1;
        frontalFaceDetector = new FrontalFaceDetector();
        if (!frontalFaceDetector.isLandmarksLoaded()) {
            frontalFaceDetector.loadLandmarks(context, new FrontalFaceDetector.Callbacks() {
                @Override
                public void onSuccess() {
                    frontalFaceDetector.initDetector();
                    isDatLoaded = true;
                }

                @Override
                public void onError(Throwable throwable) {

                }
            });
        } else {
            frontalFaceDetector.initDetector();
            isDatLoaded = true;
        }
    }

    public void release() {
        super.release();
        Object var1 = this.zzail;
        synchronized (this.zzail) {
            if (this.aEM) {
                this.aEL.zzchv();
                this.aEM = false;
            }
        }
    }

    protected void finalize() throws Throwable {
        try {
            Object var1 = this.zzail;
            synchronized (this.zzail) {
                if (this.aEM) {
                    Log.w("FaceDetector", "FaceDetector was not released with FaceDetector.release()");
                    this.release();
                }
            }
        } finally {
            super.finalize();
        }

    }

    public SparseArray<Face> detect(Frame var1) {
        if (var1 == null) {
            throw new IllegalArgumentException("No frame supplied.");
        } else {
            ByteBuffer var2 = var1.getGrayscaleImageData();
            Object var4 = this.zzail;
            Face[] var3;
            Bitmap bitmap = null;
            synchronized (this.zzail) {
                if (!this.aEM) {
                    throw new RuntimeException("Cannot use detector after release()");
                }

                var3 = this.aEL.zzb(var2, FrameMetadataParcel.zzc(var1));


                int width = var1.getMetadata().getWidth();
                int height = var1.getMetadata().getHeight();

                YuvImage yuvImage = new YuvImage(var1.getGrayscaleImageData().array(), ImageFormat.NV21, width, height, null);
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                yuvImage.compressToJpeg(new Rect(0, 0, var1.getMetadata().getWidth(), var1.getMetadata().getHeight()), 100, byteArrayOutputStream);
                byte[] jpegArray = byteArrayOutputStream.toByteArray();
                bitmap = BitmapFactory.decodeByteArray(jpegArray, 0, jpegArray.length);
            }

            int var14 = 0;
            HashSet var5 = new HashSet();
            SparseArray var6 = new SparseArray(var3.length);
            Face[] var7 = var3;
            int var8 = var3.length;

            for (int var9 = 0; var9 < var8; ++var9) {
                Face var10 = var7[var9];
                int var11 = var10.getId();
                var14 = Math.max(var14, var11);
                if (var5.contains(Integer.valueOf(var11))) {
                    ++var14;
                    var11 = var14;
                }


                var5.add(Integer.valueOf(var11));
                int var12 = this.aEK.zzzw(var11);
                PointF position = var10.getPosition();
                float width = var10.getWidth();
                float height = var10.getHeight();
                if (bitmap != null) {
                    if (isDatLoaded) {
                        final int[][][] faces = frontalFaceDetector.detectLandmarksFromFaceRect(bitmap,
                                (int) position.x, (int) position.y, (int) (position.x + width), (int) (position.y + height));
                        if (faces.length > 0) {
                            int[][] face = faces[0];
                            int i = 0;
                            Landmark[] landmarks = new Landmark[face.length];
                            for (int[] point : face) {
                                landmarks[i] = new Landmark(new PointF((float) point[0], (float) point[1]), i);
                                i++;
                            }

                            Face f = new Face(var10.getId(), new PointF(position.x + width/ 2.0F, position.y + height / 2.0F),
                                    width, height, var10.getEulerY(), var10.getEulerZ(), landmarks,
                                    var10.getIsLeftEyeOpenProbability(),
                                    var10.getIsRightEyeOpenProbability(),
                                    var10.getIsSmilingProbability());
                            //var6.append(var12,);

                            var6.append(var12, f);
                        }
                    }
                }
            }
            return var6;
        }
    }

    public boolean setFocus(int var1) {
        int var2 = this.aEK.zzzx(var1);
        Object var3 = this.zzail;
        synchronized (this.zzail) {
            if (!this.aEM) {
                throw new RuntimeException("Cannot use detector after release()");
            } else {
                return this.aEL.zzaao(var2);
            }
        }
    }

    public boolean isOperational() {
        return this.aEL.isOperational();
    }

    public static class Builder {
        private final Context mContext;
        private int aEN = 0;
        private boolean aEO = false;
        private int aEP = 0;
        private boolean aEQ = true;
        private int Dr = 0;
        private float aER = -1.0F;

        public Builder(Context var1) {
            this.mContext = var1;
        }

        public CustomFaceDetector.Builder setLandmarkType(int var1) {
            if (var1 != 0 && var1 != 1) {
                throw new IllegalArgumentException((new StringBuilder(34)).append("Invalid landmark type: ").append(var1).toString());
            } else {
                this.aEN = var1;
                return this;
            }
        }

        public CustomFaceDetector.Builder setProminentFaceOnly(boolean var1) {
            this.aEO = var1;
            return this;
        }

        public CustomFaceDetector.Builder setClassificationType(int var1) {
            if (var1 != 0 && var1 != 1) {
                throw new IllegalArgumentException((new StringBuilder(40)).append("Invalid classification type: ").append(var1).toString());
            } else {
                this.aEP = var1;
                return this;
            }
        }

        public CustomFaceDetector.Builder setTrackingEnabled(boolean var1) {
            this.aEQ = var1;
            return this;
        }

        public CustomFaceDetector.Builder setMode(int var1) {
            switch (var1) {
                case 0:
                case 1:
                    this.Dr = var1;
                    return this;
                default:
                    throw new IllegalArgumentException((new StringBuilder(25)).append("Invalid mode: ").append(var1).toString());
            }
        }

        public CustomFaceDetector.Builder setMinFaceSize(float var1) {
            if (var1 >= 0.0F && var1 <= 1.0F) {
                this.aER = var1;
                return this;
            } else {
                throw new IllegalArgumentException((new StringBuilder(47)).append("Invalid proportional face size: ").append(var1).toString());
            }
        }

        public CustomFaceDetector build(Context context) {
            FaceSettingsParcel var1 = new FaceSettingsParcel();
            var1.mode = this.Dr;
            var1.aEZ = this.aEN;
            var1.aFa = this.aEP;
            var1.aFb = this.aEO;
            var1.aFc = this.aEQ;
            var1.aFd = this.aER;
            zzf var2 = new zzf(this.mContext, var1);
            return new CustomFaceDetector(var2, context);
        }
    }
}

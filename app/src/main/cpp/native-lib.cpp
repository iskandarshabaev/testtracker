#include <jni.h>
#include <string>
#include <dlib/image_processing/frontal_face_detector.h>
#include <dlib/image_processing.h>
#include <android/bitmap.h>
#include <opencv2/core/core.hpp>
#include <opencv2/highgui/highgui.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/opencv.hpp>

using namespace dlib;
using namespace std;

#define JNI_METHOD(NAME) \
    Java_org_dlib_FrontalFaceDetector_##NAME


void convertBitmapToArray2d(JNIEnv *env,
                            jobject bitmap,
                            array2d<rgb_pixel> &out) {
    AndroidBitmapInfo bitmapInfo;
    void *pixels;
    int state;
    if (0 > (state = AndroidBitmap_getInfo(env, bitmap, &bitmapInfo))) {
        return;
    } else if (bitmapInfo.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
    }
    if (0 > (state = AndroidBitmap_lockPixels(env, bitmap, &pixels))) {
        return;
    }
    out.set_size((long) bitmapInfo.height, (long) bitmapInfo.width);
    char *line = (char *) pixels;
    for (int h = 0; h < bitmapInfo.height; ++h) {
        for (int w = 0; w < bitmapInfo.width; ++w) {
            uint32_t *color = (uint32_t *) (line + 4 * w);
            out[h][w].red = (unsigned char) (0xFF & ((*color) >> 24));
            out[h][w].green = (unsigned char) (0xFF & ((*color) >> 16));
            out[h][w].blue = (unsigned char) (0xFF & ((*color) >> 8));
        }
        line = line + bitmapInfo.stride;
    }
    AndroidBitmap_unlockPixels(env, bitmap);
}

void resize_image_with_ratio(array2d<rgb_pixel> &in, array2d<rgb_pixel> &out,
                             int max_width, int max_height) {
    float ratio = 0;
    int width = in.nc();
    int height = in.nr();

    int s_witdh = max_width;
    int s_height = max_height;

    if (width > max_width) {
        ratio = (float) max_width / (float) width;
        s_witdh = (int) (width * ratio);
        s_height = (int) (height * ratio);
    }
    if (height > max_height) {
        ratio = (float) max_height / (float) height;
        s_witdh = (int) (width * ratio);
        s_height = (int) (height * ratio);
    }
    out.set_size(s_height, s_witdh);
    resize_image(in, out);
}

frontal_face_detector sFaceDetector;
shape_predictor sp;

extern "C"
JNIEXPORT jobject JNICALL
JNI_METHOD(findLandmarks)(JNIEnv *env, jobject obj) {
    array2d<rgb_pixel> img;
    pyramid_up(img);
    jclass cls = env->FindClass("org/dlib/FullObjectDetection");
    jmethodID cid = env->GetMethodID(cls, "<init>", "()V");
    return env->NewObject(cls, cid);
}

extern "C"
JNIEXPORT void JNICALL
JNI_METHOD(initFrontalFaceDetector)(JNIEnv *env, jobject obj, jstring path) {
    sFaceDetector = get_frontal_face_detector();
    const char *nativeString = env->GetStringUTFChars(path, 0);
    deserialize(nativeString) >> sp;
    env->ReleaseStringUTFChars(path, nativeString);
}

std::vector<rectangle> dets;
array2d<rgb_pixel> resizedImg;
float hK = 0;
float wK = 0;

extern "C"
JNIEXPORT jobjectArray JNICALL
JNI_METHOD(detectLandmarksFromFace)(JNIEnv *env, jobject obj, jobject bitmap) {
    try {
        array2d<rgb_pixel> img;
        convertBitmapToArray2d(env, bitmap, img);
        //if (dets.size() == 0) {
            resize_image_with_ratio(img, resizedImg, 160, 160);
            array2d<unsigned char> img_gray;
            assign_image(img_gray, resizedImg);
            //pyramid_up(img);
            dets = sFaceDetector(img_gray);
            hK = (float)img.nr() / (float)resizedImg.nr();
            wK = (float)img.nc() / (float)resizedImg.nc();
        //}
        std::vector<full_object_detection> shapes;
        for (unsigned long j = 0; j < dets.size(); ++j) {
            full_object_detection shape = sp(resizedImg, dets[j]);
            shapes.push_back(shape);
        }
        /*dlib::array<array2d<rgb_pixel> > face_chips;
        extract_image_chips(img, get_face_chip_details(shapes), face_chips);*/

        jclass intArray1DClass = env->FindClass("[I");
        jclass intArray2DClass = env->FindClass("[[I");
        jint sizeX = shapes.size();
        jint sizeY = 68;
        jint sizeZ = 2;
        int po[2];
        jobjectArray array3D = env->NewObjectArray(sizeX, intArray2DClass, NULL);
        for (jint x = 0; x < sizeX; x++) {
            jobjectArray array2D = env->NewObjectArray(sizeY, intArray1DClass, NULL);
            for (jint y = 0; y < sizeY; y++) {
                point p = shapes.at(x).part(y);
                po[0] = (int)(p.x()*wK);
                po[1] = (int)(p.y()*hK);
                jintArray array1D = env->NewIntArray(sizeZ);
                env->SetIntArrayRegion(array1D, 0, sizeZ, po);
                env->SetObjectArrayElement(array2D, y, array1D);
            }
            env->SetObjectArrayElement(array3D, x, array2D);
        }
        return array3D;
    } catch (int a) {

    }
    return NULL;
}

extern "C"
JNIEXPORT jobjectArray JNICALL
JNI_METHOD(detectLandmarksFromFaceRect)(JNIEnv *env, jobject obj, jobject bitmap, jint left, jint top, jint right, jint bottom) {
    try {
        array2d<rgb_pixel> img;
        convertBitmapToArray2d(env, bitmap, img);
        //if (dets.size() == 0) {
        resize_image_with_ratio(img, resizedImg, 160, 160);
        array2d<unsigned char> img_gray;
        assign_image(img_gray, resizedImg);
        //pyramid_up(img);
        //dets = sFaceDetector(img_gray);
        hK = (float)img.nr() / (float)resizedImg.nr();
        wK = (float)img.nc() / (float)resizedImg.nc();
        //}
        rectangle rect = rectangle(left, top, right, bottom);
        std::vector<full_object_detection> shapes;
        full_object_detection shape = sp(resizedImg, rect);
        shapes.push_back(shape);
        /*dlib::array<array2d<rgb_pixel> > face_chips;
        extract_image_chips(img, get_face_chip_details(shapes), face_chips);*/

        jclass intArray1DClass = env->FindClass("[I");
        jclass intArray2DClass = env->FindClass("[[I");
        jint sizeX = shapes.size();
        jint sizeY = 68;
        jint sizeZ = 2;
        int po[2];
        jobjectArray array3D = env->NewObjectArray(sizeX, intArray2DClass, NULL);
        for (jint x = 0; x < sizeX; x++) {
            jobjectArray array2D = env->NewObjectArray(sizeY, intArray1DClass, NULL);
            for (jint y = 0; y < sizeY; y++) {
                point p = shapes.at(x).part(y);
                po[0] = (int)(p.x()*wK);
                po[1] = (int)(p.y()*hK);
                jintArray array1D = env->NewIntArray(sizeZ);
                env->SetIntArrayRegion(array1D, 0, sizeZ, po);
                env->SetObjectArrayElement(array2D, y, array1D);
            }
            env->SetObjectArrayElement(array3D, x, array2D);
        }
        return array3D;
    } catch (int a) {

    }
    return NULL;
}
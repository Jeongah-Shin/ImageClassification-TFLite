#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring JNICALL
Java_com_jeongari_mask_1or_1not_CameraActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

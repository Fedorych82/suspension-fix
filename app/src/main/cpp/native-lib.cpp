#include <jni.h>
#include <android/log.h>
#include <dlfcn.h>

#define TAG "SuspensionFix/Native"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)

#define HAL_PATH "/system/lib64/libqg_hal.so"

// Сигнатуры функций CAN-шины, экспортируемых системной libqg_hal.so.
typedef unsigned int (*canbus_open_t)();
typedef unsigned int (*canbus_control_t)(unsigned int cmd, unsigned char *frame);
typedef unsigned int (*canbus_close_t)();

extern "C"
JNIEXPORT jint JNICALL
Java_com_suspension_fix_CanBus_sendFrame(JNIEnv *env, jclass clazz, jint channel, jbyteArray frame) {
    const int length = env->GetArrayLength(frame);
    unsigned char buffer[10] = {0};
    jbyte *bytes = env->GetByteArrayElements(frame, nullptr);
    if (bytes == nullptr) {
        LOGI("GetByteArrayElements returned null");
        return -1;
    }
    for (int i = 0; i < length && i < 10; i++) {
        buffer[i] = static_cast<unsigned char>(bytes[i]);
        LOGI("frame[%d] = %02x", i, buffer[i]);
    }
    env->ReleaseByteArrayElements(frame, bytes, JNI_ABORT);

    void *handle = dlopen(HAL_PATH, RTLD_LAZY);
    if (!handle) {
        LOGI("dlopen(%s) failed: %s", HAL_PATH, dlerror());
        return -1;
    }

    auto canbus_open = reinterpret_cast<canbus_open_t>(dlsym(handle, "qg_canbus_open"));
    auto canbus_control = reinterpret_cast<canbus_control_t>(dlsym(handle, "qg_canbus_control"));
    auto canbus_close = reinterpret_cast<canbus_close_t>(dlsym(handle, "qg_canbus_close"));
    if (!canbus_open || !canbus_control || !canbus_close) {
        LOGI("dlsym failed: %s", dlerror());
        dlclose(handle);
        return -1;
    }

    LOGI("qg_canbus_open -> %x", canbus_open());
    unsigned int result = canbus_control(static_cast<unsigned int>(channel), buffer);
    LOGI("qg_canbus_control -> %x", result);
    LOGI("qg_canbus_close -> %x", canbus_close());

    dlclose(handle);
    return static_cast<jint>(result);
}

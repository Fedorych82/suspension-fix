#include <jni.h>
#include <android/log.h>
#include <dlfcn.h>
#include <cstdarg>
#include <cstdio>

#define TAG "SuspensionFix/Native"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)

#define HAL_PATH "/system/lib64/libqg_hal.so"

// Коды ошибок загрузки HAL. Должны совпадать с константами в CanBus.java.
#define ERR_LIB (-1001)   // libqg_hal.so не загрузилась (путь / SELinux / namespace)
#define ERR_SYM (-1002)   // в либе нет функций qg_canbus_*

// Последнее детальное сообщение (dlerror при сбое либо коды open/control/close).
// Пишется и читается только на одном фоновом потоке (SuspensionController.BUS),
// поэтому синхронизация не нужна.
static char g_detail[512] = "";

static void set_detail(const char *fmt, ...) {
    va_list ap;
    va_start(ap, fmt);
    vsnprintf(g_detail, sizeof(g_detail), fmt, ap);
    va_end(ap);
    LOGI("%s", g_detail);
}

// Сигнатуры функций CAN-шины, экспортируемых системной libqg_hal.so.
typedef unsigned int (*canbus_open_t)();
typedef unsigned int (*canbus_control_t)(unsigned int cmd, unsigned char *frame);
typedef unsigned int (*canbus_close_t)();

struct CanHal {
    void *handle;
    canbus_open_t open;
    canbus_control_t control;
    canbus_close_t close;
};

// Загружает HAL и резолвит символы. 0 — успех, иначе отрицательный код ошибки.
// При сбое кладёт текст dlerror() в g_detail.
static int load_hal(CanHal *hal) {
    hal->handle = dlopen(HAL_PATH, RTLD_LAZY);
    if (!hal->handle) {
        const char *e = dlerror();
        set_detail("dlopen %s: %s", HAL_PATH, e ? e : "unknown error");
        return ERR_LIB;
    }
    hal->open = reinterpret_cast<canbus_open_t>(dlsym(hal->handle, "qg_canbus_open"));
    hal->control = reinterpret_cast<canbus_control_t>(dlsym(hal->handle, "qg_canbus_control"));
    hal->close = reinterpret_cast<canbus_close_t>(dlsym(hal->handle, "qg_canbus_close"));
    if (!hal->open || !hal->control || !hal->close) {
        const char *e = dlerror();
        set_detail("dlsym qg_canbus_*: %s", e ? e : "symbol not found");
        dlclose(hal->handle);
        return ERR_SYM;
    }
    return 0;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_suspension_fix_CanBus_sendFrame(JNIEnv *env, jclass clazz, jint channel, jbyteArray frame) {
    const int length = env->GetArrayLength(frame);
    unsigned char buffer[10] = {0};
    jbyte *bytes = env->GetByteArrayElements(frame, nullptr);
    if (bytes == nullptr) {
        set_detail("GetByteArrayElements returned null");
        return -1;
    }
    for (int i = 0; i < length && i < 10; i++) {
        buffer[i] = static_cast<unsigned char>(bytes[i]);
    }
    env->ReleaseByteArrayElements(frame, bytes, JNI_ABORT);

    CanHal hal;
    int loaded = load_hal(&hal);
    if (loaded != 0) {
        return loaded;
    }

    unsigned int opened = hal.open();
    unsigned int result = hal.control(static_cast<unsigned int>(channel), buffer);
    unsigned int closed = hal.close();
    set_detail("open=0x%x control=0x%x close=0x%x", opened, result, closed);

    dlclose(hal.handle);
    return static_cast<jint>(result);
}

// Проверка доступности шины: грузит HAL, открывает и закрывает CAN без отправки
// кадра. Возвращает результат qg_canbus_open или код ошибки загрузки.
extern "C"
JNIEXPORT jint JNICALL
Java_com_suspension_fix_CanBus_probe(JNIEnv *env, jclass clazz) {
    CanHal hal;
    int loaded = load_hal(&hal);
    if (loaded != 0) {
        return loaded;
    }

    unsigned int opened = hal.open();
    unsigned int closed = hal.close();
    set_detail("open=0x%x close=0x%x", opened, closed);

    dlclose(hal.handle);
    return static_cast<jint>(opened);
}

// Возвращает последнее детальное сообщение (для вывода в UI).
extern "C"
JNIEXPORT jstring JNICALL
Java_com_suspension_fix_CanBus_lastDetail(JNIEnv *env, jclass clazz) {
    return env->NewStringUTF(g_detail);
}

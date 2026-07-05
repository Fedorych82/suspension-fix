package com.suspension.fix;

import android.util.Log;

/**
 * Мост в CAN-шину автомобиля. Разбирает hex-кадр в байты и отдаёт его
 * нативному слою, который пишет в шину через системную libqg_hal.so.
 */
final class CanBus {
    private static final String TAG = "SuspensionFix/CanBus";

    /** Длина CAN-кадра в байтах, которую ждёт нативный слой. */
    private static final int FRAME_SIZE = 10;

    // Коды ошибок; значения ERR_LIB/ERR_SYM должны совпадать с native-lib.cpp.
    static final int ERR_LIB_UNREACHABLE = -1001; // libqg_hal.so не грузится
    static final int ERR_SYMBOLS_MISSING = -1002; // нет функций qg_canbus_*
    static final int ERR_BAD_FRAME = -1;          // кадр неверной длины

    static {
        System.loadLibrary("suspension");
    }

    private CanBus() {
    }

    private static native int sendFrame(int channel, byte[] frame);

    /**
     * Проверка состояния шины: грузит HAL, открывает и закрывает CAN без
     * отправки кадра. Возвращает код qg_canbus_open или ошибку загрузки.
     */
    static native int probe();

    /**
     * Последнее детальное сообщение нативного слоя: текст dlerror() при сбое
     * загрузки либо коды open/control/close при успехе. Актуально сразу после
     * вызова {@link #send} / {@link #probe} на том же потоке.
     */
    static native String lastDetail();

    /**
     * Разбирает hex-строку вида "69 08 02 .." в кадр и отправляет его в CAN.
     * Возвращает код нативного слоя (см. {@link #describe(int)}).
     */
    static int send(int channel, String hexFrame) {
        byte[] frame = parse(hexFrame);
        if (frame.length != FRAME_SIZE) {
            Log.w(TAG, "Пропускаю кадр неверной длины: " + hexFrame);
            return ERR_BAD_FRAME;
        }
        int result = sendFrame(channel, frame);
        Log.i(TAG, "send [" + hexFrame + "] -> " + describe(result));
        return result;
    }

    /** Достучались ли до самой либы/функций (иначе смысла в коде результата нет). */
    static boolean isReachable(int code) {
        return code != ERR_LIB_UNREACHABLE && code != ERR_SYMBOLS_MISSING;
    }

    /** Человекочитаемое описание кода для UI и логов. */
    static String describe(int code) {
        switch (code) {
            case 0:
                return "OK (0)";
            case ERR_BAD_FRAME:
                return "неверный кадр";
            case ERR_LIB_UNREACHABLE:
                return "libqg_hal.so недоступна (путь / SELinux)";
            case ERR_SYMBOLS_MISSING:
                return "нет функций qg_canbus_* в либе";
            default:
                return "код HAL: " + code;
        }
    }

    private static byte[] parse(String hexFrame) {
        String[] tokens = hexFrame.trim().split("\\s+");
        byte[] frame = new byte[tokens.length];
        for (int i = 0; i < tokens.length; i++) {
            frame[i] = (byte) Integer.parseInt(tokens[i], 16);
        }
        return frame;
    }
}

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

    static {
        System.loadLibrary("suspension");
    }

    private CanBus() {
    }

    /** Нативная отправка одного кадра в указанный канал; возвращает код результата. */
    private static native int sendFrame(int channel, byte[] frame);

    /**
     * Разбирает hex-строку вида "69 08 02 .." в кадр и отправляет его в CAN.
     * Кадры неверной длины отбрасываются, чтобы не уронить нативный вызов.
     * Возвращает код нативного слоя (отрицательный — ошибка).
     */
    static int send(int channel, String hexFrame) {
        byte[] frame = parse(hexFrame);
        if (frame.length != FRAME_SIZE) {
            Log.w(TAG, "Пропускаю кадр неверной длины: " + hexFrame);
            return -1;
        }
        int result = sendFrame(channel, frame);
        if (result < 0) {
            Log.w(TAG, "Ошибка отправки [" + hexFrame + "], код " + result);
        } else {
            Log.i(TAG, "Отправлен [" + hexFrame + "] -> " + result);
        }
        return result;
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

package com.suspension.fix;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Единая точка работы с шиной. Всё выполняется на одном фоновом потоке:
 * это снимает работу с UI/main-потока и сериализует доступ к CAN между
 * кнопками экрана, проверкой состояния и входящими broadcast'ами.
 */
final class SuspensionController {
    /** Канал CAN, в который уходят кадры подвески. */
    private static final int CHANNEL = 1;

    /** Кадр дублируется — так надёжнее доходит до блока подвески. */
    private static final int REPEAT = 2;

    /** Общий поток исполнителя: обращения к шине строго по очереди. */
    private static final ExecutorService BUS = Executors.newSingleThreadExecutor();

    /**
     * Обратный вызов с итоговым кодом нативного слоя (см. {@link CanBus#describe(int)})
     * и детальным сообщением (dlerror / коды open-control-close).
     */
    interface ResultCallback {
        void onResult(int code, String detail);
    }

    private SuspensionController() {
    }

    /**
     * Применяет команду асинхронно. {@code callback} (может быть null)
     * вызывается на фоновом потоке с кодом результата последней отправки.
     */
    static void apply(SuspensionCommand command, ResultCallback callback) {
        BUS.execute(() -> {
            int last = 0;
            for (int i = 0; i < REPEAT; i++) {
                last = CanBus.send(CHANNEL, command.frame());
            }
            if (callback != null) {
                callback.onResult(last, CanBus.lastDetail());
            }
        });
    }

    /** Асинхронная проверка состояния шины. */
    static void probe(ResultCallback callback) {
        BUS.execute(() -> {
            int code = CanBus.probe();
            if (callback != null) {
                callback.onResult(code, CanBus.lastDetail());
            }
        });
    }
}

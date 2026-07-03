package com.suspension.fix;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Единая точка применения команды подвески. Отправка в CAN идёт на одном
 * фоновом потоке: это снимает работу с UI/main-потока и сериализует доступ
 * к шине между кнопками экрана и входящими broadcast'ами.
 */
final class SuspensionController {
    /** Канал CAN, в который уходят кадры подвески. */
    private static final int CHANNEL = 1;

    /** Кадр дублируется — так надёжнее доходит до блока подвески. */
    private static final int REPEAT = 2;

    /** Общий поток исполнителя: обращения к шине строго по очереди. */
    private static final ExecutorService BUS = Executors.newSingleThreadExecutor();

    private SuspensionController() {
    }

    /**
     * Применяет команду асинхронно. {@code onComplete} (может быть null)
     * вызывается на фоновом потоке после отправки — например, чтобы
     * закрыть {@code goAsync()} в ресивере.
     */
    static void apply(SuspensionCommand command, Runnable onComplete) {
        BUS.execute(() -> {
            try {
                for (int i = 0; i < REPEAT; i++) {
                    CanBus.send(CHANNEL, command.frame());
                }
            } finally {
                if (onComplete != null) {
                    onComplete.run();
                }
            }
        });
    }
}

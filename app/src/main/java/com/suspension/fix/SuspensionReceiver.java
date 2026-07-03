package com.suspension.fix;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Точка входа: ловит broadcast блокировки/разблокировки пневмоподвески,
 * определяет команду по action и транслирует её в CAN-шину.
 *
 * Работа с шиной уходит в фоновый поток через goAsync(): onReceive()
 * выполняется на главном потоке, а нативный вызов может блокироваться.
 * Единственный поток исполнителя ещё и сериализует доступ к шине, чтобы
 * пересекающиеся broadcast'ы не открывали её одновременно.
 */
public class SuspensionReceiver extends BroadcastReceiver {
    private static final String TAG = "SuspensionFix/Receiver";

    /** Канал CAN, в который уходят кадры подвески. */
    private static final int CHANNEL = 1;

    /** Кадр дублируется — так надёжнее доходит до блока подвески. */
    private static final int REPEAT = 2;

    /** Общий для всех broadcast'ов поток: сериализует обращения к шине. */
    private static final ExecutorService BUS = Executors.newSingleThreadExecutor();

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        SuspensionCommand command = SuspensionCommand.forAction(action);

        if (command == null) {
            Log.w(TAG, "Игнорирую неизвестный action: " + action);
            return;
        }

        Log.i(TAG, "Получен " + command + " (" + action + ")");
        PendingResult pending = goAsync();
        BUS.execute(() -> {
            try {
                for (int i = 0; i < REPEAT; i++) {
                    CanBus.send(CHANNEL, command.frame());
                }
            } finally {
                pending.finish();
            }
        });
    }
}

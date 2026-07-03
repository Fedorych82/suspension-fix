package com.suspension.fix;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Точка входа по broadcast: ловит интент блокировки/разблокировки
 * пневмоподвески, определяет команду по action и применяет её.
 *
 * Работа с шиной уходит в фон через goAsync() + SuspensionController,
 * поэтому onReceive() не блокирует главный поток.
 */
public class SuspensionReceiver extends BroadcastReceiver {
    private static final String TAG = "SuspensionFix/Receiver";

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
        SuspensionController.apply(command, pending::finish);
    }
}

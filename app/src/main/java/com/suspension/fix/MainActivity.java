package com.suspension.fix;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

/**
 * Экран с двумя кнопками — заблокировать и разблокировать подвеску.
 * Обе кнопки идут через тот же SuspensionController, что и broadcast.
 * Наличие launcher-активити ещё и выводит приложение из stopped-состояния,
 * чтобы внешние broadcast'ы гарантированно доходили.
 */
public class MainActivity extends Activity {
    private static final String TAG = "SuspensionFix/UI";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button lock = findViewById(R.id.button_lock);
        Button unlock = findViewById(R.id.button_unlock);

        lock.setOnClickListener(v -> trigger(SuspensionCommand.LOCK));
        unlock.setOnClickListener(v -> trigger(SuspensionCommand.UNLOCK));
    }

    private void trigger(SuspensionCommand command) {
        Log.i(TAG, "Кнопка: " + command);
        SuspensionController.apply(command, null);
        Toast.makeText(this, "Отправлено: " + command.label(), Toast.LENGTH_SHORT).show();
    }
}

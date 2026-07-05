package com.suspension.fix;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

/**
 * Экран управления подвеской: кнопки блокировки/разблокировки, проверка
 * состояния CAN-шины и вывод результата последней команды.
 *
 * Работа с шиной идёт через SuspensionController (фоновый поток), результат
 * возвращается в UI-поток через runOnUiThread. Наличие launcher-активити
 * также выводит приложение из stopped-состояния для внешних broadcast'ов.
 */
public class MainActivity extends Activity {
    private static final String TAG = "SuspensionFix/UI";

    private static final int COLOR_OK = 0xFF2E7D32;   // зелёный
    private static final int COLOR_ERR = 0xFFC62828;  // красный

    private TextView busStatus;
    private TextView lastResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        busStatus = findViewById(R.id.text_bus_status);
        lastResult = findViewById(R.id.text_last_result);

        Button lock = findViewById(R.id.button_lock);
        Button unlock = findViewById(R.id.button_unlock);
        Button check = findViewById(R.id.button_check);

        lock.setOnClickListener(v -> trigger(SuspensionCommand.LOCK));
        unlock.setOnClickListener(v -> trigger(SuspensionCommand.UNLOCK));
        check.setOnClickListener(v -> refreshBusStatus());
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshBusStatus();
    }

    /** Проверяет доступность шины и показывает результат. */
    private void refreshBusStatus() {
        busStatus.setText("Шина: проверка…");
        busStatus.setTextColor(Color.DKGRAY);
        SuspensionController.probe((code, detail) -> {
            String diag = DeviceDiagnostics.scan(); // фоновый поток — file IO здесь ок
            runOnUiThread(() -> {
                busStatus.setText("Шина: " + CanBus.describe(code) + "\n" + detail + "\n\n" + diag);
                busStatus.setTextColor(CanBus.isReachable(code) ? COLOR_OK : COLOR_ERR);
            });
        });
    }

    /** Отправляет команду и показывает код результата. */
    private void trigger(SuspensionCommand command) {
        Log.i(TAG, "Кнопка: " + command);
        lastResult.setText("Команда «" + command.label() + "»: отправка…");
        lastResult.setTextColor(Color.DKGRAY);
        SuspensionController.apply(command, (code, detail) -> runOnUiThread(() -> {
            lastResult.setText("Команда «" + command.label() + "» → " + CanBus.describe(code) + "\n" + detail);
            lastResult.setTextColor(CanBus.isReachable(code) ? COLOR_OK : COLOR_ERR);
        }));
    }
}

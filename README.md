# SuspensionFix

Мини-приложение для Android Automotive (Voyah): ловит broadcast-интент
блокировки/разблокировки пневмоподвески и отправляет соответствующий кадр
в CAN-шину автомобиля.

## Как работает

```
Intent (action) → SuspensionReceiver → SuspensionCommand → CanBus → JNI → libqg_hal.so → CAN
```

- **SuspensionReceiver** — `BroadcastReceiver`, принимает интент, работу с шиной
  выполняет в фоновом потоке (`goAsync()`), обращения к шине сериализованы.
- **SuspensionCommand** — `enum`, связывает `action` с CAN-кадром.
- **CanBus** — разбирает hex-кадр в байты и вызывает нативный метод.
- **native-lib.cpp** — через `dlopen("/system/lib64/libqg_hal.so")` вызывает
  `qg_canbus_open` / `qg_canbus_control` / `qg_canbus_close`.

| Action                                | CAN-кадр                        |
|---------------------------------------|---------------------------------|
| `com.suspension.fix.LOCK_SUSPENSION`  | `69 08 02 00 00 16 00 00 00 00` |
| `com.suspension.fix.UNLOCK_SUSPENSION`| `69 08 01 00 00 16 00 00 00 00` |

## Требования

Тулчейн подобран под сборку на **JDK 11** (AGP 7.4.2 / Gradle 7.6.4):

- JDK 11
- Android SDK: platform `android-33`, build-tools `33.0.1`
- NDK `25.2.9519653`, CMake `3.22.1`

Недостающие компоненты можно доустановить:

```bash
sdkmanager "platforms;android-33" "build-tools;33.0.1" "ndk;25.2.9519653" "cmake;3.22.1"
```

## Сборка

Debug-APK:

```bash
./gradlew assembleDebug
# → app/build/outputs/apk/debug/app-debug.apk
```

Release-APK (подписан debug-ключом, см. `app/build.gradle.kts`):

```bash
./gradlew assembleRelease
# → app/build/outputs/apk/release/app-release.apk
```

## Установка

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Проверка

```bash
adb shell am broadcast -a com.suspension.fix.LOCK_SUSPENSION
adb shell am broadcast -a com.suspension.fix.UNLOCK_SUSPENSION
```

Логи:

```bash
adb logcat -s SuspensionFix/Receiver SuspensionFix/CanBus SuspensionFix/Native
```

## Примечания

- Ресивер `exported="true"` без permission — broadcast может послать любое
  приложение. При необходимости закрыть — завести signature-level permission
  и повесить `android:permission` на ресивер (триггер должен её держать).
- CAN-кадры и путь к `libqg_hal.so` специфичны для платформы; на другом
  железе не сработают.

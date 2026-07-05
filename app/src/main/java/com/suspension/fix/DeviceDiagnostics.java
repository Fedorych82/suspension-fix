package com.suspension.fix;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

/**
 * Диагностика окружения: существует ли вендорная либа и объявлена ли она
 * «публичной» (тогда её можно грузить из обычного приложения через
 * uses-native-library), либо она приватная (тогда доступ выдан устройством
 * по идентичности конкретного приложения). Всё читается из world-readable
 * файлов, root не нужен.
 */
final class DeviceDiagnostics {
    private static final String LIB_PATH = "/system/lib64/libqg_hal.so";
    private static final String NEEDLE = "qg_hal";

    private static final String[] ETC_DIRS = {
            "/system/etc", "/vendor/etc", "/product/etc", "/system_ext/etc"
    };

    private DeviceDiagnostics() {
    }

    /** Короткий отчёт для вывода на экран. */
    static String scan() {
        StringBuilder sb = new StringBuilder();

        File lib = new File(LIB_PATH);
        sb.append("libqg_hal.so: ");
        if (!lib.exists()) {
            sb.append("не найдена по ").append(LIB_PATH);
        } else {
            sb.append(lib.canRead() ? "есть, читается" : "есть, чтение запрещено (SELinux)");
        }
        sb.append('\n');

        String listedIn = findInPublicLibraries();
        if (listedIn != null) {
            sb.append("публичная либа: ДА — ").append(listedIn);
        } else {
            sb.append("публичная либа: нет (не значится в public.libraries*)");
        }
        return sb.toString();
    }

    /** Возвращает путь файла public.libraries*, где упомянута либа, либо null. */
    private static String findInPublicLibraries() {
        for (String dir : ETC_DIRS) {
            File[] files = new File(dir).listFiles(
                    (d, name) -> name.startsWith("public.libraries") && name.endsWith(".txt"));
            if (files == null) {
                continue;
            }
            for (File file : files) {
                if (fileMentionsLib(file)) {
                    return file.getPath();
                }
            }
        }
        return null;
    }

    private static boolean fileMentionsLib(File file) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains(NEEDLE)) {
                    return true;
                }
            }
        } catch (Exception ignored) {
            // файл недоступен — просто пропускаем
        }
        return false;
    }
}

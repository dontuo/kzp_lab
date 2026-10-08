package ua.lpnu.kzp;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Ізолює кросплатформне читання та запис текстових файлів. */
public final class FileReport {
    /* Позначка порядку байтів, яку Блокнот Windows може додати на початок UTF-8 файла. */
    private static final String BOM = "﻿";

    /* Забороняє створення екземплярів службового класу. */
    private FileReport() {
    }

    /**
     * Читає рядки у фіксованому кодуванні UTF-8.
     *
     * <p>Якщо файл починається з BOM, його прибирають, щоб він не став частиною
     * назви першої книги.
     *
     * @param input шлях до вхідного файла
     * @return рядки файла без символів кінця рядка
     * @throws IOException якщо файл неможливо прочитати або він не є коректним UTF-8
     */
    public static List<String> readLines(Path input) throws IOException {
        // Явне кодування не залежить від налаштувань операційної системи.
        List<String> lines = new ArrayList<>(Files.readAllLines(input, StandardCharsets.UTF_8));
        if (!lines.isEmpty() && lines.get(0).startsWith(BOM)) {
            lines.set(0, lines.get(0).substring(BOM.length()));
        }
        return lines;
    }

    /**
     * Створює каталог призначення і записує звіт у кодуванні UTF-8.
     *
     * @param output шлях до файла звіту
     * @param report готовий текст звіту
     * @throws IOException якщо каталог або файл неможливо створити
     */
    public static void writeReport(Path output, String report) throws IOException {
        // Після клонування репозиторію каталог out може ще не існувати.
        Path parent = output.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(output, report, StandardCharsets.UTF_8);
    }
}

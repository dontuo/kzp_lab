package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Перевіряє, що перебудови не змінили зовнішню поведінку програми понад задокументоване.
 *
 * <p>Файл {@code lab01-report.txt} – контрольний звіт, отриманий версією 1.0.0
 * (тег {@code v1.0.0}) на тому самому {@code data/input.csv}. Версія 2.0.0 давала
 * точно такий самий звіт.
 */
class CompatibilityTest {
    /*
     * ЛР № 3 дозволила рядки з 7 полями, тому змінився текст помилки про кількість полів.
     * Номери рядків і причини пропуску лишилися ті самі.
     */
    private static final Map<String, String> CHANGED_MESSAGES_LAB03 = Map.of(
            "Рядок 7: очікується 4 поля, отримано 3", "Рядок 7: очікується 4 або 7 полів, отримано 3",
            "Рядок 8: очікується 4 поля, отримано 5", "Рядок 8: очікується 4 або 7 полів, отримано 5");

    /* Блок форматів, доданий у ЛР № 3 після таблиці показників ЛР № 1. */
    private static final List<String> FORMAT_BLOCK_LAB03 = List.of(
            "",
            "Формат видання               Книг   Вартість придбання",
            "друкована                    6      1160.39 грн",
            "електронна                   0      0.00 грн");

    @TempDir
    Path tempDir;

    @Test
    void reportDiffersFromLab01OnlyAsDocumented() throws IOException {
        List<String> actual = runReport().lines().toList();
        List<String> expected = expectedLab01Report().lines()
                .map(line -> {
                    // Рядки помилок у звіті мають відступ у два пробіли.
                    String changed = CHANGED_MESSAGES_LAB03.get(line.strip());
                    return changed == null ? line : "  " + changed;
                })
                .toList();

        // Таблиця ЛР № 1 закінчується рядком сумарної вартості; блок форматів іде одразу після неї.
        int tableEnd = expected.indexOf("Сумарна вартість каталогу    860.39 грн") + 1;
        assertEquals(expected.subList(0, tableEnd), actual.subList(0, tableEnd), "таблиця показників ЛР № 1");
        assertEquals(FORMAT_BLOCK_LAB03, actual.subList(tableEnd, tableEnd + FORMAT_BLOCK_LAB03.size()),
                "блок форматів ЛР № 3");
        assertEquals(expected.subList(tableEnd, expected.size()),
                actual.subList(tableEnd + FORMAT_BLOCK_LAB03.size(), actual.size()), "пропущені рядки");
    }

    /* Запускає програму на data/input.csv і повертає звіт у вигляді Linux. */
    private String runReport() throws IOException {
        Path output = tempDir.resolve("report.txt");
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8));
        int code;
        try {
            code = Main.run(new String[] {"--output", output.toString()});
        } finally {
            System.setOut(originalOut);
        }
        assertEquals(Main.EXIT_OK, code);
        return normalize(Files.readString(output, StandardCharsets.UTF_8));
    }

    /* Читає контрольний звіт із ресурсів тестів. */
    private static String expectedLab01Report() throws IOException {
        try (InputStream in = CompatibilityTest.class.getResourceAsStream("/lab01-report.txt")) {
            assertNotNull(in, "немає контрольного звіту lab01-report.txt");
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /*
     * Контрольний звіт записано на Linux. На Windows %n дає \r\n, а Path друкує
     * data\input.csv, тому зводимо обидві відмінності ОС до вигляду Linux.
     */
    private static String normalize(String report) {
        return report.replace("\r\n", "\n").replace('\\', '/');
    }
}

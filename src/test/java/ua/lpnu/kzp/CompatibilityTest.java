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
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Перевіряє, що перебудови не змінили зовнішню поведінку програми понад задокументоване.
 *
 * <p>Контрольні звіти в ресурсах тестів:
 * <ul>
 *   <li>{@code lab01-report.txt} – версія 1.0.0 на {@code data/input.csv}
 *       (версія 2.0.0 давала точно такий самий звіт);</li>
 *   <li>{@code lab03-books-report.txt} – версія 3.0.0 на {@code data/books.csv}.</li>
 * </ul>
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

    /* Блок запитів ЛР № 4 у кінці звіту на data/input.csv: у старому форматі немає року й жанру. */
    private static final List<String> QUERIES_BLOCK_INPUT_LAB04 = List.of(
            "",
            row("Запит", "Результат"),
            row("Книги від 2020 року", "немає даних"),
            row("Кількість книг за жанром", "немає даних"),
            row("Статистика сторінок", "кількість 6, мін. 32, макс. 320, сума 1140, середнє 190.00"),
            row("Топ-5 за сторінками",
                    "Кобзар (320), Тигролови (288), Захар Беркут (224), Лісова пісня (180), Інститутка (96)"),
            row("Жанри за форматом", "немає даних"));

    /* Блок запитів ЛР № 4 у кінці звіту на data/books.csv. */
    private static final List<String> QUERIES_BLOCK_BOOKS_LAB04 = List.of(
            "",
            row("Запит", "Результат"),
            row("Книги від 2020 року", "Амадока"),
            row("Кількість книг за жанром", "драма 1, поезія 1, роман 4"),
            row("Статистика сторінок", "кількість 7, мін. 180, макс. 832, сума 2548, середнє 364.00"),
            row("Топ-5 за сторінками", "Амадока (832), Доця (368), Інтернат (336), Кобзар (320), Тигролови (288)"),
            row("Жанри за форматом", "друкована: драма 1, поезія 1, роман 1; електронна: роман 3"));

    @TempDir
    Path tempDir;

    @Test
    void inputReportDiffersFromLab01OnlyAsDocumented() throws IOException {
        List<String> actual = runReport(Path.of("data", "input.csv"));
        List<String> lab01 = resourceLines("/lab01-report.txt").stream()
                .map(line -> {
                    // Рядки помилок у звіті мають відступ у два пробіли.
                    String changed = CHANGED_MESSAGES_LAB03.get(line.strip());
                    return changed == null ? line : "  " + changed;
                })
                .toList();

        // Таблиця ЛР № 1 закінчується рядком сумарної вартості; блок форматів іде одразу після неї.
        int tableEnd = lab01.indexOf("Сумарна вартість каталогу    860.39 грн") + 1;
        List<String> expected = new ArrayList<>(lab01.subList(0, tableEnd));
        expected.addAll(FORMAT_BLOCK_LAB03);
        expected.addAll(lab01.subList(tableEnd, lab01.size()));
        expected.addAll(QUERIES_BLOCK_INPUT_LAB04);

        assertEquals(expected, actual);
    }

    @Test
    void booksReportDiffersFromLab03OnlyByQueriesBlock() throws IOException {
        List<String> expected = new ArrayList<>(resourceLines("/lab03-books-report.txt"));
        expected.addAll(QUERIES_BLOCK_BOOKS_LAB04);

        assertEquals(withoutCurrentYear(expected), withoutCurrentYear(runReport(Path.of("data", "books.csv"))));
    }

    /* Рядок таблиці звіту: назва вирівняна ліворуч у полі шириною 28 символів. */
    private static String row(String label, String value) {
        return String.format(Locale.ROOT, "%-28s %s", label, value);
    }

    /* Запускає програму й повертає рядки звіту у вигляді Linux. */
    private List<String> runReport(Path input) throws IOException {
        Path output = tempDir.resolve("report.txt");
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8));
        int code;
        try {
            code = Main.run(new String[] {"--input", input.toString(), "--output", output.toString()});
        } finally {
            System.setOut(originalOut);
        }
        assertEquals(Main.EXIT_OK, code);
        return normalize(Files.readString(output, StandardCharsets.UTF_8)).lines().toList();
    }

    /* Читає контрольний звіт із ресурсів тестів. */
    private static List<String> resourceLines(String name) throws IOException {
        try (InputStream in = CompatibilityTest.class.getResourceAsStream(name)) {
            assertNotNull(in, "немає контрольного звіту " + name);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).lines().toList();
        }
    }

    /*
     * Контрольні звіти записано на Linux. На Windows %n дає \r\n, а Path друкує
     * data\input.csv, тому зводимо обидві відмінності ОС до вигляду Linux.
     */
    private static String normalize(String report) {
        return report.replace("\r\n", "\n").replace('\\', '/');
    }

    /*
     * Повідомлення про рік видання містить поточний рік ("від 1450 до 2026"), тому
     * контрольний звіт 2026 року інакше застарів би з настанням нового року.
     */
    private static List<String> withoutCurrentYear(List<String> lines) {
        return lines.stream()
                .map(line -> line.replace("до " + Year.now().getValue() + ":", "до <поточний рік>:")
                        .replace("до 2026:", "до <поточний рік>:"))
                .toList();
    }
}

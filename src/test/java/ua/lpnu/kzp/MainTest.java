package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MainTest {
    @TempDir
    Path tempDir;

    /* Перехоплений вивід програми та її код завершення. */
    private record Result(int code, String out, String err) {
    }

    /* Запускає Main.run, тимчасово перенаправивши System.out і System.err у буфери. */
    private static Result run(String... args) {
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(err, true, StandardCharsets.UTF_8));
        int code;
        try {
            code = Main.run(args);
        } finally {
            // Повертаємо консоль, навіть якщо run кине виняток.
            System.setOut(originalOut);
            System.setErr(originalErr);
        }
        return new Result(code, out.toString(StandardCharsets.UTF_8), err.toString(StandardCharsets.UTF_8));
    }

    @Test
    void helpPrintsUsage() {
        Result result = run("--help");
        assertEquals(Main.EXIT_OK, result.code());
        assertTrue(result.out().contains("Використання"));
    }

    @Test
    void helpHasPriorityOverOtherOptions() {
        Result result = run("--input", "missing.csv", "--help");
        assertEquals(Main.EXIT_OK, result.code());
        assertTrue(result.out().contains("Використання"));
    }

    @Test
    void versionPrintsProjectVersion() {
        Result result = run("--version");
        assertEquals(Main.EXIT_OK, result.code());
        assertTrue(result.out().contains("1.0.0"), result.out());
    }

    @Test
    void sampleDataProducesExpectedReport() throws IOException {
        // Surefire запускає тести з кореня проєкту, тому data/input.csv доступний.
        // Звіт пишемо в тимчасовий каталог, щоб тест не залишав out/ у проєкті.
        Path output = tempDir.resolve("report.txt");
        Result result = run("--output", output.toString());

        assertEquals(Main.EXIT_OK, result.code(), result.err());
        String report = Files.readString(output, StandardCharsets.UTF_8);
        // Консоль і файл отримують той самий текст.
        assertTrue(result.out().startsWith(report));
        assertTrue(report.contains(String.format(Locale.ROOT, "%-28s %s", "Коректних записів", "6")), report);
        assertTrue(report.contains("190.00"), report);
        assertTrue(report.contains("Кобзар (250.00 грн)"), report);
        assertTrue(report.contains("860.39 грн"), report);
        assertTrue(report.contains("Пропущено рядків: 19"), report);
        assertTrue(report.contains("Рядок 4: порожній рядок"), report);
    }

    @Test
    void fileWithoutValidRecordsIsError() throws IOException {
        Path input = tempDir.resolve("bad.csv");
        Files.writeString(input, "\n;Автор;1;1.00\nКнига;Автор;abc;1.00\n", StandardCharsets.UTF_8);
        Path output = tempDir.resolve("report.txt");

        Result result = run("--input", input.toString(), "--output", output.toString());

        assertEquals(Main.EXIT_ERROR, result.code());
        assertTrue(result.err().contains("немає жодного коректного запису"));
        // Звіт усе одно записується: у ньому видно причини пропуску рядків.
        assertTrue(Files.readString(output, StandardCharsets.UTF_8).contains("Пропущено рядків: 3"));
    }

    @Test
    void inputWithoutValueIsUsageError() {
        Result result = run("--input");
        assertEquals(Main.EXIT_USAGE, result.code());
        assertTrue(result.err().contains("--input"));
    }

    @Test
    void optionInsteadOfValueIsUsageError() {
        Result result = run("--output", "--help");
        assertEquals(Main.EXIT_USAGE, result.code());
    }

    @Test
    void unknownArgumentIsUsageError() {
        Result result = run("--inptu", "data/input.csv");
        assertEquals(Main.EXIT_USAGE, result.code());
        assertTrue(result.err().contains("--inptu"));
    }

    @Test
    void missingInputFileIsError() {
        Result result = run("--input", "no-such-file.csv");
        assertEquals(Main.EXIT_ERROR, result.code());
        assertTrue(result.err().contains("no-such-file.csv"));
    }
}

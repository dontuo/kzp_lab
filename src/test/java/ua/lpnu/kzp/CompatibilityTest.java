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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Перевіряє, що перебудова на класи не змінила зовнішню поведінку програми.
 *
 * <p>Файл {@code lab01-report.txt} – контрольний звіт, отриманий версією 1.0.0
 * (тег {@code v1.0.0}) на тому самому {@code data/input.csv}.
 */
class CompatibilityTest {
    @TempDir
    Path tempDir;

    @Test
    void reportIsIdenticalToLab01() throws IOException {
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
        assertEquals(expectedLab01Report(), normalize(Files.readString(output, StandardCharsets.UTF_8)));
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

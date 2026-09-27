package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class MainTest {
    /* Перевіряє, що програма запускається і виводить привітання в консоль. */
    @Test
    void mainPrintsGreeting() {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        // Тимчасово перенаправляємо System.out у буфер, щоб прочитати вивід програми.
        System.setOut(new PrintStream(buffer, true, StandardCharsets.UTF_8));
        try {
            Main.main(new String[0]);
        } finally {
            // Повертаємо консоль, навіть якщо main кине виняток.
            System.setOut(originalOut);
        }

        // %n у програмі дає роздільник поточної ОС, тому очікуємо System.lineSeparator().
        assertEquals("Hello, World!" + System.lineSeparator(), buffer.toString(StandardCharsets.UTF_8));
    }
}

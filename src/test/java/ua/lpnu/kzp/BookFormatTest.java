package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class BookFormatTest {
    @Test
    void valuesHaveStableOrder() {
        // Порядок констант задає порядок рядків у звіті.
        assertArrayEquals(new BookFormat[] {BookFormat.PRINTED, BookFormat.ELECTRONIC}, BookFormat.values());
    }

    @Test
    void labelsAreUkrainian() {
        assertEquals("друкована", BookFormat.PRINTED.getLabel());
        assertEquals("електронна", BookFormat.ELECTRONIC.getLabel());
    }

    @Test
    void fromCsvReturnsTheSameConstant() {
        // Кожна константа існує в одному екземплярі, тому порівняння через == коректне.
        assertSame(BookFormat.PRINTED, BookFormat.fromCsv("PRINTED"));
        assertSame(BookFormat.ELECTRONIC, BookFormat.fromCsv("ELECTRONIC"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"AUDIO", "printed", " PRINTED", "", "друкована"})
    void unknownFormatIsRejected(String text) {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> BookFormat.fromCsv(text));
        assertEquals("невідомий формат книги: \"" + text + "\"", error.getMessage());
    }
}

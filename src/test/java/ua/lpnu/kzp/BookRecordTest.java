package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Year;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class BookRecordTest {
    private static final BookRecord KOBZAR = new BookRecord("Кобзар", "Тарас Шевченко", 1840, 320, "поезія");

    /* Значення одного CSV-запису за назвами стовпців. */
    private static Map<String, String> values(String title, String author, String year, String pages, String genre) {
        Map<String, String> values = new HashMap<>();
        values.put(BookRecord.TITLE, title);
        values.put(BookRecord.AUTHOR, author);
        values.put(BookRecord.YEAR, year);
        values.put(BookRecord.PAGES, pages);
        values.put(BookRecord.GENRE, genre);
        return values;
    }

    /* --- Інваріанти --- */

    @Test
    void boundaryValuesAreAccepted() {
        new BookRecord("А", "Б", Book.FIRST_PRINTED_YEAR, 1, "В");
        new BookRecord("А", "Б", Year.now().getValue(), 1, "В");
    }

    @Test
    void invalidValuesAreRejected() {
        assertThrows(NullPointerException.class, () -> new BookRecord(null, "Б", 2000, 1, "В"));
        assertThrows(IllegalArgumentException.class, () -> new BookRecord(" ", "Б", 2000, 1, "В"));
        assertThrows(IllegalArgumentException.class, () -> new BookRecord("А", "Б", 2000, 0, "В"));
        assertThrows(IllegalArgumentException.class, () -> new BookRecord("А", "Б", 1449, 1, "В"));
        assertThrows(IllegalArgumentException.class, () -> new BookRecord("А", "Б", 2000, 1, ""));
    }

    @Test
    void recordEqualityComparesAllFields() {
        assertEquals(KOBZAR, new BookRecord("Кобзар", "Тарас Шевченко", 1840, 320, "поезія"));
        assertEquals(KOBZAR.hashCode(), new BookRecord("Кобзар", "Тарас Шевченко", 1840, 320, "поезія").hashCode());
        assertNotEquals(KOBZAR, new BookRecord("Кобзар", "Тарас Шевченко", 1840, 321, "поезія"));
    }

    /* --- Перетворення з Book --- */

    @Test
    void fromBookTakesVariantFields() {
        Book book = Book.fromCsv("Кобзар;Тарас Шевченко;320;250.00;ELECTRONIC;1840;поезія");
        assertTrue(BookRecord.isExportable(book));
        assertEquals(KOBZAR, BookRecord.from(book));
    }

    @Test
    void bookWithoutYearAndGenreIsNotExportable() {
        Book legacy = Book.fromCsv("Захар Беркут;Іван Франко;224;175.00");
        assertFalse(BookRecord.isExportable(legacy));
        assertThrows(IllegalArgumentException.class, () -> BookRecord.from(legacy));
    }

    /* --- fromCsv --- */

    @Test
    void fromCsvRestoresRecord() throws DataStorageException {
        assertEquals(KOBZAR, BookRecord.fromCsv(values("Кобзар", "Тарас Шевченко", "1840", "320", "поезія"), 1));
    }

    @Test
    void missingColumnIsReported() {
        Map<String, String> values = values("Кобзар", "Тарас Шевченко", "1840", "320", "поезія");
        values.remove(BookRecord.GENRE);
        DataStorageException error = assertThrows(DataStorageException.class, () -> BookRecord.fromCsv(values, 3));
        assertEquals("запис 3: немає стовпця «жанр»", error.getMessage());
    }

    @Test
    void wrongNumberKeepsCause() {
        DataStorageException error = assertThrows(DataStorageException.class,
                () -> BookRecord.fromCsv(values("Кобзар", "Тарас Шевченко", "1840", "багато", "поезія"), 2));
        assertEquals("запис 2: стовпець «сторінки» не є цілим числом: \"багато\"", error.getMessage());
        assertInstanceOf(NumberFormatException.class, error.getCause());
    }

    @Test
    void invariantViolationKeepsCause() {
        DataStorageException error = assertThrows(DataStorageException.class,
                () -> BookRecord.fromCsv(values("Кобзар", "Тарас Шевченко", "1840", "0", "поезія"), 4));
        assertTrue(error.getMessage().startsWith("запис 4: кількість сторінок має бути додатною"), error.getMessage());
        assertInstanceOf(IllegalArgumentException.class, error.getCause());
    }
}

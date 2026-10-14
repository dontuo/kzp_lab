package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.jupiter.api.Test;

class JsonExporterTest {
    /* Перенесення рядка ОС, яким експортер розділяє об'єкти. */
    private static final String NL = System.lineSeparator();

    @Test
    void recordsBecomeJsonArray() throws DataStorageException {
        String json = new JsonExporter<>(BookRecord.class).format(List.of(
                new BookRecord("Кобзар", "Тарас Шевченко", 1840, 320, "поезія")));
        // Ті самі анотовані поля й порядок, що й у CSV; числа без лапок.
        assertEquals("[" + NL
                + "  {\"автор\": \"Тарас Шевченко\", \"жанр\": \"поезія\", \"сторінки\": 320,"
                + " \"назва\": \"Кобзар\", \"рік\": 1840}" + NL
                + "]" + NL, json);
    }

    @Test
    void emptyListIsEmptyArray() throws DataStorageException {
        assertEquals("[]" + NL, new JsonExporter<>(BookRecord.class).format(List.of()));
    }

    @Test
    void specialCharactersAreEscaped() {
        assertEquals("\"Повість \\\"Intermezzo\\\" \\\\ a\\nb\\tc\\u0001\"",
                JsonExporter.quote("Повість \"Intermezzo\" \\ a\nb\tc\u0001"));
    }

    @Test
    void bothFormatsWorkThroughCommonInterface() throws DataStorageException {
        List<Exporter<BookRecord>> exporters = List.of(
                new CsvExporter<>(BookRecord.class), new JsonExporter<>(BookRecord.class));
        List<BookRecord> records = List.of(new BookRecord("А", "Б", 2000, 1, "В"));
        assertEquals("автор,жанр,сторінки,назва,рік" + NL + "Б,В,1,А,2000" + NL, exporters.get(0).format(records));
        assertEquals("[" + NL + "  {\"автор\": \"Б\", \"жанр\": \"В\", \"сторінки\": 1, \"назва\": \"А\", \"рік\": 2000}"
                + NL + "]" + NL, exporters.get(1).format(records));
    }

    @Test
    void fieldsAreCachedPerClass() throws DataStorageException {
        List<Field> first = AnnotatedFields.of(BookRecord.class);
        List<Field> second = AnnotatedFields.of(BookRecord.class);
        // Другий виклик повертає той самий об'єкт із кешу, а не сканує клас повторно.
        assertSame(first, second);
        assertEquals(AnnotatedFields.scan(BookRecord.class), first);
    }
}

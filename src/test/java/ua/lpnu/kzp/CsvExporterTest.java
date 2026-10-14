package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CsvExporterTest {
    /* Перенесення рядка ОС, яким експортер розділяє записи. */
    private static final String NL = System.lineSeparator();

    /* Клас іншої предметної області: експортер не знає про нього нічого, крім анотацій. */
    static class Point {
        @CsvColumn("x")
        private final int xCoordinate;
        @CsvColumn("підпис")
        private final String label;
        // Без анотації – не експортується.
        private final String secret = "не експортується";

        Point(int xCoordinate, String label) {
            this.xCoordinate = xCoordinate;
            this.label = label;
        }

        String secret() {
            return secret;
        }
    }

    /* Підклас: експортер має пройти ланцюжок надкласів. */
    static final class NamedPoint extends Point {
        @CsvColumn("назва")
        private final String name;

        NamedPoint(int xCoordinate, String label, String name) {
            super(xCoordinate, label);
            this.name = name;
        }
    }

    /* Клас без анотованих полів. */
    static final class Plain {
        private final String value = "x";

        String value() {
            return value;
        }
    }

    @TempDir
    Path tempDir;

    /* --- @CsvColumn --- */

    @Test
    void annotationIsAvailableAtRuntimeOnFieldsOnly() throws NoSuchFieldException {
        assertEquals(RetentionPolicy.RUNTIME, CsvColumn.class.getAnnotation(Retention.class).value());
        assertArrayEquals(new ElementType[] {ElementType.FIELD}, CsvColumn.class.getAnnotation(Target.class).value());
        // Анотація з компоненти record потрапляє на приватне поле, і рефлексія її бачить.
        CsvColumn column = BookRecord.class.getDeclaredField("title").getAnnotation(CsvColumn.class);
        assertNotNull(column);
        assertEquals("назва", column.value());
    }

    /* --- Заголовок і порядок стовпців --- */

    @Test
    void columnsAreSortedByJavaFieldName() throws DataStorageException {
        // author, genre, pages, title, year – за абеткою Java-імен, а не за порядком у record.
        assertEquals(List.of("автор", "жанр", "сторінки", "назва", "рік"),
                new CsvExporter<>(BookRecord.class).header());
    }

    @Test
    void emptyListGivesHeaderOnly() throws DataStorageException {
        assertEquals("автор,жанр,сторінки,назва,рік" + NL, new CsvExporter<>(BookRecord.class).format(List.of()));
    }

    @Test
    void bookRecordIsExported() throws DataStorageException {
        String csv = new CsvExporter<>(BookRecord.class)
                .format(List.of(new BookRecord("Кобзар", "Тарас Шевченко", 1840, 320, "поезія")));
        assertEquals("автор,жанр,сторінки,назва,рік" + NL + "Тарас Шевченко,поезія,320,Кобзар,1840" + NL, csv);
    }

    /* --- Універсальність --- */

    @Test
    void exporterWorksWithAnotherClass() throws DataStorageException {
        String csv = new CsvExporter<>(Point.class).format(List.of(new Point(3, "А"), new Point(-1, null)));
        // null записується порожнім полем; неанотоване поле не експортується.
        assertEquals("підпис,x" + NL + "А,3" + NL + ",-1" + NL, csv);
    }

    @Test
    void inheritedFieldsAreExported() throws DataStorageException {
        String csv = new CsvExporter<>(NamedPoint.class).format(List.of(new NamedPoint(1, "м", "точка")));
        assertEquals("підпис,назва,x" + NL + "м,точка,1" + NL, csv);
    }

    @Test
    void listOfSubtypeIsAccepted() throws DataStorageException {
        // List<? extends T>: список підкласів експортується експортером надкласу.
        List<NamedPoint> points = List.of(new NamedPoint(5, "п", "ім'я"));
        assertEquals("підпис,x" + NL + "п,5" + NL, new CsvExporter<>(Point.class).format(points));
    }

    @Test
    void classWithoutAnnotatedFieldsIsRejected() {
        DataStorageException error = assertThrows(DataStorageException.class,
                () -> new CsvExporter<>(Plain.class).format(List.of(new Plain())));
        assertTrue(error.getMessage().contains("не має полів з @CsvColumn"), error.getMessage());
    }

    /* --- Екранування --- */

    @Test
    void plainValueIsNotQuoted() {
        assertEquals("Кобзар", CsvExporter.escape("Кобзар"));
    }

    @Test
    void commaQuoteAndLineBreaksAreEscaped() {
        assertEquals("\"Хіба ревуть воли, як ясла повні?\"", CsvExporter.escape("Хіба ревуть воли, як ясла повні?"));
        assertEquals("\"Повість \"\"Intermezzo\"\"\"", CsvExporter.escape("Повість \"Intermezzo\""));
        assertEquals("\"рядок 1\nрядок 2\"", CsvExporter.escape("рядок 1\nрядок 2"));
        assertEquals("\"а\r\nб\"", CsvExporter.escape("а\r\nб"));
    }

    /* --- Запис у файл --- */

    @Test
    void writeUsesUtf8AndCreatesDirectories() throws DataStorageException, IOException {
        Path file = tempDir.resolve(Path.of("export", "nested", "books.csv"));
        new CsvExporter<>(BookRecord.class).write(file,
                List.of(new BookRecord("Їжак", "Ґанок", 2020, 1, "є")));
        byte[] bytes = Files.readAllBytes(file);
        assertEquals("автор,жанр,сторінки,назва,рік" + NL + "Ґанок,є,1,Їжак,2020" + NL,
                new String(bytes, StandardCharsets.UTF_8));
    }

    @Test
    void writeReplacesOldContent() throws DataStorageException, IOException {
        Path file = tempDir.resolve("books.csv");
        CsvExporter<BookRecord> exporter = new CsvExporter<>(BookRecord.class);
        exporter.write(file, List.of(
                new BookRecord("А", "Б", 2000, 1, "В"), new BookRecord("Г", "Д", 2001, 2, "Е")));
        exporter.write(file, List.of());
        // TRUNCATE_EXISTING: старі записи не лишилися після коротшого експорту.
        assertEquals("автор,жанр,сторінки,назва,рік" + NL, Files.readString(file, StandardCharsets.UTF_8));
    }

    @Test
    void fileErrorKeepsIoExceptionAsCause() throws IOException {
        // Батьківський «каталог» – звичайний файл, тому створити файл усередині неможливо на будь-якій ОС.
        Path notADirectory = Files.writeString(tempDir.resolve("file.txt"), "x", StandardCharsets.UTF_8);
        DataStorageException error = assertThrows(DataStorageException.class,
                () -> new CsvExporter<>(BookRecord.class).write(notADirectory.resolve("books.csv"), List.of()));
        assertInstanceOf(IOException.class, error.getCause());
        assertTrue(error.getMessage().contains("books.csv"), error.getMessage());
    }

    @Test
    void exporterDoesNotModifyRecordsOrList() throws DataStorageException {
        Point point = new Point(1, "а");
        List<Point> points = List.of(point);
        new CsvExporter<>(Point.class).format(points);
        assertEquals("не експортується", point.secret());
        assertEquals(List.of(point), points);
    }
}

package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Перевіряє послідовність «об'єкт → CSV → об'єкт» і помилки читання. */
class RoundTripTest {
    private final CsvExporter<BookRecord> exporter = new CsvExporter<>(BookRecord.class);

    @TempDir
    Path tempDir;

    /* Книги з контрольного файла, як їх отримує Main: коректні рядки → сховище → записи. */
    private static List<BookRecord> recordsFrom(Path input) throws IOException {
        Repository<Book> repository = new Repository<>();
        FileReport.readLines(input).stream()
                .filter(line -> BookParser.validate(line) == null)
                .map(Book::fromCsv)
                .forEach(repository::add);
        return repository.find(BookRecord::isExportable).stream().map(BookRecord::from).toList();
    }

    @Test
    void libraryRecordsSurviveRoundTrip() throws IOException, DataStorageException {
        List<BookRecord> original = recordsFrom(Path.of("data", "library.csv"));
        Path file = tempDir.resolve("books.csv");

        exporter.write(file, original);
        List<BookRecord> restored = BookRecord.readCsv(file);

        // 6 книг із роком і жанром; рядок старого формату й рядок з AUDIO не експортуються.
        assertEquals(6, original.size());
        assertEquals(original, restored);
        // Порядок записів теж збережено, а не лише склад.
        assertEquals(original.get(1).title(), restored.get(1).title());
        // Заголовок і 6 записів – 7 фізичних рядків.
        assertEquals(7, Files.readAllLines(file, StandardCharsets.UTF_8).size());
    }

    @Test
    void commaQuotesAndLineBreaksSurviveRoundTrip() throws DataStorageException, IOException {
        List<BookRecord> original = List.of(
                new BookRecord("Хіба ревуть воли, як ясла повні?", "Панас Мирний", 1880, 400, "роман"),
                new BookRecord("Повість \"Intermezzo\"", "Михайло Коцюбинський", 1908, 32, "новела"),
                new BookRecord("Назва\nу два рядки", "Автор, \"з лапками\"", 2020, 10, "жанр\r\nз CRLF"));
        Path file = tempDir.resolve("special.csv");

        exporter.write(file, original);

        assertEquals(original, BookRecord.readCsv(file));
        // Перенесення всередині полів дають більше фізичних рядків, ніж записів.
        long physicalLines = Files.readString(file, StandardCharsets.UTF_8).lines().count();
        assertTrue(physicalLines > original.size() + 1, "фізичних рядків: " + physicalLines);
    }

    @Test
    void emptyListSurvivesRoundTrip() throws DataStorageException {
        Path file = tempDir.resolve("empty-list.csv");
        exporter.write(file, List.of());
        assertEquals(List.of(), BookRecord.readCsv(file));
    }

    @Test
    void emptyFileHasNoRecords() throws DataStorageException, IOException {
        Path file = Files.writeString(tempDir.resolve("empty.csv"), "", StandardCharsets.UTF_8);
        assertEquals(List.of(), BookRecord.readCsv(file));
    }

    @Test
    void unexpectedHeaderIsRejected() throws IOException {
        Path file = Files.writeString(tempDir.resolve("header.csv"), "title,author\nКобзар,Шевченко\n",
                StandardCharsets.UTF_8);
        DataStorageException error = assertThrows(DataStorageException.class, () -> BookRecord.readCsv(file));
        assertTrue(error.getMessage().contains("очікується заголовок"), error.getMessage());
    }

    @Test
    void wrongFieldCountIsRejected() throws IOException {
        Path file = Files.writeString(tempDir.resolve("count.csv"),
                "автор,жанр,сторінки,назва,рік\nТарас Шевченко,поезія,320,Кобзар\n", StandardCharsets.UTF_8);
        DataStorageException error = assertThrows(DataStorageException.class, () -> BookRecord.readCsv(file));
        assertTrue(error.getMessage().endsWith("запис 1: очікується 5 полів, отримано 4"), error.getMessage());
    }

    @Test
    void wrongNumberInFileKeepsCause() throws IOException {
        Path file = Files.writeString(tempDir.resolve("number.csv"),
                "автор,жанр,сторінки,назва,рік\nТарас Шевченко,поезія,320,Кобзар,1840\nА,Б,1,В,тисяча\n",
                StandardCharsets.UTF_8);
        DataStorageException error = assertThrows(DataStorageException.class, () -> BookRecord.readCsv(file));
        assertEquals("запис 2: стовпець «рік» не є цілим числом: \"тисяча\"", error.getMessage());
        assertInstanceOf(NumberFormatException.class, error.getCause());
    }

    @Test
    void missingFileKeepsCause() {
        DataStorageException error = assertThrows(DataStorageException.class,
                () -> BookRecord.readCsv(tempDir.resolve("немає.csv")));
        assertInstanceOf(NoSuchFileException.class, error.getCause());
    }

    @Test
    void invalidUtf8KeepsCause() throws IOException {
        // Байт 0xFF не може з'явитися в UTF-8, тому читання з явним UTF-8 має впасти.
        Path file = Files.write(tempDir.resolve("latin1.csv"), new byte[] {'a', ',', (byte) 0xFF, '\n'});
        DataStorageException error = assertThrows(DataStorageException.class, () -> BookRecord.readCsv(file));
        assertInstanceOf(MalformedInputException.class, error.getCause());
    }
}

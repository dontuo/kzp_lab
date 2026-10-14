package ua.lpnu.kzp;

import java.nio.file.Path;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Плоский запис книги для обміну через CSV (варіант 1 «Бібліотека»).
 *
 * <p>Містить рівно поля варіанта: назву, автора, рік, сторінки й жанр. Поля позначено
 * {@link CsvColumn}, тому {@link CsvExporter} записує їх без жодного знання про цей клас.
 * Як {@code record} клас незмінний, а {@code equals}/{@code hashCode} порівнюють усі
 * поля – саме це потрібно для перевірки round-trip.
 *
 * @param title назва, не порожня
 * @param author автор, не порожній
 * @param year рік видання, від 1450 до поточного
 * @param pages кількість сторінок, більша за нуль
 * @param genre жанр, не порожній
 */
public record BookRecord(
        @CsvColumn(BookRecord.TITLE) String title,
        @CsvColumn(BookRecord.AUTHOR) String author,
        @CsvColumn(BookRecord.YEAR) int year,
        @CsvColumn(BookRecord.PAGES) int pages,
        @CsvColumn(BookRecord.GENRE) String genre) {

    /** Назва стовпця з назвою книги. */
    public static final String TITLE = "назва";
    /** Назва стовпця з автором. */
    public static final String AUTHOR = "автор";
    /** Назва стовпця з роком видання. */
    public static final String YEAR = "рік";
    /** Назва стовпця з кількістю сторінок. */
    public static final String PAGES = "сторінки";
    /** Назва стовпця з жанром. */
    public static final String GENRE = "жанр";

    /**
     * Перевіряє інваріанти запису.
     *
     * @throws NullPointerException якщо текстове поле {@code null}
     * @throws IllegalArgumentException якщо текстове поле порожнє, сторінок не більше нуля
     *     або рік поза межами
     */
    public BookRecord {
        Objects.requireNonNull(title, "назва книги не може бути null");
        Objects.requireNonNull(author, "автор не може бути null");
        Objects.requireNonNull(genre, "жанр не може бути null");
        if (title.isBlank() || author.isBlank() || genre.isBlank()) {
            throw new IllegalArgumentException("назва, автор і жанр не можуть бути порожніми");
        }
        if (pages <= 0) {
            throw new IllegalArgumentException(
                    String.format(Locale.ROOT, "кількість сторінок має бути додатною: %d", pages));
        }
        int currentYear = Year.now().getValue();
        if (year < Book.FIRST_PRINTED_YEAR || year > currentYear) {
            throw new IllegalArgumentException(String.format(Locale.ROOT,
                    "рік видання має бути від %d до %d: %d", Book.FIRST_PRINTED_YEAR, currentYear, year));
        }
    }

    /**
     * Створює запис із книги каталогу.
     *
     * @param book книга з відомими роком і жанром
     * @return запис із полями варіанта
     * @throws IllegalArgumentException якщо в книги немає року або жанру
     */
    public static BookRecord from(Book book) {
        return new BookRecord(book.getTitle(), book.getAuthor(),
                book.getYear().orElseThrow(() -> new IllegalArgumentException(
                        "книга без року видання не експортується: " + book.getTitle())),
                book.getPages(),
                book.getGenre().orElseThrow(() -> new IllegalArgumentException(
                        "книга без жанру не експортується: " + book.getTitle())));
    }

    /**
     * Перевіряє, чи можна експортувати книгу: для запису потрібні рік і жанр.
     *
     * @param book книга каталогу
     * @return {@code true}, якщо рік і жанр відомі
     */
    public static boolean isExportable(Book book) {
        return book.getYear().isPresent() && book.getGenre().isPresent();
    }

    /**
     * Відновлює запис зі значень одного CSV-запису.
     *
     * @param values значення за назвами стовпців
     * @param number номер запису (з 1), для повідомлення про помилку
     * @return відновлений запис
     * @throws DataStorageException якщо стовпця немає, число має хибний формат або
     *     значення порушують інваріанти
     */
    public static BookRecord fromCsv(Map<String, String> values, int number) throws DataStorageException {
        String title = column(values, TITLE, number);
        String author = column(values, AUTHOR, number);
        int year = number(values, YEAR, number);
        int pages = number(values, PAGES, number);
        String genre = column(values, GENRE, number);
        try {
            return new BookRecord(title, author, year, pages, genre);
        } catch (IllegalArgumentException exception) {
            throw new DataStorageException(String.format(Locale.ROOT, "запис %d: %s", number,
                    exception.getMessage()), exception);
        }
    }

    /**
     * Читає записи з CSV-файла, створеного {@link CsvExporter} для цього класу.
     *
     * @param path шлях до файла
     * @return записи у порядку файла; для порожнього файла – порожній список
     * @throws DataStorageException якщо файл неможливо прочитати, заголовок не збігається
     *     з очікуваним або запис неможливо відновити
     */
    public static List<BookRecord> readCsv(Path path) throws DataStorageException {
        CsvParser.Table table = CsvParser.readTable(path);
        if (table.header().isEmpty()) {
            return List.of();
        }
        List<String> expected = new CsvExporter<>(BookRecord.class).header();
        if (!table.header().equals(expected)) {
            throw new DataStorageException(path + ": очікується заголовок " + expected
                    + ", отримано " + table.header());
        }
        List<BookRecord> records = new ArrayList<>(table.rows().size());
        // Цикл, а не потік: fromCsv кидає перевірюваний DataStorageException і потребує номера запису.
        for (int index = 0; index < table.rows().size(); index++) {
            records.add(fromCsv(table.rows().get(index), index + 1));
        }
        return records;
    }

    /* Значення стовпця або помилка, якщо стовпця немає. */
    private static String column(Map<String, String> values, String name, int number) throws DataStorageException {
        String value = values.get(name);
        if (value == null) {
            throw new DataStorageException(String.format(Locale.ROOT, "запис %d: немає стовпця «%s»", number, name));
        }
        return value;
    }

    /* Ціле число зі стовпця; хибний формат зберігається як cause. */
    private static int number(Map<String, String> values, String name, int number) throws DataStorageException {
        String text = column(values, name, number);
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException exception) {
            throw new DataStorageException(String.format(Locale.ROOT,
                    "запис %d: стовпець «%s» не є цілим числом: \"%s\"", number, name, text), exception);
        }
    }
}

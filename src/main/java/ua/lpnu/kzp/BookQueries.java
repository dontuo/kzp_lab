package ua.lpnu.kzp;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Запити Stream API до колекції книг (варіант 1 «Бібліотека»).
 *
 * <p>Кожен метод створює новий потік із переданого списку, не змінює ні список, ні
 * книги й повертає один результат: список, карту, статистику або {@link Optional}.
 * Книги обробляються через спільний тип {@link Book}, без перевірок підтипу.
 */
public final class BookQueries {
    /** Рік, починаючи з якого книга вважається новою (включно). */
    public static final int RECENT_YEAR = 2020;
    /** Кількість книг у рейтингу за сторінками. */
    public static final int TOP_SIZE = 5;

    /**
     * Порядок рейтингу: спершу більше сторінок, за однакової кількості – назва за абеткою.
     *
     * <p>{@code reversed()} застосовано лише до першого ключа: якби він стояв після
     * {@code thenComparing}, назви теж сортувалися б у зворотному порядку.
     */
    public static final Comparator<Book> BY_PAGES_DESC_THEN_TITLE =
            Comparator.comparingInt(Book::getPages).reversed().thenComparing(Book::getTitle);

    /* Забороняє створення екземплярів службового класу. */
    private BookQueries() {
    }

    /**
     * Запит 1 (filter): книги, видані в році {@code fromYear} або пізніше.
     *
     * <p>Книги без року (рядки старого формату) не потрапляють до результату: про них
     * невідомо, чи вони нові.
     *
     * @param books книги
     * @param fromYear перший рік, що входить до результату
     * @return книги в порядку вхідного списку
     */
    public static List<Book> publishedSince(List<Book> books, int fromYear) {
        return books.stream()
                .filter(book -> book.getYear().isPresent() && book.getYear().getAsInt() >= fromYear)
                .toList();
    }

    /**
     * Запит 2 (map): назви книг.
     *
     * @param books книги
     * @return назви в порядку вхідного списку; однакові назви не об'єднуються
     */
    public static List<String> titles(List<Book> books) {
        return books.stream()
                .map(Book::getTitle)
                .toList();
    }

    /**
     * Запит 3 (groupingBy): кількість книг кожного жанру.
     *
     * <p>Книги без жанру не враховуються. {@link TreeMap} упорядковує жанри за
     * абеткою, тому звіт не залежить від порядку рядків у файлі.
     *
     * @param books книги
     * @return карта «жанр → кількість книг»; порожня, якщо жанрів немає
     */
    public static Map<String, Long> countByGenre(List<Book> books) {
        return books.stream()
                .filter(book -> book.getGenre().isPresent())
                .collect(Collectors.groupingBy(book -> book.getGenre().orElseThrow(),
                        TreeMap::new, Collectors.counting()));
    }

    /**
     * Запит 4 (summarizingInt): статистика кількості сторінок.
     *
     * <p>Для порожнього списку кількість і сума дорівнюють 0, середнє – 0.0, а мінімум і
     * максимум – {@link Integer#MAX_VALUE} і {@link Integer#MIN_VALUE}; тому перед
     * виведенням мінімуму й максимуму треба перевіряти {@code getCount() > 0}.
     *
     * @param books книги
     * @return кількість, сума, мінімум, максимум і середнє сторінок
     */
    public static IntSummaryStatistics pageStatistics(List<Book> books) {
        return books.stream()
                .collect(Collectors.summarizingInt(Book::getPages));
    }

    /**
     * Запит 5 (sorted + limit): перші {@code limit} книг за {@link #BY_PAGES_DESC_THEN_TITLE}.
     *
     * @param books книги
     * @param limit скільки книг повернути; {@code 0} дає порожній список, більше за
     *     розмір списку – усі книги
     * @return упорядкована копія; вхідний список не змінюється
     * @throws IllegalArgumentException якщо {@code limit} від'ємний
     */
    public static List<Book> topByPages(List<Book> books, int limit) {
        if (limit < 0) {
            throw new IllegalArgumentException("розмір рейтингу не може бути від'ємним: " + limit);
        }
        // Спершу sorted, потім limit: інакше відібралися б перші N книг файла, а не найбільші.
        return books.stream()
                .sorted(BY_PAGES_DESC_THEN_TITLE)
                .limit(limit)
                .toList();
    }

    /**
     * Пошук книги за точною назвою.
     *
     * @param books книги
     * @param title назва, яку шукаємо
     * @return перша книга з такою назвою в порядку списку або {@link Optional#empty()}
     */
    public static Optional<Book> findByTitle(List<Book> books, String title) {
        Objects.requireNonNull(title, "назва для пошуку не може бути null");
        return books.stream()
                .filter(book -> book.getTitle().equals(title))
                .findFirst();
    }

    /**
     * Дворівневе групування: кількість книг за форматом, а всередині – за жанром.
     *
     * @param books книги
     * @return карта «формат → (жанр → кількість)»; книги без жанру не враховуються
     */
    public static Map<BookFormat, Map<String, Long>> countByFormatAndGenre(List<Book> books) {
        return books.stream()
                .filter(book -> book.getGenre().isPresent())
                .collect(Collectors.groupingBy(Book::getFormat,
                        () -> new EnumMap<>(BookFormat.class),
                        Collectors.groupingBy(book -> book.getGenre().orElseThrow(),
                                TreeMap::new, Collectors.counting())));
    }
}

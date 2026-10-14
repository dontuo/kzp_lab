package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class BookQueriesTest {
    /* Допустима похибка для порівняння чисел із рухомою крапкою. */
    private static final double DELTA = 0.0001;

    private static final Book KOBZAR = Book.fromCsv("Кобзар;Тарас Шевченко;320;250.00;PRINTED;1840;поезія");
    private static final Book AMADOKA = Book.fromCsv("Амадока;Софія Андрухович;832;350.00;ELECTRONIC;2020;роман");
    private static final Book DOTSIA = Book.fromCsv("Доця;Тамара Горіха Зерня;368;120.00;ELECTRONIC;2019;роман");
    private static final Book RECENT = Book.fromCsv("Нова;Автор;100;10.00;PRINTED;2021;роман");
    private static final Book LEGACY = Book.fromCsv("Захар Беркут;Іван Франко;224;175.00");

    private static final List<Book> BOOKS = List.of(KOBZAR, AMADOKA, DOTSIA, RECENT, LEGACY);

    /* --- 1. filter --- */

    @Test
    void publishedSinceKeepsBoundaryYearAndOrder() {
        // 2020 входить (>=), 2019 – ні; порядок вхідного списку зберігається.
        assertEquals(List.of(AMADOKA, RECENT), BookQueries.publishedSince(BOOKS, 2020));
    }

    @Test
    void publishedSinceSkipsBooksWithoutYear() {
        assertEquals(List.of(), BookQueries.publishedSince(List.of(LEGACY), Book.FIRST_PRINTED_YEAR));
    }

    @Test
    void publishedSinceOnEmptyListIsEmpty() {
        assertEquals(List.of(), BookQueries.publishedSince(List.of(), 2020));
    }

    /* --- 2. map --- */

    @Test
    void titlesKeepOrderAndDuplicates() {
        assertEquals(List.of("Кобзар", "Амадока", "Кобзар"), BookQueries.titles(List.of(KOBZAR, AMADOKA, KOBZAR)));
    }

    @Test
    void titlesOfEmptyListAreEmpty() {
        assertEquals(List.of(), BookQueries.titles(List.of()));
    }

    /* --- 3. groupingBy --- */

    @Test
    void countByGenreCountsDuplicatesAndSortsKeys() {
        Map<String, Long> counts = BookQueries.countByGenre(List.of(AMADOKA, KOBZAR, DOTSIA, AMADOKA, LEGACY));
        // Ключі за абеткою; дублікат рахується двічі; книга без жанру не враховується.
        assertEquals(List.of("поезія", "роман"), List.copyOf(counts.keySet()));
        assertEquals(1L, counts.get("поезія"));
        assertEquals(3L, counts.get("роман"));
    }

    @Test
    void countByGenreOfEmptyListIsEmpty() {
        assertTrue(BookQueries.countByGenre(List.of()).isEmpty());
    }

    /* --- 4. статистика --- */

    @Test
    void pageStatisticsSummarizesPages() {
        IntSummaryStatistics statistics = BookQueries.pageStatistics(List.of(KOBZAR, AMADOKA, DOTSIA));
        assertEquals(3, statistics.getCount());
        assertEquals(320, statistics.getMin());
        assertEquals(832, statistics.getMax());
        assertEquals(1520, statistics.getSum());
        assertEquals(1520 / 3.0, statistics.getAverage(), DELTA);
    }

    @Test
    void pageStatisticsOfEmptyListHasNeutralValues() {
        IntSummaryStatistics statistics = BookQueries.pageStatistics(List.of());
        assertEquals(0, statistics.getCount());
        assertEquals(0, statistics.getSum());
        assertEquals(0.0, statistics.getAverage(), DELTA);
        // Мінімум і максимум порожньої статистики – крайні значення int, а не 0.
        assertEquals(Integer.MAX_VALUE, statistics.getMin());
        assertEquals(Integer.MIN_VALUE, statistics.getMax());
    }

    /* --- 5. sorted + limit --- */

    @Test
    void topByPagesSortsDescending() {
        assertEquals(List.of(AMADOKA, DOTSIA, KOBZAR), BookQueries.topByPages(BOOKS, 3));
    }

    @Test
    void equalPagesAreOrderedByTitle() {
        Book b = new PrintedBook("Б", "Автор", 300, 1.0);
        Book a = new PrintedBook("А", "Автор", 300, 1.0);
        Book c = new PrintedBook("В", "Автор", 300, 1.0);
        // Основний ключ однаковий, тому вирішує другий: назва за зростанням, а не за спаданням.
        assertEquals(List.of(a, b, c), BookQueries.topByPages(List.of(b, c, a), 3));
    }

    @Test
    void topByPagesKeepsDuplicates() {
        assertEquals(List.of(AMADOKA, AMADOKA, KOBZAR), BookQueries.topByPages(List.of(KOBZAR, AMADOKA, AMADOKA), 3));
    }

    @Test
    void zeroLimitGivesEmptyList() {
        assertEquals(List.of(), BookQueries.topByPages(BOOKS, 0));
    }

    @Test
    void limitLargerThanListGivesAllBooks() {
        assertEquals(BOOKS.size(), BookQueries.topByPages(BOOKS, 100).size());
    }

    @Test
    void negativeLimitIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> BookQueries.topByPages(BOOKS, -1));
    }

    /* --- Optional --- */

    @Test
    void findByTitleReturnsBook() {
        assertEquals(Optional.of(AMADOKA), BookQueries.findByTitle(BOOKS, "Амадока"));
    }

    @Test
    void findByTitleReturnsEmptyWhenMissing() {
        Optional<Book> result = BookQueries.findByTitle(BOOKS, "Немає такої книги");
        assertTrue(result.isEmpty());
        assertEquals("не знайдено", result.map(Book::getTitle).orElse("не знайдено"));
    }

    @Test
    void findByTitleOnEmptyListIsEmpty() {
        assertTrue(BookQueries.findByTitle(List.of(), "Кобзар").isEmpty());
    }

    @Test
    void findByTitleReturnsFirstOfDuplicates() {
        Book electronicKobzar = new ElectronicBook("Кобзар", "Тарас Шевченко", 320, 99.0, 1840, "поезія");
        assertSame(KOBZAR, BookQueries.findByTitle(List.of(KOBZAR, electronicKobzar), "Кобзар").orElseThrow());
    }

    @Test
    void findByTitleIsExact() {
        assertTrue(BookQueries.findByTitle(BOOKS, "кобзар").isEmpty());
        assertTrue(BookQueries.findByTitle(BOOKS, "Кобзар ").isEmpty());
    }

    /* --- Дворівневе групування --- */

    @Test
    void countByFormatAndGenreGroupsTwice() {
        Map<BookFormat, Map<String, Long>> groups = BookQueries.countByFormatAndGenre(BOOKS);
        assertEquals(List.of(BookFormat.PRINTED, BookFormat.ELECTRONIC), List.copyOf(groups.keySet()));
        assertEquals(Map.of("поезія", 1L, "роман", 1L), groups.get(BookFormat.PRINTED));
        assertEquals(Map.of("роман", 2L), groups.get(BookFormat.ELECTRONIC));
    }

    /* --- Відсутність побічних ефектів --- */

    @Test
    void queriesDoNotModifyInputList() {
        List<Book> input = new ArrayList<>(List.of(LEGACY, KOBZAR, RECENT, DOTSIA, AMADOKA));
        List<Book> before = List.copyOf(input);

        BookQueries.publishedSince(input, 2020);
        BookQueries.titles(input);
        BookQueries.countByGenre(input);
        BookQueries.pageStatistics(input);
        BookQueries.topByPages(input, 3);
        BookQueries.findByTitle(input, "Доця");
        BookQueries.countByFormatAndGenre(input);
        CatalogSummary.of(input);

        // Ні склад, ні порядок вхідного списку не змінилися, хоча top-N сортує.
        assertEquals(before, input);
    }

    @Test
    void resultListsAreUnmodifiable() {
        List<Book> top = BookQueries.topByPages(BOOKS, 2);
        assertThrows(UnsupportedOperationException.class, () -> top.add(KOBZAR));
    }
}

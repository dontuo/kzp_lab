package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Path;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Порівнює потокові реалізації з цикловими ({@link LoopReference}) на тих самих даних.
 */
class StreamEquivalenceTest {
    /* Допустима похибка для порівняння чисел із рухомою крапкою. */
    private static final double DELTA = 0.0001;

    /* Набори даних: обидва контрольні файли, порожній список і набір із дублікатами й однаковою ціною. */
    static Stream<List<Book>> datasets() throws IOException {
        return Stream.of(
                load(Path.of("data", "input.csv")),
                load(Path.of("data", "books.csv")),
                List.of(),
                List.of(
                        Book.fromCsv("Б;Автор;300;50.00;PRINTED;2020;роман"),
                        Book.fromCsv("А;Автор;300;50.00;ELECTRONIC;2021;роман"),
                        Book.fromCsv("Б;Автор;300;50.00;PRINTED;2020;роман"),
                        Book.fromCsv("В;Автор;10;0.00")));
    }

    /* Коректні книги з файла – так само, як їх відбирає Main. */
    private static List<Book> load(Path file) throws IOException {
        return FileReport.readLines(file).stream()
                .filter(line -> BookParser.validate(line) == null)
                .map(Book::fromCsv)
                .toList();
    }

    @ParameterizedTest
    @MethodSource("datasets")
    void summaryMatchesLoop(List<Book> books) {
        LoopReference.Summary loop = LoopReference.summary(books);
        CatalogSummary stream = CatalogSummary.of(books);

        assertEquals(loop.count(), stream.getCount());
        assertEquals(loop.totals(), stream.getTotals());
        assertEquals(loop.mostExpensiveTitle(), stream.getMostExpensiveTitle());
        assertEquals(loop.maxPrice(), stream.getMaxPrice(), DELTA);
        assertEquals(loop.byFormat(), stream.getByFormat());
    }

    @ParameterizedTest
    @MethodSource("datasets")
    void queriesMatchLoop(List<Book> books) {
        assertEquals(LoopReference.publishedSince(books, BookQueries.RECENT_YEAR),
                BookQueries.publishedSince(books, BookQueries.RECENT_YEAR));
        assertEquals(LoopReference.titles(books), BookQueries.titles(books));
        assertEquals(LoopReference.countByGenre(books), BookQueries.countByGenre(books));
        assertEquals(LoopReference.topByPages(books, BookQueries.TOP_SIZE),
                BookQueries.topByPages(books, BookQueries.TOP_SIZE));
        assertEquals(LoopReference.findByTitle(books, "Амадока"),
                BookQueries.findByTitle(books, "Амадока").orElse(null));
    }

    @ParameterizedTest
    @MethodSource("datasets")
    void statisticsMatchLoop(List<Book> books) {
        int[] loop = LoopReference.pageMinMaxSum(books);
        IntSummaryStatistics stream = BookQueries.pageStatistics(books);

        assertEquals(books.size(), stream.getCount());
        assertEquals(loop[0], stream.getMin());
        assertEquals(loop[1], stream.getMax());
        assertEquals(loop[2], stream.getSum());
    }
}

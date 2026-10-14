package ua.lpnu.kzp;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Циклові реалізації показників і запитів – еталон для перевірки еквівалентності.
 *
 * <p>Обчислення показників перенесено без змін із {@code CatalogSummary} версії 3.0.0
 * (тег {@code v3.0.0}), де вони рахувалися циклом. Запити написано тими самими
 * циклами, якими їх реалізували б без Stream API.
 */
final class LoopReference {
    /* Забороняє створення екземплярів службового класу. */
    private LoopReference() {
    }

    /* Показники в тому вигляді, як їх рахував цикл у v3.0.0. */
    record Summary(int count, PagePrice totals, String mostExpensiveTitle, double maxPrice,
            Map<BookFormat, FormatTotals> byFormat) {
    }

    /* Цикл із CatalogSummary.add версії 3.0.0. */
    static Summary summary(List<Book> books) {
        int count = 0;
        PagePrice totals = PagePrice.ZERO;
        Book mostExpensive = null;
        Map<BookFormat, FormatTotals> byFormat = new EnumMap<>(BookFormat.class);
        for (BookFormat format : BookFormat.values()) {
            byFormat.put(format, FormatTotals.ZERO);
        }
        for (Book book : books) {
            count++;
            totals = totals.plus(book.pagePrice());
            if (mostExpensive == null || book.getPrice() > mostExpensive.getPrice()) {
                mostExpensive = book;
            }
            byFormat.put(book.getFormat(), byFormat.get(book.getFormat()).plus(book));
        }
        return new Summary(count, totals, mostExpensive == null ? null : mostExpensive.getTitle(),
                mostExpensive == null ? 0.0 : mostExpensive.getPrice(), byFormat);
    }

    static List<Book> publishedSince(List<Book> books, int fromYear) {
        List<Book> result = new ArrayList<>();
        for (Book book : books) {
            if (book.getYear().isPresent() && book.getYear().getAsInt() >= fromYear) {
                result.add(book);
            }
        }
        return result;
    }

    static List<String> titles(List<Book> books) {
        List<String> result = new ArrayList<>();
        for (Book book : books) {
            result.add(book.getTitle());
        }
        return result;
    }

    static Map<String, Long> countByGenre(List<Book> books) {
        Map<String, Long> result = new TreeMap<>();
        for (Book book : books) {
            if (book.getGenre().isPresent()) {
                result.merge(book.getGenre().get(), 1L, Long::sum);
            }
        }
        return result;
    }

    /* Мінімум, максимум і сума сторінок; середнє рахує тест. */
    static int[] pageMinMaxSum(List<Book> books) {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        int sum = 0;
        for (Book book : books) {
            min = Math.min(min, book.getPages());
            max = Math.max(max, book.getPages());
            sum += book.getPages();
        }
        return new int[] {min, max, sum};
    }

    /* Сортування копії та відбір перших N – так top-N писали до Stream API. */
    static List<Book> topByPages(List<Book> books, int limit) {
        List<Book> copy = new ArrayList<>(books);
        copy.sort(BookQueries.BY_PAGES_DESC_THEN_TITLE);
        return new ArrayList<>(copy.subList(0, Math.min(limit, copy.size())));
    }

    /* Пошук із поверненням null, який Optional замінює явним «немає результату». */
    static Book findByTitle(List<Book> books, String title) {
        for (Book book : books) {
            if (book.getTitle().equals(title)) {
                return book;
            }
        }
        return null;
    }
}

package ua.lpnu.kzp;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Чотири показники каталогу – кількість книг, середня кількість сторінок, найдорожча
 * книга та сумарна вартість – і підсумки за форматом видання.
 *
 * <p>Об'єкт незмінний: усі показники обчислюються потоками один раз у {@link #of(List)}.
 * Книги обробляються через спільний тип {@link Book}, без перевірок підтипу.
 */
public final class CatalogSummary {
    private final int count;
    private final PagePrice totals;
    // null, якщо книг немає.
    private final Book mostExpensive;
    private final Map<BookFormat, FormatTotals> byFormat;

    /* Створюється лише через of(List). */
    private CatalogSummary(int count, PagePrice totals, Book mostExpensive, Map<BookFormat, FormatTotals> byFormat) {
        this.count = count;
        this.totals = totals;
        this.mostExpensive = mostExpensive;
        this.byFormat = byFormat;
    }

    /**
     * Обчислює показники для списку книг; список не змінюється.
     *
     * @param books коректні книги, можливо порожній список
     * @return підсумок
     */
    public static CatalogSummary of(List<Book> books) {
        // reduce починає з нуля, тому порожній список дає PagePrice.ZERO.
        PagePrice totals = books.stream()
                .map(Book::pagePrice)
                .reduce(PagePrice.ZERO, PagePrice::plus);
        // max за однакової ціни повертає першу з книг, як і строге «>» у циклі ЛР № 1–3.
        Book mostExpensive = books.stream()
                .max(Comparator.comparingDouble(Book::getPrice))
                .orElse(null);
        // Рядок для кожного формату, навіть без книг; EnumMap тримає порядок констант.
        Map<BookFormat, FormatTotals> byFormat = Arrays.stream(BookFormat.values())
                .collect(Collectors.toMap(Function.identity(), format -> totalsFor(books, format),
                        (first, second) -> first, () -> new EnumMap<>(BookFormat.class)));
        return new CatalogSummary(books.size(), totals, mostExpensive, byFormat);
    }

    /* Підсумок одного формату; purchaseCost() усередині FormatTotals.of – поліморфний виклик. */
    private static FormatTotals totalsFor(List<Book> books, BookFormat format) {
        return books.stream()
                .filter(book -> book.getFormat() == format)
                .map(FormatTotals::of)
                .reduce(FormatTotals.ZERO, FormatTotals::plus);
    }

    /**
     * Повертає підсумки для кожного формату видання в порядку оголошення {@link BookFormat}.
     *
     * @return незмінна копія; формати без книг мають {@link FormatTotals#ZERO}
     */
    public Map<BookFormat, FormatTotals> getByFormat() {
        return Collections.unmodifiableMap(new EnumMap<>(byFormat));
    }

    /**
     * Повертає кількість коректних записів.
     *
     * @return кількість книг
     */
    public int getCount() {
        return count;
    }

    /**
     * Повертає суму сторінок і цін усіх книг.
     *
     * @return сумарне значення; {@link PagePrice#ZERO}, якщо книг немає
     */
    public PagePrice getTotals() {
        return totals;
    }

    /**
     * Повертає середню кількість сторінок.
     *
     * @return середнє значення або 0, якщо книг немає
     */
    public double getAveragePages() {
        // Окремо обробляємо порожній каталог, щоб не ділити на нуль.
        if (count == 0) {
            return 0.0;
        }
        // Приведення до double не дає цілочисловому діленню відкинути дробову частину.
        return (double) totals.pages() / count;
    }

    /**
     * Повертає назву найдорожчої книги.
     *
     * @return назва або {@code null}, якщо книг немає
     */
    public String getMostExpensiveTitle() {
        return mostExpensive == null ? null : mostExpensive.getTitle();
    }

    /**
     * Повертає ціну найдорожчої книги.
     *
     * @return найбільша ціна або 0, якщо книг немає
     */
    public double getMaxPrice() {
        return mostExpensive == null ? 0.0 : mostExpensive.getPrice();
    }

    /**
     * Повертає сумарну вартість каталогу.
     *
     * @return сума цін усіх книг
     */
    public double getTotalPrice() {
        return totals.price();
    }
}

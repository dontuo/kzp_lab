package ua.lpnu.kzp;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Накопичує чотири показники каталогу: кількість книг, середню кількість
 * сторінок, найдорожчу книгу та сумарну вартість, а також підсумки за форматом видання.
 *
 * <p>Клас обробляє будь-які підтипи {@link Book} через спільний тип і не перевіряє,
 * друкована книга чи електронна.
 */
public final class CatalogSummary {
    private int count;
    // Сума сторінок і цін усіх книг; PagePrice незмінний, тому замінюється цілком.
    private PagePrice totals = PagePrice.ZERO;
    private Book mostExpensive;
    // EnumMap зберігає ключі в порядку оголошення констант, тому звіт стабільний.
    private final Map<BookFormat, FormatTotals> byFormat = new EnumMap<>(BookFormat.class);

    /** Створює порожній підсумок із нульовими значеннями для кожного формату. */
    public CatalogSummary() {
        for (BookFormat format : BookFormat.values()) {
            byFormat.put(format, FormatTotals.ZERO);
        }
    }

    /**
     * Обчислює показники для списку книг.
     *
     * @param books коректні книги
     * @return заповнений підсумок
     */
    public static CatalogSummary of(List<Book> books) {
        CatalogSummary summary = new CatalogSummary();
        for (Book book : books) {
            summary.add(book);
        }
        return summary;
    }

    /**
     * Додає до показників одну книгу.
     *
     * @param book книга; її коректність гарантує конструктор {@link Book}
     */
    public void add(Book book) {
        count++;
        totals = totals.plus(book.pagePrice());
        // Строге порівняння: за однакової ціни лишається перша книга.
        if (mostExpensive == null || book.getPrice() > mostExpensive.getPrice()) {
            mostExpensive = book;
        }
        // getFormat() і purchaseCost() усередині plus – поліморфні виклики, без if за підтипом.
        byFormat.put(book.getFormat(), byFormat.get(book.getFormat()).plus(book));
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
     * @return кількість доданих книг
     */
    public int getCount() {
        return count;
    }

    /**
     * Повертає суму сторінок і цін усіх доданих книг.
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

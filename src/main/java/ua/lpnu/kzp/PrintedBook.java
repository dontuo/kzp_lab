package ua.lpnu.kzp;

/**
 * Друкована книга.
 *
 * <p>Відрізняється правилом вартості: до ціни додається доставка
 * {@value #DELIVERY_COST} грн, бо паперову книгу потрібно фізично доставити.
 */
public final class PrintedBook extends Book {
    /** Фіксована вартість доставки друкованої книги, грн. */
    public static final double DELIVERY_COST = 50.0;

    /**
     * Створює друковану книгу з відомими роком і жанром.
     *
     * @param title назва, не порожня
     * @param author автор, не порожній
     * @param pages кількість сторінок, більша за нуль
     * @param price ціна, скінченна й не від'ємна
     * @param year рік видання або {@code null}, якщо невідомий
     * @param genre жанр або {@code null}, якщо невідомий
     * @throws NullPointerException якщо назва або автор {@code null}
     * @throws IllegalArgumentException якщо порушено інваріант {@link Book}
     */
    public PrintedBook(String title, String author, int pages, double price, Integer year, String genre) {
        super(title, author, pages, price, year, genre);
    }

    /**
     * Створює друковану книгу без року й жанру (формат даних ЛР № 1–2).
     *
     * @param title назва, не порожня
     * @param author автор, не порожній
     * @param pages кількість сторінок, більша за нуль
     * @param price ціна, скінченна й не від'ємна
     * @throws NullPointerException якщо назва або автор {@code null}
     * @throws IllegalArgumentException якщо порушено інваріант {@link Book}
     */
    public PrintedBook(String title, String author, int pages, double price) {
        this(title, author, pages, price, null, null);
    }

    /**
     * Повертає формат друкованої книги.
     *
     * @return {@link BookFormat#PRINTED}
     */
    @Override
    public BookFormat getFormat() {
        return BookFormat.PRINTED;
    }

    /**
     * Обчислює вартість придбання: ціна плюс доставка.
     *
     * @return ціна + {@value #DELIVERY_COST}
     */
    @Override
    public double purchaseCost() {
        return getPrice() + DELIVERY_COST;
    }
}

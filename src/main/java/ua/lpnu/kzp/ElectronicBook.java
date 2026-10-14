package ua.lpnu.kzp;

/**
 * Електронна книга.
 *
 * <p>Відрізняється правилом вартості: файл завантажується, доставка не потрібна,
 * тому вартість придбання дорівнює ціні.
 */
public final class ElectronicBook extends Book {
    /**
     * Створює електронну книгу.
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
    public ElectronicBook(String title, String author, int pages, double price, Integer year, String genre) {
        super(title, author, pages, price, year, genre);
    }

    /**
     * Повертає формат електронної книги.
     *
     * @return {@link BookFormat#ELECTRONIC}
     */
    @Override
    public BookFormat getFormat() {
        return BookFormat.ELECTRONIC;
    }

    /**
     * Обчислює вартість придбання: лише ціна, без доставки.
     *
     * @return ціна книги
     */
    @Override
    public double purchaseCost() {
        return getPrice();
    }
}

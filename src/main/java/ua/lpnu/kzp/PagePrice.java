package ua.lpnu.kzp;

/**
 * Незмінна пара «кількість сторінок, ціна».
 *
 * <p>Описує як одну книгу ({@link Book#pagePrice()}), так і суму для кількох книг
 * ({@link #plus(PagePrice)}). Кількість сторінок має тип {@code long}, щоб сума на
 * великому каталозі не переповнилася.
 *
 * @param pages кількість сторінок, не від'ємна
 * @param price ціна або сумарна вартість, скінченна й не від'ємна
 */
public record PagePrice(long pages, double price) {
    /** Нульове значення, з якого починається підсумовування. */
    public static final PagePrice ZERO = new PagePrice(0, 0.0);

    /**
     * Перевіряє, що значення не від'ємні та скінченні.
     *
     * @throws IllegalArgumentException якщо сторінки від'ємні або ціна від'ємна чи нескінченна
     */
    public PagePrice {
        if (pages < 0) {
            throw new IllegalArgumentException("кількість сторінок не може бути від'ємною");
        }
        if (!Double.isFinite(price) || price < 0) {
            throw new IllegalArgumentException("ціна має бути скінченною й не від'ємною");
        }
    }

    /**
     * Повертає суму цього значення та іншого; поточний об'єкт не змінюється.
     *
     * @param other значення, яке додається
     * @return нове значення з сумою сторінок і сумою цін
     */
    public PagePrice plus(PagePrice other) {
        return new PagePrice(pages + other.pages, price + other.price);
    }
}

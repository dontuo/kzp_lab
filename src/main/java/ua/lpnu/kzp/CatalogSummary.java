package ua.lpnu.kzp;

/**
 * Накопичує чотири показники каталогу: кількість книг, середню кількість
 * сторінок, найдорожчу книгу та сумарну вартість.
 */
public final class CatalogSummary {
    private int count;
    // long, щоб сума сторінок не переповнилася на великому каталозі.
    private long totalPages;
    private double totalPrice;
    private String mostExpensiveTitle;
    private double maxPrice;

    /**
     * Додає до показників одну книгу.
     *
     * @param fields поля рядка, який уже пройшов {@link BookParser#validate(String)}
     */
    public void add(String[] fields) {
        // Рядок уже перевірено, тому перетворення тут не кидає винятків.
        int pages = Integer.parseInt(fields[BookParser.PAGES]);
        double price = Double.parseDouble(fields[BookParser.PRICE]);

        count++;
        totalPages += pages;
        totalPrice += price;
        // Строге порівняння: за однакової ціни лишається перша книга.
        if (mostExpensiveTitle == null || price > maxPrice) {
            mostExpensiveTitle = fields[BookParser.TITLE];
            maxPrice = price;
        }
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
        return (double) totalPages / count;
    }

    /**
     * Повертає назву найдорожчої книги.
     *
     * @return назва або {@code null}, якщо книг немає
     */
    public String getMostExpensiveTitle() {
        return mostExpensiveTitle;
    }

    /**
     * Повертає ціну найдорожчої книги.
     *
     * @return найбільша ціна або 0, якщо книг немає
     */
    public double getMaxPrice() {
        return maxPrice;
    }

    /**
     * Повертає сумарну вартість каталогу.
     *
     * @return сума цін усіх книг
     */
    public double getTotalPrice() {
        return totalPrice;
    }
}

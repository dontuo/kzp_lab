package ua.lpnu.kzp;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Книга з каталогу.
 *
 * <p>Клас незмінний: усі поля {@code private final}, сеттерів немає, а всі поля
 * мають незмінні типи. Тому об'єкт, який пройшов перевірку в конструкторі,
 * залишається коректним увесь час і може безпечно передаватися між потоками.
 *
 * <p>Інваріанти: назва й автор не {@code null} і не порожні, кількість сторінок
 * більша за нуль, ціна скінченна й не від'ємна.
 */
public final class Book {
    /* Ціле число: необов'язковий мінус і лише цифри, без пробілів і знака плюс. */
    private static final Pattern INTEGER = Pattern.compile("-?\\d+");
    /*
     * Десяткове число з крапкою: цифри та необов'язкова дробова частина.
     * Відсікає те, що Double.parseDouble теж приймає: пробіли, 10d, 1e3, 0x1p3, NaN, Infinity.
     */
    private static final Pattern DECIMAL = Pattern.compile("-?\\d+(\\.\\d+)?");

    private final String title;
    private final String author;
    private final int pages;
    private final double price;

    /**
     * Створює книгу та перевіряє всі інваріанти.
     *
     * @param title назва, не порожня
     * @param author автор, не порожній
     * @param pages кількість сторінок, більша за нуль
     * @param price ціна, скінченна й не від'ємна
     * @throws NullPointerException якщо назва або автор {@code null}
     * @throws IllegalArgumentException якщо порушено інший інваріант
     */
    public Book(String title, String author, int pages, double price) {
        this.title = Objects.requireNonNull(title, "назва книги не може бути null");
        this.author = Objects.requireNonNull(author, "автор не може бути null");
        // isBlank, а не isEmpty: назва з самих пробілів теж вважається порожньою.
        if (title.isBlank()) {
            throw new IllegalArgumentException("порожня назва книги");
        }
        if (author.isBlank()) {
            throw new IllegalArgumentException("порожній автор");
        }
        // Книга без сторінок не має сенсу, тому нуль теж відкидаємо.
        if (pages <= 0) {
            throw new IllegalArgumentException(
                    String.format(Locale.ROOT, "кількість сторінок має бути додатною: %d", pages));
        }
        // Окремо від price < 0: NaN не менший за нуль, і без цієї перевірки пройшов би.
        if (!Double.isFinite(price)) {
            throw new IllegalArgumentException(
                    String.format(Locale.ROOT, "ціна має бути скінченним числом: %s", price));
        }
        // Нульова ціна допустима (безкоштовна книга), від'ємна – ні.
        if (price < 0) {
            throw new IllegalArgumentException(String.format(Locale.ROOT, "від'ємна ціна: %.2f", price));
        }
        this.pages = pages;
        this.price = price;
    }

    /**
     * Створює книгу з одного рядка CSV у форматі {@code title;author;pages;price}.
     *
     * <p>Метод перевіряє лише структуру рядка й формат чисел. Предметні правила
     * (порожні поля, додатні сторінки, не від'ємна ціна) перевіряє конструктор,
     * тому вони не дублюються.
     *
     * @param line рядок вхідного файла
     * @return створена книга
     * @throws NullPointerException якщо {@code line} дорівнює {@code null}
     * @throws IllegalArgumentException якщо рядок не можна перетворити на книгу;
     *     повідомлення описує причину
     */
    public static Book fromCsv(String line) {
        Objects.requireNonNull(line, "рядок не може бути null");
        // isBlank, а не isEmpty: рядок із самих пробілів теж вважаємо порожнім.
        if (line.isBlank()) {
            throw new IllegalArgumentException("порожній рядок");
        }

        String[] fields = BookParser.split(line);
        if (fields.length != BookParser.FIELD_COUNT) {
            throw new IllegalArgumentException(String.format(Locale.ROOT, "очікується %d поля, отримано %d",
                    BookParser.FIELD_COUNT, fields.length));
        }
        String pagesText = fields[BookParser.PAGES];
        String priceText = fields[BookParser.PRICE];
        if (pagesText.isEmpty()) {
            throw new IllegalArgumentException("порожня кількість сторінок");
        }
        if (priceText.isEmpty()) {
            throw new IllegalArgumentException("порожня ціна");
        }

        // Формат перевіряємо до перетворення: parseInt і parseDouble по-різному
        // ставляться до пробілів, а parseDouble приймає ще й 10d, 1e3, NaN.
        if (!INTEGER.matcher(pagesText).matches()) {
            throw new IllegalArgumentException(
                    String.format(Locale.ROOT, "кількість сторінок не є цілим числом: \"%s\"", pagesText));
        }
        if (!DECIMAL.matcher(priceText).matches()) {
            throw new IllegalArgumentException(String.format(Locale.ROOT, "ціна не є числом: \"%s\"", priceText));
        }

        int pages;
        try {
            pages = Integer.parseInt(pagesText);
        } catch (NumberFormatException exception) {
            // Формат правильний, тож виняток можливий лише через переповнення int.
            throw new IllegalArgumentException(
                    String.format(Locale.ROOT, "кількість сторінок завелика: \"%s\"", pagesText), exception);
        }
        return new Book(fields[BookParser.TITLE], fields[BookParser.AUTHOR], pages, Double.parseDouble(priceText));
    }

    /**
     * Повертає будівник для створення книги з іменованими параметрами.
     *
     * @return новий порожній будівник
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Повертає назву книги.
     *
     * @return назва
     */
    public String getTitle() {
        return title;
    }

    /**
     * Повертає автора книги.
     *
     * @return автор
     */
    public String getAuthor() {
        return author;
    }

    /**
     * Повертає кількість сторінок.
     *
     * @return кількість сторінок, більша за нуль
     */
    public int getPages() {
        return pages;
    }

    /**
     * Повертає ціну книги.
     *
     * @return ціна, не від'ємна
     */
    public double getPrice() {
        return price;
    }

    /**
     * Повертає сторінки й ціну цієї книги як одне значення для підсумовування.
     *
     * @return пара «сторінки, ціна»
     */
    public PagePrice pagePrice() {
        return new PagePrice(pages, price);
    }

    /**
     * Повертає текстове подання книги; ціна форматується з {@link Locale#ROOT}.
     *
     * @return наприклад, {@code Кобзар (Тарас Шевченко): 320 с., 250.00 грн}
     */
    @Override
    public String toString() {
        return String.format(Locale.ROOT, "%s (%s): %d с., %.2f грн", title, author, pages, price);
    }

    /**
     * Будівник книги.
     *
     * <p>Лише збирає значення: {@link #build()} викликає конструктор {@link Book},
     * тому будівник не може обійти перевірки інваріантів.
     */
    public static final class Builder {
        private String title;
        private String author;
        private int pages;
        private double price;

        /* Створюється лише через Book.builder(). */
        private Builder() {
        }

        /**
         * Задає назву.
         *
         * @param value назва книги
         * @return цей будівник
         */
        public Builder title(String value) {
            this.title = value;
            return this;
        }

        /**
         * Задає автора.
         *
         * @param value автор книги
         * @return цей будівник
         */
        public Builder author(String value) {
            this.author = value;
            return this;
        }

        /**
         * Задає кількість сторінок.
         *
         * @param value кількість сторінок
         * @return цей будівник
         */
        public Builder pages(int value) {
            this.pages = value;
            return this;
        }

        /**
         * Задає ціну.
         *
         * @param value ціна
         * @return цей будівник
         */
        public Builder price(double value) {
            this.price = value;
            return this;
        }

        /**
         * Створює книгу через конструктор із повною перевіркою.
         *
         * @return нова книга
         * @throws NullPointerException якщо не задано назву або автора
         * @throws IllegalArgumentException якщо значення порушують інваріанти
         */
        public Book build() {
            return new Book(title, author, pages, price);
        }
    }
}

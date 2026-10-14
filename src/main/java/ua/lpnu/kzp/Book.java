package ua.lpnu.kzp;

import java.time.Year;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.regex.Pattern;

/**
 * Книга з каталогу – спільний тип для всіх форматів видання.
 *
 * <p>Тут зібрано поля й інваріанти, однакові для всіх підтипів: назва й автор не
 * порожні, кількість сторінок більша за нуль, ціна скінченна й не від'ємна, рік
 * видання (якщо відомий) у межах від {@value #FIRST_PRINTED_YEAR} до поточного,
 * жанр (якщо відомий) не порожній. Підтипи відрізняються поведінкою
 * {@link #purchaseCost()} і власним {@link #getFormat()}.
 *
 * <p>Ієрархія {@code sealed}: інших підтипів, крім перелічених у {@code permits},
 * бути не може. Об'єкти незмінні: поля {@code private final}, сеттерів немає.
 */
public abstract sealed class Book permits PrintedBook, ElectronicBook {
    /** Рік, раніше якого друкованих книг не існувало (друкарня Гутенберга). */
    public static final int FIRST_PRINTED_YEAR = 1450;

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
    // null означає «невідомо»: так позначаються рядки старого формату з 4 полями.
    private final Integer year;
    private final String genre;

    /**
     * Перевіряє спільні інваріанти й зберігає спільні поля.
     *
     * <p>Конструктор доступний лише підтипам: створити «просто книгу» без формату не можна.
     *
     * @param title назва, не порожня
     * @param author автор, не порожній
     * @param pages кількість сторінок, більша за нуль
     * @param price ціна, скінченна й не від'ємна
     * @param year рік видання або {@code null}, якщо невідомий
     * @param genre жанр або {@code null}, якщо невідомий
     * @throws NullPointerException якщо назва або автор {@code null}
     * @throws IllegalArgumentException якщо порушено інший інваріант
     */
    protected Book(String title, String author, int pages, double price, Integer year, String genre) {
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
        int currentYear = Year.now().getValue();
        if (year != null && (year < FIRST_PRINTED_YEAR || year > currentYear)) {
            throw new IllegalArgumentException(String.format(Locale.ROOT,
                    "рік видання має бути від %d до %d: %d", FIRST_PRINTED_YEAR, currentYear, year));
        }
        if (genre != null && genre.isBlank()) {
            throw new IllegalArgumentException("порожній жанр");
        }
        this.pages = pages;
        this.price = price;
        this.year = year;
        this.genre = genre;
    }

    /**
     * Створює книгу з одного рядка CSV.
     *
     * <p>Підтримуються два формати рядка:
     * <ul>
     *   <li>{@code title;author;pages;price} – формат ЛР № 1–2; книга вважається
     *       друкованою, рік і жанр невідомі;</li>
     *   <li>{@code title;author;pages;price;format;year;genre} – повний формат,
     *       де {@code format} – {@code PRINTED} або {@code ELECTRONIC}.</li>
     * </ul>
     *
     * <p>Метод перевіряє лише структуру рядка й формат значень. Предметні правила
     * перевіряє конструктор, тому вони не дублюються.
     *
     * @param line рядок вхідного файла
     * @return книга відповідного підтипу
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
        if (fields.length != BookParser.FIELD_COUNT && fields.length != BookParser.FULL_FIELD_COUNT) {
            throw new IllegalArgumentException(String.format(Locale.ROOT, "очікується %d або %d полів, отримано %d",
                    BookParser.FIELD_COUNT, BookParser.FULL_FIELD_COUNT, fields.length));
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
        int pages = parseInt(pagesText, "кількість сторінок завелика");
        double price = Double.parseDouble(priceText);
        String title = fields[BookParser.TITLE];
        String author = fields[BookParser.AUTHOR];

        if (fields.length == BookParser.FIELD_COUNT) {
            // Старий формат: усі книги ЛР № 1–2 були друкованими.
            return create(BookFormat.PRINTED, title, author, pages, price, null, null);
        }

        BookFormat format = BookFormat.fromCsv(fields[BookParser.FORMAT]);
        String yearText = fields[BookParser.YEAR];
        if (yearText.isEmpty()) {
            throw new IllegalArgumentException("порожній рік видання");
        }
        if (!INTEGER.matcher(yearText).matches()) {
            throw new IllegalArgumentException(
                    String.format(Locale.ROOT, "рік видання не є цілим числом: \"%s\"", yearText));
        }
        int year = parseInt(yearText, "рік видання завеликий");
        return create(format, title, author, pages, price, year, fields[BookParser.GENRE]);
    }

    /* Перетворює текст, який уже пройшов перевірку формату; виняток можливий лише через переповнення. */
    private static int parseInt(String text, String overflowMessage) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    String.format(Locale.ROOT, "%s: \"%s\"", overflowMessage, text), exception);
        }
    }

    /*
     * Обирає підтип за форматом. switch без default: якщо до BookFormat додати нову
     * константу, компілятор не зібере код, доки тут не з'явиться її обробка.
     */
    private static Book create(BookFormat format, String title, String author, int pages, double price,
            Integer year, String genre) {
        return switch (format) {
            case PRINTED -> new PrintedBook(title, author, pages, price, year, genre);
            case ELECTRONIC -> new ElectronicBook(title, author, pages, price, year, genre);
        };
    }

    /**
     * Повертає будівник для створення книги з іменованими параметрами.
     *
     * @return новий порожній будівник; формат за замовчуванням – {@link BookFormat#PRINTED}
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Повертає формат видання; кожен підтип повертає власну константу.
     *
     * @return формат книги
     */
    public abstract BookFormat getFormat();

    /**
     * Обчислює вартість придбання книги.
     *
     * <p>Поліморфна операція: друкована книга додає до ціни доставку, електронна –
     * ні. Код звіту викликає цей метод через тип {@code Book} і не перевіряє підтип.
     *
     * @return вартість придбання, не менша за ціну
     */
    public abstract double purchaseCost();

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
     * Повертає рік видання.
     *
     * @return рік або порожнє значення, якщо його немає в даних
     */
    public OptionalInt getYear() {
        return year == null ? OptionalInt.empty() : OptionalInt.of(year);
    }

    /**
     * Повертає жанр.
     *
     * @return жанр або порожнє значення, якщо його немає в даних
     */
    public Optional<String> getGenre() {
        return Optional.ofNullable(genre);
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
     * Порівнює книги за логічною тотожністю: той самий підтип (формат), назва й автор.
     *
     * <p>Ціна, кількість сторінок, рік і жанр не входять до рівності: це властивості
     * конкретного запису, а не того, яку книгу він описує. Друкована й електронна
     * книга з однаковою назвою – різні видання, тому вони не рівні.
     *
     * @param other інший об'єкт
     * @return {@code true}, якщо об'єкти описують ту саму книгу в тому самому форматі
     */
    @Override
    public final boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        // getClass, а не instanceof: інакше рівність підтипів могла б стати несиметричною.
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        Book book = (Book) other;
        return title.equals(book.title) && author.equals(book.author);
    }

    /**
     * Обчислює хеш-код із тих самих полів, що й {@link #equals(Object)}.
     *
     * @return хеш-код, однаковий для рівних книг
     */
    @Override
    public final int hashCode() {
        return Objects.hash(getFormat(), title, author);
    }

    /**
     * Повертає текстове подання книги; ціна форматується з {@link Locale#ROOT}.
     *
     * @return наприклад, {@code Кобзар (Тарас Шевченко): 320 с., 250.00 грн, друкована}
     */
    @Override
    public String toString() {
        return String.format(Locale.ROOT, "%s (%s): %d с., %.2f грн, %s",
                title, author, pages, price, getFormat().getLabel());
    }

    /**
     * Будівник книги.
     *
     * <p>Лише збирає значення: {@link #build()} викликає конструктор підтипу, тому
     * будівник не може обійти перевірки інваріантів.
     */
    public static final class Builder {
        private BookFormat format = BookFormat.PRINTED;
        private String title;
        private String author;
        private int pages;
        private double price;
        private Integer year;
        private String genre;

        /* Створюється лише через Book.builder(). */
        private Builder() {
        }

        /**
         * Задає формат видання.
         *
         * @param value формат
         * @return цей будівник
         */
        public Builder format(BookFormat value) {
            this.format = Objects.requireNonNull(value, "формат не може бути null");
            return this;
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
         * Задає рік видання.
         *
         * @param value рік
         * @return цей будівник
         */
        public Builder year(int value) {
            this.year = value;
            return this;
        }

        /**
         * Задає жанр.
         *
         * @param value жанр
         * @return цей будівник
         */
        public Builder genre(String value) {
            this.genre = value;
            return this;
        }

        /**
         * Створює книгу заданого формату через конструктор із повною перевіркою.
         *
         * @return нова книга
         * @throws NullPointerException якщо не задано назву або автора
         * @throws IllegalArgumentException якщо значення порушують інваріанти
         */
        public Book build() {
            return create(format, title, author, pages, price, year, genre);
        }
    }
}

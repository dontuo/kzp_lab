package ua.lpnu.kzp;

/**
 * Описує розкладку рядка каталогу книжок і перевіряє рядок без створення об'єкта.
 *
 * <p>Підтримуються два формати рядка:
 * <ul>
 *   <li>{@code title;author;pages;price} – формат ЛР № 1–2, наприклад
 *       {@code Кобзар;Тарас Шевченко;320;250.00};</li>
 *   <li>{@code title;author;pages;price;format;year;genre} – повний формат, наприклад
 *       {@code Кобзар;Тарас Шевченко;320;250.00;PRINTED;1840;поезія}.</li>
 * </ul>
 *
 * <p>Правила перевірки зберігаються лише в {@link Book}: формат – у
 * {@link Book#fromCsv(String)}, інваріанти – у конструкторі. Цей клас лише описує
 * поля та повертає причину помилки у вигляді рядка, як у лабораторній роботі № 1.
 */
public final class BookParser {
    /** Кількість полів у рядку старого формату ЛР № 1–2. */
    public static final int FIELD_COUNT = 4;
    /** Кількість полів у рядку повного формату. */
    public static final int FULL_FIELD_COUNT = 7;
    /** Індекс поля з назвою книги. */
    public static final int TITLE = 0;
    /** Індекс поля з автором. */
    public static final int AUTHOR = 1;
    /** Індекс поля з кількістю сторінок. */
    public static final int PAGES = 2;
    /** Індекс поля з ціною. */
    public static final int PRICE = 3;
    /** Індекс поля з форматом видання (лише повний формат). */
    public static final int FORMAT = 4;
    /** Індекс поля з роком видання (лише повний формат). */
    public static final int YEAR = 5;
    /** Індекс поля з жанром (лише повний формат). */
    public static final int GENRE = 6;

    /* Роздільник полів у вхідному файлі. */
    private static final String SEPARATOR = ";";

    /* Забороняє створення екземплярів службового класу. */
    private BookParser() {
    }

    /**
     * Розділяє рядок на поля.
     *
     * <p>Другий аргумент {@code -1} зберігає порожні поля в кінці рядка, інакше
     * {@code "a;b;1;"} дав би три поля замість чотирьох.
     *
     * @param line рядок вхідного файла
     * @return масив полів
     */
    public static String[] split(String line) {
        return line.split(SEPARATOR, -1);
    }

    /**
     * Перевіряє рядок і повертає причину, з якої його не можна обробити.
     *
     * @param line рядок вхідного файла
     * @return {@code null}, якщо з рядка можна створити {@link Book}, інакше опис помилки
     */
    public static String validate(String line) {
        try {
            Book.fromCsv(line);
            return null;
        } catch (IllegalArgumentException exception) {
            return exception.getMessage();
        }
    }
}

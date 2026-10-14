package ua.lpnu.kzp;

/**
 * Описує розкладку рядка каталогу книжок і перевіряє рядок без створення об'єкта.
 *
 * <p>Формат рядка: {@code title;author;pages;price}, наприклад
 * {@code Кобзар;Тарас Шевченко;320;250.00}.
 *
 * <p>Правила перевірки зберігаються лише в {@link Book}: формат – у
 * {@link Book#fromCsv(String)}, інваріанти – у конструкторі. Цей клас лише описує
 * поля та повертає причину помилки у вигляді рядка, як у лабораторній роботі № 1.
 */
public final class BookParser {
    /** Кількість полів у коректному рядку. */
    public static final int FIELD_COUNT = 4;
    /** Індекс поля з назвою книги. */
    public static final int TITLE = 0;
    /** Індекс поля з автором. */
    public static final int AUTHOR = 1;
    /** Індекс поля з кількістю сторінок. */
    public static final int PAGES = 2;
    /** Індекс поля з ціною. */
    public static final int PRICE = 3;

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

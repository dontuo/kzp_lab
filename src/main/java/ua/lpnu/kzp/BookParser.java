package ua.lpnu.kzp;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Розбирає та перевіряє один рядок каталогу книжок.
 *
 * <p>Формат рядка: {@code title;author;pages;price}, наприклад
 * {@code Кобзар;Тарас Шевченко;320;250.00}.
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
    /* Ціле число: необов'язковий мінус і лише цифри, без пробілів і знака плюс. */
    private static final Pattern INTEGER = Pattern.compile("-?\\d+");
    /*
     * Десяткове число з крапкою: цифри та необов'язкова дробова частина.
     * Відсікає те, що Double.parseDouble теж приймає: пробіли, 10d, 1e3, 0x1p3, NaN, Infinity.
     */
    private static final Pattern DECIMAL = Pattern.compile("-?\\d+(\\.\\d+)?");

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
     * <p>Перевірки виконуються в порядку: порожній рядок, кількість полів,
     * порожні текстові поля, формат чисел, допустимі значення. Повертається
     * перша знайдена проблема.
     *
     * @param line рядок вхідного файла
     * @return {@code null}, якщо рядок коректний, інакше опис помилки
     */
    public static String validate(String line) {
        // isBlank, а не isEmpty: рядок із самих пробілів теж вважаємо порожнім.
        if (line.isBlank()) {
            return "порожній рядок";
        }

        String[] fields = split(line);
        if (fields.length != FIELD_COUNT) {
            return String.format(Locale.ROOT, "очікується %d поля, отримано %d", FIELD_COUNT, fields.length);
        }
        if (fields[TITLE].isBlank()) {
            return "порожня назва книги";
        }
        if (fields[AUTHOR].isBlank()) {
            return "порожній автор";
        }
        if (fields[PAGES].isEmpty()) {
            return "порожня кількість сторінок";
        }
        if (fields[PRICE].isEmpty()) {
            return "порожня ціна";
        }

        // Формат перевіряємо до перетворення: parseInt і parseDouble по-різному
        // ставляться до пробілів, а parseDouble приймає ще й 10d, 1e3, NaN.
        if (!INTEGER.matcher(fields[PAGES]).matches()) {
            return String.format(Locale.ROOT, "кількість сторінок не є цілим числом: \"%s\"", fields[PAGES]);
        }
        if (!DECIMAL.matcher(fields[PRICE]).matches()) {
            return String.format(Locale.ROOT, "ціна не є числом: \"%s\"", fields[PRICE]);
        }

        int pages;
        try {
            pages = Integer.parseInt(fields[PAGES]);
        } catch (NumberFormatException exception) {
            // Формат правильний, тож виняток можливий лише через переповнення int.
            return String.format(Locale.ROOT, "кількість сторінок завелика: \"%s\"", fields[PAGES]);
        }
        double price = Double.parseDouble(fields[PRICE]);

        // Книга без сторінок не має сенсу, тому нуль теж відкидаємо.
        if (pages <= 0) {
            return String.format(Locale.ROOT, "кількість сторінок має бути додатною: %d", pages);
        }
        // Число з сотень цифр parseDouble перетворює на Infinity.
        if (!Double.isFinite(price)) {
            return String.format(Locale.ROOT, "ціна має бути скінченним числом: \"%s\"", fields[PRICE]);
        }
        // Нульова ціна допустима (безкоштовна книга), від'ємна – ні.
        if (price < 0) {
            return String.format(Locale.ROOT, "від'ємна ціна: %s", fields[PRICE]);
        }
        return null;
    }
}

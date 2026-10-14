package ua.lpnu.kzp;

import java.util.Locale;

/**
 * Формат видання книги.
 *
 * <p>Множина форматів відома наперед, тому описана через {@code enum}: значення
 * не можна створити поза цим переліком, а порівнювати їх можна через {@code ==}.
 */
public enum BookFormat {
    /** Друкована книга. */
    PRINTED("друкована"),
    /** Електронна книга. */
    ELECTRONIC("електронна");

    private final String label;

    /* Конструктор enum викликається лише для констант, оголошених вище. */
    BookFormat(String label) {
        this.label = label;
    }

    /**
     * Повертає підпис формату для звіту.
     *
     * @return наприклад, {@code друкована}
     */
    public String getLabel() {
        return label;
    }

    /**
     * Перетворює текст поля CSV на формат.
     *
     * <p>Приймає лише точні назви констант ({@code PRINTED}, {@code ELECTRONIC}),
     * щоб опечатка в даних не перетворилася мовчки на інший формат.
     *
     * @param text значення поля {@code format}
     * @return відповідний формат
     * @throws IllegalArgumentException якщо такого формату немає
     */
    public static BookFormat fromCsv(String text) {
        for (BookFormat format : values()) {
            if (format.name().equals(text)) {
                return format;
            }
        }
        throw new IllegalArgumentException(String.format(Locale.ROOT, "невідомий формат книги: \"%s\"", text));
    }
}

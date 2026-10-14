package ua.lpnu.kzp;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Експорт тих самих анотованих полів у JSON – другий формат через {@link Exporter}.
 *
 * <p>Результат – масив об'єктів, ключі – назви з {@link CsvColumn} у тому самому
 * порядку, що й стовпці CSV. Числа й логічні значення записуються без лапок,
 * {@code null} – як {@code null}, усе інше – рядком JSON з екрануванням.
 *
 * @param <T> тип записів
 */
public final class JsonExporter<T> implements Exporter<T> {
    private final Class<T> type;

    /**
     * Створює експортер для класу записів.
     *
     * @param type клас записів
     */
    public JsonExporter(Class<T> type) {
        this.type = Objects.requireNonNull(type, "клас записів не може бути null");
    }

    /**
     * Формує JSON-масив записів.
     *
     * @param records записи; список не змінюється
     * @return текст JSON; для порожнього списку – {@code []}
     * @throws DataStorageException якщо клас не має анотованих полів або поле неможливо прочитати
     */
    @Override
    public String format(List<? extends T> records) throws DataStorageException {
        List<Field> fields = AnnotatedFields.of(type);
        String separator = System.lineSeparator();
        List<String> objects = new ArrayList<>(records.size());
        // Цикл, а не потік: читання поля кидає перевірюваний DataStorageException.
        for (T record : records) {
            List<String> members = new ArrayList<>(fields.size());
            for (Field field : fields) {
                members.add(quote(AnnotatedFields.columnName(field)) + ": "
                        + value(AnnotatedFields.read(field, record)));
            }
            objects.add("  {" + String.join(", ", members) + "}");
        }
        if (objects.isEmpty()) {
            return "[]" + separator;
        }
        return "[" + separator + String.join("," + separator, objects) + separator + "]" + separator;
    }

    /* JSON-подання одного значення. */
    private static String value(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        return quote(String.valueOf(value));
    }

    /* Рядок JSON: лапки, зворотна скісна риска й керівні символи екрануються. */
    static String quote(String text) {
        StringBuilder json = new StringBuilder("\"");
        for (char c : text.toCharArray()) {
            switch (c) {
                case '"' -> json.append("\\\"");
                case '\\' -> json.append("\\\\");
                case '\n' -> json.append("\\n");
                case '\r' -> json.append("\\r");
                case '\t' -> json.append("\\t");
                default -> {
                    if (c < 0x20) {
                        json.append(String.format(Locale.ROOT, "\\u%04x", (int) c));
                    } else {
                        json.append(c);
                    }
                }
            }
        }
        return json.append('"').toString();
    }
}

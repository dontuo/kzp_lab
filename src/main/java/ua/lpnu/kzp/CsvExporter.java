package ua.lpnu.kzp;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Універсальний рефлексивний експорт у CSV.
 *
 * <p>Експортер не знає жодного предметного класу: стовпці – це поля класу {@code type}
 * з {@link CsvColumn}, упорядковані за Java-іменем поля, а заголовки – значення
 * анотацій. Значення перетворюються на текст через {@link String#valueOf(Object)};
 * {@code null} записується порожнім полем.
 *
 * <p>Правила CSV: роздільник – кома; поле з комою, лапкою або перенесенням рядка
 * береться в подвійні лапки, а лапка всередині подвоюється; записи розділяються
 * роздільником рядків поточної ОС.
 *
 * @param <T> тип записів
 */
public final class CsvExporter<T> implements Exporter<T> {
    private final Class<T> type;

    /**
     * Створює експортер для класу записів.
     *
     * <p>Клас передається окремо, бо для порожнього списку немає жодного об'єкта, з
     * якого його можна було б дізнатися, а заголовок однаково потрібен.
     *
     * @param type клас записів
     */
    public CsvExporter(Class<T> type) {
        this.type = Objects.requireNonNull(type, "клас записів не може бути null");
    }

    /**
     * Повертає заголовки стовпців у порядку експорту.
     *
     * @return назви з {@link CsvColumn}
     * @throws DataStorageException якщо клас не має анотованих полів
     */
    public List<String> header() throws DataStorageException {
        return AnnotatedFields.of(type).stream().map(AnnotatedFields::columnName).toList();
    }

    /**
     * Формує CSV: рядок заголовка й по одному запису на кожен об'єкт.
     *
     * @param records записи; список не змінюється
     * @return текст CSV; для порожнього списку – лише заголовок
     * @throws DataStorageException якщо клас не має анотованих полів або поле неможливо прочитати
     */
    @Override
    public String format(List<? extends T> records) throws DataStorageException {
        List<Field> fields = AnnotatedFields.of(type);
        String separator = System.lineSeparator();
        StringBuilder csv = new StringBuilder();
        csv.append(fields.stream()
                .map(field -> escape(AnnotatedFields.columnName(field)))
                .collect(Collectors.joining(","))).append(separator);
        // Цикл, а не потік: читання поля кидає перевірюваний DataStorageException.
        for (T record : records) {
            List<String> values = new ArrayList<>(fields.size());
            for (Field field : fields) {
                Object value = AnnotatedFields.read(field, record);
                values.add(value == null ? "" : escape(String.valueOf(value)));
            }
            csv.append(String.join(",", values)).append(separator);
        }
        return csv.toString();
    }

    /**
     * Екранує одне значення за правилами CSV.
     *
     * @param value текст поля
     * @return текст без змін або в лапках із подвоєними лапками всередині
     */
    static String escape(String value) {
        boolean needsQuotes = value.indexOf(',') >= 0 || value.indexOf('"') >= 0
                || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0;
        if (!needsQuotes) {
            return value;
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}

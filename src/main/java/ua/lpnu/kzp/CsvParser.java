package ua.lpnu.kzp;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Документний CSV-парсер: розбирає весь текст машиною станів.
 *
 * <p>На відміну від {@code split(",")}, розрізняє кому й перенесення рядка всередині
 * лапок і поза ними, тому одне поле може містити кому, лапки чи кілька рядків. Повертає
 * записи, а не фізичні рядки файла. Приймає роздільники рядків {@code \n} і
 * {@code \r\n}, тож читає файли, створені на будь-якій ОС.
 */
public final class CsvParser {
    /* Забороняє створення екземплярів службового класу. */
    private CsvParser() {
    }

    /**
     * Таблиця CSV: заголовок і записи, у яких значення доступні за назвою стовпця.
     *
     * @param header назви стовпців у порядку файла; порожній для порожнього файла
     * @param rows записи у порядку файла
     */
    public record Table(List<String> header, List<Map<String, String>> rows) {
        /**
         * Робить незмінні копії списків.
         *
         * @param header назви стовпців
         * @param rows записи
         */
        public Table {
            header = List.copyOf(header);
            rows = List.copyOf(rows);
        }
    }

    /**
     * Читає CSV-файл у кодуванні UTF-8 і повертає таблицю.
     *
     * <p>Порожній файл дає таблицю без заголовка й записів, файл із самим заголовком –
     * таблицю із заголовком і без записів.
     *
     * @param path шлях до файла
     * @return заголовок і записи
     * @throws DataStorageException якщо файл неможливо прочитати, текст не є коректним CSV
     *     або кількість полів запису не збігається із заголовком
     */
    public static Table readTable(Path path) throws DataStorageException {
        String text;
        try {
            text = Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new DataStorageException("не вдалося прочитати файл " + path, exception);
        }
        List<List<String>> records = parseDocument(text);
        if (records.isEmpty()) {
            return new Table(List.of(), List.of());
        }
        List<String> header = records.get(0);
        List<Map<String, String>> rows = new ArrayList<>();
        for (int index = 1; index < records.size(); index++) {
            List<String> values = records.get(index);
            if (values.size() != header.size()) {
                throw new DataStorageException(String.format(Locale.ROOT, "%s, запис %d: очікується %d полів, отримано %d",
                        path, index, header.size(), values.size()));
            }
            Map<String, String> row = new LinkedHashMap<>();
            for (int column = 0; column < header.size(); column++) {
                row.put(header.get(column), values.get(column));
            }
            rows.add(row);
        }
        return new Table(header, rows);
    }

    /**
     * Розбирає текст CSV на записи та поля.
     *
     * @param text увесь текст документа
     * @return записи; порожній текст дає порожній список, завершальний роздільник
     *     рядків не створює порожнього запису
     * @throws DataStorageException якщо лапки не закрито або лапка стоїть усередині поля без лапок
     */
    public static List<List<String>> parseDocument(String text) throws DataStorageException {
        List<List<String>> records = new ArrayList<>();
        List<String> record = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;
        // Поле вже було в лапках і лапки закрито: далі дозволені лише кома або кінець рядка.
        boolean quotedFieldClosed = false;
        int line = 1;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"') {
                        // Подвоєна лапка всередині поля означає одну лапку.
                        field.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                        quotedFieldClosed = true;
                    }
                } else {
                    if (c == '\n') {
                        line++;
                    }
                    field.append(c);
                }
            } else if (c == ',') {
                record.add(field.toString());
                field.setLength(0);
                quotedFieldClosed = false;
            } else if (c == '\n' || c == '\r') {
                if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                    i++;
                }
                record.add(field.toString());
                records.add(List.copyOf(record));
                record.clear();
                field.setLength(0);
                quotedFieldClosed = false;
                line++;
            } else if (quotedFieldClosed) {
                throw new DataStorageException("рядок " + line + ": символ після закривальної лапки");
            } else if (c == '"') {
                if (field.length() > 0) {
                    throw new DataStorageException("рядок " + line + ": лапка всередині поля без лапок");
                }
                inQuotes = true;
            } else {
                field.append(c);
            }
        }

        if (inQuotes) {
            throw new DataStorageException("рядок " + line + ": лапки не закрито до кінця файла");
        }
        // Останній запис без завершального роздільника рядків.
        if (field.length() > 0 || !record.isEmpty() || quotedFieldClosed) {
            record.add(field.toString());
            records.add(List.copyOf(record));
        }
        return records;
    }
}

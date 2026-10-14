package ua.lpnu.kzp;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

/**
 * Експорт списку записів у текстовий формат.
 *
 * @param <T> тип записів
 */
public interface Exporter<T> {
    /**
     * Перетворює записи на текст.
     *
     * @param records записи; список не змінюється
     * @return текст у форматі експортера
     * @throws DataStorageException якщо значення полів неможливо прочитати
     */
    String format(List<? extends T> records) throws DataStorageException;

    /**
     * Записує записи у файл у кодуванні UTF-8, повністю замінюючи попередній вміст.
     *
     * @param path шлях до файла; відсутні каталоги створюються
     * @param records записи; список не змінюється
     * @throws DataStorageException якщо записи неможливо прочитати або файл записати
     */
    default void write(Path path, List<? extends T> records) throws DataStorageException {
        String text = format(records);
        try {
            Path parent = path.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            // TRUNCATE_EXISTING: після коротшого експорту в файлі не лишаються старі рядки.
            Files.writeString(path, text, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        } catch (IOException exception) {
            throw new DataStorageException("не вдалося записати файл " + path, exception);
        }
    }
}

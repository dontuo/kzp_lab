package ua.lpnu.kzp;

/**
 * Помилка збереження, читання або розбору даних.
 *
 * <p>Виняток перевірюваний (checked): виклик експорту чи імпорту має явно вирішити,
 * що робити з помилкою. {@link #getMessage()} пояснює, яка операція не вдалася, а
 * {@link #getCause()} зберігає першопричину – {@code IOException},
 * {@code NumberFormatException}, помилку рефлексії тощо.
 */
public class DataStorageException extends Exception {
    private static final long serialVersionUID = 1L;

    /**
     * Створює виняток без першопричини, коли помилку виявила сама програма.
     *
     * @param message опис операції та причини
     */
    public DataStorageException(String message) {
        super(message);
    }

    /**
     * Створює виняток, що зберігає першопричину.
     *
     * @param message опис операції, яка не вдалася
     * @param cause початковий виняток
     */
    public DataStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}

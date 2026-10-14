package ua.lpnu.kzp;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Знаходить рефлексією поля з {@link CsvColumn} і читає їхні значення.
 *
 * <p>Не знає жодного конкретного класу: працює з будь-яким класом, де є анотовані поля,
 * зокрема успадкованими від надкласів. Порядок полів детермінований – за Java-іменем
 * поля, бо {@link Class#getDeclaredFields()} не гарантує порядку.
 */
final class AnnotatedFields {
    /* Знайдені поля для кожного класу; повторний експорт того самого класу не сканує його знову. */
    private static final Map<Class<?>, List<Field>> CACHE = new ConcurrentHashMap<>();

    /* Забороняє створення екземплярів службового класу. */
    private AnnotatedFields() {
    }

    /**
     * Повертає анотовані поля класу з кешу або знаходить їх.
     *
     * @param type клас записів
     * @return незмінний список полів, упорядкований за іменем
     * @throws DataStorageException якщо полів немає, назви стовпців повторюються або доступ заборонено
     */
    static List<Field> of(Class<?> type) throws DataStorageException {
        List<Field> cached = CACHE.get(type);
        if (cached != null) {
            return cached;
        }
        List<Field> fields = scan(type);
        // Якщо інший потік уже поклав список у кеш, використовуємо його, щоб усі бачили той самий.
        List<Field> previous = CACHE.putIfAbsent(type, fields);
        return previous != null ? previous : fields;
    }

    /**
     * Знаходить анотовані поля без кешу; окремий метод потрібен для порівняння часу.
     *
     * @param type клас записів
     * @return незмінний список полів, упорядкований за іменем
     * @throws DataStorageException якщо полів немає, назви стовпців повторюються або доступ заборонено
     */
    static List<Field> scan(Class<?> type) throws DataStorageException {
        // Проходимо ланцюжок надкласів до Object, щоб врахувати й успадковані поля.
        List<Field> fields = Stream.<Class<?>>iterate(type, current -> current != null && current != Object.class,
                        current -> current.getSuperclass())
                .flatMap(current -> Arrays.stream(current.getDeclaredFields()))
                .filter(field -> field.isAnnotationPresent(CsvColumn.class))
                .filter(field -> !Modifier.isStatic(field.getModifiers()))
                .sorted(Comparator.comparing(Field::getName)
                        .thenComparing(field -> field.getDeclaringClass().getName()))
                .toList();

        if (fields.isEmpty()) {
            throw new DataStorageException("клас " + type.getName() + " не має полів з @CsvColumn");
        }
        long distinctNames = fields.stream().map(AnnotatedFields::columnName).collect(Collectors.toSet()).size();
        if (distinctNames != fields.size()) {
            throw new DataStorageException("клас " + type.getName() + " має повторювані назви стовпців");
        }
        for (Field field : fields) {
            // trySetAccessible не кидає виняток, а повертає false – тому перевіряємо результат.
            if (!field.trySetAccessible()) {
                throw new DataStorageException("немає доступу до поля " + field.getName() + " класу " + type.getName());
            }
        }
        return fields;
    }

    /**
     * Повертає назву стовпця з анотації поля.
     *
     * @param field анотоване поле
     * @return значення {@link CsvColumn#value()}
     */
    static String columnName(Field field) {
        return field.getAnnotation(CsvColumn.class).value();
    }

    /**
     * Читає значення поля об'єкта.
     *
     * @param field поле, отримане з {@link #of(Class)}
     * @param record об'єкт, з якого читається значення
     * @return значення поля, можливо {@code null}
     * @throws DataStorageException якщо значення неможливо прочитати
     */
    static Object read(Field field, Object record) throws DataStorageException {
        try {
            return field.get(record);
        } catch (IllegalAccessException exception) {
            throw new DataStorageException("не вдалося прочитати поле " + field.getName(), exception);
        }
    }
}

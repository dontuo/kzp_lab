package ua.lpnu.kzp;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Типобезпечне сховище об'єктів у пам'яті.
 *
 * <p>Параметр типу {@code T} задається під час створення: {@code Repository<Book>}
 * приймає лише книги, і компілятор не дасть додати інший тип чи потребувати
 * приведення під час читання.
 *
 * @param <T> тип записів
 */
public final class Repository<T> {
    private final List<T> items = new ArrayList<>();

    /**
     * Додає запис у кінець сховища.
     *
     * @param item запис
     * @throws NullPointerException якщо {@code item} дорівнює {@code null}
     */
    public void add(T item) {
        items.add(Objects.requireNonNull(item, "запис не може бути null"));
    }

    /**
     * Повертає незмінний знімок усіх записів у порядку додавання.
     *
     * <p>Знімок не змінюється після наступних {@link #add(Object)}, а спроба змінити
     * його кидає {@link UnsupportedOperationException}, тому зовнішній код не може
     * змінити вміст сховища.
     *
     * @return копія списку записів
     */
    public List<T> all() {
        return List.copyOf(items);
    }

    /**
     * Повертає записи, що задовольняють умову, у порядку додавання.
     *
     * <p>{@code ? super T} дозволяє передати умову для {@code T} або його супертипу:
     * наприклад, {@code Predicate<Book>} для {@code Repository<PrintedBook>}.
     *
     * @param condition умова відбору
     * @return незмінний список знайдених записів
     */
    public List<T> find(Predicate<? super T> condition) {
        Objects.requireNonNull(condition, "умова не може бути null");
        return items.stream().filter(condition).toList();
    }

    /**
     * Повертає кількість записів.
     *
     * @return кількість доданих записів
     */
    public int size() {
        return items.size();
    }
}

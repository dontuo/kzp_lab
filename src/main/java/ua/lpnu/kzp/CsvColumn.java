package ua.lpnu.kzp;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Позначає поле, яке експортується в CSV, і задає назву стовпця.
 *
 * <p>{@code RUNTIME} залишає анотацію в класі під час виконання, тому експортер
 * знаходить її рефлексією. {@code FIELD} дозволяє ставити анотацію лише на поля.
 * Анотація задає тільки текст заголовка; порядок стовпців визначає експортер.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface CsvColumn {
    /**
     * Повертає назву CSV-стовпця.
     *
     * @return текст заголовка стовпця
     */
    String value();
}

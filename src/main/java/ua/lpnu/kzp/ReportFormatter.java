package ua.lpnu.kzp;

import java.nio.file.Path;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Locale;

/** Формує текст звіту, однаковий для консолі та файла. */
public final class ReportFormatter {
    /* Шаблон рядка таблиці: назва показника вирівняна ліворуч у полі шириною 28 символів. */
    private static final String ROW = "%-28s %s%n";
    /* Рядок таблиці форматів: назва, кількість книг, вартість придбання. */
    private static final String FORMAT_ROW = "%-28s %-6s %s%n";

    /* Забороняє створення екземплярів службового класу. */
    private ReportFormatter() {
    }

    /**
     * Формує звіт за показниками каталогу та списком пропущених рядків.
     *
     * <p>Числа форматуються з {@link Locale#ROOT}, тому десятковий роздільник –
     * завжди крапка, незалежно від налаштувань ОС.
     *
     * @param input шлях до вхідного файла, який показується в заголовку
     * @param summary обчислені показники
     * @param errors повідомлення про пропущені рядки
     * @return готовий текст звіту
     */
    public static String format(Path input, CatalogSummary summary, List<String> errors) {
        StringBuilder report = new StringBuilder();
        report.append(String.format(Locale.ROOT, "Звіт: каталог книжок%n"));
        report.append(String.format(Locale.ROOT, "Вхідний файл: %s%n%n", input));

        report.append(String.format(Locale.ROOT, ROW, "Показник", "Значення"));
        report.append(String.format(Locale.ROOT, ROW, "Коректних записів",
                String.format(Locale.ROOT, "%d", summary.getCount())));
        report.append(String.format(Locale.ROOT, ROW, "Середня кількість сторінок",
                String.format(Locale.ROOT, "%.2f", summary.getAveragePages())));
        report.append(String.format(Locale.ROOT, ROW, "Найдорожча книга", mostExpensive(summary)));
        report.append(String.format(Locale.ROOT, ROW, "Сумарна вартість каталогу",
                String.format(Locale.ROOT, "%.2f грн", summary.getTotalPrice())));

        // Новий блок ЛР № 3 додано після таблиці ЛР № 1, щоб попередні рядки звіту не змінилися.
        report.append(String.format(Locale.ROOT, "%n" + FORMAT_ROW, "Формат видання", "Книг", "Вартість придбання"));
        report.append(summary.getByFormat().entrySet().stream()
                .map(entry -> String.format(Locale.ROOT, FORMAT_ROW, entry.getKey().getLabel(),
                        String.format(Locale.ROOT, "%d", entry.getValue().count()),
                        String.format(Locale.ROOT, "%.2f грн", entry.getValue().purchaseCost())))
                .collect(Collectors.joining()));

        report.append(String.format(Locale.ROOT, "%nПропущено рядків: %d%n", errors.size()));
        report.append(errors.stream()
                .map(error -> String.format(Locale.ROOT, "  %s%n", error))
                .collect(Collectors.joining()));
        return report.toString();
    }

    /**
     * Формує блок із результатами запитів {@link BookQueries}.
     *
     * <p>Блок додається в кінець звіту, тому попередні рядки звіту не змінюються.
     *
     * @param books коректні книги
     * @return текст блоку, що починається з порожнього рядка
     */
    public static String formatQueries(List<Book> books) {
        StringBuilder block = new StringBuilder();
        block.append(String.format(Locale.ROOT, "%n" + ROW, "Запит", "Результат"));
        block.append(String.format(Locale.ROOT, ROW,
                String.format(Locale.ROOT, "Книги від %d року", BookQueries.RECENT_YEAR),
                orNoData(String.join(", ", BookQueries.titles(
                        BookQueries.publishedSince(books, BookQueries.RECENT_YEAR))))));
        block.append(String.format(Locale.ROOT, ROW, "Кількість книг за жанром",
                orNoData(joinCounts(BookQueries.countByGenre(books)))));
        block.append(String.format(Locale.ROOT, ROW, "Статистика сторінок",
                pageStatistics(BookQueries.pageStatistics(books))));
        block.append(String.format(Locale.ROOT, ROW,
                String.format(Locale.ROOT, "Топ-%d за сторінками", BookQueries.TOP_SIZE),
                orNoData(BookQueries.topByPages(books, BookQueries.TOP_SIZE).stream()
                        .map(book -> String.format(Locale.ROOT, "%s (%d)", book.getTitle(), book.getPages()))
                        .collect(Collectors.joining(", ")))));
        block.append(String.format(Locale.ROOT, ROW, "Жанри за форматом",
                orNoData(BookQueries.countByFormatAndGenre(books).entrySet().stream()
                        .map(entry -> entry.getKey().getLabel() + ": " + joinCounts(entry.getValue()))
                        .collect(Collectors.joining("; ")))));
        return block.toString();
    }

    /* «жанр кількість» через кому, у порядку ключів карти. */
    private static String joinCounts(Map<String, Long> counts) {
        return counts.entrySet().stream()
                .map(entry -> String.format(Locale.ROOT, "%s %d", entry.getKey(), entry.getValue()))
                .collect(Collectors.joining(", "));
    }

    /* Для порожньої статистики мінімум і максимум не мають сенсу (MAX_VALUE і MIN_VALUE). */
    private static String pageStatistics(IntSummaryStatistics statistics) {
        if (statistics.getCount() == 0) {
            return "немає даних";
        }
        return String.format(Locale.ROOT, "кількість %d, мін. %d, макс. %d, сума %d, середнє %.2f",
                statistics.getCount(), statistics.getMin(), statistics.getMax(), statistics.getSum(),
                statistics.getAverage());
    }

    /* Порожній результат запиту показується явно, а не порожнім місцем. */
    private static String orNoData(String text) {
        return text.isEmpty() ? "немає даних" : text;
    }

    /* Описує найдорожчу книгу або повідомляє, що книг немає. */
    private static String mostExpensive(CatalogSummary summary) {
        if (summary.getMostExpensiveTitle() == null) {
            return "немає даних";
        }
        return String.format(Locale.ROOT, "%s (%.2f грн)", summary.getMostExpensiveTitle(), summary.getMaxPrice());
    }
}

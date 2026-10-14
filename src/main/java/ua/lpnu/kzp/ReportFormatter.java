package ua.lpnu.kzp;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
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
        for (Map.Entry<BookFormat, FormatTotals> entry : summary.getByFormat().entrySet()) {
            report.append(String.format(Locale.ROOT, FORMAT_ROW, entry.getKey().getLabel(),
                    String.format(Locale.ROOT, "%d", entry.getValue().count()),
                    String.format(Locale.ROOT, "%.2f грн", entry.getValue().purchaseCost())));
        }

        report.append(String.format(Locale.ROOT, "%nПропущено рядків: %d%n", errors.size()));
        for (String error : errors) {
            report.append(String.format(Locale.ROOT, "  %s%n", error));
        }
        return report.toString();
    }

    /* Описує найдорожчу книгу або повідомляє, що книг немає. */
    private static String mostExpensive(CatalogSummary summary) {
        if (summary.getMostExpensiveTitle() == null) {
            return "немає даних";
        }
        return String.format(Locale.ROOT, "%s (%.2f грн)", summary.getMostExpensiveTitle(), summary.getMaxPrice());
    }
}

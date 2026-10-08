package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class ReportFormatterTest {
    @Test
    void reportContainsAllIndicatorsAndErrors() {
        CatalogSummary summary = new CatalogSummary();
        summary.add(BookParser.split("Кобзар;Тарас Шевченко;320;250.00"));
        summary.add(BookParser.split("Лісова пісня;Леся Українка;180;145.50"));

        String report = ReportFormatter.format(Path.of("data", "input.csv"), summary,
                List.of("Рядок 3: порожній рядок"));

        assertTrue(report.contains("Коректних записів"));
        assertTrue(report.contains("250.00"));
        assertTrue(report.contains("Кобзар (250.00 грн)"));
        assertTrue(report.contains("395.50 грн"));
        assertTrue(report.contains("Пропущено рядків: 1"));
        assertTrue(report.contains("Рядок 3: порожній рядок"));
    }

    @Test
    void decimalSeparatorIsDotEvenWithCommaLocale() {
        Locale original = Locale.getDefault();
        // Українська локаль використовує кому; звіт усе одно має містити крапку.
        Locale.setDefault(Locale.forLanguageTag("uk-UA"));
        try {
            CatalogSummary summary = new CatalogSummary();
            summary.add(BookParser.split("А;Б;3;1.50"));
            String report = ReportFormatter.format(Path.of("in.csv"), summary, List.of());
            assertTrue(report.contains("1.50 грн"), report);
            assertFalse(report.contains("1,50"), report);
        } finally {
            Locale.setDefault(original);
        }
    }

    @Test
    void emptyCatalogIsReportedWithoutBook() {
        String report = ReportFormatter.format(Path.of("in.csv"), new CatalogSummary(), List.of());
        assertTrue(report.contains("немає даних"));
        assertTrue(report.contains("0.00"));
    }

    @Test
    void reportUsesPlatformLineSeparator() {
        String report = ReportFormatter.format(Path.of("in.csv"), new CatalogSummary(), List.of());
        assertTrue(report.endsWith(System.lineSeparator()));
    }
}

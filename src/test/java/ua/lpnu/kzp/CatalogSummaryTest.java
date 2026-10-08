package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class CatalogSummaryTest {
    /* Допустима похибка для порівняння чисел із рухомою крапкою. */
    private static final double DELTA = 0.0001;

    /* Створює підсумок із готових рядків каталогу. */
    private static CatalogSummary summaryOf(String... lines) {
        CatalogSummary summary = new CatalogSummary();
        for (String line : lines) {
            summary.add(BookParser.split(line));
        }
        return summary;
    }

    @Test
    void emptyCatalogDoesNotDivideByZero() {
        CatalogSummary summary = new CatalogSummary();
        assertEquals(0, summary.getCount());
        assertEquals(0.0, summary.getAveragePages(), DELTA);
        assertEquals(0.0, summary.getTotalPrice(), DELTA);
        assertNull(summary.getMostExpensiveTitle());
    }

    @Test
    void averageKeepsFractionalPart() {
        // 100 + 101 = 201; цілочислове ділення дало б 100 замість 100.5.
        CatalogSummary summary = summaryOf("А;Б;100;1.00", "В;Г;101;1.00");
        assertEquals(100.5, summary.getAveragePages(), DELTA);
    }

    @Test
    void sampleDataGivesExpectedIndicators() {
        CatalogSummary summary = summaryOf(
                "Кобзар;Тарас Шевченко;320;250.00",
                "Лісова пісня;Леся Українка;180;145.50",
                "Тигролови;Іван Багряний;288;199.99",
                "Інститутка;Марко Вовчок;96;89.90",
                "Intermezzo;Михайло Коцюбинський;32;0.00",
                "Захар Беркут;Іван Франко;224;175.00");

        assertEquals(6, summary.getCount());
        assertEquals(190.0, summary.getAveragePages(), DELTA);
        assertEquals("Кобзар", summary.getMostExpensiveTitle());
        assertEquals(250.0, summary.getMaxPrice(), DELTA);
        assertEquals(860.39, summary.getTotalPrice(), DELTA);
    }

    @Test
    void mostExpensiveIsFoundAnywhereInList() {
        CatalogSummary summary = summaryOf("А;Б;10;5.00", "Дорога;Автор;10;99.99", "В;Г;10;20.00");
        assertEquals("Дорога", summary.getMostExpensiveTitle());
    }

    @Test
    void firstBookWinsOnEqualPrice() {
        CatalogSummary summary = summaryOf("Перша;Автор;10;50.00", "Друга;Автор;10;50.00");
        assertEquals("Перша", summary.getMostExpensiveTitle());
    }

    @Test
    void freeBookCanBeMostExpensiveWhenItIsTheOnlyOne() {
        CatalogSummary summary = summaryOf("Intermezzo;Михайло Коцюбинський;32;0.00");
        assertEquals("Intermezzo", summary.getMostExpensiveTitle());
        assertEquals(0.0, summary.getMaxPrice(), DELTA);
    }
}

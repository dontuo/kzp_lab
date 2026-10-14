package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class FormatTotalsTest {
    /* Допустима похибка для порівняння чисел із рухомою крапкою. */
    private static final double DELTA = 0.0001;

    @Test
    void plusAddsOneBookUsingItsOwnCostRule() {
        FormatTotals printed = FormatTotals.ZERO.plus(new PrintedBook("А", "Б", 1, 100.0));
        FormatTotals electronic = FormatTotals.ZERO.plus(new ElectronicBook("А", "Б", 1, 100.0, null, null));
        assertEquals(new FormatTotals(1, 150.0), printed);
        assertEquals(new FormatTotals(1, 100.0), electronic);
    }

    @Test
    void plusDoesNotChangeOperand() {
        FormatTotals start = new FormatTotals(2, 10.0);
        start.plus(new ElectronicBook("А", "Б", 1, 5.0, null, null));
        assertEquals(new FormatTotals(2, 10.0), start);
    }

    @Test
    void invalidValuesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new FormatTotals(-1, 0.0));
        assertThrows(IllegalArgumentException.class, () -> new FormatTotals(0, -1.0));
        assertThrows(IllegalArgumentException.class, () -> new FormatTotals(0, Double.NaN));
    }

    @Test
    void summaryGroupsBooksByFormatWithoutTypeChecks() {
        CatalogSummary summary = CatalogSummary.of(List.of(
                Book.fromCsv("Кобзар;Тарас Шевченко;320;250.00;PRINTED;1840;поезія"),
                Book.fromCsv("Тигролови;Іван Багряний;288;99.00;ELECTRONIC;1944;роман"),
                Book.fromCsv("Захар Беркут;Іван Франко;224;175.00")));

        Map<BookFormat, FormatTotals> byFormat = summary.getByFormat();
        assertEquals(List.of(BookFormat.PRINTED, BookFormat.ELECTRONIC), List.copyOf(byFormat.keySet()));
        assertEquals(2, byFormat.get(BookFormat.PRINTED).count());
        assertEquals(250.0 + 175.0 + 2 * PrintedBook.DELIVERY_COST, byFormat.get(BookFormat.PRINTED).purchaseCost(),
                DELTA);
        assertEquals(new FormatTotals(1, 99.0), byFormat.get(BookFormat.ELECTRONIC));
    }

    @Test
    void emptySummaryHasZeroForEveryFormat() {
        Map<BookFormat, FormatTotals> byFormat = new CatalogSummary().getByFormat();
        assertEquals(FormatTotals.ZERO, byFormat.get(BookFormat.PRINTED));
        assertEquals(FormatTotals.ZERO, byFormat.get(BookFormat.ELECTRONIC));
    }

    @Test
    void byFormatCannotBeModifiedFromOutside() {
        Map<BookFormat, FormatTotals> byFormat = new CatalogSummary().getByFormat();
        assertThrows(UnsupportedOperationException.class, () -> byFormat.put(BookFormat.PRINTED, FormatTotals.ZERO));
    }
}

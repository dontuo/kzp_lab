package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Year;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Перевіряє базовий тип, підтипи та поліморфну поведінку. */
class BookHierarchyTest {
    /* Допустима похибка для порівняння чисел із рухомою крапкою. */
    private static final double DELTA = 0.0001;

    /* --- Підтипи та поліморфізм --- */

    @Test
    void printedBookAddsDelivery() {
        Book book = new PrintedBook("Кобзар", "Тарас Шевченко", 320, 250.0);
        assertEquals(BookFormat.PRINTED, book.getFormat());
        assertEquals(250.0 + PrintedBook.DELIVERY_COST, book.purchaseCost(), DELTA);
    }

    @Test
    void electronicBookCostsItsPrice() {
        Book book = new ElectronicBook("Тигролови", "Іван Багряний", 288, 99.0, 1944, "роман");
        assertEquals(BookFormat.ELECTRONIC, book.getFormat());
        assertEquals(99.0, book.purchaseCost(), DELTA);
    }

    @Test
    void sameDataGivesDifferentCostInDifferentSubtypes() {
        Book printed = new PrintedBook("Книга", "Автор", 100, 80.0, 2020, "роман");
        Book electronic = new ElectronicBook("Книга", "Автор", 100, 80.0, 2020, "роман");
        assertEquals(PrintedBook.DELIVERY_COST, printed.purchaseCost() - electronic.purchaseCost(), DELTA);
    }

    @Test
    void polymorphicCollectionIsProcessedThroughBaseType() {
        // Список спільного типу: цикл не знає, які підтипи в ньому лежать.
        List<Book> books = List.of(
                new PrintedBook("А", "Б", 10, 100.0),
                new ElectronicBook("В", "Г", 10, 40.0, 2020, "роман"),
                new PrintedBook("Д", "Е", 10, 0.0));
        double total = 0;
        for (Book book : books) {
            total += book.purchaseCost();
        }
        assertEquals(100.0 + 50.0 + 40.0 + 0.0 + 50.0, total, DELTA);
    }

    @Test
    void sharedInvariantsApplyToEverySubtype() {
        // Перевірки живуть лише в конструкторі Book, але діють для обох підтипів.
        assertThrows(IllegalArgumentException.class, () -> new PrintedBook("Назва", "Автор", 0, 1.0));
        assertThrows(IllegalArgumentException.class, () -> new ElectronicBook("Назва", "Автор", 0, 1.0, null, null));
        assertThrows(NullPointerException.class, () -> new ElectronicBook(null, "Автор", 1, 1.0, null, null));
    }

    /* --- Рік і жанр --- */

    @Test
    void legacyConstructorHasNoYearAndGenre() {
        Book book = new PrintedBook("Кобзар", "Тарас Шевченко", 320, 250.0);
        assertEquals(OptionalInt.empty(), book.getYear());
        assertEquals(Optional.empty(), book.getGenre());
    }

    @Test
    void yearBoundariesAreAccepted() {
        int currentYear = Year.now().getValue();
        assertEquals(OptionalInt.of(Book.FIRST_PRINTED_YEAR),
                new PrintedBook("А", "Б", 1, 1.0, Book.FIRST_PRINTED_YEAR, "роман").getYear());
        assertEquals(OptionalInt.of(currentYear), new PrintedBook("А", "Б", 1, 1.0, currentYear, "роман").getYear());
    }

    @Test
    void yearOutsideRangeIsRejected() {
        int nextYear = Year.now().getValue() + 1;
        assertThrows(IllegalArgumentException.class,
                () -> new PrintedBook("А", "Б", 1, 1.0, Book.FIRST_PRINTED_YEAR - 1, "роман"));
        assertThrows(IllegalArgumentException.class, () -> new PrintedBook("А", "Б", 1, 1.0, nextYear, "роман"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  "})
    void blankGenreIsRejected(String genre) {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> new ElectronicBook("А", "Б", 1, 1.0, 2020, genre));
        assertEquals("порожній жанр", error.getMessage());
    }

    /* --- fromCsv: повний формат --- */

    @Test
    void fullPrintedRowCreatesPrintedBook() {
        Book book = Book.fromCsv("Кобзар;Тарас Шевченко;320;250.00;PRINTED;1840;поезія");
        PrintedBook printed = assertInstanceOf(PrintedBook.class, book);
        assertEquals("Кобзар", printed.getTitle());
        assertEquals(OptionalInt.of(1840), printed.getYear());
        assertEquals(Optional.of("поезія"), printed.getGenre());
    }

    @Test
    void fullElectronicRowCreatesElectronicBook() {
        Book book = Book.fromCsv("Амадока;Софія Андрухович;832;350.00;ELECTRONIC;2020;роман");
        assertInstanceOf(ElectronicBook.class, book);
        assertEquals(350.0, book.purchaseCost(), DELTA);
    }

    @Test
    void legacyRowCreatesPrintedBookWithoutYear() {
        Book book = Book.fromCsv("Захар Беркут;Іван Франко;224;175.00");
        assertInstanceOf(PrintedBook.class, book);
        assertEquals(OptionalInt.empty(), book.getYear());
    }

    @Test
    void unknownFormatInRowIsRejected() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> Book.fromCsv("Карбід;Андрій Любка;256;150.00;AUDIO;2015;роман"));
        assertEquals("невідомий формат книги: \"AUDIO\"", error.getMessage());
    }

    @Test
    void emptyYearIsRejected() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> Book.fromCsv("Енеїда;Іван Котляревський;304;140.00;PRINTED;;поема"));
        assertEquals("порожній рік видання", error.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"тисяча", "19.40", " 1840", "+1840"})
    void nonIntegerYearIsRejected(String year) {
        String error = BookParser.validate("Кобзар;Тарас Шевченко;320;250.00;PRINTED;" + year + ";поезія");
        assertTrue(error.startsWith("рік видання не є цілим числом"), error);
    }

    @Test
    void yearOverflowKeepsCause() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> Book.fromCsv("А;Б;1;1.00;PRINTED;99999999999;роман"));
        assertInstanceOf(NumberFormatException.class, error.getCause());
    }

    @Test
    void futureYearInRowIsRejectedByConstructor() {
        String error = BookParser.validate("Місто;Валер'ян Підмогильний;352;180.00;PRINTED;3000;роман");
        assertTrue(error.startsWith("рік видання має бути від 1450 до"), error);
    }

    @Test
    void sixFieldsAreRejected() {
        assertEquals("очікується 4 або 7 полів, отримано 6",
                BookParser.validate("Хіба ревуть воли;Панас Мирний;400;150.00;PRINTED;1880"));
    }

    /* --- Будівник --- */

    @Test
    void builderCreatesRequestedSubtype() {
        Book book = Book.builder().format(BookFormat.ELECTRONIC)
                .title("Доця").author("Тамара Горіха Зерня").pages(368).price(120.0).year(2019).genre("роман")
                .build();
        assertInstanceOf(ElectronicBook.class, book);
        assertEquals(OptionalInt.of(2019), book.getYear());
    }

    @Test
    void builderDefaultsToPrinted() {
        Book book = Book.builder().title("А").author("Б").pages(1).price(1.0).build();
        assertInstanceOf(PrintedBook.class, book);
    }
}

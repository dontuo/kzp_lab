package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class BookParserTest {
    @Test
    void validLineHasNoError() {
        assertNull(BookParser.validate("Кобзар;Тарас Шевченко;320;250.00"));
    }

    @Test
    void ukrainianTextIsKeptAsIs() {
        String[] fields = BookParser.split("Кайдашева сім'я;Іван Нечуй-Левицький;208;120.00");
        assertArrayEquals(new String[] {"Кайдашева сім'я", "Іван Нечуй-Левицький", "208", "120.00"}, fields);
    }

    @Test
    void zeroPriceIsValid() {
        assertNull(BookParser.validate("Intermezzo;Михайло Коцюбинський;32;0.00"));
    }

    @Test
    void splitKeepsEmptyLastField() {
        // Без limit = -1 порожнє останнє поле було б відкинуте.
        assertEquals(4, BookParser.split("Земля;Ольга Кобилянська;272;").length);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", " \t "})
    void blankLineIsRejected(String line) {
        assertEquals("порожній рядок", BookParser.validate(line));
    }

    @Test
    void tooFewFieldsAreRejected() {
        assertEquals("очікується 4 поля, отримано 3", BookParser.validate("Сад Гетсиманський;Іван Багряний;400"));
    }

    @Test
    void tooManyFieldsAreRejected() {
        assertEquals("очікується 4 поля, отримано 5",
                BookParser.validate("Місто;Валер'ян Підмогильний;352;180.00;2026"));
    }

    @ParameterizedTest
    @ValueSource(strings = {";Василь Барка;416;210.00", "   ;Ольга Кобилянська;256;130.00"})
    void blankTitleIsRejected(String line) {
        assertEquals("порожня назва книги", BookParser.validate(line));
    }

    @Test
    void blankAuthorIsRejected() {
        assertEquals("порожній автор", BookParser.validate("Кайдашева сім'я;;208;120.00"));
    }

    @Test
    void emptyPagesAreRejected() {
        assertEquals("порожня кількість сторінок", BookParser.validate("Хіба ревуть воли;Панас Мирний;;150.00"));
    }

    @Test
    void emptyPriceIsRejected() {
        assertEquals("порожня ціна", BookParser.validate("Земля;Ольга Кобилянська;272;"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "240.5", " 280", "280 ", "+280", "1e2"})
    void nonIntegerPagesAreRejected(String pages) {
        String error = BookParser.validate("Книга;Автор;" + pages + ";100.00");
        assertNotNull(error);
        assertTrue(error.startsWith("кількість сторінок не є цілим числом"), error);
    }

    @Test
    void pagesOverflowIsRejected() {
        assertEquals("кількість сторінок завелика: \"99999999999\"",
                BookParser.validate("Собор;Олесь Гончар;99999999999;200.00"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ціна", "99,50", ".5", "5.", "NaN", "Infinity", "-Infinity"})
    void nonNumericPriceIsRejected(String price) {
        String error = BookParser.validate("Книга;Автор;100;" + price);
        assertNotNull(error);
        assertTrue(error.startsWith("ціна не є числом"), error);
    }

    /* #11: Double.parseDouble приймає суфікси типів, експоненту та шістнадцятковий запис. */
    @ParameterizedTest
    @ValueSource(strings = {"10d", "10D", "10f", "1e3", "1E3", "0x1p3"})
    void javaSpecificPriceFormatsAreRejected(String price) {
        String error = BookParser.validate("Книга;Автор;100;" + price);
        assertNotNull(error, price);
        assertTrue(error.startsWith("ціна не є числом"), error);
    }

    /* #12: пробіли навколо числа відкидаються однаково для обох числових полів. */
    @ParameterizedTest
    @ValueSource(strings = {"А;Б; 280;10.00", "А;Б;280 ;10.00", "В;Г;280; 10.00", "В;Г;280;10.00 ", "В;Г;280;\t10.00"})
    void spacesAroundNumbersAreRejectedInBothFields(String line) {
        assertNotNull(BookParser.validate(line), line);
    }

    @ParameterizedTest
    @ValueSource(strings = {"10", "10.5", "10.50", "0.99"})
    void plainDecimalPriceIsValid(String price) {
        assertNull(BookParser.validate("Книга;Автор;100;" + price));
    }

    @ParameterizedTest
    @ValueSource(strings = {"-5", "0"})
    void nonPositivePagesAreRejected(String pages) {
        String error = BookParser.validate("Маруся Чурай;Ліна Костенко;" + pages + ";160.00");
        assertNotNull(error);
        assertTrue(error.startsWith("кількість сторінок має бути додатною"), error);
    }

    @Test
    void negativePriceIsRejected() {
        assertEquals("від'ємна ціна: -99.00", BookParser.validate("Енеїда;Іван Котляревський;304;-99.00"));
    }

    @Test
    void hugePriceIsRejected() {
        // 400 цифр проходять перевірку формату, але parseDouble дає Infinity.
        String price = "9".repeat(400);
        String error = BookParser.validate("Книга;Автор;100;" + price);
        assertNotNull(error);
        assertTrue(error.startsWith("ціна має бути скінченним числом"), error);
    }
}

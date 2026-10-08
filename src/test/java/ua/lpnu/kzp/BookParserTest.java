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
    @ValueSource(strings = {"abc", "240.5", "99999999999", " 280"})
    void nonIntegerPagesAreRejected(String pages) {
        String error = BookParser.validate("Книга;Автор;" + pages + ";100.00");
        assertNotNull(error);
        assertTrue(error.startsWith("кількість сторінок не є цілим числом"), error);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ціна", "99,50"})
    void nonNumericPriceIsRejected(String price) {
        String error = BookParser.validate("Книга;Автор;100;" + price);
        assertNotNull(error);
        assertTrue(error.startsWith("ціна не є числом"), error);
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

    @ParameterizedTest
    @ValueSource(strings = {"NaN", "Infinity", "-Infinity"})
    void nonFinitePriceIsRejected(String price) {
        String error = BookParser.validate("Книга;Автор;100;" + price);
        assertNotNull(error);
        assertTrue(error.startsWith("ціна має бути скінченним числом"), error);
    }
}

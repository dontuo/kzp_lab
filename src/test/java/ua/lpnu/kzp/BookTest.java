package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class BookTest {
    /* Допустима похибка для порівняння чисел із рухомою крапкою. */
    private static final double DELTA = 0.0001;

    /* --- Конструктор --- */

    @Test
    void constructorKeepsValidValues() {
        Book book = new Book("Кобзар", "Тарас Шевченко", 320, 250.0);
        assertEquals("Кобзар", book.getTitle());
        assertEquals("Тарас Шевченко", book.getAuthor());
        assertEquals(320, book.getPages());
        assertEquals(250.0, book.getPrice(), DELTA);
    }

    @Test
    void boundaryValuesAreAccepted() {
        // Найменші допустимі значення: одна сторінка й безкоштовна книга.
        Book book = new Book("І", "Я", 1, 0.0);
        assertEquals(1, book.getPages());
        assertEquals(0.0, book.getPrice(), DELTA);
    }

    @Test
    void nullTitleIsRejected() {
        assertThrows(NullPointerException.class, () -> new Book(null, "Автор", 10, 1.0));
    }

    @Test
    void nullAuthorIsRejected() {
        assertThrows(NullPointerException.class, () -> new Book("Назва", null, 10, 1.0));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\t"})
    void blankTitleIsRejected(String title) {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> new Book(title, "Автор", 10, 1.0));
        assertEquals("порожня назва книги", error.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void blankAuthorIsRejected(String author) {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> new Book("Назва", author, 10, 1.0));
        assertEquals("порожній автор", error.getMessage());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, Integer.MIN_VALUE})
    void nonPositivePagesAreRejected(int pages) {
        assertThrows(IllegalArgumentException.class, () -> new Book("Назва", "Автор", pages, 1.0));
    }

    @Test
    void negativePriceIsRejected() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> new Book("Енеїда", "Іван Котляревський", 304, -99.0));
        assertEquals("від'ємна ціна: -99.00", error.getMessage());
    }

    @ParameterizedTest
    @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void nonFinitePriceIsRejected(double price) {
        // NaN окремо важливий: NaN < 0 дає false, тому без isFinite він пройшов би.
        assertThrows(IllegalArgumentException.class, () -> new Book("Назва", "Автор", 10, price));
    }

    /* --- Незмінність --- */

    @Test
    void allFieldsArePrivateFinal() {
        for (Field field : Book.class.getDeclaredFields()) {
            int modifiers = field.getModifiers();
            assertTrue(Modifier.isPrivate(modifiers), field.getName());
            assertTrue(Modifier.isFinal(modifiers), field.getName());
        }
        assertTrue(Modifier.isFinal(Book.class.getModifiers()), "клас не можна розширити й змінити поведінку");
    }

    @Test
    void thereAreNoSetters() {
        for (Method method : Book.class.getMethods()) {
            assertTrue(!method.getName().startsWith("set"), method.getName());
        }
    }

    /* --- fromCsv --- */

    @Test
    void fromCsvCreatesBook() {
        Book book = Book.fromCsv("Кобзар;Тарас Шевченко;320;250.00");
        assertEquals("Кобзар", book.getTitle());
        assertEquals("Тарас Шевченко", book.getAuthor());
        assertEquals(320, book.getPages());
        assertEquals(250.0, book.getPrice(), DELTA);
    }

    @Test
    void fromCsvKeepsUkrainianText() {
        Book book = Book.fromCsv("Кайдашева сім'я;Іван Нечуй-Левицький;208;120.00");
        assertEquals("Кайдашева сім'я", book.getTitle());
        assertEquals("Іван Нечуй-Левицький", book.getAuthor());
    }

    @Test
    void fromCsvRejectsNull() {
        assertThrows(NullPointerException.class, () -> Book.fromCsv(null));
    }

    @Test
    void fromCsvRejectsWrongFieldCount() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> Book.fromCsv("Сад Гетсиманський;Іван Багряний;400"));
        assertEquals("очікується 4 поля, отримано 3", error.getMessage());
    }

    @Test
    void fromCsvRejectsWrongNumber() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> Book.fromCsv("Жовтий князь;Василь Барка;abc;190.00"));
        assertEquals("кількість сторінок не є цілим числом: \"abc\"", error.getMessage());
    }

    @Test
    void fromCsvKeepsCauseOfOverflow() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> Book.fromCsv("Собор;Олесь Гончар;99999999999;200.00"));
        assertTrue(error.getCause() instanceof NumberFormatException);
    }

    @Test
    void fromCsvReportsInvariantFromConstructor() {
        // Формат правильний, але порушено інваріант: перевіряє вже конструктор.
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> Book.fromCsv("Маруся Чурай;Ліна Костенко;0;160.00"));
        assertEquals("кількість сторінок має бути додатною: 0", error.getMessage());
    }

    /* --- toString і PagePrice --- */

    @Test
    void toStringUsesDotEvenWithCommaLocale() {
        Locale original = Locale.getDefault();
        Locale.setDefault(Locale.forLanguageTag("uk-UA"));
        try {
            assertEquals("Лісова пісня (Леся Українка): 180 с., 145.50 грн",
                    new Book("Лісова пісня", "Леся Українка", 180, 145.5).toString());
        } finally {
            Locale.setDefault(original);
        }
    }

    @Test
    void pagePriceDescribesOneBook() {
        assertEquals(new PagePrice(320, 250.0), new Book("Кобзар", "Тарас Шевченко", 320, 250.0).pagePrice());
    }

    /* --- Будівник --- */

    @Test
    void builderCreatesSameBookAsConstructor() {
        Book built = Book.builder().title("Кобзар").author("Тарас Шевченко").pages(320).price(250.0).build();
        assertEquals(new Book("Кобзар", "Тарас Шевченко", 320, 250.0).toString(), built.toString());
    }

    @Test
    void builderDoesNotBypassValidation() {
        Book.Builder builder = Book.builder().title("Назва").author("Автор").pages(0).price(1.0);
        assertThrows(IllegalArgumentException.class, builder::build);
    }

    @Test
    void builderRequiresTitleAndAuthor() {
        assertThrows(NullPointerException.class, () -> Book.builder().pages(10).price(1.0).build());
    }
}

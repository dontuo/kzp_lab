package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class PagePriceTest {
    /* Допустима похибка для порівняння чисел із рухомою крапкою. */
    private static final double DELTA = 0.0001;

    @Test
    void componentsAreAccessible() {
        PagePrice value = new PagePrice(320, 250.0);
        assertEquals(320, value.pages());
        assertEquals(250.0, value.price(), DELTA);
    }

    @Test
    void recordUsesValueEquality() {
        // Різні об'єкти з однаковими компонентами рівні, і hashCode у них однаковий.
        PagePrice first = new PagePrice(180, 145.5);
        PagePrice second = new PagePrice(180, 145.5);
        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first, new PagePrice(180, 145.0));
    }

    @Test
    void plusReturnsNewValueAndKeepsOperands() {
        PagePrice first = new PagePrice(100, 10.0);
        PagePrice second = new PagePrice(50, 5.5);

        assertEquals(new PagePrice(150, 15.5), first.plus(second));
        // record незмінний: операнди лишилися тими самими.
        assertEquals(new PagePrice(100, 10.0), first);
        assertEquals(new PagePrice(50, 5.5), second);
    }

    @Test
    void zeroIsNeutralForPlus() {
        PagePrice value = new PagePrice(7, 3.0);
        assertEquals(value, PagePrice.ZERO.plus(value));
    }

    @Test
    void zeroPagesAndPriceAreAllowed() {
        assertEquals(PagePrice.ZERO, new PagePrice(0, 0.0));
    }

    @Test
    void negativePagesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new PagePrice(-1, 0.0));
    }

    @Test
    void negativeOrNonFinitePriceIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new PagePrice(0, -0.01));
        assertThrows(IllegalArgumentException.class, () -> new PagePrice(0, Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> new PagePrice(0, Double.POSITIVE_INFINITY));
    }

    @Test
    void toStringShowsComponents() {
        assertEquals("PagePrice[pages=320, price=250.0]", new PagePrice(320, 250.0).toString());
    }
}

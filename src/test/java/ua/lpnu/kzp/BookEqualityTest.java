package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

/** Перевіряє узгодженість equals і hashCode та їх роботу в HashSet і HashMap. */
class BookEqualityTest {
    private final Book kobzar = new PrintedBook("Кобзар", "Тарас Шевченко", 320, 250.0, 1840, "поезія");
    // Той самий друкований «Кобзар», але інший запис: інша ціна, сторінки й без року.
    private final Book kobzarOtherEdition = new PrintedBook("Кобзар", "Тарас Шевченко", 400, 300.0);
    private final Book kobzarElectronic = new ElectronicBook("Кобзар", "Тарас Шевченко", 320, 99.0, 1840, "поезія");

    @Test
    void equalsIsReflexive() {
        assertEquals(kobzar, kobzar);
    }

    @Test
    void sameTitleAuthorAndSubtypeAreEqual() {
        assertEquals(kobzar, kobzarOtherEdition);
        assertEquals(kobzarOtherEdition, kobzar);
    }

    @Test
    void equalBooksHaveEqualHashCodes() {
        assertEquals(kobzar.hashCode(), kobzarOtherEdition.hashCode());
    }

    @Test
    void differentSubtypesAreNotEqualEvenWithSameTitle() {
        // Друкована й електронна книга – різні видання, і рівність симетрична в обидва боки.
        assertNotEquals(kobzar, kobzarElectronic);
        assertNotEquals(kobzarElectronic, kobzar);
    }

    @Test
    void differentTitleOrAuthorAreNotEqual() {
        assertNotEquals(kobzar, new PrintedBook("Кобзар 2", "Тарас Шевченко", 320, 250.0));
        assertNotEquals(kobzar, new PrintedBook("Кобзар", "Інший автор", 320, 250.0));
    }

    @Test
    void equalsIsTransitive() {
        Book third = new PrintedBook("Кобзар", "Тарас Шевченко", 1, 0.0);
        assertEquals(kobzar, kobzarOtherEdition);
        assertEquals(kobzarOtherEdition, third);
        assertEquals(kobzar, third);
    }

    @Test
    void notEqualToNullOrOtherType() {
        assertFalse(kobzar.equals(null));
        assertFalse(kobzar.equals("Кобзар"));
    }

    @Test
    void hashSetKeepsOneBookPerTitleAuthorAndFormat() {
        Set<Book> set = new HashSet<>(List.of(kobzar, kobzarOtherEdition, kobzarElectronic));
        assertEquals(2, set.size());
        assertTrue(set.contains(new PrintedBook("Кобзар", "Тарас Шевченко", 1, 1.0)));
        assertTrue(set.contains(new ElectronicBook("Кобзар", "Тарас Шевченко", 1, 1.0, null, null)));
    }

    @Test
    void hashMapFindsValueByEqualKey() {
        Map<Book, String> shelves = new HashMap<>();
        shelves.put(kobzar, "полиця 1");
        shelves.put(kobzarElectronic, "сервер");
        // Інший об'єкт, але рівний ключу: map знаходить те саме значення.
        assertEquals("полиця 1", shelves.get(kobzarOtherEdition));
        assertEquals(2, shelves.size());
    }
}

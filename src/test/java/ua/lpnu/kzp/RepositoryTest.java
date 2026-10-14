package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

class RepositoryTest {
    private static final Book KOBZAR = Book.fromCsv("Кобзар;Тарас Шевченко;320;250.00;PRINTED;1840;поезія");
    private static final Book AMADOKA = Book.fromCsv("Амадока;Софія Андрухович;832;350.00;ELECTRONIC;2020;роман");

    @Test
    void newRepositoryIsEmpty() {
        Repository<Book> repository = new Repository<>();
        assertEquals(0, repository.size());
        assertEquals(List.of(), repository.all());
    }

    @Test
    void allReturnsItemsInInsertionOrder() {
        Repository<Book> repository = new Repository<>();
        repository.add(AMADOKA);
        repository.add(KOBZAR);
        assertEquals(List.of(AMADOKA, KOBZAR), repository.all());
        assertEquals(2, repository.size());
    }

    @Test
    void snapshotDoesNotChangeAfterAdd() {
        Repository<Book> repository = new Repository<>();
        repository.add(KOBZAR);
        List<Book> snapshot = repository.all();
        repository.add(AMADOKA);
        assertEquals(List.of(KOBZAR), snapshot);
    }

    @Test
    void snapshotCannotBeModified() {
        Repository<Book> repository = new Repository<>();
        repository.add(KOBZAR);
        List<Book> snapshot = repository.all();
        assertThrows(UnsupportedOperationException.class, () -> snapshot.add(AMADOKA));
        assertThrows(UnsupportedOperationException.class, snapshot::clear);
        assertEquals(1, repository.size());
    }

    @Test
    void nullIsRejected() {
        Repository<Book> repository = new Repository<>();
        assertThrows(NullPointerException.class, () -> repository.add(null));
    }

    @Test
    void findFiltersByPredicate() {
        Repository<Book> repository = new Repository<>();
        repository.add(KOBZAR);
        repository.add(AMADOKA);
        assertEquals(List.of(AMADOKA), repository.find(book -> book.getPages() > 500));
        assertEquals(List.of(), repository.find(book -> book.getPages() > 1000));
    }

    @Test
    void findAcceptsPredicateOfSupertype() {
        // Predicate<Book> підходить для Repository<PrintedBook> завдяки ? super T.
        Repository<PrintedBook> printed = new Repository<>();
        printed.add(new PrintedBook("Кобзар", "Тарас Шевченко", 320, 250.0));
        printed.add(new PrintedBook("Інститутка", "Марко Вовчок", 96, 89.9));
        Predicate<Book> cheap = book -> book.getPrice() < 100;
        Predicate<Object> any = item -> true;

        assertEquals(1, printed.find(cheap).size());
        assertEquals(2, printed.find(any).size());
    }

    @Test
    void repositoryWorksWithAnyType() {
        Repository<String> strings = new Repository<>();
        strings.add("перший");
        strings.add("другий");
        assertTrue(strings.find(text -> text.startsWith("д")).contains("другий"));
    }
}

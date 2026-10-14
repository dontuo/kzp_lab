package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class CsvParserTest {
    @Test
    void simpleRecordsAreSplitByComma() throws DataStorageException {
        assertEquals(List.of(List.of("а", "б"), List.of("в", "г")), CsvParser.parseDocument("а,б\nв,г\n"));
    }

    @Test
    void emptyTextHasNoRecords() throws DataStorageException {
        assertEquals(List.of(), CsvParser.parseDocument(""));
    }

    @Test
    void lastRecordWithoutLineBreakIsKept() throws DataStorageException {
        assertEquals(List.of(List.of("а", "б")), CsvParser.parseDocument("а,б"));
    }

    @Test
    void windowsLineBreaksAreAccepted() throws DataStorageException {
        assertEquals(List.of(List.of("а"), List.of("б")), CsvParser.parseDocument("а\r\nб\r\n"));
    }

    @Test
    void commaInsideQuotesIsPartOfField() throws DataStorageException {
        // split(",") розбив би цей запис на 4 поля замість 3.
        assertEquals(List.of(List.of("Панас Мирний", "Хіба ревуть воли, як ясла повні?", "1880")),
                CsvParser.parseDocument("Панас Мирний,\"Хіба ревуть воли, як ясла повні?\",1880\n"));
    }

    @Test
    void doubledQuoteMeansOneQuote() throws DataStorageException {
        assertEquals(List.of(List.of("Повість \"Intermezzo\"")),
                CsvParser.parseDocument("\"Повість \"\"Intermezzo\"\"\"\n"));
    }

    @Test
    void lineBreakInsideQuotesIsPartOfField() throws DataStorageException {
        // Три фізичні рядки, але лише два записи.
        List<List<String>> records = CsvParser.parseDocument("назва,рік\n\"рядок 1\nрядок 2\",2020\n");
        assertEquals(2, records.size());
        assertEquals(List.of("рядок 1\nрядок 2", "2020"), records.get(1));
    }

    @Test
    void emptyFieldsArePreserved() throws DataStorageException {
        assertEquals(List.of(List.of("", "а", "")), CsvParser.parseDocument(",а,\n"));
        assertEquals(List.of(List.of("")), CsvParser.parseDocument("\"\"\n"));
    }

    @Test
    void unclosedQuoteIsRejected() {
        DataStorageException error = assertThrows(DataStorageException.class,
                () -> CsvParser.parseDocument("назва\n\"Кобзар\n"));
        assertTrue(error.getMessage().contains("лапки не закрито"), error.getMessage());
    }

    @Test
    void quoteInsideUnquotedFieldIsRejected() {
        DataStorageException error = assertThrows(DataStorageException.class,
                () -> CsvParser.parseDocument("Ко\"бзар,1\n"));
        assertTrue(error.getMessage().startsWith("рядок 1"), error.getMessage());
    }

    @Test
    void textAfterClosingQuoteIsRejected() {
        assertThrows(DataStorageException.class, () -> CsvParser.parseDocument("\"Кобзар\"x,1\n"));
    }
}

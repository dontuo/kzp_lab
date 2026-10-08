package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileReportTest {
    @TempDir
    Path tempDir;

    @Test
    void ukrainianTextSurvivesWriteAndRead() throws IOException {
        Path file = tempDir.resolve("books.csv");
        FileReport.writeReport(file, "Кобзар;Тарас Шевченко;320;250.00\nЇжак;Ґанок;1;1.00\n");

        assertEquals(List.of("Кобзар;Тарас Шевченко;320;250.00", "Їжак;Ґанок;1;1.00"), FileReport.readLines(file));
    }

    @Test
    void writeCreatesMissingDirectories() throws IOException {
        Path report = tempDir.resolve(Path.of("out", "nested", "report.txt"));
        FileReport.writeReport(report, "звіт");

        assertEquals("звіт", Files.readString(report, StandardCharsets.UTF_8));
    }

    @Test
    void byteOrderMarkIsRemoved() throws IOException {
        Path file = tempDir.resolve("bom.csv");
        // Так зберігає UTF-8 файл Блокнот Windows: BOM перед першим символом.
        Files.writeString(file, "﻿Кобзар;Тарас Шевченко;320;250.00", StandardCharsets.UTF_8);

        assertEquals("Кобзар", BookParser.split(FileReport.readLines(file).get(0))[BookParser.TITLE]);
    }
}

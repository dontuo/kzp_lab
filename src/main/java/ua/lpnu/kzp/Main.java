package ua.lpnu.kzp;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

/** Точка входу консольної програми «Каталог книжок». */
public final class Main {
    /** Код завершення: програма відпрацювала успішно. */
    static final int EXIT_OK = 0;
    /** Код завершення: помилка під час роботи (наприклад, немає вхідного файла). */
    static final int EXIT_ERROR = 1;
    /** Код завершення: неправильні аргументи командного рядка. */
    static final int EXIT_USAGE = 2;

    /* Забороняє створення екземплярів службового класу. */
    private Main() {
    }

    /**
     * Точка входу до програми.
     *
     * <p>Уся логіка винесена в {@link #run(String[])}, а тут лише передається код
     * завершення операційній системі. {@code System.exit} викликається тільки тут,
     * щоб не зупиняти JVM під час тестів.
     *
     * @param args аргументи командного рядка
     */
    public static void main(String[] args) {
        int code = run(args);
        if (code != EXIT_OK) {
            System.exit(code);
        }
    }

    /**
     * Розбирає аргументи командного рядка й виконує обраний режим.
     *
     * @param args аргументи командного рядка
     * @return код завершення: {@link #EXIT_OK}, {@link #EXIT_ERROR} або {@link #EXIT_USAGE}
     */
    static int run(String[] args) {
        Path input = Path.of("data", "input.csv");   // значення за замовчуванням
        Path output = Path.of("out", "report.txt");  // значення за замовчуванням
        Path export = null;                          // експорт лише на запит
        boolean help = false;
        boolean version = false;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--help" -> help = true;
                case "--version" -> version = true;
                case "--input", "--output", "--export" -> {
                    String option = args[i];
                    // Значення має бути наступним аргументом і не може бути іншою опцією.
                    if (i + 1 >= args.length || args[i + 1].startsWith("--")) {
                        System.err.printf("Опція %s потребує шляху до файла%n", option);
                        printHint();
                        return EXIT_USAGE;
                    }
                    // ++i: забираємо значення і пропускаємо його в наступній ітерації.
                    Path value = Path.of(args[++i]);
                    switch (option) {
                        case "--input" -> input = value;
                        case "--output" -> output = value;
                        default -> export = value;
                    }
                }
                default -> {
                    System.err.printf("Невідомий аргумент: %s%n", args[i]);
                    printHint();
                    return EXIT_USAGE;
                }
            }
        }

        // Довідка та версія мають пріоритет над обробкою файла.
        if (help) {
            printHelp();
            return EXIT_OK;
        }
        if (version) {
            System.out.printf("%s%n", versionText());
            return EXIT_OK;
        }

        if (!Files.isRegularFile(input)) {
            System.err.printf("Файл не знайдено: %s%n", input);
            return EXIT_ERROR;
        }

        try {
            return process(input, output, export);
        } catch (IOException exception) {
            System.err.printf("Помилка роботи з файлом: %s%n", exception.getMessage());
            return EXIT_ERROR;
        }
    }

    /**
     * Читає записи, обчислює показники, виводить звіт у консоль і записує його у файл.
     *
     * <p>Кожен рядок перетворюється на {@link Book} через {@link Book#fromCsv(String)}.
     * Хибний рядок не зупиняє обробку й не потрапляє до списку книг, а додається до
     * списку пропущених з номером і причиною.
     *
     * <p>Коректні книги зберігаються в {@link Repository}; показники й запити отримують
     * його незмінний знімок.
     *
     * @param input вхідний CSV-файл
     * @param output файл звіту
     * @param export файл для CSV-експорту або {@code null}, якщо експорт не потрібен
     * @return {@link #EXIT_OK} або {@link #EXIT_ERROR}, якщо немає жодного коректного запису
     *     чи експорт не вдався
     * @throws IOException якщо файл неможливо прочитати або записати
     */
    static int process(Path input, Path output, Path export) throws IOException {
        List<String> lines = FileReport.readLines(input);
        Repository<Book> repository = new Repository<>();
        List<String> errors = new ArrayList<>();

        for (int index = 0; index < lines.size(); index++) {
            try {
                repository.add(Book.fromCsv(lines.get(index)));
            } catch (IllegalArgumentException exception) {
                // Номер рядка відомий лише тут, у циклі читання, тому додаємо його до причини.
                // Номер рахуємо з 1, як у текстовому редакторі.
                errors.add(String.format(Locale.ROOT, "Рядок %d: %s", index + 1, exception.getMessage()));
            }
        }
        List<Book> books = repository.all();
        CatalogSummary summary = CatalogSummary.of(books);

        // Один текст звіту для обох приймачів, щоб консоль і файл не розходилися.
        String report = ReportFormatter.format(input, summary, errors) + ReportFormatter.formatQueries(books);
        System.out.print(report);
        FileReport.writeReport(output, report);
        System.out.printf("%nЗвіт записано у файл: %s%n", output);

        if (summary.getCount() == 0) {
            System.err.printf("У файлі %s немає жодного коректного запису%n", input);
            return EXIT_ERROR;
        }
        return export == null ? EXIT_OK : export(repository, export);
    }

    /**
     * Експортує книги з роком і жанром у CSV, читає файл назад і порівнює записи.
     *
     * @param repository сховище коректних книг
     * @param export файл для CSV-експорту
     * @return {@link #EXIT_OK}, якщо відновлені записи дорівнюють експортованим, інакше {@link #EXIT_ERROR}
     */
    static int export(Repository<Book> repository, Path export) {
        List<BookRecord> records = repository.find(BookRecord::isExportable).stream()
                .map(BookRecord::from)
                .toList();
        try {
            new CsvExporter<>(BookRecord.class).write(export, records);
            List<BookRecord> restored = BookRecord.readCsv(export);
            System.out.printf("Експортовано записів: %d у файл %s (без року чи жанру пропущено: %d)%n",
                    records.size(), export, repository.size() - records.size());
            if (!restored.equals(records)) {
                System.err.printf("Round-trip: відновлені записи відрізняються від експортованих%n");
                return EXIT_ERROR;
            }
            System.out.printf("Round-trip: відновлено %d з %d записів без змін%n", restored.size(), records.size());
            return EXIT_OK;
        } catch (DataStorageException exception) {
            // Повідомлення описує операцію, cause – першопричину; обидва показуються користувачу.
            System.err.printf("Помилка експорту: %s%n", exception.getMessage());
            if (exception.getCause() != null) {
                System.err.printf("Причина: %s%n", exception.getCause());
            }
            return EXIT_ERROR;
        }
    }

    /* Виводить довідку про аргументи командного рядка. */
    private static void printHelp() {
        System.out.printf("Використання: java -jar book-catalog.jar [--help] [--version] [--input <файл>] [--output <файл>] [--export <файл>]%n");
        System.out.printf("%n");
        System.out.printf("Читає каталог книжок, перевіряє записи й формує звіт.%n");
        System.out.printf("%n");
        System.out.printf("  --help             показати цю довідку%n");
        System.out.printf("  --version          показати версію програми та номер збірки%n");
        System.out.printf("  --input <файл>     вхідний CSV-файл (за замовчуванням data/input.csv)%n");
        System.out.printf("  --output <файл>    файл звіту (за замовчуванням out/report.txt)%n");
        System.out.printf("  --export <файл>    експортувати книги з роком і жанром у CSV і перевірити round-trip%n");
    }

    /* Підказує, як отримати довідку після помилки в аргументах. */
    private static void printHint() {
        System.err.printf("Запустіть з --help, щоб побачити список аргументів%n");
    }

    /**
     * Формує рядок версії з файла {@code version.properties}, який Maven заповнює під час збірки.
     *
     * @return назва, версія та номер збірки, або {@code book-catalog dev}, якщо файл недоступний
     */
    static String versionText() {
        InputStream in = Main.class.getResourceAsStream("/version.properties");
        if (in == null) {
            return "book-catalog dev";
        }
        Properties properties = new Properties();
        // Закриття reader закриває і вкладений потік.
        try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (IOException exception) {
            return "book-catalog dev";
        }
        return String.format("%s %s (build %s)",
                properties.getProperty("name", "book-catalog"),
                properties.getProperty("version", "dev"),
                properties.getProperty("build", "local"));
    }
}

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
        boolean help = false;
        boolean version = false;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--help" -> help = true;
                case "--version" -> version = true;
                case "--input", "--output" -> {
                    String option = args[i];
                    // Значення має бути наступним аргументом і не може бути іншою опцією.
                    if (i + 1 >= args.length || args[i + 1].startsWith("--")) {
                        System.err.printf("Опція %s потребує шляху до файла%n", option);
                        printHint();
                        return EXIT_USAGE;
                    }
                    // ++i: забираємо значення і пропускаємо його в наступній ітерації.
                    Path value = Path.of(args[++i]);
                    if (option.equals("--input")) {
                        input = value;
                    } else {
                        output = value;
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
            return process(input, output);
        } catch (IOException exception) {
            System.err.printf("Помилка роботи з файлом: %s%n", exception.getMessage());
            return EXIT_ERROR;
        }
    }

    /**
     * Читає записи, обчислює показники, виводить звіт у консоль і записує його у файл.
     *
     * <p>Порядок для кожного рядка: прочитати → перевірити → включити в обчислення.
     * Хибний рядок не зупиняє обробку, а потрапляє до списку пропущених з номером і причиною.
     *
     * @param input вхідний CSV-файл
     * @param output файл звіту
     * @return {@link #EXIT_OK} або {@link #EXIT_ERROR}, якщо немає жодного коректного запису
     * @throws IOException якщо файл неможливо прочитати або записати
     */
    static int process(Path input, Path output) throws IOException {
        List<String> lines = FileReport.readLines(input);
        CatalogSummary summary = new CatalogSummary();
        List<String> errors = new ArrayList<>();

        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);
            String error = BookParser.validate(line);
            if (error != null) {
                // Номер рядка рахуємо з 1, як у текстовому редакторі.
                errors.add(String.format(Locale.ROOT, "Рядок %d: %s", index + 1, error));
                continue;
            }
            summary.add(BookParser.split(line));
        }

        // Один текст звіту для обох приймачів, щоб консоль і файл не розходилися.
        String report = ReportFormatter.format(input, summary, errors);
        System.out.print(report);
        FileReport.writeReport(output, report);
        System.out.printf("%nЗвіт записано у файл: %s%n", output);

        if (summary.getCount() == 0) {
            System.err.printf("У файлі %s немає жодного коректного запису%n", input);
            return EXIT_ERROR;
        }
        return EXIT_OK;
    }

    /* Виводить довідку про аргументи командного рядка. */
    private static void printHelp() {
        System.out.printf("Використання: java -jar lab01.jar [--help] [--version] [--input <файл>] [--output <файл>]%n");
        System.out.printf("%n");
        System.out.printf("Читає каталог книжок, перевіряє записи й формує звіт.%n");
        System.out.printf("%n");
        System.out.printf("  --help             показати цю довідку%n");
        System.out.printf("  --version          показати версію програми та номер збірки%n");
        System.out.printf("  --input <файл>     вхідний CSV-файл (за замовчуванням data/input.csv)%n");
        System.out.printf("  --output <файл>    файл звіту (за замовчуванням out/report.txt)%n");
    }

    /* Підказує, як отримати довідку після помилки в аргументах. */
    private static void printHint() {
        System.err.printf("Запустіть з --help, щоб побачити список аргументів%n");
    }

    /**
     * Формує рядок версії з файла {@code version.properties}, який Maven заповнює під час збірки.
     *
     * @return версія та номер збірки, або {@code lab01 dev}, якщо файл недоступний
     */
    static String versionText() {
        InputStream in = Main.class.getResourceAsStream("/version.properties");
        if (in == null) {
            return "lab01 dev";
        }
        Properties properties = new Properties();
        // Закриття reader закриває і вкладений потік.
        try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (IOException exception) {
            return "lab01 dev";
        }
        return String.format("lab01 %s (build %s)",
                properties.getProperty("version", "dev"),
                properties.getProperty("build", "local"));
    }
}

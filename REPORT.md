# Звіт до лабораторної роботи № 5

Звіти попередніх робіт: [ЛР № 1](docs/reports/lab01.md) (`v1.0.0`),
[ЛР № 2](docs/reports/lab02.md) (`v2.0.0`), [ЛР № 3](docs/reports/lab03.md) (`v3.0.0`),
[ЛР № 4](docs/reports/lab04.md) (`v4.0.0`).

## 1. Тема, номер і варіант

- Тема: параметризоване сховище, винятки й CSV-експорт у Java-проєкті.
- Лабораторна робота № 5, варіант 1 – «Бібліотека» (той самий каталог книжок, що й у ЛР № 1–4).
- Операційна система: Gentoo Linux, OpenJDK 25 (код компілюється з `--release 21`).
- Репозиторій: https://github.com/dontuo/kzp_lab
- Стан лабораторної роботи № 5: тег `v5.0.0` – https://github.com/dontuo/kzp_lab/tree/v5.0.0

## 2. Мета роботи

Завершити збереження даних у Java-продукті: замінити список на параметризоване
сховище, описати CSV-стовпці анотацією, реалізувати універсальний рефлексивний
експортер, прочитати файл назад і довести, що об'єкти не змінилися.

Досягнуто:

- `Repository<T>` з `add`, знімком `all()` і пошуком `find(Predicate<? super T>)`;
  `Main` зберігає книги в `Repository<Book>`;
- `@CsvColumn`, плоский запис варіанта `BookRecord` і універсальний `CsvExporter<T>`;
- документний `CsvParser` (машина станів) і відновлення `BookRecord.readCsv`;
- перевірюваний виняток `DataStorageException` із першопричиною;
- опція `--export <файл>`: запис CSV, читання назад і перевірка round-trip;
- завдання із зірочкою: `Exporter<T>` + `JsonExporter<T>`, кеш полів із вимірюванням;
- 194 тести ЛР № 1–4 проходять разом із 59 новими.

## 3. Стан до і після цієї роботи

| Що | ЛР № 4 (`v4.0.0`) | ЛР № 5 (`v5.0.0`) |
|---|---|---|
| Зберігання книг у `Main` | `List<Book>` + `ArrayList` | `Repository<Book>` |
| Збереження на диск | лише текстовий звіт | CSV-експорт записів і читання назад |
| Опис CSV-стовпців | немає | `@CsvColumn` над полями `BookRecord` |
| Помилки збереження | `IOException` | `DataStorageException` з `cause` |
| Формати експорту | немає | CSV, JSON (через `Exporter<T>`) |

Звіт програми, показники й запити не змінилися (розділ 14); експорт виконується лише
з опцією `--export`.

## 4. `Repository<T>`

```java
public final class Repository<T> {
    public void add(T item)                              // null → NullPointerException
    public List<T> all()                                 // List.copyOf – незмінний знімок
    public List<T> find(Predicate<? super T> condition)  // незмінний список знайдених
    public int size()
}
```

- **Параметр типу.** `Repository<Book>` приймає лише `Book`; читання не потребує
  приведень. Raw type `Repository` не використовується ніде.
- **Знімок.** `all()` повертає `List.copyOf`: зовнішній код не може змінити сховище, а
  знімок не змінюється після наступних `add` (тести `snapshotDoesNotChangeAfterAdd`,
  `snapshotCannotBeModified`).
- **Предикат.** `? super T` дозволяє передати умову для супертипу:
  `Repository<PrintedBook>.find(Predicate<Book>)` і навіть `Predicate<Object>`.
- **Використання.** `Main.process` додає коректні книги в `Repository<Book>`, звіт і
  запити отримують `repository.all()`, а експорт –
  `repository.find(BookRecord::isExportable)`.

## 5. Модель даних

`public record BookRecord(String title, String author, int year, int pages, String genre)`

| Поле | Тип | CSV-стовпець | Інваріант |
|---|---|---|---|
| `title` | `String` | `назва` | не `null`, не порожнє |
| `author` | `String` | `автор` | не `null`, не порожнє |
| `year` | `int` | `рік` | від 1450 до поточного року |
| `pages` | `int` | `сторінки` | `> 0` |
| `genre` | `String` | `жанр` | не `null`, не порожнє |

- Поля – рівно поля варіанта 1 («Бібліотека»). Ціна й формат видання в запис не
  входять: варіант їх не передбачає, і ці дані надалі потрібні лише Java-звіту.
- `record` незмінний; `equals`/`hashCode` порівнюють усі п'ять полів – сильніша
  перевірка round-trip, ніж лише ключ «назва + автор» із варіанта.
- `BookRecord.from(Book)` бере дані з книги каталогу; книги без року чи жанру (рядки
  старого формату) не експортуються – `isExportable(book)` повертає `false`.
- Інваріанти перевіряє компактний конструктор; `fromCsv` перетворює порушення на
  `DataStorageException` із `cause`.

## 6. `@CsvColumn`

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface CsvColumn {
    String value();
}
```

- `RUNTIME` – анотація лишається в класі під час виконання, тому експортер знаходить
  її через `isAnnotationPresent` / `getAnnotation`.
- `FIELD` – анотацію можна поставити лише на поле. Анотація на компоненті `record`
  потрапляє саме на приватне поле (тест `annotationIsAvailableAtRuntimeOnFieldsOnly`).
- Анотація задає лише **назву** стовпця. **Порядок** визначає експортер: сортування
  Java-імен полів `author`, `genre`, `pages`, `title`, `year` → стовпці
  `автор, жанр, сторінки, назва, рік`.

## 7. Рефлексивний експортер

`AnnotatedFields.scan(type)`:

1. проходить ланцюжок класів від `type` до `Object` (`Stream.iterate` +
   `getSuperclass`), тож враховує й успадковані поля;
2. бере `getDeclaredFields()` кожного класу й залишає нестатичні поля з `@CsvColumn`;
3. сортує за іменем поля, а за однакових імен – за іменем класу;
4. перевіряє, що поля є і назви стовпців не повторюються;
5. для кожного поля викликає `trySetAccessible()` і перевіряє результат – метод не
   кидає виняток, а повертає `false`.

`CsvExporter<T>`:

- отримує клас у конструкторі: для порожнього списку немає об'єкта, з якого можна
  дізнатися клас, а заголовок однаково потрібен;
- заголовок – значення анотацій, значення – `String.valueOf(field.get(record))`,
  `null` – порожнє поле;
- не містить жодної назви поля, класу чи підтипу домену й не викликає геттерів;
  тест `exporterWorksWithAnotherClass` експортує клас `Point`, про який експортер нічого
  не знає, а `inheritedFieldsAreExported` – клас з полями надкласу;
- `IllegalAccessException` перетворюється на `DataStorageException` з `cause`.

Екранування (`CsvExporter.escape`): якщо значення містить `,`, `"`, `\n` або `\r`, воно
береться в подвійні лапки, а кожна `"` усередині подвоюється.

## 8. Формат CSV

- Кодування UTF-8 під час запису й читання; роздільник полів – кома; роздільник
  записів – `System.lineSeparator()` (на Windows `\r\n`, на Linux і macOS `\n`); парсер
  приймає обидва.
- Запис через `Files.writeString` з `CREATE`, `TRUNCATE_EXISTING`, `WRITE`: після
  коротшого експорту старі рядки не лишаються.

Повний результат `--export` для `data/library.csv` (`data/library-export.csv`):

```text
автор,жанр,сторінки,назва,рік
Тарас Шевченко,поезія,320,Кобзар,1840
Панас Мирний,роман,400,"Хіба ревуть воли, як ясла повні?",1880
Іван Багряний,роман,288,Тигролови,1944
Сергій Жадан,роман,336,Інтернат,2017
Софія Андрухович,роман,832,Амадока,2020
Леся Українка,драма-феєрія,180,Лісова пісня,1911
```

Приклади екранування з тестів:

| Значення | У CSV |
|---|---|
| `Хіба ревуть воли, як ясла повні?` | `"Хіба ревуть воли, як ясла повні?"` |
| `Повість "Intermezzo"` | `"Повість ""Intermezzo"""` |
| `Назва` + перенесення + `у два рядки` | `"Назва` ⏎ `у два рядки"` (одне поле на двох фізичних рядках) |

## 9. Імпорт і round-trip

1. `CsvParser.readTable(path)` читає файл через `Files.readString(path, UTF_8)`.
2. `CsvParser.parseDocument(text)` – машина станів над усім текстом: стан «у лапках»
   / «поза лапками»; кома й перенесення рядка розділяють поля й записи лише поза
   лапками; `""` у лапках – одна лапка. Повертає записи, а не фізичні рядки.
3. Перший запис – заголовок; кожен наступний має мати стільки ж полів і
   перетворюється на `Map<назва стовпця, значення>`.
4. `BookRecord.readCsv` перевіряє, що заголовок збігається з `CsvExporter.header()`, і
   для кожного запису викликає `BookRecord.fromCsv(values, номер)`: текстові поля – як
   є, `рік` і `сторінки` – `Integer.parseInt`, далі конструктор з інваріантами.
5. Порівняння: `restored.equals(records)` – списки рівні, якщо рівні всі записи в тому
   самому порядку (`record.equals` порівнює всі поля).

`Main.export` виконує ці кроки після запису й друкує результат:

```text
Експортовано записів: 6 у файл data/library-export.csv (без року чи жанру пропущено: 1)
Round-trip: відновлено 6 з 6 записів без змін
```

`split(",")` для цього не підходить: рядок
`Панас Мирний,роман,400,"Хіба ревуть воли, як ясла повні?",1880` він розбив би на 6
полів замість 5, а поле з перенесенням рядка – на два різні записи.

## 10. Помилки

| Рівень | Приклад | Повідомлення | `getCause()` | Тест |
|---|---|---|---|---|
| Модель | 0 сторінок, рік 1449, порожній жанр | `IllegalArgumentException` у конструкторі `BookRecord` | – | `BookRecordTest.invalidValuesAreRejected` |
| Модель під час імпорту | `сторінки` = 0 у CSV | `запис 4: кількість сторінок має бути додатною: 0` | `IllegalArgumentException` | `invariantViolationKeepsCause` |
| Формат | 4 поля замість 5 | `…, запис 1: очікується 5 полів, отримано 4` | – | `wrongFieldCountIsRejected` |
| Формат | чужий заголовок | `…: очікується заголовок [автор, жанр, сторінки, назва, рік], отримано […]` | – | `unexpectedHeaderIsRejected` |
| Формат | незакриті лапки | `рядок N: лапки не закрито до кінця файла` | – | `unclosedQuoteIsRejected` |
| Формат | лапка всередині поля без лапок | `рядок N: лапка всередині поля без лапок` | – | `quoteInsideUnquotedFieldIsRejected` |
| Значення | `рік` = `тисяча` | `запис 2: стовпець «рік» не є цілим числом: "тисяча"` | `NumberFormatException` | `wrongNumberInFileKeepsCause` |
| Значення | немає стовпця | `запис 3: немає стовпця «жанр»` | – | `missingColumnIsReported` |
| Файл | немає файла | `не вдалося прочитати файл …` | `NoSuchFileException` | `missingFileKeepsCause` |
| Файл | файл не в UTF-8 | `не вдалося прочитати файл …` | `MalformedInputException` | `invalidUtf8KeepsCause` |
| Файл | батьківський «каталог» є файлом | `не вдалося записати файл …` | `IOException` | `fileErrorKeepsIoExceptionAsCause` |
| Рефлексія | клас без анотованих полів | `клас … не має полів з @CsvColumn` | – | `classWithoutAnnotatedFieldsIsRejected` |
| Рефлексія | доступ до поля заборонено | `немає доступу до поля …` / `не вдалося прочитати поле …` | `IllegalAccessException` | перевіряється в коді |

`DataStorageException` – перевірюваний виняток: експорт і імпорт – операції, де помилка
файлу чи даних очікувана, і виклик має явно вирішити, що з нею робити. Порожніх `catch`
немає: `Main.export` друкує повідомлення й причину та повертає код 1.

Порожній файл (0 байт) дає порожній список записів; файл із самим заголовком – теж
порожній список. Обидва випадки допустимі й перевірені (`emptyFileHasNoRecords`,
`emptyListSurvivesRoundTrip`).

## 11. Тестування

```text
./mvnw clean verify
Tests run: 253, Failures: 0, Errors: 0, Skipped: 0
BugInstance size is 0
BUILD SUCCESS
```

| Тестовий клас | Тести | Що перевіряє |
|---|---|---|
| `RepositoryTest` (новий) | 8 | порожнє сховище, порядок, незмінний знімок, `null`, `find`, `Predicate` супертипу, інший тип `T` |
| `CsvExporterTest` (новий) | 14 | `RUNTIME`/`FIELD`, порядок стовпців, порожній список, інший клас, успадковані поля, `List<? extends T>`, клас без полів, екранування коми, лапок, `\n`, `\r\n`, UTF-8, `TRUNCATE_EXISTING`, помилка файлу з `cause`, відсутність змін об'єктів |
| `CsvParserTest` (новий) | 11 | прості записи, порожній текст, останній запис без перенесення, `\r\n`, кома й лапки в полі, багаторядкове поле, порожні поля, незакриті лапки, лапка в полі без лапок, текст після закривальної лапки |
| `BookRecordTest` (новий) | 9 | межі року й сторінок, хибні значення, рівність, `from(Book)`, книга без року, `fromCsv`: коректний, немає стовпця, хибне число з `cause`, порушення інваріанта з `cause` |
| `RoundTripTest` (новий) | 9 | `data/library.csv` → CSV → об'єкти; кома, лапки, `\n`, `\r\n`; порожній список; порожній файл; чужий заголовок; кількість полів; хибне число; відсутній файл; не-UTF-8 |
| `JsonExporterTest` (новий) | 5 | JSON-масив, порожній список, екранування, обидва формати через `Exporter<T>`, кеш полів |
| `MainTest` | 10 + 3 | `--export`: файл і повідомлення round-trip; без значення – код 2; помилка файлу – код 1 і причина |
| попередні тести ЛР № 1–4 | 194 | без змін, окрім очікуваної версії `4.0.0` → `5.0.0` у `MainTest` |

**Фізичні рядки й записи:**

| Файл | Фізичних рядків | Записів (без заголовка) | Відновлено об'єктів |
|---|---|---|---|
| `data/library-export.csv` | 7 | 6 | 6 |
| `special.csv` у `commaQuotesAndLineBreaksSurviveRoundTrip` | більше за 4 (два поля містять перенесення) | 3 | 3 |
| порожній список | 1 (заголовок) | 0 | 0 |
| порожній файл | 0 | 0 | 0 |

## 12. Інфраструктура

Maven, Wrapper, SpotBugs, CI і шаблони перенесено без змін; у `pom.xml` змінено лише
версію на `5.0.0`.

| Команда | Результат |
|---|---|
| `./mvnw test` | 253 тести, 0 помилок |
| `./mvnw verify` | SpotBugs: 0 дефектів |
| `./mvnw package` | `target/book-catalog-5.0.0.jar` |
| `java -jar target/book-catalog-5.0.0.jar --version` | `book-catalog 5.0.0 (build local)` |
| `java -jar target/book-catalog-5.0.0.jar --input data/library.csv --export data/library-export.csv` | CSV з розділу 8, `Round-trip: відновлено 6 з 6 записів без змін` |

Кодування й роздільники рядків на різних ОС перевіряє CI: `RoundTripTest` і
`CsvExporterTest` записують файли з `System.lineSeparator()` поточної ОС (на Windows
`\r\n`) і читають їх назад тим самим парсером; `writeUsesUtf8AndCreatesDirectories`
порівнює байти файла, декодовані як UTF-8.

CI:

- успішний запуск CI № 46 (коміт `1852102`): https://github.com/dontuo/kzp_lab/actions/runs/38611489648
- артефакти jar:
  - [book-catalog-ubuntu-latest-build-46](https://github.com/dontuo/kzp_lab/actions/runs/38611489648/artifacts/11917601444)
  - [book-catalog-windows-latest-build-46](https://github.com/dontuo/kzp_lab/actions/runs/38611489648/artifacts/11917671356)
  - [book-catalog-macos-latest-build-46](https://github.com/dontuo/kzp_lab/actions/runs/38611489648/artifacts/11918426287)
- створений CSV: [`data/library-export.csv`](data/library-export.csv)
- результати на трьох ОС (253 тести, зокрема `RoundTripTest` і `CsvExporterTest` з роздільником рядків кожної ОС, SpotBugs, пакування, запуск jar):

  | ОС | Результат |
  |---|---|
  | ubuntu-latest | [success](https://github.com/dontuo/kzp_lab/actions/runs/38611489648/job/116015701641) |
  | windows-latest | [success](https://github.com/dontuo/kzp_lab/actions/runs/38611489648/job/116015702473) |
  | macos-latest | [success](https://github.com/dontuo/kzp_lab/actions/runs/38611489648/job/116015702007) |
- Git-тег: `v5.0.0`

## 13. GitHub Issues і Pull Request

| GitHub Issue | Зміна | Перевірка | Pull Request |
|---|---|---|---|
| [#42](https://github.com/dontuo/kzp_lab/issues/42) Repository<T> (`feature`) | `Repository<T>`, `Main` на `Repository<Book>` | `RepositoryTest` | [#50](https://github.com/dontuo/kzp_lab/pull/50) |
| [#43](https://github.com/dontuo/kzp_lab/issues/43) Анотація @CsvColumn і BookRecord (`feature`) | `CsvColumn`, `BookRecord` | `BookRecordTest`, `CsvExporterTest` | [#50](https://github.com/dontuo/kzp_lab/pull/50) |
| [#44](https://github.com/dontuo/kzp_lab/issues/44) Рефлексивний CSV-експортер (`feature`) | `AnnotatedFields`, `Exporter<T>`, `CsvExporter<T>` | `CsvExporterTest` | [#50](https://github.com/dontuo/kzp_lab/pull/50) |
| [#45](https://github.com/dontuo/kzp_lab/issues/45) CSV-парсер і round-trip (`feature`) | `CsvParser`, `BookRecord.readCsv`, `--export`, `data/library.csv` | `CsvParserTest`, `RoundTripTest`, `MainTest` | [#50](https://github.com/dontuo/kzp_lab/pull/50) |
| [#46](https://github.com/dontuo/kzp_lab/issues/46) Власний виняток і обробка помилок (`feature`) | `DataStorageException`, повідомлення з причиною в `Main` | `RoundTripTest`, `BookRecordTest`, `MainTest` | [#50](https://github.com/dontuo/kzp_lab/pull/50) |
| [#47](https://github.com/dontuo/kzp_lab/issues/47) Тести ЛР5 (`feature`) | 59 нових тестів | 253 тести, CI | [#50](https://github.com/dontuo/kzp_lab/pull/50) |
| [#48](https://github.com/dontuo/kzp_lab/issues/48) Версія 5.0.0, JSON-експорт і кеш полів | `pom.xml`, ⭐ `JsonExporter<T>`, кеш у `AnnotatedFields` | `JsonExporterTest`, вимірювання | [#50](https://github.com/dontuo/kzp_lab/pull/50) |
| [#49](https://github.com/dontuo/kzp_lab/issues/49) Документація ЛР5 | README, REPORT, javadoc, `docs/reports/lab04.md` | javadoc без попереджень | [#50](https://github.com/dontuo/kzp_lab/pull/50) + окремий PR зі звітом |

Усі зміни коду надійшли одним Pull Request [#50](https://github.com/dontuo/kzp_lab/pull/50) з прикладом CSV, результатом
round-trip і таблицею помилок в описі; кожне `Closes #N` записано окремим рядком,
тому GitHub автоматично закрив #42–#49.

## 14. Порівняння стану

- Звіт на `data/input.csv` і `data/books.csv` не змінився: `CompatibilityTest`
  порівнює його з контрольними звітами `v1.0.0` і `v3.0.0` з тим самим переліком
  задокументованих відмінностей, що й у ЛР № 4.
- Показники й запити рахуються з `repository.all()` тими самими методами
  `CatalogSummary` і `BookQueries`; `StreamEquivalenceTest` проходить без змін.
- Вхідні файли `data/input.csv` і `data/books.csv` не змінювалися; додано лише
  `data/library.csv` і результат експорту `data/library-export.csv`.
- Усі 194 попередні тести проходять; змінено лише очікувану версію в одному тесті.

**Кешування полів (завдання із зірочкою).** Вимірювання на OpenJDK 25, 200 000
викликів після прогріву (орієнтовні значення, залежать від машини):

| Операція | Час на виклик |
|---|---|
| `AnnotatedFields.scan` (рефлексія без кешу) | ≈ 420–470 нс |
| `AnnotatedFields.of` (з кешу) | ≈ 2 нс |
| `CsvExporter.format` для 10 записів (з кешем) | ≈ 1 600 нс |

Без кешу кожен експорт невеликого списку був би приблизно на чверть повільнішим.
`ConcurrentHashMap.putIfAbsent` гарантує, що за одночасного першого звернення з
кількох потоків усі отримають той самий список полів.

## 15. Академічна доброчесність

Під час роботи використано Claude Code (Anthropic, модель Claude Opus 5.5).

**Ролі-агенти:** каталог `ai/` – Менеджер, DevOps, Розробник, Валідатор,
Документатор, Рецензент.

**Внесок ШІ:**

- аналіз вимог ЛР № 5 і план компонентів;
- реалізація `Repository<T>`, `@CsvColumn`, `BookRecord`, `AnnotatedFields`,
  `Exporter<T>`, `CsvExporter<T>`, `JsonExporter<T>`, `CsvParser`,
  `DataStorageException`, опції `--export`;
- тести нових компонентів, вимірювання кешу, README і цей звіт.

**Прийняті та відхилені рішення:**

- прийнято: `BookRecord` як `record` з рівно полями варіанта – незмінність і повна
  рівність для round-trip без ручного `equals`;
- прийнято: читання за назвами стовпців і перевірка заголовка, а не за позицією;
- прийнято: експорт лише на запит (`--export`), щоб звіт і поведінка без опції не
  змінилися;
- відхилено: експортувати `Book` напряму – підтип і ціна не входять до полів варіанта,
  а рік і жанр у старих записах відсутні.

**Виправлені помилки:**

- компіляція: `Stream.iterate(type, …, Class::getSuperclass)` не виводив тип –
  виправлено явним `Stream.<Class<?>>iterate`;
- SpotBugs `RV_RETURN_VALUE_OF_PUTIFABSENT_IGNORED`: результат `putIfAbsent` у кеші
  ігнорувався – тепер повертається значення, яке вже поклав інший потік.

**Мій внесок:** код цієї роботи я створював разом із ШІ в діалоговому режимі:
я формулював завдання для кожного кроку (сховище, анотація й експорт, парсер і
round-trip, помилки), ШІ генерував реалізацію й тести, а я переглядав результат і
вирішував, що прийняти. Також я:

- створив GitHub Issues #42–#49 для сховища, анотації, експортера, парсера, помилок,
  тестів, інфраструктури й документації з критеріями готовності;
- відкрив Pull Request #50 з прикладом CSV, результатом round-trip, таблицею помилок і
  `Closes #N` для кожного Issue, дочекався зеленого CI на трьох ОС і виконав merge.

**Редакційний процес:** кожну запропоновану ШІ зміну я переглядав перед
прийняттям. Перевірка: локальний `./mvnw clean verify` (253 тести, SpotBugs без
дефектів), запуск `--export` на `data/library.csv` з повідомленням про успішний
round-trip, порівняння звітів із контрольними виводами попередніх робіт і зелений CI
на Ubuntu, Windows і macOS перед merge.

**Відповідальність:** підтверджую, що наведені результати запуску, тестів і CI
правдиві та отримані на поданому коді. Я розумію параметр типу й `? super T` у
`Repository`, роль `RUNTIME` і `FIELD` в `@CsvColumn`, як `AnnotatedFields` знаходить
і читає поля, правила екранування, роботу машини станів у `CsvParser`, формування
`cause` у `DataStorageException` і що доводить round-trip, та можу пояснити їх на
захисті. Відповідальність за поданий код і звіт несу я.

## 16. Відповіді на контрольні питання

TODO: відповіді власними словами.

1. Яку проблему розв'язує параметр типу в Repository<T>?
2. Чим параметризований клас відрізняється від raw type?
3. Навіщо Predicate<? super T> у методі пошуку?
4. Чому all() має повертати незмінний знімок?
5. Що таке checked exception і коли він доречний у цій роботі?
6. Для чого зберігати cause власного винятку?
7. Чим Path і Files кращі за складання шляху рядком?
8. Чому кодування потрібно задавати і під час запису, і під час читання?
9. Що роблять @Retention(RUNTIME) і @Target(FIELD)?
10. Чому анотація не виконує експорт сама?
11. Чим getDeclaredFields() відрізняється від отримання публічних полів?
12. Навіщо перевіряти trySetAccessible()?
13. Чому порядок полів для CSV потрібно зробити детермінованим?
14. Чому split(",") не підходить для загального CSV?
15. Як екрануються кома, лапка й перенесення рядка в CSV?
16. Чому порожній список потребує окремого параметра Class<T>?
17. Чим універсальний експортер відрізняється від предметного fromCsv?
18. Які рівні помилок розрізняє програма?
19. Що доводить тест round-trip?
20. Чому виняток не можна поглинати порожнім catch?

## 17. Висновки

Java-продукт отримав параметризоване сховище, власний тип помилки з першопричиною,
метадані CSV в анотації та універсальний рефлексивний експортер. Дані записуються й
читаються з явним UTF-8, коми, лапки й перенесення рядків екрануються, а round-trip
доводить, що записи не змінюються під час збереження.

Створений CSV (`data/library-export.csv`) стане вхідними даними для Python-блоку
(ЛР № 6–7). Він містить рівно поля варіанта «Бібліотека» – назву, автора, рік,
сторінки й жанр – у UTF-8 з комою як роздільником, тому його можна прочитати
стандартним `csv.DictReader`. Назви стовпців у файлі українською; Python-програма
зіставить їх зі своїми машинними назвами `title`, `author`, `year`, `pages`, `genre`.

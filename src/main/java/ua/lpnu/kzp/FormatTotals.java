package ua.lpnu.kzp;

/**
 * Незмінний підсумок для одного формату видання: кількість книг і їхня вартість придбання.
 *
 * @param count кількість книг, не від'ємна
 * @param purchaseCost сумарна вартість придбання, скінченна й не від'ємна
 */
public record FormatTotals(int count, double purchaseCost) {
    /** Нульове значення, з якого починається підсумовування. */
    public static final FormatTotals ZERO = new FormatTotals(0, 0.0);

    /**
     * Перевіряє, що значення не від'ємні та скінченні.
     *
     * @throws IllegalArgumentException якщо кількість від'ємна або вартість від'ємна чи нескінченна
     */
    public FormatTotals {
        if (count < 0) {
            throw new IllegalArgumentException("кількість книг не може бути від'ємною");
        }
        if (!Double.isFinite(purchaseCost) || purchaseCost < 0) {
            throw new IllegalArgumentException("вартість має бути скінченною й не від'ємною");
        }
    }

    /**
     * Повертає підсумок, до якого додано одну книгу; поточний об'єкт не змінюється.
     *
     * @param book книга будь-якого підтипу
     * @return новий підсумок
     */
    public FormatTotals plus(Book book) {
        // purchaseCost() – поліморфний виклик: правило вартості обирає підтип книги.
        return new FormatTotals(count + 1, purchaseCost + book.purchaseCost());
    }
}

package market.repo;

/**
 * Утилитарный класс для безопасной работы со строками CSV.
 * <p>
 * Предоставляет методы экранирования и деэкранирования значений
 * в соответствии со стандартом CSV (RFC 4180).
 * <p>
 * Не предназначен для создания экземпляров — все методы статические.
 */
public final class CsvUtil {
    /** Закрытый конструктор предотвращает создание экземпляров утилитарного класса. */
    private CsvUtil() {}

    /**
     * Экранирует строку для корректной записи в CSV.
     * <p>
     * - Если строка содержит запятую, кавычки или перевод строки — она заключается в двойные кавычки. <br>
     * - Все двойные кавычки внутри значения удваиваются.
     *
     * <p><b>Примеры:</b>
     * <pre>
     * "Hello" → """Hello"""
     * Hello,world → "Hello,world"
     * line1\nline2 → "line1\nline2"
     * </pre>
     *
     * @param s исходная строка
     * @return экранированная строка, готовая к записи в CSV (никогда не {@code null})
     */
    public static String esc(String s) {
        if (s == null) return "";
        String v = s.replace("\"", "\"\"");
        if (v.contains(",") || v.contains("\"") || v.contains("\n")) {
            return "\"" + v + "\"";
        }
        return v;
    }

    /**
     * Выполняет деэкранирование строки, считанной из CSV.
     * <p>
     * Если строка заключена в кавычки — они удаляются,
     * а все двойные кавычки внутри заменяются на одиночные.
     *
     * <p><b>Примеры:</b>
     * <pre>
     * """Hello""" → "Hello"
     * "Hello,world" → Hello,world
     * "line1\nline2" → line1\nline2
     * </pre>
     *
     * @param s строка из CSV-файла
     * @return восстановленное оригинальное значение без экранирования
     */
    public static String unesc(String s) {
        String v = s.trim();
        if (v.startsWith("\"") && v.endsWith("\"")) {
            v = v.substring(1, v.length() - 1).replace("\"\"", "\"");
        }
        return v;
    }
}

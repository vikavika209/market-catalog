
package market.repo;
public final class CsvUtil {
    private CsvUtil(){}
    public static String esc(String s){
        if (s == null) return "";
        String v = s.replace("\"","\"\"");
        if (v.contains(",") || v.contains("\"") || v.contains("\n")) {
            return "\"" + v + "\"";
        }
        return v;
    }
    public static String unesc(String s){
        String v = s.trim();
        if (v.startsWith("\"") && v.endsWith("\"")) {
            v = v.substring(1, v.length()-1).replace("\"\"", "\"");
        }
        return v;
    }
}

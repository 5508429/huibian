package utils;

public class StringUtils {
    public static String filterBlank(String str) {
        return str.replace(" ", "").replace("\n", "").replace("\t", "");
    }
}

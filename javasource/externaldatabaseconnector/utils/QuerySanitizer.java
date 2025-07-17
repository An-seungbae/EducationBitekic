package externaldatabaseconnector.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QuerySanitizer {

    /**
     * Removes comments and trailing semicolons from the query.
     */
    public String sanitize(String query) {
        if (query == null || query.trim().isEmpty()) {
            return null;
        }

        return removeComments(query);
    }

    /**
     * Removes comments from the query.
     */
    private String removeComments(String query) {

        // Regex pattern to match single-line and multi-line comments
        String pattern = "(--.*?$)|(/\\*.*?\\*/)";
        Pattern regex = Pattern.compile(pattern, Pattern.MULTILINE | Pattern.DOTALL);
        Matcher matcher = regex.matcher(query);
        return matcher.replaceAll("").trim();
    }
}
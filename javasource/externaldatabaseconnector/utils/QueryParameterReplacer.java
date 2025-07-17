package externaldatabaseconnector.utils;

import externaldatabaseconnector.database.constants.IMxDatabaseTypes;
import externaldatabaseconnector.pojo.QueryDetails;
import externaldatabaseconnector.pojo.QueryParameter;

import java.util.regex.Pattern;

public class QueryParameterReplacer {

    /**
     * Replaces placeholders in the query with '?'.
     */
    public String replacePlaceholders(QueryDetails queryDetails, String databaseType) {
        String query = queryDetails.getQuery();

        if (query == null || query.trim().isEmpty()) {
            return query;
        }

        //The regular expression uses the (?i) flag to perform case-insensitive matching of placeholders.
        //Replace placeholders with '?' to prepare the query for parameter binding
        for (QueryParameter queryParameter : queryDetails.getQueryParameters()) {
            String parameterRegex = "\\{" + Pattern.quote(queryParameter.getName()) + "\\}";
            query = query.replaceAll("(?i)" + parameterRegex, "?");
        }

        // Add curly braces for MSSQL callable queries
        if (query.trim().toLowerCase().startsWith("call") && IMxDatabaseTypes.MSSQL.equals(databaseType)) {
            query = "{" + query.trim() + "}";
        }

        return query.trim();
    }
}
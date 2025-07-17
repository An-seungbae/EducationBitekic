package externaldatabaseconnector.utils;

import externaldatabaseconnector.pojo.QueryDetails;
import externaldatabaseconnector.pojo.QueryParameter;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QueryParameterSorter {

    /**
     * Orders query parameters based on their appearance in the query.
     */
    public QueryDetails sortParameters(QueryDetails queryDetails) {
        String query = queryDetails.getQuery();

        if (query == null || query.trim().isEmpty()) {
            return queryDetails;
        }
        
        // Extract placeholders from the query in order of appearance
        List<String> placeholderOrder = extractPlaceholders(query);

        Map<String, QueryParameter> parameterMap = new HashMap<>();
        for (QueryParameter queryParameter : queryDetails.getQueryParameters()) {
            parameterMap.put(queryParameter.getName().toLowerCase(), queryParameter);
        }

        // Map to quickly lookup parameters by name
        List<QueryParameter> sortedParameters = new ArrayList<>();
        for (String placeholder : placeholderOrder) {
            QueryParameter parameter = parameterMap.get(placeholder.toLowerCase());
            if (parameter != null) {
                sortedParameters.add(parameter);
            }
        }

        queryDetails.setQueryParameters(sortedParameters);
        return queryDetails;
    }

    /**
     * Extracts placeholders from the query in the order they appear.
     */
    private List<String> extractPlaceholders(String query) {
        List<String> placeholders = new ArrayList<>();
        Pattern pattern = Pattern.compile("\\{(\\w+)\\}"); // Match {paramName}
        Matcher matcher = pattern.matcher(query);

        while (matcher.find()) {
            placeholders.add(matcher.group(1));  // Extract the placeholder name without braces
        }

        return placeholders;
    }
}
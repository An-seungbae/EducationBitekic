package externaldatabaseconnector.utils;

import com.mendix.logging.ILogNode;

import externaldatabaseconnector.pojo.MxDatabase;
import externaldatabaseconnector.pojo.QueryDetails;

public class QueryUtils {

    private final MxDatabase mxDatabase;
    private final QuerySanitizer querySanitizer;
    private final QueryParameterReplacer parameterReplacer;
    private final DateTimeConverter dateTimeConverter;
    private final QueryParameterSorter parameterSorter;

    public QueryUtils(MxDatabase aMxDatabase, ILogNode aLogNode) {
        this.mxDatabase = aMxDatabase;
        this.querySanitizer = new QuerySanitizer();
        this.parameterReplacer = new QueryParameterReplacer();
        this.dateTimeConverter = new DateTimeConverter(aLogNode);
        this.parameterSorter = new QueryParameterSorter();
    }

    /**
     * Updates the query details by sanitizing, sorting parameters, converting date/time, and replacing placeholders.
     */
    public QueryDetails getUpdatedQueryDetails() {
        String databaseType = this.mxDatabase.getConnectionDetails().getDatabaseType();
        QueryDetails queryDetails = this.mxDatabase.getQueryDetails();
        queryDetails.setQuery(querySanitizer.sanitize(queryDetails.getQuery()));
        queryDetails = parameterSorter.sortParameters(queryDetails);
        dateTimeConverter.convertToEpoch(queryDetails.getQueryParameters());
        queryDetails.setQuery(parameterReplacer.replacePlaceholders(queryDetails, databaseType));
        return queryDetails;
    }
}
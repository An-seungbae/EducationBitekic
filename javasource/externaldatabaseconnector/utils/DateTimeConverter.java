package externaldatabaseconnector.utils;

import com.mendix.logging.ILogNode;
import externaldatabaseconnector.pojo.QueryParameter;
import externaldatabaseconnector.database.constants.IMxMendixDataTypes;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;

public class DateTimeConverter {

    public static final String DATE_FORMAT_PATTERN = "yyyy-MM-dd'T'HH:mm:ss";
    private final ILogNode logNode;

    public DateTimeConverter(ILogNode logNode) {
        this.logNode = logNode;
    }

    /**
     * Converts date/time parameters to epoch time.
     */
    public void convertToEpoch(List<QueryParameter> queryParameters) {
        SimpleDateFormat dateFormatWithSeconds = new SimpleDateFormat(DATE_FORMAT_PATTERN);

        for (QueryParameter queryParameter : queryParameters) {
            String parameterValue = queryParameter.getValue();

            if (null == parameterValue
                || !IMxMendixDataTypes.DATETIME.equals(queryParameter.getDataType())
                || parameterValue.isBlank()
                || "null".equals(parameterValue)) {
                continue;
            }

            try {
                long epochMillis = dateFormatWithSeconds.parse(parameterValue).getTime();
                queryParameter.setValue(String.valueOf(epochMillis));
            } catch (ParseException exception) {
                logNode.error("Failed to parse date: " + parameterValue, exception);
            }
        }
    }
}
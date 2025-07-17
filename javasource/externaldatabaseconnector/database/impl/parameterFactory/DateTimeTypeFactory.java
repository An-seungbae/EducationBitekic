package externaldatabaseconnector.database.impl.parameterFactory;

import externaldatabaseconnector.database.impl.callablestatement.SqlParameterDatetime;
import externaldatabaseconnector.database.impl.callablestatement.SqlParameterPrimitiveValue;
import externaldatabaseconnector.database.impl.callablestatement.SqlParameterTime;
import externaldatabaseconnector.database.impl.callablestatement.SqlParameterTimeStamp;
import externaldatabaseconnector.pojo.QueryParameter;

public class DateTimeTypeFactory {
  public static final String TIME_WITH_TIME_ZONE_TYPE = "time with time zone";
  public static final String TIME_WITHOUT_TIME_ZONE_TYPE = "time without time zone";
  public static final String TIMESTAMP_WITH_TIME_ZONE_TYPE = "timestamp with time zone";
  public static final String TIMESTAMP_WITHOUT_TIME_ZONE_TYPE = "timestamp without time zone";

  public static SqlParameterPrimitiveValue GetDateTimeSqlParameter(QueryParameter queryParameter, int index){
    switch (queryParameter.getSqlDataType().toLowerCase()) {
      case TIME_WITH_TIME_ZONE_TYPE:
      case TIME_WITHOUT_TIME_ZONE_TYPE:
        return new SqlParameterTime(queryParameter, index);
      case TIMESTAMP_WITH_TIME_ZONE_TYPE:
      case TIMESTAMP_WITHOUT_TIME_ZONE_TYPE:
        return new SqlParameterTimeStamp(queryParameter, index);
      default:
        return new SqlParameterDatetime(queryParameter, index);
    }
  }
}

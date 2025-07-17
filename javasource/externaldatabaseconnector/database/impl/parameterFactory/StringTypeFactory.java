package externaldatabaseconnector.database.impl.parameterFactory;

import externaldatabaseconnector.database.impl.callablestatement.SqlParameterPrimitiveValue;
import externaldatabaseconnector.database.impl.callablestatement.SqlParameterString;
import externaldatabaseconnector.database.impl.callablestatement.SqlParameterUuid;
import externaldatabaseconnector.pojo.QueryParameter;

import java.nio.charset.StandardCharsets;
import java.sql.Types;

public class StringTypeFactory {
  public static final String CHAR_TYPE = "character";
  public static final String UUID_TYPE = "uuid";

  public static SqlParameterPrimitiveValue GetStringSqlParameter(QueryParameter queryParameter, int index){
    String value = queryParameter.getValue();
    if(value != null){
      queryParameter.setValue(java.net.URLDecoder.decode(value, StandardCharsets.UTF_8));
    }

    switch (queryParameter.getSqlDataType().toLowerCase()) {
      case CHAR_TYPE:
        return new SqlParameterString(queryParameter, index, Types.CHAR);
      case UUID_TYPE:
        return new SqlParameterUuid(queryParameter, index);
      default:
        return new SqlParameterString(queryParameter, index, Types.VARCHAR);
    }
  }
}

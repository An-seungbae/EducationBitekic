package externaldatabaseconnector.database.impl.parameterFactory;

import externaldatabaseconnector.database.impl.callablestatement.SqlParameterDecimal;
import externaldatabaseconnector.database.impl.callablestatement.SqlParameterDouble;
import externaldatabaseconnector.database.impl.callablestatement.SqlParameterFloat;
import externaldatabaseconnector.database.impl.callablestatement.SqlParameterPrimitiveValue;
import externaldatabaseconnector.pojo.QueryParameter;

import java.sql.Types;

public class DecimalTypeFactory {
  public static final String DOUBLE_PRECISION_TYPE = "double precision";
  public static final String FLOAT_TYPE = "float";
  public static final String NUMERIC_TYPE = "numeric";
  public static final String REAL_TYPE = "real";

  public static SqlParameterPrimitiveValue GetDecimalSqlParameter(QueryParameter queryParameter, int index){
    switch (queryParameter.getSqlDataType().toLowerCase()) {
      case FLOAT_TYPE:
        return new SqlParameterFloat(queryParameter, index, Types.FLOAT);
      case REAL_TYPE:
        return new SqlParameterFloat(queryParameter, index, Types.REAL);
      case DOUBLE_PRECISION_TYPE:
        return new SqlParameterDouble(queryParameter, index);
      case NUMERIC_TYPE:
      default:
        return new SqlParameterDecimal(queryParameter, index);
    }
  }
}

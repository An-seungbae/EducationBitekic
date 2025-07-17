package externaldatabaseconnector.database.impl.parameterFactory;

import externaldatabaseconnector.database.exceptions.MxDataTypeNotSupported;
import externaldatabaseconnector.database.impl.callablestatement.SqlParameterBoolean;
import externaldatabaseconnector.database.impl.callablestatement.SqlParameterPrimitiveValue;
import externaldatabaseconnector.exceptions.IMxErrorMessages;
import externaldatabaseconnector.pojo.QueryParameter;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class SqlParameterFactory {
  public static SqlParameterPrimitiveValue getSqlParameter(QueryParameter queryParameter, int index)
      throws SQLException, MxDataTypeNotSupported {
    try {
      switch (queryParameter.getDataType()) {
        case "INTEGER":
          return IntegerTypeFactory.GetIntegerSqlParameter(queryParameter, index);
        case "STRING":
          return StringTypeFactory.GetStringSqlParameter(queryParameter, index);
        case "BOOLEAN":
          return new SqlParameterBoolean(queryParameter, index);
        case "DECIMAL":
          return DecimalTypeFactory.GetDecimalSqlParameter(queryParameter, index);
        case "DATETIME":
          return DateTimeTypeFactory.GetDateTimeSqlParameter(queryParameter, index);
        default:
          String errorMessage =
              String.format(IMxErrorMessages.INVALID_PARAMETER_TYPE, queryParameter.getDataType(), queryParameter.getName());

          String messageCode = "E072";
          Map<String, String> messageDataMap = new HashMap<>();
          messageDataMap.put("parameterDataType", queryParameter.getDataType());
          messageDataMap.put("parameterName", queryParameter.getName());

          throw new MxDataTypeNotSupported(errorMessage, messageCode, messageDataMap);
      }
    } catch (Exception exception) {

      //If exception is caught with type of MxDataTypeNotSupported, do not alter message throw it as it is.
      if (exception.getClass().equals(MxDataTypeNotSupported.class)) {
        throw exception;
      }

      String message = String.format("An error has occurred while handling the parameter with name: '%s', \nCause : %s",
          queryParameter.getName(), exception.toString());
      throw new SQLException(message, exception);
    }
  }
}

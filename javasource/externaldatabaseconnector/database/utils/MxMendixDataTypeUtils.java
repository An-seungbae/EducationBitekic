package externaldatabaseconnector.database.utils;

import externaldatabaseconnector.database.constants.IMxMendixDataTypes;
import externaldatabaseconnector.database.constants.IMxSqlDataTypes;
import externaldatabaseconnector.database.exceptions.MxDataTypeNotSupported;
import externaldatabaseconnector.exceptions.IMxErrorMessages;

import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

public class MxMendixDataTypeUtils {
  public static String getMendixDataTypeByType(int sqlType, String sqlTypeName) throws MxDataTypeNotSupported {
    switch (sqlType) {
      case Types.CHAR:
      case Types.NCHAR:
      case Types.VARCHAR:
      case Types.NVARCHAR:
      case Types.LONGVARCHAR:
      case Types.CLOB:
      case Types.NCLOB:
      case 2016: //Oracle JSON
        return IMxMendixDataTypes.STRING;
      case Types.INTEGER:
      case Types.SMALLINT:
      case Types.TINYINT:
        return IMxMendixDataTypes.INTEGER;
      case Types.BIGINT:
        return IMxMendixDataTypes.LONG;
      case Types.DOUBLE:
      case Types.FLOAT:
      case Types.REAL:
      case Types.DECIMAL:
      case Types.NUMERIC:
        return IMxMendixDataTypes.DECIMAL;
      case Types.DATE:
      case Types.TIME:
      case Types.TIMESTAMP:
        return IMxMendixDataTypes.DATETIME;
      case Types.BOOLEAN:
      case Types.BIT:
        return IMxMendixDataTypes.BOOLEAN;
      case Types.OTHER:
        return getMendixDataTypeForOther(sqlTypeName);
      case Types.BLOB:
      default:
        String errorMessage = String.format(IMxErrorMessages.SQL_DATA_TYPE_NOT_SUPPORTED, sqlTypeName);

        String messageCode = "E073";
        Map<String, String> messageDataMap = new HashMap<>();
        messageDataMap.put("SqlDataType", sqlTypeName);

        throw new MxDataTypeNotSupported(errorMessage,  messageCode, messageDataMap);
    }
  }

  private static String getMendixDataTypeForOther(String sqlTypeName) throws MxDataTypeNotSupported {
    switch (sqlTypeName) {
      case IMxSqlDataTypes.UUID:
      case IMxSqlDataTypes.JSONB:
        return IMxMendixDataTypes.STRING;
      default:
        String errorMessage = String.format(IMxErrorMessages.SQL_DATA_TYPE_NOT_SUPPORTED, sqlTypeName);

        String messageCode = "E073";
        Map<String, String> messageDataMap = new HashMap<>();
        messageDataMap.put("SqlDataType", sqlTypeName);

        throw new MxDataTypeNotSupported(errorMessage, messageCode, messageDataMap);
    }
  }
}

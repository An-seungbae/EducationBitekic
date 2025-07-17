package externaldatabaseconnector.database.utils;

import externaldatabaseconnector.database.constants.IMxMendixDataTypes;
import externaldatabaseconnector.database.exceptions.MxDataTypeNotSupported;
import externaldatabaseconnector.livepreview.responses.pojo.ColumnMetaData;
import externaldatabaseconnector.livepreview.responses.pojo.RowData;
import externaldatabaseconnector.livepreview.responses.pojo.TableResult;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class MxResultSetUtils {

  //Read result set and fill data into table result
  public static TableResult buildTableResult(ResultSet resultSet) throws MxDataTypeNotSupported, SQLException {

    if (resultSet == null)
      return null;

    ResultSetMetaData metaData = resultSet.getMetaData();
    int columnCount = metaData.getColumnCount();

    List<ColumnMetaData> columnMetadataList = new ArrayList<>();
    Pattern whiteSpacePattern = Pattern.compile("\\s");

    for (int i = 1; i <= columnCount; i++) {
      //Used getColumnLabel() rather than getColumnName() to retrieve the column name when aliases are used.
      String columnName = metaData.getColumnLabel(i);


      String dataType = MxMendixDataTypeUtils.getMendixDataTypeByType(metaData.getColumnType(i), metaData.getColumnTypeName(i));
      String sqlDataType = metaData.getColumnTypeName(i);
      int columnSize = getColumnSize(metaData.getColumnDisplaySize(i), dataType);

      ColumnMetaData columnMetadata = new ColumnMetaData(columnName, dataType, sqlDataType, columnSize);
      columnMetadataList.add(columnMetadata);
    }

    //Read all rows available in the resultset
    List<RowData> rowDataList = new ArrayList<>();
    int resultRecordCount = 0;
    while (resultSet.next()) {
      if (resultRecordCount < 100) {
        RowData rowData = new RowData();
        List<Object> row = new ArrayList<>();
        for (int i = 1; i <= columnCount; i++) {
          Object columnValue = getValueByType(resultSet, i, metaData.getColumnType(i));
          row.add(columnValue);
        }
        rowData.setRow(row);
        rowDataList.add(rowData);
      }
      resultRecordCount++;
    }

    //Close result set
    resultSet.close();

    return new TableResult(columnMetadataList, rowDataList, resultRecordCount);
  }

  private static int getColumnSize(int columnDisplaySize, String dataType) {
    if (!dataType.equals(IMxMendixDataTypes.STRING))
      return -1;

    if (columnDisplaySize > 10000 || columnDisplaySize < 0)
      return 0;

    return columnDisplaySize;
  }

  // Method to retrieve values dynamically based on SQL type
  private static Object getValueByType(ResultSet resultSet, int columnIndex, int sqlType) throws SQLException {

    //Return null for null columns to maintain consistency with the C# design-time behavior.
    if (resultSet.getObject(columnIndex) == null) {
      return null;
    }

    switch (sqlType) {
      case Types.CHAR:
      case Types.NCHAR:
      case Types.VARCHAR:
      case Types.NVARCHAR:
      case Types.LONGVARCHAR:
      case Types.CLOB://Used getString for clob column instead of getClob, getString() directly retrieves the CLOB content as a String
      case Types.NCLOB:
      case 2016://Oracle JSON
        return resultSet.getString(columnIndex);
      case Types.INTEGER:
        return resultSet.getInt(columnIndex);
      case Types.BIGINT:
        return resultSet.getLong(columnIndex);
      case Types.SMALLINT:
        return resultSet.getShort(columnIndex);
      case Types.TINYINT:
        return resultSet.getByte(columnIndex);
      case Types.DOUBLE:
        return resultSet.getDouble(columnIndex);
      case Types.FLOAT:
      case Types.REAL:
        return resultSet.getFloat(columnIndex);
      case Types.DECIMAL:
      case Types.NUMERIC:
        return resultSet.getBigDecimal(columnIndex);
      case Types.DATE:
        return resultSet.getDate(columnIndex);
      case Types.TIME:
        return resultSet.getTime(columnIndex);
      case Types.TIMESTAMP:
        return resultSet.getTimestamp(columnIndex);
      case Types.BOOLEAN:
      case Types.BIT:
        return resultSet.getBoolean(columnIndex);
      case Types.BLOB:
        return resultSet.getBlob(columnIndex);
      default:
        return resultSet.getObject(columnIndex);
    }
  }
}

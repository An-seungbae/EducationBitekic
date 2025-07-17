package externaldatabaseconnector.database.impl.dbschema.helper;

import externaldatabaseconnector.database.enums.CallableParameterMode;
import externaldatabaseconnector.livepreview.responses.pojo.CallableMetaData;
import externaldatabaseconnector.livepreview.responses.pojo.CallableParameter;
import externaldatabaseconnector.livepreview.responses.pojo.ColumnMetaData;
import externaldatabaseconnector.livepreview.responses.pojo.TableViewMetaData;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ResultProcessor {
  public static List<TableViewMetaData> processTableViewMetadata(ResultSet resultSet) throws SQLException {
    Map<String, TableViewMetaData> tableViewMap = new HashMap<>();

    while (resultSet.next()) {
      String schemaName = resultSet.getString(1);
      String tableName = resultSet.getString(2);
      String columnName = resultSet.getString(3);
      String sqlDataType = resultSet.getString(4);

      ColumnMetaData columnMetaData = new ColumnMetaData(columnName, "", sqlDataType, -1);

      String key = schemaName + "." + tableName;
      TableViewMetaData tableViewMetaData = tableViewMap.get(key);

      if (tableViewMetaData == null) {
        tableViewMetaData = new TableViewMetaData(tableName, new ArrayList<>(), schemaName);
        tableViewMap.put(key, tableViewMetaData);
      }
      tableViewMetaData.getColumnMetaDataList().add(columnMetaData);
    }

    return new ArrayList<>(tableViewMap.values());
  }

  public static List<CallableMetaData> processCallableMetadata(ResultSet resultSet) throws SQLException {
    List<CallableMetaData> metaDataList = new ArrayList<>();
    int fieldCount = resultSet.getMetaData().getColumnCount();

    while (resultSet.next()) {
      String schemaName = resultSet.getString(1);
      String callableName = resultSet.getString(2);
      String parameters = resultSet.getString(3);
      List<CallableParameter> callableParameterList = getCallableParameterData(parameters);

      // for functions and in some dbs like snowflake have returnType also
      String returnType = fieldCount > 3 ? resultSet.getString(4) : "";

      metaDataList.add(new CallableMetaData(callableName, callableParameterList, schemaName, returnType));
    }

    return metaDataList;
  }

  private static List<CallableParameter> getCallableParameterData(String parameterString) {
    if (parameterString == null || parameterString.isBlank()) {
      return Collections.emptyList();
    }

    return Arrays.stream(parameterString.split(", "))
        .map(parameter -> {
          String[] parameterParts = parameter.split(" ");
          return new CallableParameter(
              parameterParts[1],
              String.join(" ", Arrays.copyOfRange(parameterParts, 2, parameterParts.length)),
              CallableParameterMode.valueOf(parameterParts[0]),
              "",
              "");
        })
        .collect(Collectors.toList());
  }
}



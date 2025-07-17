package externaldatabaseconnector.database.impl.dbschema.provider;


import externaldatabaseconnector.database.constants.IMxDatabaseSchema;
import externaldatabaseconnector.database.enums.CallableParameterMode;
import externaldatabaseconnector.database.impl.dbschema.helper.CallableParameterData;
import externaldatabaseconnector.livepreview.responses.pojo.CallableMetaData;
import externaldatabaseconnector.livepreview.responses.pojo.CallableParameter;
import externaldatabaseconnector.livepreview.responses.pojo.ColumnMetaData;
import externaldatabaseconnector.livepreview.responses.pojo.TableViewMetaData;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public abstract class MxMetaDataBasedSchemaInfoProvider extends
    MxBaseSchemaInfoProvider {
  protected MxMetaDataBasedSchemaInfoProvider(Connection connection) throws SQLException {
    super(connection);
  }

  @Override
  protected List<TableViewMetaData> getTableMetaData() throws SQLException {
    return getTableViewMetaData(new String[]{ IMxDatabaseSchema.TABLE_TYPE, IMxDatabaseSchema.BASE_TABLE });
  }

  @Override
  protected List<TableViewMetaData> getViewMetaData() throws SQLException {
    return getTableViewMetaData(new String[]{ IMxDatabaseSchema.VIEW_TYPE });
  }

  private List<TableViewMetaData> getTableViewMetaData(String[] types) throws SQLException {
    List<TableViewMetaData> tableViewMetaDataList = new ArrayList<>();
    try (ResultSet tableResultSet = metaData.getTables(catalog, schema, "%", types)) {
      while (tableResultSet.next()) {
        String tableName = tableResultSet.getString(IMxDatabaseSchema.TABLE_NAME);

        List<ColumnMetaData> columns = getTableViewColumns(tableName);
        tableViewMetaDataList.add(new TableViewMetaData(tableName, columns, resolvedSchemaName));
      }
    }
    return tableViewMetaDataList;
  }

  private List<ColumnMetaData> getTableViewColumns(String tableName) throws SQLException {
    List<ColumnMetaData> columnMetaDataList = new ArrayList<>();
    try (ResultSet columnResultSet = metaData.getColumns(catalog, schema, tableName, "%")) {
      while (columnResultSet.next()) {
        String columnName = columnResultSet.getString(IMxDatabaseSchema.COLUMN_NAME);
        String sqlDataType = columnResultSet.getString(IMxDatabaseSchema.TYPE_NAME);

        columnMetaDataList.add(new ColumnMetaData(columnName, "", sqlDataType, -1));
      }
    }
    return columnMetaDataList;
  }


  @Override
  protected List<CallableMetaData> getProcedureMetaData() throws SQLException {
    return getCallableMetaData(IMxDatabaseSchema.PROCEDURE_NAME, IMxDatabaseSchema.PROCEDURE_TYPE);
  }

  @Override
  protected List<CallableMetaData> getFunctionMetaData() throws SQLException {
    return getCallableMetaData(IMxDatabaseSchema.FUNCTION_NAME, IMxDatabaseSchema.FUNCTION_TYPE);
  }

  private List<CallableMetaData> getCallableMetaData(String nameColumn, String type)
      throws SQLException {
    List<CallableMetaData> callableMetaDataList = new ArrayList<>();

    try(ResultSet callableResultSet = type.equals(IMxDatabaseSchema.PROCEDURE_TYPE)
        ? metaData.getProcedures(catalog, schema, "%")
        : metaData.getFunctions(catalog, schema, "%")){
      while (callableResultSet.next()) {
        String callableName = callableResultSet.getString(nameColumn);

        CallableParameterData parameterResult = getCallableParameters(callableName, type);
        callableMetaDataList.add(
            new CallableMetaData(callableName, parameterResult.getParameters(), resolvedSchemaName, parameterResult.getReturnType()));
      }
    } catch (SQLException e) {
      return callableMetaDataList;
    }
    return callableMetaDataList;
  }

  private CallableParameterData getCallableParameters(String callableName, String type) throws SQLException {
    List<CallableParameter> parameters = new ArrayList<>();
    String returnType = "";

    try (ResultSet paramsResultSet = type.equals(IMxDatabaseSchema.PROCEDURE_TYPE)
        ? metaData.getProcedureColumns(catalog, schema, callableName, "%")
        : metaData.getFunctionColumns(catalog, schema, callableName, "%")) {
      while (paramsResultSet.next()) {
        String paramName = paramsResultSet.getString(4); // parameter_name or column_name
        String paramSqlType = paramsResultSet.getString(IMxDatabaseSchema.TYPE_NAME); //sql type_name
        int columnType = paramsResultSet.getInt(5); // column_type or parameter_type  in/out/inout

        CallableParameterMode paramMode = type.equals(IMxDatabaseSchema.PROCEDURE_TYPE)
            ? mapProcedureColumnTypeToParameterMode(columnType)
            : mapFunctionColumnTypeToParameterMode(columnType);

        if (paramMode == CallableParameterMode.RETURN) {
          returnType = paramSqlType; // Capture return type
        } else {
          parameters.add(new CallableParameter(paramName, paramSqlType, paramMode, "", ""));
        }
      }
    }
    return new CallableParameterData(parameters, returnType);
  }

  private CallableParameterMode mapProcedureColumnTypeToParameterMode(int columnType) {
    switch (columnType) {
      case DatabaseMetaData.procedureColumnIn:
        return CallableParameterMode.IN;
      case DatabaseMetaData.procedureColumnOut:
        return CallableParameterMode.OUT;
      case DatabaseMetaData.procedureColumnInOut:
        return CallableParameterMode.INOUT;
      case DatabaseMetaData.procedureColumnReturn:
        return CallableParameterMode.RETURN;
      default:
        return CallableParameterMode.UNKNOWN;
    }
  }

  private CallableParameterMode mapFunctionColumnTypeToParameterMode(int columnType) {
    switch (columnType) {
      case DatabaseMetaData.functionColumnIn:
        return CallableParameterMode.IN;
      case DatabaseMetaData.functionColumnOut:
        return CallableParameterMode.OUT;
      case DatabaseMetaData.functionColumnInOut:
        return CallableParameterMode.INOUT;
      case DatabaseMetaData.functionReturn:
        return CallableParameterMode.RETURN;
      default:
        return CallableParameterMode.UNKNOWN;
    }
  }
}
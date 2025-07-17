package externaldatabaseconnector.database.impl.dbschema.provider;

import java.sql.Connection;
import java.sql.SQLException;

public class MxMSSQLSchemaInfoProvider extends MxCommonQuerySchemaInfoProvider {
  public MxMSSQLSchemaInfoProvider(Connection connection) throws SQLException {
    super(connection);
  }

  @Override
  protected String getProcedureMetaDataQuery() {
    return "SELECT SCHEMA_NAME(schema_id) AS SchemaName, o.name AS ProcedureName, "
        + "STUFF((SELECT ', ' + CASE WHEN p.is_output = 1 THEN 'OUT ' ELSE 'IN ' END + p.name + ' ' + TYPE_NAME(p.user_type_id) "
        + "FROM sys.parameters p "
        + "WHERE p.object_id = o.object_id "
        + "ORDER BY p.parameter_id FOR XML PATH('')), 1, 2, '') AS Parameters "
        + "FROM sys.objects o "
        + "WHERE o.type = 'P' "
        + "ORDER BY SchemaName, ProcedureName";
  }

  @Override
  protected String getFunctionMetaDataQuery() {
    return "SELECT SCHEMA_NAME(o.schema_id) AS SchemaName, o.name AS FunctionName, "
        + "STUFF((SELECT ', IN ' + p.name + ' ' + TYPE_NAME(p.user_type_id) "
        + "FROM sys.parameters p "
        + "WHERE p.object_id = o.object_id AND p.is_output != 1 "
        + "ORDER BY p.parameter_id FOR XML PATH('')), 1, 2, '') AS Parameters, "
        + "CASE WHEN o.type = 'IF' THEN 'TABLE' ELSE (SELECT TOP 1 TYPE_NAME(p.user_type_id) "
        + "FROM sys.parameters p "
        + "WHERE p.object_id = o.object_id AND p.is_output = 1) END AS ReturnType "
        + "FROM sys.objects o "
        + "WHERE o.type IN ('FN', 'IF') "
        + "ORDER BY SchemaName, FunctionName";
  }
}

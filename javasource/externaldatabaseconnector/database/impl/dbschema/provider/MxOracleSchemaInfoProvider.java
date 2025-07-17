package externaldatabaseconnector.database.impl.dbschema.provider;

import java.sql.Connection;
import java.sql.SQLException;

public class MxOracleSchemaInfoProvider extends MxQueryBasedSchemaInfoProvider {
  public MxOracleSchemaInfoProvider(Connection connection) throws SQLException {
    super(connection);
  }

  @Override
  protected String getTableMetaDataQuery() {
    return "SELECT OWNER, TABLE_NAME, COLUMN_NAME, DATA_TYPE "
        + "FROM all_tab_columns WHERE TABLE_NAME "
        + "IN (SELECT TABLE_NAME FROM ALL_TABLES) "
        + "ORDER BY TABLE_NAME, COLUMN_ID";
  }

  @Override
  protected String getViewMetaDataQuery() {
    return "SELECT OWNER, TABLE_NAME, COLUMN_NAME, DATA_TYPE "
        + "FROM all_tab_columns WHERE TABLE_NAME "
        + "IN (SELECT VIEW_NAME FROM ALL_VIEWS) "
        + "ORDER BY TABLE_NAME, COLUMN_ID";
  }

  @Override
  protected String getProcedureMetaDataQuery() {
    return "SELECT p.OWNER AS SchemaName, p.OBJECT_NAME AS ProcedureName, "
        + "LISTAGG(CASE WHEN a.IN_OUT = 'IN' THEN 'IN ' WHEN a.IN_OUT = 'OUT' THEN 'OUT ' WHEN a.IN_OUT = 'IN/OUT' THEN 'INOUT ' ELSE '' END || a.ARGUMENT_NAME || ' ' || a.DATA_TYPE, ', ') "
        + "WITHIN GROUP (ORDER BY a.POSITION) AS Parameters "
        + "FROM ALL_PROCEDURES p LEFT JOIN ALL_ARGUMENTS a "
        + "ON p.OWNER = a.OWNER AND p.OBJECT_NAME = a.OBJECT_NAME AND a.ARGUMENT_NAME IS NOT NULL "
        + "WHERE p.OBJECT_TYPE = 'PROCEDURE' "
        + "GROUP BY p.OWNER, p.OBJECT_NAME "
        + "ORDER BY p.OWNER, p.OBJECT_NAME";
  }

  @Override
  protected String getFunctionMetaDataQuery() {
    return "SELECT p.OWNER AS SchemaName, p.OBJECT_NAME AS FunctionName, "
        + "RTRIM(XMLCAST(XMLAGG(XMLELEMENT(e, CASE "
        + "WHEN a.POSITION <> 0 THEN CASE WHEN a.IN_OUT = 'IN' THEN 'IN ' "
        + "WHEN a.IN_OUT = 'OUT' THEN 'OUT ' WHEN a.IN_OUT = 'IN/OUT' THEN 'INOUT ' "
        + "ELSE '' END || a.ARGUMENT_NAME || ' ' || a.DATA_TYPE || ', ' ELSE '' END) ORDER BY a.POSITION) AS CLOB), ', ') AS Parameters, "
        + "MAX(CASE WHEN a.POSITION = 0 THEN a.DATA_TYPE ELSE NULL END) AS ReturnType "
        + "FROM ALL_OBJECTS p LEFT JOIN ALL_ARGUMENTS a "
        + "ON p.OWNER = a.OWNER AND p.OBJECT_NAME = a.OBJECT_NAME "
        + "WHERE p.OBJECT_TYPE = 'FUNCTION' "
        + "GROUP BY p.OWNER, p.OBJECT_NAME "
        + "ORDER BY p.OWNER, p.OBJECT_NAME";
  }
}

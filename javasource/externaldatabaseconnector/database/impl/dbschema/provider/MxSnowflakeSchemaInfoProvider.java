package externaldatabaseconnector.database.impl.dbschema.provider;

import java.sql.Connection;
import java.sql.SQLException;

public class MxSnowflakeSchemaInfoProvider extends MxQueryBasedSchemaInfoProvider {

  private final String database;

  public MxSnowflakeSchemaInfoProvider(Connection connection) throws SQLException {
    super(connection);
    this.database = connection.getCatalog();
  }

  @Override
  protected String getTableMetaDataQuery() {
    return "SELECT c.TABLE_SCHEMA, c.TABLE_NAME, c.COLUMN_NAME, c.DATA_TYPE "
        + "FROM " + database + ".INFORMATION_SCHEMA.COLUMNS c "
        + "WHERE TABLE_NAME IN (SELECT DISTINCT t.TABLE_NAME FROM " + database + ".INFORMATION_SCHEMA.TABLES t "
        + "WHERE t.TABLE_TYPE = 'BASE TABLE' AND t.TABLE_SCHEMA = c.TABLE_SCHEMA) "
        + "ORDER BY c.TABLE_SCHEMA, c.TABLE_NAME, c.ORDINAL_POSITION";
  }

  @Override
  protected String getViewMetaDataQuery() {
    return "SELECT c.TABLE_SCHEMA, c.TABLE_NAME, c.COLUMN_NAME, c.DATA_TYPE "
        + "FROM " + database + ".INFORMATION_SCHEMA.COLUMNS c "
        + "WHERE TABLE_NAME IN (SELECT DISTINCT v.TABLE_NAME FROM " + database + ".INFORMATION_SCHEMA.VIEWS v) "
        + "ORDER BY c.TABLE_SCHEMA, c.TABLE_NAME, c.ORDINAL_POSITION";
  }

  @Override
  protected String getProcedureMetaDataQuery() {
    return "SELECT PROCEDURE_SCHEMA, PROCEDURE_NAME, "
        + "CASE WHEN ARGUMENT_SIGNATURE = '()' THEN '' ELSE CONCAT('IN ', REPLACE(REPLACE(REPLACE(ARGUMENT_SIGNATURE, '(', ''), ')', ''), ',', ', IN')) END AS PROCEDURE_ARGUMENT_SIGNATURE,"
        + " DATA_TYPE FROM INFORMATION_SCHEMA.PROCEDURES "
        + "WHERE PROCEDURE_CATALOG = '" + database + "' "
        + "ORDER BY PROCEDURE_SCHEMA, PROCEDURE_NAME";
  }

  @Override
  protected String getFunctionMetaDataQuery() {
    return "SELECT FUNCTION_SCHEMA, FUNCTION_NAME, "
        + "CASE WHEN ARGUMENT_SIGNATURE = '()' THEN '' ELSE CONCAT('IN ', REPLACE(REPLACE(REPLACE(ARGUMENT_SIGNATURE, '(', ''), ')', ''), ',', ', IN')) END AS PROCEDURE_ARGUMENT_SIGNATURE,"
        + " DATA_TYPE FROM INFORMATION_SCHEMA.FUNCTIONS "
        + "WHERE FUNCTION_CATALOG = '" + database + "' "
        + "ORDER BY FUNCTION_SCHEMA, FUNCTION_NAME";
  }
}

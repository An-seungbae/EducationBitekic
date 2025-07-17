package externaldatabaseconnector.database.impl.dbschema.provider;

import java.sql.Connection;
import java.sql.SQLException;

public abstract class MxCommonQuerySchemaInfoProvider extends MxQueryBasedSchemaInfoProvider{

  public MxCommonQuerySchemaInfoProvider(Connection connection) throws SQLException {
    super(connection);
  }

  @Override
  protected String getTableMetaDataQuery() {
    return "SELECT TABLE_SCHEMA, TABLE_NAME, COLUMN_NAME, DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS"
        + " WHERE TABLE_NAME IN (SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_TYPE = 'BASE TABLE')"
        + " ORDER BY TABLE_NAME, ORDINAL_POSITION";
  }

  @Override
  protected String getViewMetaDataQuery() {
    return "SELECT TABLE_SCHEMA, TABLE_NAME, COLUMN_NAME, DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS"
        + " WHERE TABLE_NAME IN (SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_TYPE = 'VIEW')"
        + " ORDER BY TABLE_NAME, ORDINAL_POSITION";
  }

  @Override
  protected abstract String getProcedureMetaDataQuery();

  @Override
  protected abstract String getFunctionMetaDataQuery();
}

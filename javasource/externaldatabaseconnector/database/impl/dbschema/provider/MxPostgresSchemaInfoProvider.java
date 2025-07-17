package externaldatabaseconnector.database.impl.dbschema.provider;

import java.sql.Connection;
import java.sql.SQLException;

public class MxPostgresSchemaInfoProvider extends MxCommonQuerySchemaInfoProvider {
  public MxPostgresSchemaInfoProvider(Connection connection) throws SQLException {
    super(connection);
  }

  @Override
  protected String getProcedureMetaDataQuery() {
    return "SELECT proc.ROUTINE_SCHEMA, proc.ROUTINE_NAME, "
        + "COALESCE(STRING_AGG(param.PARAMETER_MODE || ' ' || param.PARAMETER_NAME || ' ' || param.DATA_TYPE, ', '), '') AS parameters "
        + "FROM INFORMATION_SCHEMA.ROUTINES proc LEFT JOIN INFORMATION_SCHEMA.PARAMETERS param "
        + "ON param.SPECIFIC_NAME = proc.SPECIFIC_NAME "
        + "WHERE proc.ROUTINE_TYPE = 'PROCEDURE' AND proc.ROUTINE_SCHEMA NOT LIKE 'pg_%' AND proc.ROUTINE_SCHEMA != 'information_schema' "
        + "GROUP BY proc.ROUTINE_SCHEMA, proc.ROUTINE_NAME, proc.SPECIFIC_NAME";
  }

  @Override
  protected String getFunctionMetaDataQuery() {
    return "SELECT proc.ROUTINE_SCHEMA, proc.ROUTINE_NAME, "
        + "COALESCE(STRING_AGG(COALESCE(param.PARAMETER_MODE || ' ' || param.PARAMETER_NAME || ' ' || param.DATA_TYPE, ''), ', ' ORDER BY param.ORDINAL_POSITION), '') AS parameters, "
        + "proc.DATA_TYPE AS return_type "
        + "FROM INFORMATION_SCHEMA.ROUTINES proc LEFT JOIN INFORMATION_SCHEMA.PARAMETERS param "
        + "ON param.SPECIFIC_NAME = proc.SPECIFIC_NAME "
        + "WHERE proc.ROUTINE_TYPE = 'FUNCTION' AND proc.ROUTINE_SCHEMA NOT LIKE 'pg_%' AND proc.ROUTINE_SCHEMA != 'information_schema' "
        + "GROUP BY proc.ROUTINE_SCHEMA, proc.ROUTINE_NAME, proc.SPECIFIC_NAME, proc.DATA_TYPE";
  }
}

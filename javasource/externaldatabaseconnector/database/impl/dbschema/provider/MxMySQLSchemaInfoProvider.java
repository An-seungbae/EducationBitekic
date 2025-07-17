package externaldatabaseconnector.database.impl.dbschema.provider;

import java.sql.Connection;
import java.sql.SQLException;

public class MxMySQLSchemaInfoProvider extends MxCommonQuerySchemaInfoProvider {
  public MxMySQLSchemaInfoProvider(Connection connection) throws SQLException {
    super(connection);
  }

  @Override
  protected String getProcedureMetaDataQuery() {
    return "SELECT R.ROUTINE_SCHEMA, R.ROUTINE_NAME, "
        + "GROUP_CONCAT(CONCAT"
        + "(CASE WHEN P.PARAMETER_MODE = 'IN' THEN 'IN ' WHEN P.PARAMETER_MODE = 'OUT' THEN 'OUT ' WHEN P.PARAMETER_MODE = 'INOUT' THEN 'INOUT ' ELSE '' END,"
        + " P.PARAMETER_NAME, ' ', P.DATA_TYPE)"
        + " ORDER BY P.ORDINAL_POSITION SEPARATOR ', ') AS parameters "
        + "FROM information_schema.PARAMETERS AS P RIGHT JOIN information_schema.ROUTINES AS R"
        + " ON P.SPECIFIC_NAME = R.SPECIFIC_NAME WHERE R.ROUTINE_TYPE = 'PROCEDURE' "
        + "GROUP BY R.ROUTINE_SCHEMA, R.ROUTINE_NAME ORDER BY R.ROUTINE_SCHEMA, R.ROUTINE_NAME";
  }

  @Override
  protected String getFunctionMetaDataQuery() {
    return "SELECT R.ROUTINE_SCHEMA, R.ROUTINE_NAME, "
        + "GROUP_CONCAT(CONCAT"
        + "(CASE WHEN P.PARAMETER_MODE = 'IN' THEN 'IN ' ELSE '' END, P.PARAMETER_NAME, ' ', P.DATA_TYPE) ORDER BY P.ORDINAL_POSITION SEPARATOR ', ') AS PARAMETERS,"
        + " R.DATA_TYPE AS RESULT_TYPE "
        + "FROM information_schema.PARAMETERS AS P RIGHT JOIN information_schema.ROUTINES AS R "
        + "ON P.SPECIFIC_NAME = R.SPECIFIC_NAME WHERE R.ROUTINE_TYPE = 'FUNCTION' "
        + "GROUP BY R.ROUTINE_SCHEMA, R.ROUTINE_NAME, R.DATA_TYPE ORDER BY R.ROUTINE_SCHEMA, R.ROUTINE_NAME";
  }
}

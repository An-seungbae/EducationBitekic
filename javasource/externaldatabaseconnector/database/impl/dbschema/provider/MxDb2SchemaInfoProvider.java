package externaldatabaseconnector.database.impl.dbschema.provider;

import java.sql.Connection;
import java.sql.SQLException;

public class MxDb2SchemaInfoProvider extends MxQueryBasedSchemaInfoProvider{

  public MxDb2SchemaInfoProvider(Connection connection) throws SQLException {
    super(connection);
  }

  @Override
  protected String getTableMetaDataQuery() {
    return "SELECT TABSCHEMA AS TABLE_SCHEMA, TABNAME AS TABLE_NAME, COLNAME AS COLUMN_NAME,"
        + " TYPENAME AS DATA_TYPE"
        + " FROM SYSCAT.COLUMNS"
        + " WHERE TABNAME IN (SELECT TABNAME FROM SYSCAT.TABLES WHERE TYPE = 'T' AND OWNERTYPE = 'U')"
        + " ORDER BY TABNAME, COLNO";
  }

  @Override
  protected String getViewMetaDataQuery() {
    return "SELECT TABSCHEMA AS TABLE_SCHEMA, TABNAME AS TABLE_NAME, COLNAME AS COLUMN_NAME,"
        + " TYPENAME AS DATA_TYPE"
        + " FROM SYSCAT.COLUMNS"
        + " WHERE TABNAME IN (SELECT TABNAME FROM SYSCAT.TABLES WHERE TYPE = 'V' AND OWNERTYPE = 'U')"
        + " ORDER BY TABNAME, COLNO";
  }

  @Override
  protected String getProcedureMetaDataQuery() {
    return "SELECT proc.PROCSCHEMA AS ROUTINE_SCHEMA, proc.PROCNAME AS ROUTINE_NAME,"
        + " COALESCE(LISTAGG(parm.PARM_MODE || ' ' || parm.PARMNAME || ' ' || parm.TYPENAME, ', ')"
        + " WITHIN GROUP (ORDER BY parm.ORDINAL), '') AS PARAMETERS "
        + "FROM SYSCAT.PROCEDURES proc LEFT JOIN SYSCAT.PROCPARMS parm ON proc.SPECIFICNAME = parm.SPECIFICNAME "
        + "WHERE proc.PROCSCHEMA NOT LIKE 'SYS%' GROUP BY proc.PROCSCHEMA, proc.PROCNAME";
  }

  @Override
  protected String getFunctionMetaDataQuery() {
    return "SELECT func.FUNCSCHEMA AS ROUTINE_SCHEMA, func.FUNCNAME AS ROUTINE_NAME,"
        + " COALESCE(LISTAGG(CASE WHEN parm.ORDINAL > 0 THEN 'IN ' || parm.PARMNAME || ' ' || parm.TYPENAME "
        + "ELSE NULL END, ', ') WITHIN GROUP (ORDER BY parm.ORDINAL), '') AS PARAMETERS, "
        + "MAX(CASE WHEN parm.ORDINAL = 0 THEN parm.TYPENAME ELSE NULL END) AS RETURN_TYPE "
        + "FROM SYSCAT.FUNCTIONS func LEFT JOIN SYSCAT.FUNCPARMS parm ON func.SPECIFICNAME = parm.SPECIFICNAME "
        + "WHERE func.FUNCSCHEMA NOT LIKE 'SYS%' GROUP BY func.FUNCSCHEMA, func.FUNCNAME";
  }
}

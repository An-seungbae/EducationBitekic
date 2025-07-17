package externaldatabaseconnector.database.impl.callablestatement;

import externaldatabaseconnector.pojo.QueryParameter;

import java.sql.CallableStatement;
import java.sql.SQLException;
import java.sql.Types;

public class SqlParameterShort extends SqlParameterPrimitiveValue<Short> {

  public SqlParameterShort(QueryParameter queryParameter, int index) {
    super(queryParameter, Types.SMALLINT, index);
  }

  public Short getStatementValue(CallableStatement callableStatement) throws SQLException {
    return callableStatement.getShort(this.parameterIndex);
  }

  public void setStatementValue(CallableStatement callableStatement, String value) throws SQLException {
    callableStatement.setShort(this.parameterIndex, Short.parseShort(value));
  }
}

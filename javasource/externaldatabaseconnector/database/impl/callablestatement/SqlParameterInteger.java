package externaldatabaseconnector.database.impl.callablestatement;

import externaldatabaseconnector.pojo.QueryParameter;

import java.sql.CallableStatement;
import java.sql.SQLException;
import java.sql.Types;

public class SqlParameterInteger extends SqlParameterPrimitiveValue<Integer> {
  public SqlParameterInteger(QueryParameter queryParameter, int index) {
    super(queryParameter, Types.INTEGER, index);
  }

  public Integer getStatementValue(CallableStatement callableStatement) throws SQLException {
    return callableStatement.getInt(this.parameterIndex);
  }

  public void setStatementValue(CallableStatement callableStatement, String value) throws SQLException {
    callableStatement.setInt(this.parameterIndex, Integer.parseInt(value));
  }
}

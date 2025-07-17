package externaldatabaseconnector.database.impl.callablestatement;

import externaldatabaseconnector.pojo.QueryParameter;

import java.sql.CallableStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.UUID;

public class SqlParameterUuid extends SqlParameterPrimitiveValue<String> {

  public SqlParameterUuid(QueryParameter queryParameter, int index) {
    super(queryParameter, Types.OTHER, index);
  }

  public String getStatementValue(CallableStatement callableStatement) throws SQLException {
    return callableStatement.getObject(this.parameterIndex).toString();
  }

  public void setStatementValue(CallableStatement callableStatement, String value) throws SQLException {
    UUID uuidValue = UUID.fromString(value);
    callableStatement.setObject(this.parameterIndex, uuidValue);
  }
}

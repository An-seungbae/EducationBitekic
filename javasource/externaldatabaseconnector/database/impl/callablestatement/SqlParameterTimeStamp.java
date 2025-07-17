package externaldatabaseconnector.database.impl.callablestatement;

import externaldatabaseconnector.pojo.QueryParameter;

import java.sql.CallableStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;

public class SqlParameterTimeStamp extends SqlParameterPrimitiveValue<Timestamp> {
  public SqlParameterTimeStamp(QueryParameter queryParameter, int index) {
    super(queryParameter, Types.TIMESTAMP, index);
  }

  public Timestamp getStatementValue(CallableStatement callableStatement) throws SQLException {
    return callableStatement.getTimestamp(this.parameterIndex);
  }

  public void setStatementValue(CallableStatement callableStatement, String value) throws SQLException {
    long epochTime = Long.parseLong(value);
    callableStatement.setTimestamp(this.parameterIndex, new Timestamp(epochTime));
  }
}
